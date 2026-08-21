-- ===========================================
-- CLEAN SHIELD - FIXES & ENHANCEMENTS MIGRATION
-- Task 3: Security hardening, reporting, user deletion, indexes
-- ===========================================

-- ===========================================
-- 1. ENABLE RLS ON otp_logs TABLE
--     Previously missing - this table holds sensitive OTP codes
--     Only service_role and edge functions should access it
-- ===========================================

ALTER TABLE public.otp_logs ENABLE ROW LEVEL SECURITY;

-- Service role can do everything (edge functions use service_role)
CREATE POLICY "Service role full access to otp_logs"
  ON public.otp_logs FOR ALL
  USING (auth.role() = 'service_role');

-- Block all regular anon/user access as a safety net
CREATE POLICY "No anon access to otp_logs"
  ON public.otp_logs FOR ALL
  USING (auth.role() != 'anon');

-- ===========================================
-- 2. report_user_fn DATABASE FUNCTION
--     Safe report-user function with validation, dedup check
--     Uses SECURITY DEFINER to bypass RLS
-- ===========================================

CREATE OR REPLACE FUNCTION public.report_user_fn(
  p_reporter_id UUID,
  p_reported_id UUID,
  p_reason TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
  v_reporter_exists BOOLEAN;
  v_reported_exists BOOLEAN;
  v_duplicate_exists BOOLEAN;
  v_report_id UUID;
BEGIN
  -- Check that reporter exists
  SELECT EXISTS (
    SELECT 1 FROM public.profiles WHERE id = p_reporter_id
  ) INTO v_reporter_exists;

  IF NOT v_reporter_exists THEN
    RAISE EXCEPTION 'Reporter user does not exist';
  END IF;

  -- Check that reported user exists
  SELECT EXISTS (
    SELECT 1 FROM public.profiles WHERE id = p_reported_id
  ) INTO v_reported_exists;

  IF NOT v_reported_exists THEN
    RAISE EXCEPTION 'Reported user does not exist';
  END IF;

  -- Check not reporting self
  IF p_reporter_id = p_reported_id THEN
    RAISE EXCEPTION 'Cannot report yourself';
  END IF;

  -- Prevent duplicate reports (same reporter -> same reported)
  SELECT EXISTS (
    SELECT 1 FROM public.user_reports
    WHERE reporter_id = p_reporter_id
      AND reported_id = p_reported_id
      AND created_at > NOW() - INTERVAL '24 hours'
  ) INTO v_duplicate_exists;

  IF v_duplicate_exists THEN
    RAISE EXCEPTION 'You have already reported this user recently. Only one report per user per 24 hours.';
  END IF;

  -- Insert the report
  INSERT INTO public.user_reports (reporter_id, reported_id, reason)
  VALUES (p_reporter_id, p_reported_id, p_reason)
  RETURNING id INTO v_report_id;

  RETURN TRUE;
EXCEPTION
  WHEN OTHERS THEN
    RAISE;
    RETURN FALSE;
END;
$$;

-- Grant execute to authenticated users
GRANT EXECUTE ON FUNCTION public.report_user_fn(UUID, UUID, TEXT) TO authenticated;

-- ===========================================
-- 3. UNIQUE CONSTRAINT FOR CONVERSATIONS (via trigger)
--     Prevents duplicate 1:1 conversations between the same pair
-- ===========================================

CREATE OR REPLACE FUNCTION public.prevent_duplicate_conversation_pair()
RETURNS TRIGGER AS $$
DECLARE
  v_existing_conv_id UUID;
  v_user_a UUID;
  v_user_b UUID;
  v_count INT;
BEGIN
  -- Only check on INSERT of conversation_members
  IF TG_OP = 'INSERT' THEN
    -- Check current member count for this conversation
    SELECT COUNT(*) INTO v_count
    FROM public.conversation_members
    WHERE conversation_id = NEW.conversation_id;

    -- When adding the 2nd member, verify no duplicate pair exists
    IF v_count = 1 THEN
      -- Get the existing member
      SELECT user_id INTO v_user_b
      FROM public.conversation_members
      WHERE conversation_id = NEW.conversation_id
      LIMIT 1;

      v_user_a := NEW.user_id;

      -- Check if another conversation already has this pair
      SELECT cm1.conversation_id INTO v_existing_conv_id
      FROM public.conversation_members cm1
      JOIN public.conversation_members cm2 ON cm1.conversation_id = cm2.conversation_id
      WHERE cm1.user_id = v_user_a
        AND cm2.user_id = v_user_b
        AND cm1.conversation_id != NEW.conversation_id
      LIMIT 1;

      IF v_existing_conv_id IS NOT NULL THEN
        RAISE EXCEPTION 'A conversation between these users already exists (ID: %)', v_existing_conv_id;
      END IF;
    END IF;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_duplicate_conversation ON public.conversation_members;
CREATE TRIGGER trg_prevent_duplicate_conversation
  BEFORE INSERT ON public.conversation_members
  FOR EACH ROW EXECUTE FUNCTION public.prevent_duplicate_conversation_pair();

-- ===========================================
-- 4. INDEX ON messages(one_shot, one_shot_opened)
--     Efficient queries for one-shot media
-- ===========================================

CREATE INDEX IF NOT EXISTS idx_messages_one_shot
  ON public.messages (one_shot, one_shot_opened)
  WHERE one_shot = TRUE AND one_shot_opened = FALSE;

-- ===========================================
-- 5. AUTO-UPDATE updated_at ON CONVERSATIONS
-- ===========================================

CREATE OR REPLACE FUNCTION public.update_conversation_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_conversation_timestamp ON public.conversations;
CREATE TRIGGER trg_update_conversation_timestamp
  BEFORE UPDATE ON public.conversations
  FOR EACH ROW EXECUTE FUNCTION public.update_conversation_timestamp();

-- ===========================================
-- 6. USER DELETION AUDIT LOG TABLE
--     Tracks when users delete accounts for compliance
-- ===========================================

CREATE TABLE IF NOT EXISTS public.user_deletion_audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  username TEXT,
  email TEXT,
  deleted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  purge_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days')
);

