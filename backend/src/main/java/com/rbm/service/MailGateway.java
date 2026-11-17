package com.rbm.service;

public interface MailGateway {
    void send(String to, String subject, String html) throws Exception;
}