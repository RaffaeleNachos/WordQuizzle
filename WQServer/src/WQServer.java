import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WQServer {
	
	private static ThreadPoolExecutor executor;
	private static LinkedBlockingQueue<Runnable> myQueue;

	public static void main(String[] args) {
		System.out.println("Server Starting...");
		WQDatabase db = null;
		if (!Files.exists(Paths.get("./passwords.json")) || !Files.exists(Paths.get("./users.json"))) {
			db = new WQDatabase(false);
		} 
		else {
			db = new WQDatabase(true);
		}
		try {
			//esportazione oggetto remoto
			RegistrationInterface stub = (RegistrationInterface) UnicastRemoteObject.exportObject(db, 0);
			//creazione del registro che fa da bootstrap, esso gestirà le richieste che arrivano sulla porta 6789 del server
			Registry r = LocateRegistry.createRegistry(6789);
			//registazione di stub nel registry, necessario per poi essere reperito da un altro host
			//rebind altrimenti eccezione
			r.rebind("USER-REG", stub);
		} catch (RemoteException e) {
			System.err.println("Qualcosa è andato storto nella procedura RMI");
			e.printStackTrace();
		}
		myQueue = new LinkedBlockingQueue<Runnable>();
		executor = new ThreadPoolExecutor(20, 50, 60L, TimeUnit.MILLISECONDS, myQueue);
		System.out.println("Server Ready");
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(6790);
			while(true){
				Socket socket = serverSocket.accept();
				WQTask t = new WQTask(db,socket);
				executor.execute(t);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
}
