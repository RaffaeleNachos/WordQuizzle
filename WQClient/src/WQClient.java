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
	private Socket c_socket;
	private DatagramSocket mys;
	private static FXMLLoader loader;
	private static Stage stage;
	private BufferedWriter writer;
	private BufferedReader reader;
	public String user;
	private JSONParser parser = new JSONParser();
	//porta per le notifiche
	private int UDPport;
	//porta per la sfida
	public int TCPport;
	private WQNotify thnotify;
	private RegisterLoginController logincontroller;
	private MainViewController maincontroller;
	private GameViewController gamecontroller;
	private static String serverIA = "localhost";
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		primaryStage.setTitle("Word Quizzle");
		gotoLogin();
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		Registry r;
		//RemoteObject: spazio di indirizzamento diverso, gira su un'altra JVM
		Remote remoteObj;
		try {
			//getRegistry(String host, int port) nel caso di host remoti
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
		launch();
	}
	
	//meotodo che mi permette di tornare alla finestra di login/registrazione
	public void gotoLogin() {
		try {
			//carica il file di stile della schermata
			loader = new FXMLLoader();
			loader.setLocation(Paths.get("src/views/QuizzleLogin.fxml").toUri().toURL());
			Parent layoutmain = loader.load();
			Scene scene = stage.getScene();
			if (scene == null) {
        		scene = new Scene(layoutmain);
            	stage.setScene(scene);
        	} else {
        		stage.getScene().setRoot(layoutmain);
        	}
			stage.sizeToScene();
			//a questo punto setta il controller dei vari componenti della gui -> RegisterLoginController
        	logincontroller = loader.getController();
        	//passo riferimento di questa classe per accedere al registry per la registrazione e per accedere ai metodi handler definiti qui
            logincontroller.setClient(this);
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
            maincontroller.setClient(this);
            //setto i punti e username nella tab Your. Info.
            maincontroller.setPoints(points_handler());
            maincontroller.setUsername(user);
            //popolo la lista delle amicizie
            maincontroller.populateList(list_handler());
            //la gui ha di default la vista della tab delle notifiche che però deve essere vista solo quando c'è 
            //una effettiva notifica. per questo appena entrato la posto in modalità invisibile (diventerà visibile alla prima notifica)
            maincontroller.setNotifyTabInvisible();
            //qui appunto passo il riferimento al controller al thread delle notifiche perchè sarà lui ad occuparsi di rendere visibile la notification Tab.
            thnotify.setController(maincontroller);
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
            gamecontroller.setClient(this);
            //passo come parametro la porta della sfida e l'indirizzo del server
            gamecontroller.setSocket(TCPport, c_socket.getInetAddress());
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
			c_socket = new Socket(serverIA, 6790);
			//creo porta e processo per le notifiche
			UDPport = (int) ((Math.random() * ((65535 - 1024) + 1)) + 1024);
			//scrivo tramite tcp le informazioni di login
			writer = new BufferedWriter(new OutputStreamWriter(c_socket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(c_socket.getInputStream()));
			writer.write("LOGIN " + username + " " + password + " " + c_socket.getInetAddress().getHostAddress() + " " + UDPport); 
			writer.newLine(); 
			writer.flush();
			err = Integer.parseInt(reader.readLine());
			//nel caso in cui il login è errato cancello la socket creata
			if (err != 12) {
				System.out.println("Cleaning...");
				c_socket.close();
				writer.close();
				reader.close();
				return err;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		//se il login è ok a questo punto setto user che è una variabile che mi serve per tutte quelle operazioni che la gui 
		//fa in automatico come richiedere il punteggio o la lista degli amici e faccio partire il thread delle notifiche
		try {
			mys = new DatagramSocket(UDPport);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		thnotify = new WQNotify(this, mys);
		thnotify.start();
		user = username;
		return err;
	}
	
	public int logout_handler() {
		try {
			writer.write("LOGOUT " + user); 
			writer.newLine(); 
			writer.flush();
			if (thnotify.isAlive()) mys.close();
			return Integer.parseInt(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int add_handler(String nickname) {
		try {
			writer.write("ADD " + user + " " + nickname); 
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
			writer.write("REMOVE " + user + " " + nickname); 
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
			writer.write("POINTS " + user); 
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
			writer.write("LIST " + user); 
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
			writer.write("RANK " + user); 
			writer.newLine(); 
			writer.flush();
			//risposta
			String jsonfile = reader.readLine();
			//System.out.println(jsonfile);
			JSONArray jsonOutArray = null;
			ArrayList<String> out = new ArrayList<>();
			try {
				System.out.println(jsonfile);
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
			writer.write("CHALL " + user + " " + nickname); 
			writer.newLine(); 
			writer.flush();
			return Integer.parseInt(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public void accept_handler() {
		//UDP accettazione
		thnotify.accept();
		//setto invisibile la tab notifiche
		maincontroller.setNotifyTabInvisible();
		//mando la GUI in modalità GIOCO per lo sfidato
		gotoGame();
	}
	
	public void decline_handler() {
		//utilizzo UDP per declinare l'offerta di sfida
		thnotify.decline();
		//setto invisibile la tab notifiche
		maincontroller.setNotifyTabInvisible();
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
