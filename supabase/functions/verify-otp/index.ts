import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { email, otp_code, type = "SIGNUP" } = await req.json();

    if (!email || !otp_code || otp_code.length !== 6 || !/^\d{6}$/.test(otp_code)) {
      return new Response(
        JSON.stringify({ error: "Please enter a valid 6-digit verification code." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    const cleanEmail = email.toLowerCase().trim();

    // Get latest active OTP
    const { data: activeOtp, error: otpError } = await supabase
      .from("otp_logs")
      .select("*")
      .eq("email", cleanEmail)
      .eq("type", type)
      .eq("is_used", false)
      .order("created_at", { ascending: false })
      .limit(1)
      .single();

    if (otpError || !activeOtp) {
      return new Response(
        JSON.stringify({ error: "No active verification code found. Please request a new code." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Check expiration
    if (new Date(activeOtp.expires_at) < new Date()) {
      return new Response(
        JSON.stringify({ error: "Verification code has expired. Please request a new code." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Check attempts
    if (activeOtp.attempts_count >= 5) {
      return new Response(
        JSON.stringify({ error: "Too many incorrect attempts. Please request a new code." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Increment attempts
    await supabase
      .from("otp_logs")
      .update({ attempts_count: activeOtp.attempts_count + 1 })
      .eq("id", activeOtp.id);

    // Verify code
    if (activeOtp.otp_code !== otp_code) {
      const remaining = 4 - activeOtp.attempts_count;
      return new Response(
        JSON.stringify({
          error: remaining > 0
            ? `Incorrect verification code. ${remaining} attempts remaining.`
            : "Incorrect code. Please request a new code.",
        }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Mark OTP as used
    await supabase
      .from("otp_logs")
      .update({ is_used: true })
      .eq("id", activeOtp.id);

    // For SIGNUP: mark profile email_verified
    if (type === "SIGNUP") {
      await supabase
        .from("profiles")
        .update({ email_verified: true, updated_at: new Date().toISOString() })
        .eq("email", cleanEmail);
    }

    return new Response(
      JSON.stringify({ success: true, message: "Code verified successfully." }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    console.error("verify-otp error:", error);
    return new Response(
      JSON.stringify({ error: "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
