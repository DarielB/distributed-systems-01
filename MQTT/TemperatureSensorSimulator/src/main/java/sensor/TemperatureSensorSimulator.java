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

//public class TemperatureSensorSimulator {
//
//	private static final String BROKER = "tcp://localhost:1883";
//	private static final String CLIENT_ID = "DynamicTempSensorSim";
//	private static final String TOPIC = "sensores/temperatura";
//
//	// Intervalo de envio de dados por sensor
//	private static final int SENSOR_PUBLISH_INTERVAL_SECONDS = 10;
//	// Intervalo para mudar a quantidade de sensores
//	private static final int SENSOR_CHANGE_INTERVAL_SECONDS = 10;
//	private static final int MAX_SENSORS = 10;
//
//	private static MqttClient client;
//
//	private static final Random random = new Random();
//
//	// Map que guarda os sensores ativos e suas tarefas agendadas
//	private static final Map<String, ScheduledFuture<?>> activeSensors = new ConcurrentHashMap<>();
//
//	// Executor para agendamento das tarefas
//	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
//
//	public static void main(String[] args) throws MqttException {
//		client = new MqttClient(BROKER, CLIENT_ID);
//
//		MqttConnectOptions options = new MqttConnectOptions();
//		options.setAutomaticReconnect(true);
//		options.setCleanSession(true);
//		client.connect(options);
//
//		System.out.println("Simulador dinâmico de sensores iniciado.");
//
//		// Agendamos tarefa que altera dinamicamente a quantidade de sensores
//		scheduler.scheduleAtFixedRate(TemperatureSensorSimulator::updateSensors, 0, SENSOR_CHANGE_INTERVAL_SECONDS,
//				TimeUnit.SECONDS);
//	}
//
//	// Função que adiciona ou remove sensores aleatoriamente
//	private static void updateSensors() {
//		int action = random.nextInt(2); // 0 = adicionar sensor, 1 = remover sensor
//
//		if (action == 0 && activeSensors.size() < MAX_SENSORS) {
//			// Adiciona um novo sensor
//			String newSensorId = generateNewSensorId();
//			ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> publishTemperature(newSensorId), 0,
//					SENSOR_PUBLISH_INTERVAL_SECONDS, TimeUnit.SECONDS);
//			activeSensors.put(newSensorId, future);
//			System.out.println("[+] Sensor ativado: " + newSensorId);
//
//		} else if (action == 1 && !activeSensors.isEmpty()) {
//			// Remove um sensor aleatório
//			List<String> sensorIds = new ArrayList<>(activeSensors.keySet());
//			String toRemove = sensorIds.get(random.nextInt(sensorIds.size()));
//			ScheduledFuture<?> future = activeSensors.get(toRemove);
//			future.cancel(false);
//			activeSensors.remove(toRemove);
//			System.out.println("[-] Sensor desativado: " + toRemove);
//		}
//	}
//
//	// Publica uma mensagem de temperatura para o sensor dado
//	private static void publishTemperature(String sensorId) {
//		try {
//			double temperature = 180 + random.nextGaussian() * 20;
//
//			JsonObject json = new JsonObject();
//			json.addProperty("sensor_id", sensorId);
//			json.addProperty("timestamp", Instant.now().toString());
//			json.addProperty("temperature", Math.round(temperature * 10.0) / 10.0);
//
//			MqttMessage message = new MqttMessage(json.toString().getBytes(StandardCharsets.UTF_8));
//			message.setQos(1);
//
//			client.publish(TOPIC, message);
//
//			System.out.printf("Sensor %s publicou: %s%n", sensorId, json);
//		} catch (Exception e) {
//			System.err.println("Erro ao publicar temperatura do sensor " + sensorId + ": " + e.getMessage());
//		}
//	}
//
//	// Gera ID único para novo sensor (ex: temp_sensor_01, temp_sensor_02...)
//	private static String generateNewSensorId() {
//		int id = 1;
//		while (activeSensors.containsKey(String.format("temp_sensor_%02d", id))) {
//			id++;
//		}
//		return String.format("temp_sensor_%02d", id);
//	}
//}
