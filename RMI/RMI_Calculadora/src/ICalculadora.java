import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICalculadora extends Remote{

	public int soma(int a, int b) throws RemoteException;
}
