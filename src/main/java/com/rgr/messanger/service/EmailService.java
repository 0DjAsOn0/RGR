package com.rgr.messanger.service;


public interface EmailService {

    public void send(String to, String subject, String body);

}