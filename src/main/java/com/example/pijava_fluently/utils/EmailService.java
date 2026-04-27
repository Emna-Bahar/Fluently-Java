package com.example.pijava_fluently.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // ── Configure these with your Gmail ──────────────────────────────────────
    private static final String FROM_EMAIL    = "azeraissaoui123@gmail.com";
    private static final String FROM_PASSWORD = "hezpnncgljpdgyhq";
    // ─────────────────────────────────────────────────────────────────────────

    public static void sendResetCode(String toEmail, String code) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.protocols",   "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, "Fluently"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject("🔐 Code de réinitialisation Fluently");

        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:auto;
                        background:#f9f9f9;border-radius:12px;overflow:hidden;border:1px solid #eee;">
              <div style="background:#6c63ff;padding:28px 32px;">
                <h2 style="color:white;margin:0;font-size:22px;">🎓 Fluently</h2>
                <p style="color:rgba(255,255,255,0.85);margin:6px 0 0;">Réinitialisation de mot de passe</p>
              </div>
              <div style="padding:32px;">
                <p style="color:#333;font-size:15px;">Bonjour,</p>
                <p style="color:#555;font-size:14px;">
                  Voici ton code de vérification pour réinitialiser ton mot de passe&nbsp;:
                </p>
                <div style="text-align:center;margin:28px 0;">
                  <span style="font-size:38px;font-weight:bold;letter-spacing:10px;
                               color:#6c63ff;background:#f0eeff;padding:14px 28px;
                               border-radius:10px;display:inline-block;">%s</span>
                </div>
                <p style="color:#888;font-size:13px;">Ce code expire dans <strong>10 minutes</strong>.</p>
                <p style="color:#aaa;font-size:12px;margin-top:24px;">
                  Si tu n'as pas demandé cette réinitialisation, ignore cet email.
                </p>
              </div>
            </div>
            """.formatted(code);

        msg.setContent(html, "text/html; charset=UTF-8");
        Transport.send(msg);
    }
}