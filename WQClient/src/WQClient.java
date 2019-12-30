import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
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
	private static FXMLLoader loader;
	private static Stage stage;
	private BufferedWriter writer;
	private BufferedReader reader;
	private String user;
	private JSONParser parser = new JSONParser();
	private int UDPport;
	private WQNotify thnotify;
	private RegisterLoginController logincontroller;
	private MainViewController maincontroller;
	private GameViewController gamecontroller;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		primaryStage.setTitle("Word Quizzle");
		gotoLogin();
		primaryStage.show();
	}
	
	public void gotoLogin() {
		try {
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
        	logincontroller = loader.getController();
            logincontroller.setClient(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void gotoMain() {
		try {
			loader = new FXMLLoader();
			loader.setLocation(Paths.get("src/views/MainView.fxml").toUri().toURL());
			Parent layoutmain = loader.load();
			Scene scene = stage.getScene();
			if (scene == null) {
        		scene = new Scene(layoutmain);
            	stage.setScene(scene);
        	} else {
        		stage.getScene().setRoot(layoutmain);
        	}
			stage.sizeToScene();
        	maincontroller = loader.getController();
            maincontroller.setClient(this);
            maincontroller.setPoints(points_handler());
            maincontroller.setUsername(user);
            maincontroller.populateList(list_handler());
            maincontroller.setNotifyTabInvisible();
            thnotify.setController(maincontroller);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void gotoGame() {
		try {
			loader = new FXMLLoader();
			loader.setLocation(Paths.get("src/views/GameView.fxml").toUri().toURL());
			Parent layoutmain = loader.load();
			Scene scene = stage.getScene();
			if (scene == null) {
        		scene = new Scene(layoutmain);
            	stage.setScene(scene);
        	} else {
        		stage.getScene().setRoot(layoutmain);
        	}
			stage.sizeToScene();
        	gamecontroller = loader.getController();
            gamecontroller.setClient(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		launch();
	}
	
	public int login_handler(String username, String password) {
		try {
			user = username;
			c_socket = new Socket("localhost", 6790);
			//creo porta e processo per le notifiche
			UDPport = (int) ((Math.random() * ((65535 - 1024) + 1)) + 1024);
			thnotify = new WQNotify(UDPport, this);
			thnotify.start();
			//scrivo tramite tcp le informazioni di login
			writer = new BufferedWriter(new OutputStreamWriter(c_socket.getOutputStream()));
			writer.write("LOGIN " + username + " " + password + " " + c_socket.getInetAddress().getHostAddress() + " " + UDPport); 
			writer.newLine(); 
			writer.flush();
			reader = new BufferedReader(new InputStreamReader(c_socket.getInputStream()));
			int err = Integer.parseInt(reader.readLine());
			//nel caso in cui il login è errato pulisco
			if (err != 12) {
				c_socket.close();
				if (thnotify.isAlive()) thnotify.interrupt();
				writer.close();
				reader.close();
				return err;
			} else {
				return err;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int logout_handler() {
		try {
			writer.write("LOGOUT " + user); 
			writer.newLine(); 
			writer.flush();
			if (thnotify.isAlive()) thnotify.interrupt();
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
			String jsonfile = reader.readLine();
			System.out.println(jsonfile);
			JSONArray jsonOutArray = null;
			ArrayList<String> out = new ArrayList<>();
			try {
				System.out.println(jsonfile);
				jsonOutArray = (JSONArray) parser.parse(jsonfile);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(int i = 0; i<jsonOutArray.size(); i++) {
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
			String jsonfile = reader.readLine();
			System.out.println(jsonfile);
			JSONArray jsonOutArray = null;
			ArrayList<String> out = new ArrayList<>();
			try {
				System.out.println(jsonfile);
				jsonOutArray = (JSONArray) parser.parse(jsonfile);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(int i = 0; i<jsonOutArray.size(); i++) {
				JSONObject usr = (JSONObject) jsonOutArray.get(i);
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
		thnotify.accept();
		maincontroller.setNotifyTabInvisible();
		gotoGame();
	}
	
	public void decline_handler() {
		thnotify.decline();
		maincontroller.setNotifyTabInvisible();
	}
	
	public static String codetoString(int code) {
		switch(code) {
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
				return "Non eravate amici";
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
