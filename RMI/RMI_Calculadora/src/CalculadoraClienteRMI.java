import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Stack;

// Campos da interface gráfica
public class CalculadoraClienteRMI extends JFrame {

    // Campos da interface gráfica
    private JTextField campoExpressao;
    private JTextField campoResultado;
    private JButton botaoCalcular;
    private ICalculadora calculadora;

    // Construtor da classe
    public CalculadoraClienteRMI() {
        super("Calculadora RMI");

        try {
            // Obtém o registro RMI no localhost na porta padrão 1099
            Registry reg = LocateRegistry.getRegistry(1099);

            // Busca o objeto remoto registrado com o nome "calculadora"
            calculadora = (ICalculadora) reg.lookup("calculadora");
        } catch (Exception e) {
            // Mostra erro se não conseguir conectar ao servidor
            JOptionPane.showMessageDialog(this, "Erro ao conectar com servidor RMI:\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
             // Encerra a aplicação
            System.exit(0);
        }

        // Define o layout da janela (3 linhas, 2 colunas, com espaçamento de 10px)
        setLayout(new GridLayout(3, 2, 10, 10));

        // Primeira linha: rótulo e campo para digitar a expressão
        add(new JLabel("Expressão:"));
        campoExpressao = new JTextField(20);
        add(campoExpressao);

        // Segunda linha: botão de calcular (deixa uma célula vazia à esquerda)
        add(new JPanel());
        botaoCalcular = new JButton("Calcular");
        add(botaoCalcular);

        // Terceira linha: rótulo e campo para mostrar o resultado
        add(new JLabel("Resultado:"));
        campoResultado = new JTextField(20);
        campoResultado.setEditable(false);
        add(campoResultado);

        // Ação do botão "Calcular"
        botaoCalcular.addActionListener(e -> {
            try {
                String expressao = campoExpressao.getText();
                double resultado = avaliar(expressao);
                campoResultado.setText(String.valueOf(resultado));
            } catch (Exception ex) {
                // Mostra mensagem de erro, caso a expressão seja inválida
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Configurações da janela
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 150);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Remove espaços e avalia a expressão com parênteses
    private double avaliar(String expr) throws Exception {
        return avaliarExpr(expr.replaceAll("\\s+", ""));
    }

    // Avaliação da expressão
    private double avaliarExpr(String expr) throws Exception {
        Stack<Double> valores = new Stack<>();      // Pilha de números
        Stack<Character> operacoes = new Stack<>(); // Pilha de operadores
        StringBuilder numBuffer = new StringBuilder(); // Acumula os números em texto

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                // Se for dígito ou ponto decimal, adiciona ao número atual
                numBuffer.append(c);
            } else {
                // Se encontrou operador/parênteses, converte número acumulado e o empilha
                if (numBuffer.length() > 0) {
                    valores.push(Double.parseDouble(numBuffer.toString()));
                    numBuffer.setLength(0);
                }

                if (c == '(') {
                    // Abre novo escopo de parênteses
                    operacoes.push(c);
                } else if (c == ')') {
                    // Fecha escopo e resolve operações internas
                    while (operacoes.peek() != '(') {
                        aplicarOperacao(valores, operacoes.pop());
                    }
                    operacoes.pop(); // remove '('
                } else if ("+-*/".indexOf(c) != -1) {
                    // Enquanto há operadores com maior ou igual precedência, executa
                    while (!operacoes.isEmpty() && precedencia(operacoes.peek()) >= precedencia(c)) {
                        aplicarOperacao(valores, operacoes.pop());
                    }
                    // Empilha o operador atual
                    operacoes.push(c);
                }
            }
        }
        // Converte o último número restante, se houver
        if (numBuffer.length() > 0) {
            valores.push(Double.parseDouble(numBuffer.toString()));
        }

        // Aplica operações restantes na pilha
        while (!operacoes.isEmpty()) {
            aplicarOperacao(valores, operacoes.pop());
        }

        // Retorna o resultado final
        return valores.pop();
    }

    // Define precedência dos operadores: + e - = 1, * e / = 2
    private int precedencia(char op) {
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/') return 2;
        return 0;
    }

    // Aplica a operação chamando o método remoto
    private void aplicarOperacao(Stack<Double> valores, char operador) throws Exception {
        double b = valores.pop();
        double a = valores.pop();

        // Chama o método remoto correspondente e empilha o resultado
        switch (operador) {
            case '+': valores.push(calculadora.soma(a, b)); break;
            case '-': valores.push(calculadora.subtracao(a, b)); break;
            case '*': valores.push(calculadora.multiplicacao(a, b)); break;
            case '/': valores.push(calculadora.divisao(a, b)); break;
        }
    }

    // Método principal que inicializa a aplicação na thread da interface gráfica
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CalculadoraClienteRMI::new);
    }
}
