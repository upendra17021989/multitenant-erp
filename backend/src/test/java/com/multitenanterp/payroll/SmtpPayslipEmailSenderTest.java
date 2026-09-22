package com.multitenanterp.payroll;

import jakarta.mail.Session;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpPayslipEmailSenderTest {
    @Test void buildsCompanyEmailWithOneRecipientAndPdfAttachment() throws Exception {
        JavaMailSender mail=mock(JavaMailSender.class);
        MimeMessage message=new MimeMessage(Session.getInstance(new Properties()));
        when(mail.createMimeMessage()).thenReturn(message);
        var beans=new StaticListableBeanFactory(); beans.addBean("mail",mail);
        var sender=new SmtpPayslipEmailSender(beans.getBeanProvider(JavaMailSender.class),true,"payroll@example.com","smtp.example.com");
        byte[] pdf="%PDF attachment".getBytes();
        sender.send("employee@example.com","Acme",2026,9,"payslip.pdf",pdf);
        assertThat(message.getAllRecipients()).hasSize(1);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("employee@example.com");
        assertThat(message.getSubject()).contains("Acme", "2026-09");
        Multipart parts=(Multipart)message.getContent();
        var attachment=parts.getBodyPart(1);
        assertThat(attachment.getFileName()).isEqualTo("payslip.pdf");
        assertThat(attachment.getInputStream().readAllBytes()).isEqualTo(pdf);
        verify(mail).send(message);
    }
    @Test void disabledSenderNeedsNoCredentialsButEnabledSenderRequiresConfiguration() {
        var provider=new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class);
        assertThat(new SmtpPayslipEmailSender(provider,false,"","").enabled()).isFalse();
        assertThatThrownBy(()->new SmtpPayslipEmailSender(provider,true,"payroll@example.com","")).isInstanceOf(IllegalStateException.class);
    }
}
