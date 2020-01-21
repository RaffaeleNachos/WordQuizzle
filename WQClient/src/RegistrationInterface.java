import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrationInterface extends Remote{
	
	//REQUIRES: nickname != null 6& password!=null
	//MODIFIES: this
	//EFFECTS: aggiunge alla collezione l'utente
	//THROWS:	RemoteException nel caso di problemi con RMI
	//			NullpointerException se nickname || password == null
	//RETURNS: restituisce un codice di errore relativo al problema riscontrato
	public int user_registration(String nickname, String password) throws RemoteException, NullPointerException;
}