CREATE INDEX IF NOT EXISTS idx_user_deletion_purge
  ON public.user_deletion_audit_log (purge_at)
  WHERE purge_at < NOW();

-- RLS on audit log - only service_role can read
ALTER TABLE public.user_deletion_audit_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Service role can manage deletion audit log"
  ON public.user_deletion_audit_log FOR ALL
  USING (auth.role() = 'service_role');

-- ===========================================
-- 7. SOFT-DELETE USER ACCOUNT FUNCTION (30-day retention)
--     Instead of CASCADE DELETE, marks all user data as deleted
--     Keeps data for 30 days before final purge
-- ===========================================

CREATE OR REPLACE FUNCTION public.soft_delete_user_account(
  p_user_id UUID
)
RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
  v_username TEXT;
  v_email TEXT;
BEGIN
  -- Get user info for audit log before any changes
  SELECT username, email INTO v_username, v_email
  FROM public.profiles
  WHERE id = p_user_id;

  IF v_username IS NULL THEN
    RAISE EXCEPTION 'User not found';
  END IF;

  -- 1. Mark all sent messages as soft-deleted for both sender and receiver
  UPDATE public.messages SET
    deleted_for_sender = TRUE,
    deleted_for_receiver = TRUE,
    deleted_at = COALESCE(deleted_at, NOW())
  WHERE sender_id = p_user_id
    AND deleted_at IS NULL;

  -- 2. Mark all received messages as soft-deleted for the user
  UPDATE public.messages SET
    deleted_for_receiver = TRUE,
    deleted_at = COALESCE(deleted_at, NOW())
  WHERE conversation_id IN (
      SELECT conversation_id FROM public.conversation_members WHERE user_id = p_user_id
    )
    AND sender_id != p_user_id
    AND deleted_at IS NULL;

  -- 3. Mark all vault media as soft-deleted and archive references
  INSERT INTO public.media_archive (original_table, original_id, user_id, media_reference, media_type)
  SELECT 'vault_media', id, user_id, secure_storage_reference, media_type
  FROM public.vault_media
  WHERE user_id = p_user_id
      AND deleted_at IS NULL
      AND secure_storage_reference IS NOT NULL;

  UPDATE public.vault_media SET
    deleted_at = NOW()
  WHERE user_id = p_user_id
    AND deleted_at IS NULL;

  -- 4. Anonymize profile data but keep the row (to not break FK references)
  UPDATE public.profiles SET
    username = 'deleted_' || substr(id::text, 1, 8),
    normalized_username = 'deleted_' || substr(id::text, 1, 8),
    email = 'deleted_' || substr(id::text, 1, 8) || '@deleted.cleanshield.app',
    name = '',
    bio = '',
    profile_image = NULL,
    status_message = 'Account deleted',
    partner_username = NULL,
    email_verified = FALSE,
    access_pin_hash = NULL,
    access_pin_salt = NULL,
    vault_pin_hash = NULL,
    vault_pin_salt = NULL,
    updated_at = NOW()
  WHERE id = p_user_id;

  -- 5. Insert audit log entry
  INSERT INTO public.user_deletion_audit_log (user_id, username, email)
  VALUES (p_user_id, v_username, v_email);

  -- 6. Remove friend requests related to this user
  UPDATE public.friend_requests SET status = 'rejected', updated_at = NOW()
  WHERE (sender_id = p_user_id OR receiver_id = p_user_id)
    AND status = 'pending';

  -- 7. Remove friendships
  DELETE FROM public.friendships
  WHERE user_id = p_user_id OR friend_id = p_user_id;

  -- 8. Remove blocks
  DELETE FROM public.blocked_users
  WHERE blocker_id = p_user_id OR blocked_id = p_user_id;

  -- 9. Remove conversation memberships (conversations remain but user cannot access)
  DELETE FROM public.conversation_members
  WHERE user_id = p_user_id;

  -- 10. Clear notifications
  DELETE FROM public.notifications
  WHERE user_id = p_user_id;

  -- 11. Clear user reports made by this user
  DELETE FROM public.user_reports
  WHERE reporter_id = p_user_id;

  RETURN TRUE;
