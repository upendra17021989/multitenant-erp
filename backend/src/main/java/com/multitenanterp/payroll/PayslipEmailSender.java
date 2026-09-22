package com.multitenanterp.payroll;

public interface PayslipEmailSender {
    boolean enabled();
    void send(String recipient, String company, int year, int month, String filename, byte[] pdf) throws Exception;
}
