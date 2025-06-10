package cat;

import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ComputeAverageTemperatureService {

	private static final String appConfigPath = "./src/main/resources/application.properties";

	// Tempo de janela de leitura para cálculo da média (em segundos)
	private static final int TIME_WINDOW_SECONDS = 120;

	// Lista thread-safe para armazenar as leituras recentes
	private static final List<TemperatureReading> readings = new CopyOnWriteArrayList<>();
	private static Double lastAverage = null; // Última média calculada

	public static void main(String[] args) {

		try {
			// Configurações do broker MQTT e tópico de alertas
			Properties appProps = new Properties();
			appProps.load(new FileInputStream(appConfigPath));
			String BROKER = appProps.getProperty("URL_BROKER");
			String CLIENT_ID = appProps.getProperty("CLIENT_ID");
			String INPUT_TOPIC = appProps.getProperty("INPUT_TOPIC");
			String ALERT_TOPIC = appProps.getProperty("ALERT_TOPIC");
			// Cria o cliente MQTT
			MqttClient client = new MqttClient(BROKER, CLIENT_ID);

			// Configurações de conexão MQTT
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);

			// Conecta ao broker MQTT
			client.connect(options);

			// Inscreve no tópico de temperatura
			client.subscribe(INPUT_TOPIC, (topic, msg) -> {
				String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
				processTemperatureMessage(payload, client, ALERT_TOPIC); // Processa a leitura recebida
			});

			System.out.println("CAT Service está rodando e ouvindo o tópico " + INPUT_TOPIC);

		} catch (IOException ex) {
			// Caso ocorra erro ao ler ou interpretar a mensagem JSON
			System.err.println("Falha na obtenção das propriedades: " + ex.getMessage());
		} catch (Exception ex) {
			// Caso ocorra erro ao ler ou interpretar a mensagem JSON
			System.err.println("Erro ao processar alerta MQTT: " + ex.getMessage());
		}
	}

	// Processa cada mensagem recebida com temperatura
	private static void processTemperatureMessage(String payload, MqttClient client, String alertTopic) {
		try {
			Gson gson = new Gson();
			JsonObject json = gson.fromJson(payload, JsonObject.class);

			// Extrai os campos da mensagem JSON
			double temperature = json.get("temperature").getAsDouble();
			Instant timestamp = Instant.parse(json.get("timestamp").getAsString());

			// Adiciona a nova leitura à lista
			readings.add(new TemperatureReading(timestamp, temperature));

			// Remove leituras antigas que estão fora da janela de 120s
			removeOldReadings();

			// Calcula a média das leituras dentro da janela
			double currentAverage = readings.stream().mapToDouble(r -> r.temperature).average().orElse(0.0);

			// Verifica se a média excede 200°C
			if (currentAverage > 200) {
				publishAlert(client, "TEMPERATURA_ALTA",
						String.format("Média de temperatura dos últimos 120s é %.1f°C", currentAverage), alertTopic);
			} else if (lastAverage != null && (currentAverage - lastAverage) >= 5) { // Verifica se houve um aumento repentino na temperatura (≥ 5°C)
				publishAlert(client, "AUMENTO_TEMPERATURA_REPENTINO",
						String.format("Média subiu de %.1f°C para %.1f°C em 120s", lastAverage, currentAverage),
						alertTopic);
			}

			// Atualiza a última média
			lastAverage = currentAverage;

		} catch (Exception e) {
			System.err.println("Erro ao processar mensagem: " + e.getMessage());
		}
	}

	// Remove leituras fora da janela de tempo
	private static void removeOldReadings() {
		Instant cutoff = Instant.now().minusSeconds(TIME_WINDOW_SECONDS);
		readings.removeIf(r -> r.timestamp.isBefore(cutoff));
	}

	// Publica uma mensagem de alerta no tópico de alertas
	private static void publishAlert(MqttClient client, String type, String message, String alertTopic) {
		try {

			// Cria um objeto JSON com os dados do alerta
			JsonObject alert = new JsonObject();
			alert.addProperty("timestamp", Instant.now().toString());
			alert.addProperty("type", type);
			alert.addProperty("message", message);

			// Constrói a mensagem MQTT
			MqttMessage mqttMessage = new MqttMessage(alert.toString().getBytes(StandardCharsets.UTF_8));
			mqttMessage.setQos(1); // Garantia mínima de entrega

			// Publica no tópico de alertas
			client.publish(alertTopic, mqttMessage);
			System.out.println("Alerta publicado: " + alert);
		} catch (MqttException e) {
			System.err.println("Erro ao publicar alerta: " + e.getMessage());
		}
	}

	// Classe interna para representar uma leitura de temperatura
	private static class TemperatureReading {
		Instant timestamp;
		double temperature;

		public TemperatureReading(Instant timestamp, double temperature) {
			this.timestamp = timestamp;
			this.temperature = temperature;
		}
	}
}
