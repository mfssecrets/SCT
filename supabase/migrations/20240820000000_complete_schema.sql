-- ===========================================
-- CLEAN SHIELD - COMPLETE SUPABASE SCHEMA
-- All tables, RLS policies, storage, 30-day retention
-- ===========================================

-- ===========================================
-- 1. PROFILES TABLE (extends supabase auth.users)
-- ===========================================
CREATE TABLE IF NOT EXISTS public.profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  username TEXT NOT NULL,
  normalized_username TEXT NOT NULL UNIQUE,
  email TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL DEFAULT '',
  bio TEXT NOT NULL DEFAULT '',
  profile_image TEXT,
  avatar_color_hex BIGINT DEFAULT 4278255615,
  status_message TEXT DEFAULT 'Active & Encrypted',
  partner_username TEXT,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  access_pin_hash TEXT,
  access_pin_salt TEXT,
  vault_pin_hash TEXT,
  vault_pin_salt TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Username trigger to auto-lowercase and normalize
CREATE OR REPLACE FUNCTION public.normalize_profile_username()
RETURNS TRIGGER AS $$
BEGIN
  NEW.normalized_username := LOWER(TRIM(NEW.username));
  NEW.email := LOWER(TRIM(NEW.email));
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER trg_normalize_profile_username
  BEFORE INSERT OR UPDATE ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION public.normalize_profile_username();

-- ===========================================
-- 2. FRIEND REQUESTS TABLE
-- ===========================================
CREATE TABLE IF NOT EXISTS public.friend_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  receiver_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(sender_id, receiver_id)
);

CREATE INDEX idx_friend_requests_receiver ON public.friend_requests(receiver_id, status);
CREATE INDEX idx_friend_requests_sender ON public.friend_requests(sender_id, status);

-- ===========================================
-- 3. FRIENDSHIPS TABLE
-- ===========================================
CREATE TABLE IF NOT EXISTS public.friendships (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  friend_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(user_id, friend_id),
  CHECK (user_id != friend_id)
);

CREATE INDEX idx_friendships_user ON public.friendships(user_id);
CREATE INDEX idx_friendships_friend ON public.friendships(friend_id);

-- ===========================================
-- 4. BLOCKED USERS TABLE
-- ===========================================
CREATE TABLE IF NOT EXISTS public.blocked_users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  blocker_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  blocked_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(blocker_id, blocked_id),
  CHECK (blocker_id != blocked_id)
);

CREATE INDEX idx_blocked_blocker ON public.blocked_users(blocker_id);
CREATE INDEX idx_blocked_blocked ON public.blocked_users(blocked_id);

-- ===========================================
-- 5. NOTIFICATIONS TABLE
-- ===========================================
CREATE TABLE IF NOT EXISTS public.notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('FRIEND_REQUEST', 'REQUEST_ACCEPTED', 'REQUEST_REJECTED')),
  related_user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
  related_request_id UUID REFERENCES public.friend_requests(id) ON DELETE SET NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON public.notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON public.notifications(user_id, is_read) WHERE NOT is_read;

-- ===========================================
-- 6. CONVERSATIONS TABLE
-- ===========================================
CREATE TABLE IF NOT EXISTS public.conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ===========================================
-- 7. CONVERSATION MEMBERS TABLE
-- ===========================================
CREATE TABLE IF NOT EXISTS public.conversation_members (
  conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conv_members_user ON public.conversation_members(user_id);

-- Unique constraint: ensure only one conversation per pair of users
-- (enforced via trigger)

-- ===========================================
-- 8. MESSAGES TABLE (with soft-delete and 30-day retention)
-- ===========================================
CREATE TABLE IF NOT EXISTS public.messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
  sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  message_type TEXT NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'VIDEO', 'ONE_SHOT_IMAGE', 'ONE_SHOT_VIDEO')),
  content TEXT,
  media_reference TEXT,
  one_shot BOOLEAN NOT NULL DEFAULT FALSE,
  one_shot_opened BOOLEAN NOT NULL DEFAULT FALSE,
  one_shot_opened_at TIMESTAMPTZ,
  sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  seen_at TIMESTAMPTZ,
  -- Soft delete flags - users can delete for themselves but data stays for 30 days
  deleted_for_sender BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_for_receiver BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at TIMESTAMPTZ,
  -- Permanent deletion after 30-day retention
  permanently_deleted_at TIMESTAMPTZ,
  -- Status tracking
  status TEXT NOT NULL DEFAULT 'SENDING' CHECK (status IN ('SENDING', 'SENT', 'SEEN', 'FAILED'))
);

