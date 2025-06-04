import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICalculadora extends Remote{

	public double soma(double a, double b) throws RemoteException;
	public double subtracao(double a, double b) throws RemoteException;
	public double divisao(double a, double b) throws RemoteException;
	public double multiplicacao(double a, double b) throws RemoteException;
}
