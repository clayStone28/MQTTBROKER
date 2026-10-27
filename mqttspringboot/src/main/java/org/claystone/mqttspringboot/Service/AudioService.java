package org.claystone.mqttspringboot.Service;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class AudioService {

    private Voice voice;

    @PostConstruct
    public void init() {
        // Required for FreeTTS voice lookup
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        VoiceManager voiceManager = VoiceManager.getInstance();
        this.voice = voiceManager.getVoice("kevin16");

        if (this.voice != null) {
            this.voice.allocate();
        } else {
            System.err.println("Warning: FreeTTS voice 'kevin16' could not be initialized.");
        }
    }

    public synchronized void speak(String text) {
        System.out.println("🔊 [SOUNDBOX SPEAKER BROADCAST]: " + text);
        if (this.voice != null) {
            this.voice.speak(text);
        }
    }
}