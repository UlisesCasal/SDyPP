package com.grupoamarillo.trabajopractico.Hit6;

public class Message {
    public String msg;
    public String from;
    public String action;
    public int port;
    public String ip;

    public Message() {}

    public Message(String msg, String from) {
        this.msg = msg;
        this.from = from;
    }
}