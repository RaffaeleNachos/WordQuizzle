import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;  

public class WQClient extends Application{
	
	public static RegistrationInterface regGest;
	private Socket myTCPsocket;
	private DatagramSocket myUDPsocket;
	private static FXMLLoader loader;
	private static Stage stage;
	private BufferedWriter writer;
	private BufferedReader reader;
	public String myUsername;
	private JSONParser parser = new JSONParser();
	//porta per le notifiche
	private int UDPport;
	//porta per la sfida
	public int ChallengeTCPport;
	private WQNotify notifyThread;
	private RegisterLoginController logincontroller;
	private MainViewController maincontroller;
	private GameViewController gamecontroller;
	//nel caso di un server non in locale, basta cambiare questa stringa con l'indirizzo del server
	private static String serverIA = "localhost";
	
	public static void main(String[] args) {
		Registry r;
		//RemoteObject: spazio di indirizzamento diverso, gira su un'altra JVM
		Remote remoteObj;
		try {
			//r = LocateRegistry.getRegistry(serverIA, 6789); //nel caso di host remoti
			r = LocateRegistry.getRegistry(6789);
			//eseguo la lookup nel registry e mi restituisce un oggetto remoto
			remoteObj = r.lookup("USER-REG");
			regGest = (RegistrationInterface) remoteObj;
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		//dopo aver instaurato la connessione con il registry offerto dal server parte l'interfaccia grafica
		System.out.println("WQClient | launching graphic interface");
		launch();
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		primaryStage.setTitle("Word Quizzle");
		gotoLogin();
		primaryStage.show();
	}
	
	//meotodo che mi permette di tornare alla finestra di login/registrazione
	public void gotoLogin() {
		try {
			//carica il file di stile della schermata
			loader = new FXMLLoader();
			loader.setLocation(Paths.get("src/views/QuizzleLogin.fxml").toUri().toURL());
			Parent layoutmain = loader.load();
			//a questo punto setta il controller dei vari componenti della gui -> RegisterLoginController
        	logincontroller = loader.getController();
        	//passo riferimento di questa classe per accedere al registry per la registrazione e per accedere ai metodi handler definiti in questa classe
            logincontroller.setClient(this);
			Scene scene = stage.getScene();
			if (scene == null) {
        		scene = new Scene(layoutmain);
            	stage.setScene(scene);
        	} else {
        		stage.getScene().setRoot(layoutmain);
        	}
			stage.sizeToScene();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void gotoMain() {
		try {
			loader = new FXMLLoader();
			loader.setLocation(Paths.get("src/views/MainView.fxml").toUri().toURL());
			Parent layoutmain = loader.load();
			//setta il controller per gli elementi della gui della finestra main
			maincontroller = loader.getController();
			//passo riferimento di questa classe e del thread delle notifiche
            maincontroller.setClient(this, notifyThread);
            //setto i punti e username nella tab "Your Info."
            maincontroller.setPoints(points_handler());
            maincontroller.setUsername(myUsername);
            //popolo la lista delle amicizie
            maincontroller.populateList(list_handler());
            //la gui ha di default la vista della tab delle notifiche che però deve essere vista solo quando c'è 
            //una effettiva notifica. per questo appena entrato la posto in modalità invisibile (diventerà visibile alla prima notifica o se ci sono notifiche in coda)
            maincontroller.setNotifyTabInvisible();
            //qui appunto passo il riferimento al controller al thread delle notifiche perchè sarà lui ad occuparsi di rendere visibile la notification Tab.
            notifyThread.setController(maincontroller);
			Scene scene = stage.getScene();
			if (scene == null) {
        		scene = new Scene(layoutmain);
            	stage.setScene(scene);
        	} else {
        		stage.getScene().setRoot(layoutmain);
        	}
			stage.sizeToScene();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void gotoGame() {
		try {
			loader = new FXMLLoader();
			loader.setLocation(Paths.get("src/views/GameView.fxml").toUri().toURL());
			Parent layoutmain = loader.load();
			//setto controller per il gioco
			gamecontroller = loader.getController();
			//passo il riferimento di questa classe
            gamecontroller.setClient(this);
            //setto la porta della sfida e l'indirizzo del server
            gamecontroller.setSocket(ChallengeTCPport, myTCPsocket.getInetAddress());
			Scene scene = stage.getScene();
			if (scene == null) {
        		scene = new Scene(layoutmain);
            	stage.setScene(scene);
        	} else {
        		stage.getScene().setRoot(layoutmain);
        	}
			stage.sizeToScene();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int login_handler(String username, String password) {
		int err = 0;
		try {
			myTCPsocket = new Socket(serverIA, 6790);
			//creo porta e processo per le notifiche
			UDPport = (int) ((Math.random() * ((65535 - 1024) + 1)) + 1024);
			//scrivo tramite tcp le informazioni di login
			writer = new BufferedWriter(new OutputStreamWriter(myTCPsocket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(myTCPsocket.getInputStream()));
			writer.write("LOGIN " + username + " " + password + " " + myTCPsocket.getInetAddress().getHostAddress() + " " + UDPport); 
			writer.newLine(); 
			writer.flush();
			err = Integer.parseInt(reader.readLine());
			//nel caso in cui il login è errato cancello la socket creata
			if (err != 12) {
				System.out.println("WQClient | login failed. Cleaning...");
				myTCPsocket.close();
				writer.close();
				reader.close();
				return err;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		//se il login è ok a questo punto setto myUsername che è una variabile che mi serve per tutte quelle operazioni che la gui 
		//fa in automatico come richiedere il punteggio o la lista degli amici e faccio partire il thread delle notifiche
		System.out.println("WQClient | UDP notifications on port: " + UDPport);
		try {
			myUDPsocket = new DatagramSocket(UDPport);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		notifyThread = new WQNotify(this, myUDPsocket);
		notifyThread.start();
		myUsername = username;
		return err;
	}
	
	public int logout_handler() {
		try {
			writer.write("LOGOUT " + myUsername); 
			writer.newLine(); 
			writer.flush();
			if (notifyThread.isAlive()) myUDPsocket.close();
			return Integer.parseInt(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int add_handler(String nickname) {
		try {
			writer.write("ADD " + myUsername + " " + nickname); 
			writer.newLine(); 
			writer.flush();
			return Integer.parseInt(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int del_handler(String nickname) {
		try {
			writer.write("REMOVE " + myUsername + " " + nickname); 
			writer.newLine(); 
			writer.flush();
			return Integer.parseInt(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int points_handler() {
		try {
			writer.write("POINTS " + myUsername); 
			writer.newLine(); 
			writer.flush();
			return Integer.parseInt(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public ArrayList<String> list_handler() {
		try {
			writer.write("LIST " + myUsername); 
			writer.newLine(); 
			writer.flush();
			//aspetto la risposta
			String jsonfile = reader.readLine();
			JSONArray jsonOutArray = null;
			ArrayList<String> out = new ArrayList<>();
			try {
				jsonOutArray = (JSONArray) parser.parse(jsonfile);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			for(int i = 0; i<jsonOutArray.size(); i++) {
				//inserisco gli username in un ArrayList di stringhe necessario per poter andare a popolare la listview della GUI
				out.add(jsonOutArray.get(i).toString());
			}
			return out;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public ArrayList<String> rank_handler() {
		try {
			writer.write("RANK " + myUsername); 
			writer.newLine(); 
			writer.flush();
			//risposta
			String jsonfile = reader.readLine();
			//System.out.println(jsonfile);
			JSONArray jsonOutArray = null;
			ArrayList<String> out = new ArrayList<>();
			try {
				jsonOutArray = (JSONArray) parser.parse(jsonfile);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			for(int i = 0; i<jsonOutArray.size(); i++) {
				JSONObject usr = (JSONObject) jsonOutArray.get(i);
				//creo arraylist di stringhe per popolare la listview della GUI
				out.add(usr.get("username") + " \t Points:" + usr.get("points"));
			}
			return out;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public int chall_handler(String nickname) {
		try {
			writer.write("CHALL " + myUsername + " " + nickname); 
			writer.newLine(); 
			writer.flush();
			return Integer.parseInt(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static String codetoString(int code) {
		switch(code) {
			case 9: 
				return "Operazione non valida";
			case 10:
				return "Avvenuta Registrazione";
			case 11:
				return "Nickname già presente";
			case 12:
				return "Login ok";
			case 13:
				return "Password errata";
			case 14:
				return "Utente inesistente";
			case 15:
				return "Utente già collegato";
			case 16:
				return "Lougout ok";
			case 17: 
				return "Già nella lista amici";
			case 18:
				return "Ora siete amici";
			case 19: 
				return "Non siete amici";
			case 20:
				return "Avvenuta rimozione amicizia";
			case 21:
				return "Invio richiesta sfida...";
			case 22:
				return "Utente non online";
			default:
				return "Codice non riconosciuto";
		}			
	}
}
