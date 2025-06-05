package alarm;
import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;

public class AlarmService {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String CLIENT_ID = "AlarmService";
    private static final String TOPIC = "sistema/alertas";

    public static void main(String[] args) throws MqttException {
        MqttClient client = new MqttClient(BROKER, CLIENT_ID);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options);

        client.subscribe(TOPIC, (topic, msg) -> {
            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
            processAlarmMessage(payload);
        });

        System.out.println("AlarmService iniciado. Aguardando alertas...");
    }

    private static void processAlarmMessage(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();

            String type = json.get("type").getAsString();
            String timestamp = json.get("timestamp").getAsString();
            String message = json.get("message").getAsString();

            // Simulação de exibição gráfica (substitua por envio à interface real)
            System.out.println("🚨 ALARME DETECTADO 🚨");
            System.out.println("Tipo: " + type);
            System.out.println("Horário: " + timestamp);
            System.out.println("Mensagem: " + message);
            System.out.println("====================================");

            // Aqui poderia chamar um endpoint REST, atualizar uma tela via WebSocket etc.

        } catch (Exception e) {
            System.err.println("Erro ao processar alerta: " + e.getMessage());
        }
    }
}
