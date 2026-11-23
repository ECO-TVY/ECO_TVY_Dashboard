package com.ecotvy.application.port.out;

public interface MailPort {
    /** 텍스트 메일 발송. 실패하면 예외 */
    void send(String to, String subject, String body);
}
