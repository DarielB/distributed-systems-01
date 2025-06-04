import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.Stack;

// Classe principal que representa o cliente com interface gráfica
public class CalculadoraClientSocket extends JFrame {

    // Campos da interface gráfica
    private JTextField campoExpressao;
    private JTextField campoResultado;
    private JButton botaoCalcular;

    // Construtor da interface gráfica
    public CalculadoraClientSocket() {
        super("Calculadora Cliente Socket");

        // Define layout da janela: 3 linhas e 2 colunas com espaçamento
        setLayout(new GridLayout(3, 2, 10, 10));

        // Linha 1 - Label e campo de entrada da expressão
        add(new JLabel("Expressão:"));
        campoExpressao = new JTextField(20);
        add(campoExpressao);

        // Linha 2 - Espaço vazio e botão "Calcular"
        add(new JPanel()); // Painel vazio para preencher espaço
        botaoCalcular = new JButton("Calcular");
        add(botaoCalcular);

        // Linha 3 - Label e campo de resultado (somente leitura)
        add(new JLabel("Resultado:"));
        campoResultado = new JTextField(20);
        campoResultado.setEditable(false); // Impede edição manual do campo
        add(campoResultado);

        // Define ação do botão "Calcular"
        botaoCalcular.addActionListener(e -> {
            try {
                String expressao = campoExpressao.getText();
                double resultado = avaliar(expressao);
                campoResultado.setText(String.valueOf(resultado));
            } catch (Exception ex) {
                // Mostra mensagem de erro em caso de exceção
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Configurações da janela
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Fecha o programa ao fechar a janela
        setSize(400, 150);          // Define tamanho da janela
        setLocationRelativeTo(null);           // Centraliza na tela
        setVisible(true);                      // Torna a janela visível
    }

    // Avalia a expressão, removendo espaços e chamando a função de avaliação
    private double avaliar(String expr) throws Exception {
        return avaliarExpr(expr.replaceAll("\\s+", "")); // Remove espaços em branco
    }

    // Avalia expressões com parênteses e operações básicas
    private double avaliarExpr(String expr) throws Exception {
        Stack<Double> valores = new Stack<>();         // Pilha para os valores numéricos
        Stack<Character> operacoes = new Stack<>();    // Pilha para os operadores
        StringBuilder numBuffer = new StringBuilder(); // Acumulador para construir números com mais de um dígito

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Se o caractere é um número ou ponto, acumula no buffer
            if (Character.isDigit(c) || c == '.') {
                numBuffer.append(c);
            } else {
                // Quando encontrar um operador, transforma o buffer acumulado em número
                if (numBuffer.length() > 0) {
                    valores.push(Double.parseDouble(numBuffer.toString()));
                    numBuffer.setLength(0); // Limpa buffer
                }

                // Abre parêntese: adiciona à pilha de operações
                if (c == '(') {
                    operacoes.push(c);
                }
                // Fecha parêntese: resolve as operações até encontrar o '(' correspondente
                else if (c == ')') {
                    while (operacoes.peek() != '(') {
                        aplicarOperacao(valores, operacoes.pop());
                    }
                    operacoes.pop(); // Remove o '(' da pilha
                }
                // Operadores básicos
                else if ("+-*/".indexOf(c) != -1) {
                    // Aplica operações anteriores de maior ou igual precedência
                    while (!operacoes.isEmpty() && precedencia(operacoes.peek()) >= precedencia(c)) {
                        aplicarOperacao(valores, operacoes.pop());
                    }
                    operacoes.push(c); // Adiciona operador atual à pilha
                }
            }
        }

        // Adiciona último número que restou no buffer
        if (numBuffer.length() > 0) {
            valores.push(Double.parseDouble(numBuffer.toString()));
        }

        // Aplica as operações restantes
        while (!operacoes.isEmpty()) {
            aplicarOperacao(valores, operacoes.pop());
        }

        return valores.pop(); // Resultado final
    }

    // Retorna a precedência de cada operador
    private int precedencia(char op) {
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/') return 2;
        return 0;
    }

    // Aplica uma operação básica enviando os dados ao servidor
    private void aplicarOperacao(Stack<Double> valores, char operador) throws IOException {
        double b = valores.pop(); // Segundo operando
        double a = valores.pop(); // Primeiro operando
        String operacao = String.valueOf(operador); // Converte operador para String
        valores.push(enviarParaServidor(operacao, a, b)); // Chama o servidor e empilha o resultado
    }

    // Envia a operação para o servidor e recebe o resultado
    private double enviarParaServidor(String operacao, double op1, double op2) throws IOException {
        try (
            Socket socket = new Socket("127.0.0.1", 9090); // Cria conexão com servidor local na porta 9090
            DataOutputStream saida = new DataOutputStream(socket.getOutputStream()); // Canal de envio
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream())) // Canal de leitura
        ) {
            // Envia operação e operandos como strings
            saida.writeBytes(operacao + "\n");
            saida.writeBytes(op1 + "\n");
            saida.writeBytes(op2 + "\n");
            saida.flush();

            // Lê o resultado enviado pelo servidor
            return Double.parseDouble(entrada.readLine());
        }
    }

    // Método principal: inicializa a interface gráfica
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CalculadoraClientSocket::new);
    }
}
