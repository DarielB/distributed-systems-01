package com.httocalculadora;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class CalculadoraClienteHTTP extends JFrame {

    // Campos de texto e botão da interface gráfica
    private JTextField campoExpressao;
    private JTextField campoResultado;
    private JButton botaoCalcular;

    public CalculadoraClienteHTTP() {
        super("Calculadora HTTP"); // Título da janela

        setLayout(new BorderLayout(10, 10)); // Define layout principal com espaçamento

        // Painel superior com os campos de expressão e resultado
        JPanel painelCampos = new JPanel(new GridLayout(2, 2, 10, 10));

        // Campo para digitar a expressão matemática
        painelCampos.add(new JLabel("Expressão:"));
        campoExpressao = new JTextField(20);
        painelCampos.add(campoExpressao);

        // Campo para exibir o resultado
        painelCampos.add(new JLabel("Resultado:"));
        campoResultado = new JTextField(20);
        campoResultado.setEditable(false); // Não permite edição
        painelCampos.add(campoResultado);

        add(painelCampos, BorderLayout.CENTER); // Adiciona painel ao centro da janela

        // Botão para acionar o cálculo
        botaoCalcular = new JButton("Calcular");
        add(botaoCalcular, BorderLayout.SOUTH); // Adiciona botão na parte inferior

        // Define ação ao clicar no botão
        botaoCalcular.addActionListener(e -> {
            try {
                String expressao = campoExpressao.getText(); // Lê a expressão
                double resultado = avaliar(expressao);       // Avalia a expressão
                campoResultado.setText(String.valueOf(resultado)); // Exibe o resultado
            } catch (Exception ex) {
                // Mostra mensagem de erro
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Configurações da janela
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 150);
        setLocationRelativeTo(null); // Centraliza na tela
        setVisible(true);            // Exibe a janela
    }

    // Método principal para iniciar a aplicação
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CalculadoraClienteHTTP::new); // Cria a interface de forma segura para a thread gráfica
    }

    // Remove espaços e chama método de avaliação da expressão
    private double avaliar(String expr) throws Exception {
        return avaliarExpr(expr.replaceAll("\\s+", ""));
    }

    // Avalia uma expressão aritmética usando pilhas (shunting yard simplificado)
    private double avaliarExpr(String expr) throws Exception {
        Stack<Double> valores = new Stack<>();     // Pilha de operandos
        Stack<Character> operacoes = new Stack<>();// Pilha de operadores
        StringBuilder numBuffer = new StringBuilder(); // Buffer para montar números

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            // Se for número ou ponto decimal, acumula no buffer
            if (Character.isDigit(c) || c == '.') {
                numBuffer.append(c);
            } else {
                // Se há número no buffer, converte e empilha
                if (numBuffer.length() > 0) {
                    valores.push(Double.parseDouble(numBuffer.toString()));
                    numBuffer.setLength(0);
                }

                // Trata parênteses e operadores
                if (c == '(') {
                    operacoes.push(c);
                } else if (c == ')') {
                    // Resolve até encontrar '('
                    while (!operacoes.isEmpty() && operacoes.peek() != '(') {
                        aplicarOperacaoHTTP(valores, operacoes.pop());
                    }
                    if (!operacoes.isEmpty() && operacoes.peek() == '(') {
                        operacoes.pop(); // Remove o '(' correspondente
                    } else {
                        throw new Exception("Parênteses desbalanceados.");
                    }
                } else if ("+-*/".indexOf(c) != -1) {
                    // Aplica operadores de maior ou igual precedência antes de empilhar o novo
                    while (!operacoes.isEmpty() && precedencia(operacoes.peek()) >= precedencia(c)) {
                        if (operacoes.peek() == '(') break;
                        aplicarOperacaoHTTP(valores, operacoes.pop());
                    }
                    operacoes.push(c); // Empilha o operador atual
                }
            }
        }

        // Empilha último número, se houver
        if (numBuffer.length() > 0) {
            valores.push(Double.parseDouble(numBuffer.toString()));
        }

        // Aplica operações restantes
        while (!operacoes.isEmpty()) {
            if (operacoes.peek() == '(') {
                throw new Exception("Parênteses desbalanceados.");
            }
            aplicarOperacaoHTTP(valores, operacoes.pop());
        }

        // A pilha deve conter apenas o resultado final
        if (valores.size() != 1) {
            throw new Exception("Expressão inválida.");
        }

        return valores.pop();
    }

    // Retorna a precedência dos operadores
    private int precedencia(char op) {
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/') return 2;
        return 0;
    }

    // Aplica uma operação entre dois valores, chamando o servidor via HTTP
    private void aplicarOperacaoHTTP(Stack<Double> valores, char operador) throws Exception {
        double b = valores.pop(); // Operando 2
        double a = valores.pop(); // Operando 1
        int opCode;

        // Mapeia operador para código usado no servidor
        switch (operador) {
            case '+': opCode = 1; break;
            case '-': opCode = 2; break;
            case '*': opCode = 3; break;
            case '/': opCode = 4; break;
            default: throw new IllegalArgumentException("Operador inválido: " + operador);
        }

        // Envia requisição HTTP e obtém resposta JSON
        String jsonResposta = enviarRequisicaoHTTP(a, b, opCode);

        // Exibe o JSON completo para depuração
        JOptionPane.showMessageDialog(this, jsonResposta, "Resposta completa do servidor (JSON)", JOptionPane.INFORMATION_MESSAGE);

        // Extrai o campo "resultado" do JSON
        JsonObject jsonObj = JsonParser.parseString(jsonResposta).getAsJsonObject();
        double resultado = jsonObj.get("resultado").getAsDouble();

        // Empilha o resultado da operação
        valores.push(resultado);
    }

    // Envia uma requisição HTTP POST ao servidor PHP e retorna o JSON de resposta
