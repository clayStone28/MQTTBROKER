package org.claystone.mqttspringboot.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.claystone.mqttspringboot.DTO.PaymentAlertPayload;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class MqttSoundboxGateway implements MqttCallback {

    @Value("${soundbox.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${soundbox.mqtt.client-id:Spring_Soundbox_Backend}")
    private String clientId;

    @Value("${soundbox.mqtt.topic-prefix:devices/trendit}")
    private String topicPrefix;

    private MqttClient mqttClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AudioService audioService;

    public MqttSoundboxGateway(RedisTemplate<String, Object> redisTemplate,
                               ObjectMapper objectMapper,
                               AudioService audioService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.audioService = audioService;
    }

    @PostConstruct
    public void init() {
        connectToMqttBroker();
    }

    private void connectToMqttBroker() {
        try {
            mqttClient = new MqttClient(brokerUrl, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false); // Retain subscription state across drops
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);

            mqttClient.setCallback(this);
            mqttClient.connect(options);

            // Subscribe to all hardware replay request channels: devices/trendit/+/replay_req
            String replayTopicPattern = topicPrefix + "/+/replay_req";
            mqttClient.subscribe(replayTopicPattern, 1);
            System.out.println("✅ Connected to MQTT Broker at " + brokerUrl + " and subscribed to: " + replayTopicPattern);

        } catch (MqttException e) {
            System.err.println("❌ Failed to connect to MQTT Broker: " + e.getMessage());
        }
    }

    /**
     * Outbound API: Publishes a payment voice alert down to the designated Soundbox device topic.
     */
    public void publishPaymentAlert(String imei, PaymentAlertPayload payload) {
        try {
            // 1. Cache the last successful transaction in Redis (Expires in 24h)
            String cacheKey = "last_txn:" + imei;
            redisTemplate.opsForValue().set(cacheKey, payload, 24, TimeUnit.HOURS);

            // 2. Publish payload to target device topic: devices/trendit/{imei}/pay_alert
            String topic = String.format("%s/%s/pay_alert", topicPrefix, imei);
            byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);

            MqttMessage message = new MqttMessage(payloadBytes);
            message.setQos(1); // Guaranteed delivery

            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(topic, message);
                System.out.println("📤 [MQTT OUTBOUND] Published alert to topic: " + topic);
            } else {
                System.err.println("⚠️ MQTT Client not connected. Failed to send alert to " + imei);
            }

        } catch (Exception e) {
            System.err.println("Error publishing payment alert: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    /**
     * Inbound MQTT Listener: Handles physical hardware buttons (e.g., REPLAY request)
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            System.out.println("📥 [MQTT INBOUND] Message arrived on topic: " + topic);

            // Match Replay Channel: devices/trendit/{imei}/replay_req
            if (topic.endsWith("/replay_req")) {
                String[] topicSegments = topic.split("/");
                if (topicSegments.length >= 3) {
                    String imei = topicSegments[2];
                    handleReplayRequest(imei);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing inbound MQTT message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    /**
     * Fallback Logic: Queries Redis for the last transaction and re-publishes the speech broadcast.
     */
    private void handleReplayRequest(String imei) throws Exception {
        String cacheKey = "last_txn:" + imei;
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

        String targetTopic = String.format("%s/%s/pay_alert", topicPrefix, imei);

        if (cachedValue != null) {
            PaymentAlertPayload lastTxn;

            // Handle deserialization from Redis depending on template setup
            if (cachedValue instanceof PaymentAlertPayload) {
                lastTxn = (PaymentAlertPayload) cachedValue;
            } else {
                lastTxn = objectMapper.convertValue(cachedValue, PaymentAlertPayload.class);
            }

            lastTxn.setType("PAYMENT_REPLAY");
            lastTxn.setTtsText("Replaying last payment: " + lastTxn.getTtsText());

            byte[] bytes = objectMapper.writeValueAsBytes(lastTxn);
            mqttClient.publish(targetTopic, new MqttMessage(bytes));

            // Speak locally on server if simulator audio is enabled
            audioService.speak(lastTxn.getTtsText());
            System.out.println("🔄 [REPLAY SUCCESS] Re-sent last transaction to Soundbox IMEI: " + imei);

        } else {
            // Handle empty cache state
            PaymentAlertPayload errorPayload = new PaymentAlertPayload(
                    "ERR_" + System.currentTimeMillis(),
                    "ERROR",
                    0.0,
                    "INR",
                    "No recent transaction found to replay",
                    System.currentTimeMillis() / 1000
            );

            byte[] bytes = objectMapper.writeValueAsBytes(errorPayload);
            mqttClient.publish(targetTopic, new MqttMessage(bytes));
            audioService.speak(errorPayload.getTtsText());
            System.out.println("⚠️ [REPLAY EMPTY] No cached record found for Soundbox IMEI: " + imei);
        }
    }
}