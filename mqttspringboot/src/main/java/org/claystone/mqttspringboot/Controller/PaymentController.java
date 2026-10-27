package org.claystone.mqttspringboot.Controller;

import org.claystone.mqttspringboot.DTO.PaymentAlertPayload;
import org.claystone.mqttspringboot.Service.AudioService;
import org.claystone.mqttspringboot.Service.MqttSoundboxGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final MqttSoundboxGateway mqttGateway;
    private final AudioService audioService;

    public PaymentController(MqttSoundboxGateway mqttGateway, AudioService audioService) {
        this.mqttGateway = mqttGateway;
        this.audioService = audioService;
    }

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerPayment(@RequestBody Map<String, Object> request) {
        String imei = (String) request.get("device_imei");
        double amount = Double.parseDouble(request.get("amount").toString());

        String ttsText = String.format("Received payment of %.0f rupees", amount);

        PaymentAlertPayload payload = new PaymentAlertPayload(
                "TXN_" + System.currentTimeMillis(),
                "PAYMENT_ALERT",
                amount,
                "INR",
                ttsText,
                System.currentTimeMillis() / 1000
        );

        // 1. Push payload down MQTT Channel
        mqttGateway.publishPaymentAlert(imei, payload);

        // 2. Play local speaker output
        audioService.speak(ttsText);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "device_imei", imei,
                "broadcast_text", ttsText
        ));
    }
}