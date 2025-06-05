package sensor;

import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TemperatureSensorSimulator {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String CLIENT_ID = "SensorSim";
    private static final String TOPIC = "sensores/temperatura";

    private static final int SENSOR_COUNT = 3; // Número de sensores simulados
    private static final int INTERVAL_SECONDS = 60; // Intervalo de envio

    private static final Random random = new Random();

    public static void main(String[] args) throws MqttException {
        MqttClient client = new MqttClient(BROKER, CLIENT_ID);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SENSOR_COUNT);

        for (int i = 1; i <= SENSOR_COUNT; i++) {
            String sensorId = "temp_sensor_" + String.format("%02d", i);
            scheduler.scheduleAtFixedRate(() -> publishTemperature(client, sensorId),
                    0, INTERVAL_SECONDS, TimeUnit.SECONDS);
        }

        System.out.println("Simulador de sensores iniciado.");
    }

    private static void publishTemperature(MqttClient client, String sensorId) {
        try {
            double temperature = 180 + random.nextGaussian() * 20; // Gera variação em torno de 180°C

            JsonObject json = new JsonObject();
            json.addProperty("sensor_id", sensorId);
            json.addProperty("timestamp", Instant.now().toString());
            json.addProperty("temperature", Math.round(temperature * 10.0) / 10.0); // Uma casa decimal

            MqttMessage message = new MqttMessage(json.toString().getBytes(StandardCharsets.UTF_8));
            message.setQos(1);

            client.publish(TOPIC, message);
            System.out.printf("Sensor %s publicou: %s%n", sensorId, json);
        } catch (Exception e) {
            System.err.println("Erro ao publicar leitura: " + e.getMessage());
        }
    }
}
