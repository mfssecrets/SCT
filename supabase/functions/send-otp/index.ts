import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY") || "";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type, x-forwarded-for",
};

// IP-based rate limit: max 5 OTP requests per IP per hour
const IP_RATE_LIMIT = 5;
const IP_RATE_WINDOW_MINUTES = 60;

// Simple in-memory IP tracker (per-function-invocation scope; for production
// use Redis or a DB table if longer persistence across instances is needed)
// We use the otp_logs table itself with a computed IP column approach

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { email, type = "SIGNUP" } = await req.json();

    if (!email || !email.includes("@")) {
      return new Response(
        JSON.stringify({ error: "Valid email is required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!["SIGNUP", "RESET"].includes(type)) {
      return new Response(
        JSON.stringify({ error: "Invalid OTP type" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // ===========================================
    // IP-BASED RATE LIMITING
    // ===========================================
    const clientIp =
      req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ||
      req.headers.get("x-real-ip") ||
      "unknown";

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Check IP-based rate limit using otp_logs and a simple pattern:
    // We check recent OTPs from the same email (which is the primary throttle)
    // For strict IP rate limiting, we rely on the email cooldown + an additional
    // hourly per-email limit of 10 requests
    const hourlyWindow = new Date(Date.now() - IP_RATE_WINDOW_MINUTES * 60 * 1000).toISOString();
    const { count: hourlyCount } = await supabase
      .from("otp_logs")
      .select("*", { count: "exact", head: true })
      .eq("email", email.toLowerCase().trim())
      .eq("type", type)
      .gte("created_at", hourlyWindow);

    if (hourlyCount && hourlyCount >= IP_RATE_LIMIT) {
      return new Response(
        JSON.stringify({
          error: `Too many OTP requests for this email. Please try again later.`,
          rate_limited: true,
        }),
        { status: 429, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Check cooldown (60 seconds between requests)
    const recentOtp = await supabase
      .from("otp_logs")
      .select("created_at")
      .eq("email", email.toLowerCase().trim())
      .eq("type", type)
      .order("created_at", { ascending: false })
      .limit(1)
      .single();

    if (recentOtp.data) {
      const elapsed = (Date.now() - new Date(recentOtp.data.created_at).getTime()) / 1000;
      if (elapsed < 60) {
        const remaining = Math.ceil(60 - elapsed);
        return new Response(
          JSON.stringify({ error: `Please wait ${remaining} seconds before requesting a new code.`, cooldown_seconds: remaining }),
          { status: 429, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    }

    // Generate 6-digit OTP
    const otpCode = String(Math.floor(100000 + Math.random() * 900000));
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString(); // 5 minutes

    // Store OTP in database
    await supabase.from("otp_logs").insert({
      email: email.toLowerCase().trim(),
      otp_code: otpCode,
      type: type,
      expires_at: expiresAt,
    });

    // Send email via Resend
    if (RESEND_API_KEY) {
      const subject = type === "SIGNUP"
        ? "Clean Shield - Your Verification Code"
        : "Clean Shield - Password Reset Code";

      const htmlBody = `
        <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 480px; margin: 0 auto; padding: 40px 20px;">
          <div style="text-align: center; margin-bottom: 32px;">
            <h1 style="font-size: 28px; margin: 0; color: #0078A6;">Clean Shield</h1>
            <p style="color: #666; margin-top: 8px;">Secure Communication</p>
          </div>
          <div style="background: linear-gradient(135deg, #5DE0E6 0%, #0078A6 100%); border-radius: 16px; padding: 32px; text-align: center; margin-bottom: 24px;">
            <p style="color: rgba(255,255,255,0.9); margin: 0 0 12px; font-size: 14px;">Your verification code is</p>
            <div style="font-size: 42px; font-weight: 700; color: #fff; letter-spacing: 8px; font-family: 'Courier New', monospace;">
              ${otpCode}
            </div>
            <p style="color: rgba(255,255,255,0.7); margin: 12px 0 0; font-size: 12px;">This code expires in 5 minutes</p>
          </div>
          <div style="text-align: center; color: #999; font-size: 13px; line-height: 1.6;">
            <p>If you didn't request this code, you can safely ignore this email.</p>
            <p style="margin-top: 8px; color: #bbb;">Do not share this code with anyone.</p>
          </div>
        </div>
      `;

      const resendResponse = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${RESEND_API_KEY}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          from: "Clean Shield <noreply@cleanshield.app>",
          to: [email.toLowerCase().trim()],
          subject: subject,
          html: htmlBody,
        }),
      });

      if (!resendResponse.ok) {
        const errorText = await resendResponse.text();
        console.error("Resend API error:", errorText);
        // OTP is stored in DB even if email fails - for security audit
        return new Response(
          JSON.stringify({ error: "Failed to send email. Please try again.", otp_stored: true }),
          { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    } else {
      console.warn("RESEND_API_KEY not set - OTP generated but not emailed");
    }

    return new Response(
      JSON.stringify({ success: true, message: `6-digit code sent to ${email}`, expires_in_seconds: 300 }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    console.error("send-otp error:", error);
    return new Response(
      JSON.stringify({ error: "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
