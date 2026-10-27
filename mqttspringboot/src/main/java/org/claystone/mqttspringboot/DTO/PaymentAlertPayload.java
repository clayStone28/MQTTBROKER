package org.claystone.mqttspringboot.DTO;

import java.io.Serializable;

public class PaymentAlertPayload implements Serializable {
    private String msgId;
    private String type; // PAYMENT_ALERT, PAYMENT_REPLAY
    private double amount;
    private String currency;
    private String ttsText;
    private long timestamp;

    // Default Constructor
    public PaymentAlertPayload() {}

    public PaymentAlertPayload(String msgId, String type, double amount, String currency, String ttsText, long timestamp) {
        this.msgId = msgId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.ttsText = ttsText;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getTtsText() { return ttsText; }
    public void setTtsText(String ttsText) { this.ttsText = ttsText; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}