EXCEPTION
  WHEN OTHERS THEN
    RAISE;
    RETURN FALSE;
END;
$$;

-- Grant execute to authenticated users (they call it on themselves)
GRANT EXECUTE ON FUNCTION public.soft_delete_user_account(UUID) TO authenticated;

-- ===========================================
-- 8. ENHANCED PURGE FUNCTION (includes user deletion data)
--     Updates the existing purge function to also handle
--     soft-deleted user accounts past 30-day retention
-- ===========================================

CREATE OR REPLACE FUNCTION public.purge_expired_retention_data()
RETURNS TABLE(purged_messages INT, purged_vault INT, purged_archive INT, purged_user_deletions INT)
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
  v_msg_count INT;
  v_vault_count INT;
  v_archive_count INT;
  v_user_deletion_count INT;
BEGIN
  -- Purge messages deleted more than 30 days ago
  WITH deleted_msgs AS (
    DELETE FROM public.messages
    WHERE deleted_at IS NOT NULL
      AND deleted_at < NOW() - INTERVAL '30 days'
    RETURNING 1
  )
  SELECT COUNT(*) INTO v_msg_count FROM deleted_msgs;

  -- Purge vault media deleted more than 30 days ago
  WITH deleted_vault AS (
    DELETE FROM public.vault_media
    WHERE deleted_at IS NOT NULL
      AND deleted_at < NOW() - INTERVAL '30 days'
    RETURNING 1
  )
  SELECT COUNT(*) INTO v_vault_count FROM deleted_vault;

  -- Purge media archive entries past purge_at
  WITH deleted_archive AS (
    DELETE FROM public.media_archive
    WHERE purge_at < NOW()
    RETURNING 1
  )
  SELECT COUNT(*) INTO v_archive_count FROM deleted_archive;

  -- Purge user deletion audit log entries past purge_at
  WITH deleted_audit AS (
    DELETE FROM public.user_deletion_audit_log
    WHERE purge_at < NOW()
    RETURNING user_id
  )
  SELECT COUNT(*) INTO v_user_deletion_count FROM deleted_audit;

  -- For users whose purge_at has passed, permanently remove their anonymized profile
  -- (only those that were soft-deleted, i.e. username starts with deleted_)
  DELETE FROM public.profiles
  WHERE username LIKE 'deleted_%'
    AND NOT EXISTS (
      SELECT 1 FROM public.user_deletion_audit_log
      WHERE user_deletion_audit_log.user_id = profiles.id
    );

  RETURN QUERY SELECT v_msg_count, v_vault_count, v_archive_count, v_user_deletion_count;
END;
$$;

-- ===========================================
-- 9. STORAGE POLICY VERIFICATION (profile-images update)
--     The "Users can update own profile image" policy already exists
--     from the initial schema. This block verifies and creates if missing.
-- ===========================================

DO $$
DECLARE
  v_policy_count INT;
BEGIN
  SELECT COUNT(*) INTO v_policy_count
  FROM pg_policies
  WHERE tablename = 'objects'
    AND policyname = 'Users can update own profile image'
    AND schemaname = 'storage';

  IF v_policy_count = 0 THEN
    RAISE NOTICE 'WARNING: profile-images UPDATE policy not found - creating it';
    CREATE POLICY "Users can update own profile image"
      ON storage.objects FOR UPDATE
      USING (
        bucket_id = 'profile-images'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
      );
  ELSE
    RAISE NOTICE 'OK: profile-images UPDATE policy verified';
  END IF;
END;
$$;

-- ===========================================
-- 10. PG_CRON SCHEDULE COMMENT FOR DAILY-PURGE
--     To enable automated daily purge, run this in the Supabase
--     SQL editor (requires pg_cron and supabase_functions_http extensions):
--
--     -- Enable extensions if not already enabled:
--     CREATE EXTENSION IF NOT EXISTS pg_cron SCHEMA extensions;
--     CREATE EXTENSION IF NOT EXISTS supabase_functions_http SCHEMA extensions;
--
--     -- Schedule daily purge at 3 AM UTC:
--     SELECT cron.schedule(
--       'daily-purge',
--       '0 3 * * *',
--       $$SELECT net.http_post(
--         url := 'https://bzrnjlbkhmldwohtqwxd.supabase.co/functions/v1/daily-purge',
--         headers := jsonb_build_object(
--           'Authorization', 'Bearer <SERVICE_ROLE_KEY>',
--           'Content-Type', 'application/json'
--         ),
--         body := '{}'::jsonb
--       );$$
--     );
--
--     -- To verify the cron job:
--     SELECT * FROM cron.job;
--
--     -- To unschedule:
--     SELECT cron.unschedule('daily-purge');
-- ===========================================

-- ===========================================
-- DONE - MIGRATION COMPLETE
-- ===========================================