private String enviarRequisicaoHTTP(double oper1, double oper2, int operacao) throws IOException {
    int tentativas = 0; // Contador de tentativas
    int maxTentativas = 3; // Número máximo de tentativas
    long espera = 5000; // Tempo de espera entre tentativas (em milissegundos)

    while (tentativas < maxTentativas) { // Enquanto não atingir o máximo de tentativas
        try {
            // Criação da URL do servidor PHP
            URL url = new URL("http://127.0.0.1:9000/PHPServerHTTP_Calculadora.php");

            // Abre conexão HTTP
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Define tempo de leitura e conexão
            conn.setReadTimeout(10000); // em milissegundos
            conn.setConnectTimeout(15000); // em milissegundos

            // Define método e permissões
            conn.setRequestMethod("POST"); // Método POST
            conn.setDoInput(true); // Permite entrada de dados
            conn.setDoOutput(true); // Permite saída de dados

            // Prepara os dados da requisição
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            String dados = "oper1=" + oper1 + "&oper2=" + oper2 + "&operacao=" + operacao;
            writer.write(dados);
            writer.flush();
            writer.close();
            os.close();

            // Obtém código de resposta do servidor
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Lê resposta do servidor
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String linha;
                while ((linha = br.readLine()) != null) {
                    response.append(linha.trim()); // Monta a resposta completa
                }
                return response.toString(); // Retorna resposta JSON como string
            } else {
                // Lança exceção se resposta não for OK
                throw new IOException("Erro HTTP: código " + responseCode);
            }

        } catch (IOException e) {
            // Em caso de erro, incrementa tentativas
            tentativas++;

            if (tentativas >= maxTentativas) {
                throw new IOException("Falha após " + maxTentativas + " tentativas: " + e.getMessage(), e);
            }

            // Espera antes de tentar novamente
            try {
                System.out.println("Tentativa " + tentativas + " falhou. Aguardando 5 segundos para tentar novamente...");
                Thread.sleep(espera); // Aguarda 5 segundos
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // Restaura estado de interrupção da thread
                throw new IOException("Retry interrompido", ie);
            }
        }
    }
    // Fallback final, não deve ser alcançado
    throw new IOException("Erro desconhecido.");
}
}
