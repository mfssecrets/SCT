import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// This function runs daily to purge data past 30-day retention
// Call via Supabase cron or external scheduler
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader || !authHeader.includes(Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "")) {
      return new Response(
        JSON.stringify({ error: "Unauthorized" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    const cutoff = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString();
    let purgedMessages = 0;
    let purgedVault = 0;
    let purgedArchive = 0;

    // 1. Get media references from messages being purged (for storage cleanup)
    const { data: expiringMessages } = await supabase
      .from("messages")
      .select("id, media_reference")
      .not("deleted_at", "is", null)
      .lt("deleted_at", cutoff);

    if (expiringMessages && expiringMessages.length > 0) {
      // Collect storage paths to delete
      const mediaPaths = expiringMessages
        .filter((m) => m.media_reference)
        .map((m) => m.media_reference);

      // Delete from storage
      for (const path of mediaPaths) {
        try {
          await supabase.storage.from("chat-media").remove([path]);
        } catch (e) {
          console.error(`Failed to delete storage object: ${path}`, e);
        }
      }

      // Delete from DB
      const { count } = await supabase
        .from("messages")
        .delete({ count: "exact" })
        .not("deleted_at", "is", null)
        .lt("deleted_at", cutoff);

      purgedMessages = count || 0;
    }

    // 2. Purge vault media past 30 days
    const { data: expiringVault } = await supabase
      .from("vault_media")
      .select("id, secure_storage_reference")
      .not("deleted_at", "is", null)
      .lt("deleted_at", cutoff);

    if (expiringVault && expiringVault.length > 0) {
      const vaultPaths = expiringVault
        .filter((v) => v.secure_storage_reference)
        .map((v) => v.secure_storage_reference);

      for (const path of vaultPaths) {
        try {
          await supabase.storage.from("vault-media").remove([path]);
        } catch (e) {
          console.error(`Failed to delete vault storage: ${path}`, e);
        }
      }

      const { count } = await supabase
        .from("vault_media")
        .delete({ count: "exact" })
        .not("deleted_at", "is", null)
        .lt("deleted_at", cutoff);

      purgedVault = count || 0;
    }

    // 3. Purge media archive entries past purge_at
    const { count: archiveCount } = await supabase
      .from("media_archive")
      .delete({ count: "exact" })
      .lt("purge_at", new Date().toISOString());

    purgedArchive = archiveCount || 0;

    return new Response(
      JSON.stringify({
        success: true,
        purged_messages: purgedMessages,
        purged_vault_media: purgedVault,
        purged_archive_entries: purgedArchive,
        executed_at: new Date().toISOString(),
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    console.error("daily-purge error:", error);
    return new Response(
      JSON.stringify({ error: "Internal server error", details: String(error) }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
