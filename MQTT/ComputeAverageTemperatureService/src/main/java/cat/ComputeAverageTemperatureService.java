package cat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ComputeAverageTemperatureService {

	private static final String BROKER = "tcp://localhost:1883";
	private static final String CLIENT_ID = "CATService";
	private static final String INPUT_TOPIC = "sensores/temperatura";
	private static final String ALERT_TOPIC = "sistema/alertas";
	private static final int TIME_WINDOW_SECONDS = 120;

	private static final List<TemperatureReading> readings = new CopyOnWriteArrayList<>();
	private static Double lastAverage = null;

	public static void main(String[] args) {
		try {
		MqttClient client = new MqttClient(BROKER, CLIENT_ID);
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		client.connect(options);

		client.subscribe(INPUT_TOPIC, (topic, msg) -> {
			String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
			processTemperatureMessage(payload, client);
		});

		System.out.println("CAT Service is running and subscribed to " + INPUT_TOPIC);
		}catch(MqttException ex) {
			System.out.println("Erro na comunicação com o broker: ");
			ex.printStackTrace();
		}
	}

	private static void processTemperatureMessage(String payload, MqttClient client) {
		try {
			Gson gson = new Gson();
			JsonObject json = gson.fromJson(payload, JsonObject.class);

			double temperature = json.get("temperature").getAsDouble();
			Instant timestamp = Instant.parse(json.get("timestamp").getAsString());

			readings.add(new TemperatureReading(timestamp, temperature));
			removeOldReadings();

			double currentAverage = readings.stream().mapToDouble(r -> r.temperature).average().orElse(0.0);

			if (lastAverage != null && (currentAverage - lastAverage) >= 5) {
				publishAlert(client, "AUMENTO_TEMPERATURA_REPENTINO",
						String.format("Média subiu de %.1f°C para %.1f°C em 120s", lastAverage, currentAverage));
			}

			if (currentAverage > 200) {
				publishAlert(client, "TEMPERATURA_ALTA",
						String.format("Média de temperatura dos últimos 120s é %.1f°C", currentAverage));
			}

			lastAverage = currentAverage;

		} catch (Exception e) {
			System.err.println("Erro ao processar mensagem: " + e.getMessage());
		}
	}

	private static void removeOldReadings() {
		Instant cutoff = Instant.now().minusSeconds(TIME_WINDOW_SECONDS);
		readings.removeIf(r -> r.timestamp.isBefore(cutoff));
	}

	private static void publishAlert(MqttClient client, String type, String message) {
		try {
			JsonObject alert = new JsonObject();
			alert.addProperty("timestamp", Instant.now().toString());
			alert.addProperty("type", type);
			alert.addProperty("message", message);

			MqttMessage mqttMessage = new MqttMessage(alert.toString().getBytes(StandardCharsets.UTF_8));
			mqttMessage.setQos(1);
			client.publish(ALERT_TOPIC, mqttMessage);

			System.out.println("Alerta publicado: " + alert);
		} catch (MqttException e) {
			System.err.println("Erro ao publicar alerta: " + e.getMessage());
		}
	}

	private static class TemperatureReading {
		Instant timestamp;
		double temperature;

		public TemperatureReading(Instant timestamp, double temperature) {
			this.timestamp = timestamp;
			this.temperature = temperature;
		}
	}
}
