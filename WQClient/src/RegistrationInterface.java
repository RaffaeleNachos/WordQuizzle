import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrationInterface extends Remote{
	
	//REQUIRES:
	//MODIFIES:
	//EFFECTS:
	//THROWS:
	//RETURNS:
	public int user_registration(String nickname, String password) throws RemoteException, NullPointerException;

}
