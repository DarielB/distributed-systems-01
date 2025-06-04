import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

// Classe Calculadora implementa a interface remota ICalculadora
public class Calculadora  implements ICalculadora {

	private static final long serialVersionUID = 1L;

	private static int chamadasSoma = 1, chamadasSubtracao = 1, chamadasDivisao = 1, chamadasMultiplicacao = 1; // Contador de quantas vezes os métodos foram chamados

	// Implementação do método soma da interface remota
	public double soma(double a, double b) throws RemoteException {
		System.out.println("Método soma chamado " + chamadasSoma++);
		return a + b;
	}

	// Implementação do método subtração da interface remota
	public double subtracao(double a, double b) throws RemoteException {
		System.out.println("Método subtracao chamado " + chamadasSubtracao++);
		return a - b;
	}

	// Implementação do método divisão da interface remota
	public double divisao(double a, double b) throws RemoteException {
		System.out.println("Método divisao chamado " + chamadasDivisao++);
		if(b != 0){
			return a / b; // Retorna divisão se o divisor for diferente de zero
		}
		return -1;  // Retorna -1 se for divisão por zero
	}

	// Implementação do método multiplicação da interface remota
	public double multiplicacao(double a, double b) throws RemoteException {
		System.out.println("Método multiplicacao chamado " + chamadasMultiplicacao++);
		return a * b;
	}

	// Método main para iniciar o servidor RMI
	public static void main(String[] args) throws AccessException, RemoteException, AlreadyBoundException  {

		// Cria a instância da calculadora (objeto real)
		Calculadora calculadora = new Calculadora();		

		// Referência ao RMI Registry
		Registry reg = null; 

		// Exporta o objeto calculadora como objeto remoto (stub), usando a porta 1100
		ICalculadora stub = (ICalculadora) UnicastRemoteObject.
				exportObject(calculadora, 1100);
		try {
			// Tenta criar um novo registro RMI na porta 1099
			System.out.println("Creating registry...");
			reg = LocateRegistry.createRegistry(1099);
		} catch (Exception e) {
			try {
				// Se já estiver rodando, apenas conecta ao registry existente
				reg = LocateRegistry.getRegistry(1099);
			} catch (Exception e1) {
				// Se falhar novamente, finaliza o programa
				System.exit(0);
			}
		}

		// Registra o stub no RMI Registry com o nome "calculadora"
		reg.rebind("calculadora", stub);
	}
}
