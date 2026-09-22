package com.multitenanterp.payroll;

import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpPayslipEmailSender implements PayslipEmailSender {
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;

    public SmtpPayslipEmailSender(ObjectProvider<JavaMailSender> senders,
            @Value("${app.payslip-email.enabled:false}") boolean enabled,
            @Value("${app.payslip-email.from:}") String from,
            @Value("${spring.mail.host:}") String host) {
        this.sender = senders.getIfAvailable();
        this.enabled = enabled;
        this.from = from;
        if (enabled && (sender == null || host.isBlank() || !validAddress(from))) {
            throw new IllegalStateException("Payslip email requires SMTP configuration and a valid sender address");
        }
    }

    public boolean enabled() { return enabled; }

    static boolean validAddress(String value) {
        if (value == null || value.isBlank() || value.contains("\r") || value.contains("\n")) return false;
        try {
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
            return address.getPersonal() == null && value.equals(address.getAddress()) && value.contains("@");
        } catch (Exception e) { return false; }
    }

    public void send(String recipient, String company, int year, int month, String filename, byte[] pdf) throws Exception {
        if (!enabled) throw new IllegalStateException("Payslip email is disabled");
        var message = sender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(recipient);
        helper.setSubject(company.replaceAll("[\\r\\n]", " ") + " — Payslip " + year + "-" + String.format("%02d", month));
        helper.setText("Your released payslip is attached. You can also view and acknowledge receipt in the employee portal.\n\n" + company);
        helper.addAttachment(filename, new ByteArrayResource(pdf), "application/pdf");
        sender.send(message);
    }
}
