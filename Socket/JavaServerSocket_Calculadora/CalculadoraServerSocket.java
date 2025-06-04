import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class CalculadoraServerSocket {
	public static void main(String[] args) {
		ServerSocket welcomeSocket;
		DataOutputStream socketOutput;     	
	    BufferedReader socketEntrada;
	    Calculadora calc = new Calculadora();
		try {
			welcomeSocket = new ServerSocket(9090);
			int i=0; //numero de clientes
	  
	    	System.out.println ("Servidor no ar");
	    	while(true) { 
	        	Socket connectionSocket = welcomeSocket.accept(); 
	        	i++;
	        	System.out.println ("Nova conexao");

	        	// Interpretando dados do servidor
	        	socketEntrada = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            	String operacao= socketEntrada.readLine();
            	String oper1=socketEntrada.readLine();
            	String oper2=socketEntrada.readLine();

            	// Chamando a calculadora
				String result= "";
				switch (operacao) {
					case "+":
						result += calc.soma(Double.parseDouble(oper1),Double.parseDouble(oper2)); 	
					break;
					case "-":
						result += calc.subtracao(Double.parseDouble(oper1),Double.parseDouble(oper2)); 	
					break;
					case "/":
						result += calc.divisao(Double.parseDouble(oper1),Double.parseDouble(oper2)); 	
					break;
					case "*":
						result += calc.multiplicacao(Double.parseDouble(oper1),Double.parseDouble(oper2)); 	
					break;					
					default:
					break;
			   }

            	//Enviando dados para o servidor
            	socketOutput= new DataOutputStream(connectionSocket.getOutputStream());
	        	socketOutput.writeBytes(result+ '\n');
	        	System.out.println (result);  
	        	socketOutput.flush();
	        	socketOutput.close();
	      }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}