package sensor;

import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;

public class TemperatureSensorSimulator {

	private static final String appConfigPath = "./src/main/resources/application.properties";

	private static final Random random = new Random();

	public static void main(String[] args) {
		try {
			// Configurações do broker MQTT e tópico de alertas
			Properties appProps = new Properties();
			appProps.load(new FileInputStream(appConfigPath));
			String BROKER = appProps.getProperty("URL_BROKER");
			String CLIENT_ID = appProps.getProperty("CLIENT_ID");
			String ALERT_TOPIC = appProps.getProperty("ALERT_TOPIC");
			Integer SENSOR_COUNT = Integer.parseInt(appProps.getProperty("SENSOR_COUNT"));// Número de sensores simulados
			Integer INTERVAL_SECONDS = Integer.parseInt(appProps.getProperty("INTERVAL_SECONDS")); // Intervalo de envio
			MqttClient client = new MqttClient(BROKER, CLIENT_ID);
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);
			client.connect(options);

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SENSOR_COUNT);

			for (int i = 1; i <= SENSOR_COUNT; i++) {
				String sensorId = "temp_sensor_" + String.format("%02d", i);
				scheduler.scheduleAtFixedRate(() -> publishTemperature(client, sensorId, ALERT_TOPIC), 0, INTERVAL_SECONDS,
						TimeUnit.SECONDS);
			}

			System.out.println("Simulador de sensores iniciado.");
		} catch (MqttException ex) {
			System.err.println("Erro ao processar alerta MQTT: " + ex.getMessage());
		} catch (IOException ex) {
			System.err.println("Falha na obtenção das propriedades: " + ex.getMessage());
		}
	}

	private static void publishTemperature(MqttClient client, String sensorId, String alertTopic) {
		try {
			double temperature = 180 + random.nextGaussian() * 40; // Gera variação em torno de 180°C

			JsonObject json = new JsonObject();
			json.addProperty("sensor_id", sensorId);
			json.addProperty("timestamp", Instant.now().toString());
			json.addProperty("temperature", Math.round(temperature * 10.0) / 10.0); // Uma casa decimal

			MqttMessage message = new MqttMessage(json.toString().getBytes(StandardCharsets.UTF_8));
			message.setQos(1);

			client.publish(alertTopic, message);
			System.out.printf("Sensor %s publicou: %s%n", sensorId, json);
		} catch (Exception e) {
			System.err.println("Erro ao publicar leitura: " + e.getMessage());
		}
	}
}