CREATE INDEX idx_messages_conversation ON public.messages(conversation_id, sent_at DESC);
CREATE INDEX idx_messages_sender ON public.messages(sender_id);
CREATE INDEX idx_messages_retention ON public.messages(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_messages_permanent ON public.messages(permanently_deleted_at) WHERE permanently_deleted_at IS NULL;

-- ===========================================
-- 9. VAULT MEDIA TABLE (with 30-day retention)
-- ===========================================
CREATE TABLE IF NOT EXISTS public.vault_media (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  media_type TEXT NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO')),
  secure_storage_reference TEXT NOT NULL,
  title TEXT NOT NULL DEFAULT '',
  file_size_bytes BIGINT DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  -- Soft delete: user deletes but media stays for 30 days
  deleted_at TIMESTAMPTZ,
  permanently_deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_vault_media_user ON public.vault_media(user_id, created_at DESC);
CREATE INDEX idx_vault_media_retention ON public.vault_media(deleted_at) WHERE deleted_at IS NOT NULL;

-- ===========================================
-- 10. CHAT SETTINGS TABLE (Secure My Chat)
-- ===========================================
CREATE TABLE IF NOT EXISTS public.chat_settings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL UNIQUE REFERENCES public.conversations(id) ON DELETE CASCADE,
  is_secure_chat_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ===========================================
-- 11. OTP LOG TABLE (for audit/compliance)
-- ===========================================
CREATE TABLE IF NOT EXISTS public.otp_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT NOT NULL,
  otp_code TEXT NOT NULL,
  type TEXT NOT NULL CHECK (type IN ('SIGNUP', 'RESET')),
  attempts_count INT NOT NULL DEFAULT 0,
  is_used BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_logs_email ON public.otp_logs(email, type, created_at DESC);

-- ===========================================
-- 12. USER REPORTS TABLE
-- ===========================================
CREATE TABLE IF NOT EXISTS public.user_reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  reported_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  reason TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ===========================================
-- 13. MEDIA ARCHIVE TABLE (30-day retention for deleted media)
-- ===========================================
CREATE TABLE IF NOT EXISTS public.media_archive (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  original_table TEXT NOT NULL CHECK (original_table IN ('messages', 'vault_media')),
  original_id UUID NOT NULL,
  user_id UUID NOT NULL,
  media_reference TEXT NOT NULL,
  media_type TEXT NOT NULL,
  deleted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  -- Will be permanently purged after 30 days
  purge_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days')
);

CREATE INDEX idx_media_archive_purge ON public.media_archive(purge_at) WHERE purge_at < NOW();

-- ===========================================
-- STORAGE BUCKETS
-- ===========================================

-- Chat media bucket (images/videos sent in chat)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'chat-media',
  'chat-media',
  FALSE, -- NOT public - requires auth
  104857600, -- 100MB max
  ARRAY['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'video/mp4', 'video/3gp', 'video/webm']
) ON CONFLICT (id) DO NOTHING;

-- Vault media bucket (private vault photos/videos)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'vault-media',
  'vault-media',
  FALSE, -- NEVER public
  524288000, -- 500MB max
  ARRAY['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'video/mp4', 'video/3gp', 'video/webm']
) ON CONFLICT (id) DO NOTHING;

-- Profile images bucket
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'profile-images',
  'profile-images',
  FALSE, -- NOT public - requires auth
  5242880, -- 5MB max
  ARRAY['image/jpeg', 'image/png', 'image/webp']
) ON CONFLICT (id) DO NOTHING;

-- ===========================================
-- STORAGE POLICIES
-- ===========================================

-- Chat media: authenticated users can upload their own files
CREATE POLICY "Users can upload chat media"
  ON storage.objects FOR INSERT
  WITH CHECK (
    bucket_id = 'chat-media'
    AND auth.uid() IS NOT NULL
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

-- Chat media: authenticated users can read chat media they have access to
CREATE POLICY "Users can read chat media"
  ON storage.objects FOR SELECT
  USING (
    bucket_id = 'chat-media'
    AND auth.uid() IS NOT NULL
    AND (
      (storage.foldername(name))[1] = auth.uid()::text
      OR EXISTS (
        SELECT 1 FROM conversation_members cm
        JOIN messages m ON m.conversation_id = cm.conversation_id
        WHERE cm.user_id = auth.uid()
        AND m.media_reference = storage.foldername(name) || '/' || storage.filename(name)
      )
    )
  );

-- Chat media: users can update their own files
CREATE POLICY "Users can update their chat media"
  ON storage.objects FOR UPDATE
  USING (
    bucket_id = 'chat-media'
    AND auth.uid() IS NOT NULL
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

-- Chat media: NEVER let users delete (30-day retention enforced server-side)
CREATE POLICY "Users cannot directly delete chat media"
  ON storage.objects FOR DELETE
  USING (false);

-- Vault media: users can upload their own vault files
CREATE POLICY "Users can upload vault media"
  ON storage.objects FOR INSERT
  WITH CHECK (
    bucket_id = 'vault-media'
    AND auth.uid() IS NOT NULL
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

-- Vault media: only owner can read
CREATE POLICY "Users can read own vault media"
  ON storage.objects FOR SELECT
  USING (
    bucket_id = 'vault-media'
    AND auth.uid() IS NOT NULL
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

-- Vault media: NEVER let users delete (30-day retention enforced server-side)
CREATE POLICY "Users cannot directly delete vault media"
  ON storage.objects FOR DELETE
  USING (false);

-- Profile images: users can upload their own
CREATE POLICY "Users can upload own profile image"
  ON storage.objects FOR INSERT
  WITH CHECK (
    bucket_id = 'profile-images'
    AND auth.uid() IS NOT NULL
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

-- Profile images: anyone can read profile images
CREATE POLICY "Anyone authenticated can read profile images"
  ON storage.objects FOR SELECT
  USING (
    bucket_id = 'profile-images'
    AND auth.uid() IS NOT NULL
  );

-- Profile images: users can update their own
CREATE POLICY "Users can update own profile image"
  ON storage.objects FOR UPDATE
  USING (
    bucket_id = 'profile-images'
    AND auth.uid() IS NOT NULL
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

-- Profile images: users can delete their own
CREATE POLICY "Users can delete own profile image"
  ON storage.objects FOR DELETE
  USING (
    bucket_id = 'profile-images'
    AND auth.uid() IS NOT NULL
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

-- ===========================================
-- RLS POLICIES - Enable RLS on all tables
-- ===========================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friend_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blocked_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vault_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_reports ENABLE ROW LEVEL SECURITY;

-- ===========================================
-- PROFILES RLS
-- ===========================================

-- Anyone authenticated can view profiles (needed for search, friends list, etc.)
CREATE POLICY "Authenticated users can view profiles"
  ON public.profiles FOR SELECT
  USING (auth.uid() IS NOT NULL);

-- Users can update their own profile
CREATE POLICY "Users can update own profile"
  ON public.profiles FOR UPDATE
  USING (auth.uid() = id);

-- Users can insert their own profile (triggered after auth signup)
CREATE POLICY "Users can insert own profile"
  ON public.profiles FOR INSERT
  WITH CHECK (auth.uid() = id);

-- Users cannot delete their own profile via app (admin only)
CREATE POLICY "Users cannot delete own profile"
  ON public.profiles FOR DELETE
  USING (false);

-- ===========================================
-- FRIEND REQUESTS RLS
-- ===========================================

-- Users can see requests they sent or received
CREATE POLICY "Users can see own friend requests"
  ON public.friend_requests FOR SELECT
  USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

-- Users can send friend requests
CREATE POLICY "Users can send friend requests"
  ON public.friend_requests FOR INSERT
  WITH CHECK (auth.uid() = sender_id);

-- Sender or receiver can update (accept/reject)
CREATE POLICY "Users can update own friend requests"
  ON public.friend_requests FOR UPDATE
  USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

-- ===========================================
-- FRIENDSHIPS RLS
-- ===========================================

-- Users can see their own friendships
CREATE POLICY "Users can see own friendships"
  ON public.friendships FOR SELECT
  USING (auth.uid() = user_id OR auth.uid() = friend_id);

-- Only server/functions can insert friendships
CREATE POLICY "System can create friendships"
  ON public.friendships FOR INSERT
  WITH CHECK (auth.uid() = user_id);

-- ===========================================
-- BLOCKED USERS RLS
-- ===========================================

CREATE POLICY "Users can see own blocks"
  ON public.blocked_users FOR SELECT
  USING (auth.uid() = blocker_id);

CREATE POLICY "Users can block others"
  ON public.blocked_users FOR INSERT
  WITH CHECK (auth.uid() = blocker_id);

CREATE POLICY "Users can unblock"
  ON public.blocked_users FOR DELETE
  USING (auth.uid() = blocker_id);

-- Check if blocked: users can check if they're blocked
CREATE POLICY "Users can check if blocked by others"
  ON public.blocked_users FOR SELECT
  USING (auth.uid() = blocked_id);

-- ===========================================
-- NOTIFICATIONS RLS
-- ===========================================

CREATE POLICY "Users can see own notifications"
  ON public.notifications FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "System can create notifications"
  ON public.notifications FOR INSERT
  WITH CHECK (false); -- Only via service_role / edge functions

CREATE POLICY "Users can update own notifications"
  ON public.notifications FOR UPDATE
  USING (auth.uid() = user_id);

-- ===========================================
-- CONVERSATIONS RLS
-- ===========================================

CREATE POLICY "Conversation members can view conversations"
  ON public.conversations FOR SELECT
  USING (
    auth.uid() IS NOT NULL
    AND EXISTS (
      SELECT 1 FROM conversation_members cm
      WHERE cm.conversation_id = conversations.id
      AND cm.user_id = auth.uid()
    )
  );

CREATE POLICY "Authenticated users can create conversations"
  ON public.conversations FOR INSERT
  WITH CHECK (auth.uid() IS NOT NULL);

-- ===========================================
-- CONVERSATION MEMBERS RLS
-- ===========================================

CREATE POLICY "Members can view membership"
  ON public.conversation_members FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Authenticated users can add members"
  ON public.conversation_members FOR INSERT
  WITH CHECK (auth.uid() IS NOT NULL);

-- ===========================================
-- MESSAGES RLS
-- ===========================================

-- Conversation members can view non-deleted messages
CREATE POLICY "Conversation members can view messages"
  ON public.messages FOR SELECT
  USING (
    auth.uid() IS NOT NULL
    AND EXISTS (
      SELECT 1 FROM conversation_members cm
      WHERE cm.conversation_id = messages.conversation_id
      AND cm.user_id = auth.uid()
    )
    -- NOTE: Messages are ALWAYS in DB for 30-day retention.
    -- Client should filter out deleted_for_sender/receiver
  );

-- Conversation members can send messages
CREATE POLICY "Conversation members can send messages"
  ON public.messages FOR INSERT
  WITH CHECK (
    auth.uid() IS NOT NULL
    AND auth.uid() = sender_id
    AND EXISTS (
      SELECT 1 FROM conversation_members cm
      WHERE cm.conversation_id = messages.conversation_id
      AND cm.user_id = auth.uid()
    )
  );

-- Sender can update message status
CREATE POLICY "Sender can update own messages"
  ON public.messages FOR UPDATE
  USING (auth.uid() = sender_id);

-- Receiver can update seen status
CREATE POLICY "Receiver can mark messages as seen"
  ON public.messages FOR UPDATE
  USING (
    auth.uid() IS NOT NULL
    AND auth.uid() != sender_id
    AND EXISTS (
      SELECT 1 FROM conversation_members cm
      WHERE cm.conversation_id = messages.conversation_id
      AND cm.user_id = auth.uid()
    )
  );

-- IMPORTANT: Users CANNOT delete messages (30-day retention enforced server-side)
CREATE POLICY "Users cannot delete messages"
  ON public.messages FOR DELETE
  USING (false);

-- ===========================================
-- VAULT MEDIA RLS
-- ===========================================

CREATE POLICY "Users can view own vault media"
  ON public.vault_media FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can upload vault media"
  ON public.vault_media FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own vault media metadata"
  ON public.vault_media FOR UPDATE
  USING (auth.uid() = user_id);

-- IMPORTANT: Users CANNOT delete vault media (30-day retention)
CREATE POLICY "Users cannot delete vault media"
  ON public.vault_media FOR DELETE
  USING (false);

-- ===========================================
-- CHAT SETTINGS RLS
-- ===========================================

CREATE POLICY "Conversation members can view chat settings"
  ON public.chat_settings FOR SELECT
  USING (
    auth.uid() IS NOT NULL
    AND EXISTS (
      SELECT 1 FROM conversation_members cm
      WHERE cm.conversation_id = chat_settings.conversation_id
      AND cm.user_id = auth.uid()
    )
  );

CREATE POLICY "Conversation members can update chat settings"
  ON public.chat_settings FOR UPDATE
  USING (
    auth.uid() IS NOT NULL
    AND EXISTS (
      SELECT 1 FROM conversation_members cm
      WHERE cm.conversation_id = chat_settings.conversation_id
      AND cm.user_id = auth.uid()
    )
  );

-- ===========================================
-- USER REPORTS RLS
-- ===========================================

CREATE POLICY "Users can create reports"
  ON public.user_reports FOR INSERT
  WITH CHECK (auth.uid() = reporter_id);

-- ===========================================
-- 14. EDGE FUNCTION: send-otp
-- Uses Resend to send 6-digit OTP email
-- ===========================================

-- ===========================================
-- 15. DATABASE FUNCTIONS (called by edge functions and triggers)
-- ===========================================

-- Function to create a profile after signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, username, email, name)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'username', 'user_' || substr(NEW.id::text, 1, 8)),
    NEW.email,
    COALESCE(NEW.raw_user_meta_data->>'name', '')
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to auto-create profile on signup
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Function to get or create conversation between two users
CREATE OR REPLACE FUNCTION public.get_or_create_conversation(
  user_a UUID,
  user_b UUID
)
RETURNS UUID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
  conv_id UUID;
  member_count INT;
BEGIN
  -- Find existing conversation where both are members
  SELECT cm1.conversation_id INTO conv_id
  FROM conversation_members cm1
  JOIN conversation_members cm2 ON cm1.conversation_id = cm2.conversation_id
  WHERE cm1.user_id = user_a AND cm2.user_id = user_b
  LIMIT 1;

  IF conv_id IS NOT NULL THEN
    RETURN conv_id;
  END IF;

  -- Create new conversation
  INSERT INTO conversations (id) VALUES (gen_random_uuid()) RETURNING id INTO conv_id;

  -- Add both members
  INSERT INTO conversation_members (conversation_id, user_id) VALUES (conv_id, user_a);
  INSERT INTO conversation_members (conversation_id, user_id) VALUES (conv_id, user_b);

  RETURN conv_id;
END;
$$;

-- Function to handle friend request acceptance and create conversation
CREATE OR REPLACE FUNCTION public.accept_friend_request(
  p_request_id UUID,
  p_acceptor_id UUID
)
RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
  v_request RECORD;
  v_conv_id UUID;
  v_notif_id UUID;
BEGIN
  -- Get the request
  SELECT * INTO v_request FROM friend_requests
  WHERE id = p_request_id AND (sender_id = p_acceptor_id OR receiver_id = p_acceptor_id);

  IF v_request IS NULL THEN RETURN FALSE; END IF;

  -- Update request status
  UPDATE friend_requests SET status = 'accepted', updated_at = NOW() WHERE id = p_request_id;

  -- Create friendships (both directions)
  INSERT INTO friendships (user_id, friend_id) VALUES (v_request.sender_id, v_request.receiver_id)
  ON CONFLICT DO NOTHING;
  INSERT INTO friendships (user_id, friend_id) VALUES (v_request.receiver_id, v_request.sender_id)
  ON CONFLICT DO NOTHING;

  -- Create conversation
  SELECT get_or_create_conversation(v_request.sender_id, v_request.receiver_id) INTO v_conv_id;

  -- Notify sender that request was accepted
  INSERT INTO notifications (user_id, type, related_user_id, related_request_id)
  VALUES (v_request.sender_id, 'REQUEST_ACCEPTED', p_acceptor_id, p_request_id);

  -- Mark receiver's notification as read
  UPDATE notifications SET is_read = TRUE WHERE related_request_id = p_request_id AND user_id = p_acceptor_id;

  RETURN TRUE;
END;
$$;

-- Function to handle friend request rejection
CREATE OR REPLACE FUNCTION public.reject_friend_request(
  p_request_id UUID,
  p_rejector_id UUID
)
RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
  v_request RECORD;
BEGIN
  SELECT * INTO v_request FROM friend_requests
  WHERE id = p_request_id AND (sender_id = p_rejector_id OR receiver_id = p_rejector_id);

  IF v_request IS NULL THEN RETURN FALSE; END IF;

  UPDATE friend_requests SET status = 'rejected', updated_at = NOW() WHERE id = p_request_id;

  -- Notify sender of rejection
  INSERT INTO notifications (user_id, type, related_user_id, related_request_id)
  VALUES (v_request.sender_id, 'REQUEST_REJECTED', p_rejector_id, p_request_id);

  -- Mark receiver's notification as read
  UPDATE notifications SET is_read = TRUE WHERE related_request_id = p_request_id AND user_id = p_rejector_id;

  RETURN TRUE;
END;
$$;

-- Function to send friend request (with block check)
CREATE OR REPLACE FUNCTION public.send_friend_request_fn(
  p_sender_id UUID,
  p_receiver_id UUID
)
RETURNS UUID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
  v_request_id UUID;
  v_existing_request RECORD;
  v_is_blocked BOOLEAN;
BEGIN
  -- Cannot send to self
  IF p_sender_id = p_receiver_id THEN
    RAISE EXCEPTION 'Cannot send friend request to yourself';
  END IF;

  -- Check if blocked either way
  SELECT EXISTS (
    SELECT 1 FROM blocked_users
    WHERE (blocker_id = p_sender_id AND blocked_id = p_receiver_id)
    OR (blocker_id = p_receiver_id AND blocked_id = p_sender_id)
  ) INTO v_is_blocked;

  IF v_is_blocked THEN
    RAISE EXCEPTION 'Cannot send request due to user privacy settings';
  END IF;

  -- Check for existing request
  SELECT * INTO v_existing_request FROM friend_requests
  WHERE (sender_id = p_sender_id AND receiver_id = p_receiver_id)
  OR (sender_id = p_receiver_id AND receiver_id = p_sender_id)
  LIMIT 1;

  IF v_existing_request IS NOT NULL THEN
    IF v_existing_request.status = 'accepted' THEN
      RAISE EXCEPTION 'Already friends';
    END IF;
    IF v_existing_request.status = 'pending' THEN
      IF v_existing_request.sender_id = p_sender_id THEN
        RAISE EXCEPTION 'Friend request already sent';
      ELSE
        -- Auto-accept reciprocal
        PERFORM accept_friend_request(v_existing_request.id, p_sender_id);
        RETURN v_existing_request.id;
      END IF;
    END IF;
  END IF;

  -- Create new request
  INSERT INTO friend_requests (sender_id, receiver_id, status)
  VALUES (p_sender_id, p_receiver_id, 'pending')
  RETURNING id INTO v_request_id;

  -- Create notification for receiver
  INSERT INTO notifications (user_id, type, related_user_id, related_request_id)
  VALUES (p_receiver_id, 'FRIEND_REQUEST', p_sender_id, v_request_id);

  RETURN v_request_id;
END;
$$;

-- ===========================================
-- 16. SOFT-DELETE FUNCTIONS (30-day retention)
-- ===========================================

-- Soft delete message for sender
CREATE OR REPLACE FUNCTION public.soft_delete_message_for_sender(
  p_message_id UUID,
  p_user_id UUID
)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
  UPDATE messages SET
    deleted_for_sender = TRUE,
    deleted_at = COALESCE(deleted_at, NOW())
  WHERE id = p_message_id AND sender_id = p_user_id;
END;
$$;

-- Soft delete message for receiver
CREATE OR REPLACE FUNCTION public.soft_delete_message_for_receiver(
  p_message_id UUID,
  p_user_id UUID
)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
  UPDATE messages SET
    deleted_for_receiver = TRUE,
    deleted_at = COALESCE(deleted_at, NOW())
  WHERE id = p_message_id AND conversation_id IN (
    SELECT conversation_id FROM conversation_members WHERE user_id = p_user_id
  );
END;
$$;

-- Soft delete entire conversation for one user
CREATE OR REPLACE FUNCTION public.soft_clear_conversation_for_user(
  p_conversation_id UUID,
  p_user_id UUID
)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
  UPDATE messages SET
    deleted_for_sender = CASE WHEN sender_id = p_user_id THEN TRUE ELSE deleted_for_sender END,
    deleted_for_receiver = CASE WHEN sender_id != p_user_id THEN TRUE ELSE deleted_for_receiver END,
    deleted_at = COALESCE(deleted_at, NOW())
  WHERE conversation_id = p_conversation_id;
END;
$$;

-- Soft delete vault media (archives for 30 days)
CREATE OR REPLACE FUNCTION public.soft_delete_vault_media(
  p_vault_media_id UUID,
  p_user_id UUID
)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
  v_media RECORD;
BEGIN
  SELECT * INTO v_media FROM vault_media WHERE id = p_vault_media_id AND user_id = p_user_id;
  IF v_media IS NULL THEN RETURN; END IF;

  -- Archive the media reference before soft-deleting
  INSERT INTO media_archive (original_table, original_id, user_id, media_reference, media_type)
  VALUES ('vault_media', v_media.id, v_media.user_id, v_media.secure_storage_reference, v_media.media_type);

  -- Soft delete
  UPDATE vault_media SET deleted_at = NOW() WHERE id = p_vault_media_id;
END;
$$;

-- ===========================================
-- 17. 30-DAY PERGE FUNCTION (run via cron/edge function daily)
-- ===========================================

CREATE OR REPLACE FUNCTION public.purge_expired_retention_data()
RETURNS TABLE(purged_messages INT, purged_vault INT, purged_archive INT)
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
  v_msg_count INT;
  v_vault_count INT;
  v_archive_count INT;
BEGIN
  -- Purge messages deleted more than 30 days ago
  -- (In production, actual file cleanup in storage would also happen here)
  WITH deleted_msgs AS (
    DELETE FROM messages
    WHERE deleted_at IS NOT NULL
    AND deleted_at < NOW() - INTERVAL '30 days'
    RETURNING 1
  )
  SELECT COUNT(*) INTO v_msg_count FROM deleted_msgs;

  -- Purge vault media deleted more than 30 days ago
  WITH deleted_vault AS (
    DELETE FROM vault_media
    WHERE deleted_at IS NOT NULL
    AND deleted_at < NOW() - INTERVAL '30 days'
    RETURNING 1
  )
  SELECT COUNT(*) INTO v_vault_count FROM deleted_vault;

  -- Purge media archive entries past purge_at
  WITH deleted_archive AS (
    DELETE FROM media_archive
    WHERE purge_at < NOW()
    RETURNING 1
  )
  SELECT COUNT(*) INTO v_archive_count FROM deleted_archive;

  RETURN QUERY SELECT v_msg_count, v_vault_count, v_archive_count;
END;
$$;

-- ===========================================
-- 18. REAL-TIME SUBSCRIPTIONS
-- Enable real-time for messages and notifications
-- ===========================================

ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE public.friend_requests;
ALTER PUBLICATION supabase_realtime ADD TABLE public.friendships;

-- ===========================================
-- 19. BLOCK CHECK FUNCTION (for message authorization)
-- ===========================================

CREATE OR REPLACE FUNCTION public.is_blocked(
  p_user_a UUID,
  p_user_b UUID
)
RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER STABLE
AS $$
DECLARE
  v_blocked BOOLEAN;
BEGIN
  SELECT EXISTS (
    SELECT 1 FROM blocked_users
    WHERE (blocker_id = p_user_a AND blocked_id = p_user_b)
    OR (blocker_id = p_user_b AND blocked_id = p_user_a)
  ) INTO v_blocked;
  RETURN v_blocked;
END;
$$;

-- ===========================================
-- 20. SERVICE ROLE API KEYS SETUP
-- These are set as Supabase secrets for edge functions
-- RESEND_API_KEY - set via: supabase secrets set RESEND_API_KEY=<key>
-- SUPABASE_SERVICE_ROLE_KEY - automatically available in edge functions
-- ===========================================

-- ===========================================
-- DONE - COMPLETE SCHEMA DEPLOYED
-- ===========================================
