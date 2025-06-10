package alarm;

import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class AlarmService {

	private static final String appConfigPath = "./src/main/resources/application.properties";
	
	public static void main(String[] args) throws MqttException {
		try {
			// Configurações do broker MQTT e tópico de alertas
			Properties appProps = new Properties();
			appProps.load(new FileInputStream(appConfigPath));
			String BROKER = appProps.getProperty("URL_BROKER");
			String CLIENT_ID = appProps.getProperty("CLIENT_ID");
			String ALERT_TOPIC = appProps.getProperty("ALERT_TOPIC");
			
			// Cria o cliente MQTT com ID único
			MqttClient client = new MqttClient(BROKER, CLIENT_ID);

			// Configura opções de conexão MQTT
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true); // Reconectar automaticamente se desconectar
			options.setCleanSession(true); // Sessão limpa para não receber mensagens antigas

			// Conecta ao broker MQTT
			client.connect(options);
			
			System.out.println("AlarmService rodando e aguardando alertas no tópico: " + ALERT_TOPIC);
			
			// Inscreve-se para receber mensagens do tópico de alertas
			client.subscribe(ALERT_TOPIC, (topic, message) -> {
				// Callback chamado a cada nova mensagem recebida no tópico

				// Converte payload (bytes) para String UTF-8
				String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

				// Faz parse da mensagem JSON recebida
				JsonObject alertJson = JsonParser.parseString(payload).getAsJsonObject();

				// Extrai informações do alerta
				String type = alertJson.get("type").getAsString();
				String alertMsg = alertJson.get("message").getAsString();
				String timestamp = alertJson.get("timestamp").getAsString();

				// Exibe o alerta formatado para o operador humano
				System.out.printf("[ALERTA] %s - Tipo: %s | Mensagem: %s%n", timestamp, type, alertMsg);

			});
		} catch (IOException ex) {
			// Caso ocorra erro ao ler ou interpretar a mensagem JSON
			System.err.println("Falha na obtenção das propriedades: " + ex.getMessage());
		}
		catch (Exception ex) {
			// Caso ocorra erro ao ler ou interpretar a mensagem JSON
			System.err.println("Erro ao processar alerta MQTT: " + ex.getMessage());
		}

		
	}
}
