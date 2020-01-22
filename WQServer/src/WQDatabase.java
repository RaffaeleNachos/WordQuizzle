import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class WQDatabase extends RemoteServer implements RegistrationInterface{
	
	/**
	 * Serial UID per serializzazione RMI
	 */
	private static final long serialVersionUID = -6559344702339809559L;
	
	private HashMap<String, String> passwords;
	private HashMap<String, WQUser> users;
	
	//necessario per l'hashing
	private static MessageDigest digest;
	
	private static String ppath = "./passwords.json";
	private static String upath = "./users.json";
	
	public WQDatabase(boolean exist) {
		//se non esistono i file di persistenza
		if (exist == false) {
			System.out.println("Persistencies files not present");
			passwords = new HashMap<>();
			users = new HashMap<>();
		} else { //se i file di persistenza 
			System.out.println("Persistencies files found");
			passwords = new HashMap<>();
			users = new HashMap<>();
			Gson gson = new Gson();
			//passwords file
			String pjson = null;
			try {
				pjson = getFileStringy(ppath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Type typea = new TypeToken<HashMap<String, String>>(){}.getType();
			passwords = gson.fromJson(pjson, typea);
			
			//users file
			String ujson = null;
			try {
				ujson = getFileStringy(upath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Type typeb = new TypeToken<HashMap<String, WQUser>>(){}.getType();
	        users = gson.fromJson(ujson, typeb);
		}
	}
	
	//nella regsitrazione aggiungo i dati nel database degli utenti per il login poi inserisco una istanza di user nel grafo
	@Override
	public synchronized int user_registration(String nickname, String password) throws RemoteException, NullPointerException{
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (password == null) throw new NullPointerException("Invalid password (NULL)");
		if (nickname.equals("") || password.equals("")) return 9;
		if (passwords.containsKey(nickname)) return 11;
		if (passwords.put(nickname, hashMyPass(nickname + password)) == null) {
			if (!users.containsKey(nickname)) {
				users.put(nickname, new WQUser(nickname));
				//aggiorno entrambi i file (db utenti e password) con la nuova registrazione
				updatePJSON();
				updateUJSON();
				return 10;
			}
			else return 11;
		}
		else return 11;
	}
	
	public synchronized int user_login(String nickname, String password, String ineta, int port) throws NullPointerException{
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (password == null) throw new NullPointerException("Invalid password (NULL)");
		if (passwords.containsKey(nickname)) {
			if (passwords.get(nickname).equals(hashMyPass(nickname + password))) {
				if (users.get(nickname).online == true) return 15;
				users.get(nickname).online = true;
				users.get(nickname).setIA(ineta);
				users.get(nickname).setPort(port);
				return 12;
			}
			else return 13;
		} else {
			return 14;
		}
	}
	
	public synchronized int user_logout(String nickname) throws NullPointerException{
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (users.containsKey(nickname)) {
				users.get(nickname).online = false;
				return 16;
		} else {
			return 14;
		}
	}
	
	public synchronized int add_friend(String nickname, String nickfriend) throws NullPointerException{
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (nickfriend == null) throw new NullPointerException("Invalid friend's nickname (NULL)");
		if (users.containsKey(nickname)) {
			if (users.containsKey(nickfriend)) {
				if (users.get(nickname).addFriend(nickfriend) == 0) {
					return 17;
				};
				if (users.get(nickfriend).addFriend(nickname) == 0) {
					return 17;
				};
				updateUJSON();
				return 18;
			} else {
				return 14;
			}
		} else {
			return 14;
		}
	}
	
	public synchronized int remove_friend(String nickname, String nickfriend) throws NullPointerException{
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (nickfriend == null) throw new NullPointerException("Invalid friend's nickname (NULL)");
		if (users.containsKey(nickname)) {
			if (users.containsKey(nickfriend)) {
				if (users.get(nickname).removeFriend(nickfriend) == 0) {
					return 19;
				};
				if (users.get(nickfriend).removeFriend(nickname) == 0) {
					return 19;
				};
				updateUJSON();
				return 20;
			} else {
				return 14;
			}
		} else {
			return 14;
		}
	}
	
	public int show_points(String nickname) {
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (users.containsKey(nickname)) {
			return users.get(nickname).points;
		} else {
			return -1;
		}
	}
	
	public JSONArray friend_list(String nickname) {
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (users.containsKey(nickname)) {
			//nel caso della sola lista di amici (senza i punti) si potrebbe pensare di potersi risparmiare una iterazione sulla lista 
			//di amici poichè essa è già pronta sul file, ma il parsing del file, la ricerca della chiave user e la restituzione del JSONArray è 
			//sicuramente più costoso di una iterazione O(n) sulla lista di amici
			Iterator<String> itr = users.get(nickname).getFriends().iterator();
			JSONArray lista = new JSONArray();
			while(itr.hasNext()) {
				lista.add(itr.next());
			}
			return lista;
		}
		return null;
	}
	
	public JSONArray show_ranking(String nickname) {
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (users.containsKey(nickname)) {
			Iterator<String> itr = users.get(nickname).getFriends().iterator();
			ArrayList<WQUser> realRank = new ArrayList<>();
			while(itr.hasNext()) {
				//costo costante sulla get dell'istanza del vero utente nella struttura dati
				realRank.add(users.get(itr.next()));
			}
			//utilizza il compareTo definito nella classe User (ordina secondo il punteggio)
			Collections.sort(realRank);
			//a questo punto mi costruisco il JSON da inviare
			Iterator<WQUser> iter = realRank.iterator();
			JSONArray lista = new JSONArray();
			while(iter.hasNext()) {
				JSONObject usr = new JSONObject();
				WQUser x = iter.next();
				usr.put("username", x.username);
				usr.put("points", x.points);
				lista.add(usr);
			}
			return lista;
		}
		return null;
	}
	
	//metodo che mi restituisce una istanza di user, mi serve per permettere al thread della challenge di aggiungere punti al giocatore
	public WQUser getUser(String nickname) {
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (users.containsKey(nickname)) {
			return users.get(nickname);
		} else {
			return null;
		}
	}
	
	//invia su UDP allo sfidato CH A B
	public int challenge(String nickname, String nickfriend, DatagramSocket s, int chport) {
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (nickfriend == null) throw new NullPointerException("Invalid friend's nickname (NULL)");
		if (users.containsKey(nickname)) {
			if (users.containsKey(nickfriend)) {
				if (users.get(nickname).getFriends().contains(nickfriend) == true) {
					//manda la richiesta solo se è online
					if(users.get(nickfriend).online == true) {
						//UDP
						String tmp = "CH " + nickname + " " + chport;
						byte[] buffer=tmp.getBytes();
						//essendo online siamo sicuri che l'indirizzo e porta dell'amico sia corretto quindi li prendo dal database
						DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, users.get(nickfriend).getIA(), users.get(nickfriend).getPort());
						try {
							s.send(mypacket);
						} catch (IOException e) {
							e.printStackTrace();
						}
						//System.out.println("ho inviato " + tmp);
						return 21;
					}
					else return 22;
				} else {
					return 14;
				}
			} else {
				return 14;
			}
		} else {
			return 14;
		}
	}
	
	//inviati allo sfidante - sfida accettata
	public void challengeaccepted(String nickname, DatagramSocket s, int chport) {
		String tmp = "ACCEPTED " + chport;
		byte[] buffer = tmp.getBytes();
		DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, users.get(nickname).getIA(), users.get(nickname).getPort());
		try {
			s.send(mypacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println("ho inviato " + tmp);
	}
	
	//nel caso in cui la sfida non sia accetatta è scaduto il timer avviso il client dello sfidante
	public void challengedeclined(String nickname, DatagramSocket s) {
		String tmp = "DECLINED";
		byte[] buffer = tmp.getBytes();
		DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, users.get(nickname).getIA(), users.get(nickname).getPort());
		try {
			s.send(mypacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println("ho inviato " + tmp);
	}
	
	//inviati allo sfidato
	public void timeout(String nickname, String nickfriend, DatagramSocket s) {
		String tmp = "TIMEOUT " + nickname;
		byte[] buffer=tmp.getBytes();
		DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, users.get(nickfriend).getIA(), users.get(nickfriend).getPort());
		try {
			s.send(mypacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println("ho inviato " + tmp);		
	}
	
	//metodo che si occupa di hashare le password tramite l'algoritmo di hashing one-way SHA-256
	private static String hashMyPass(String password) {
		byte[] hash = null;
		try {
			//algoritmo di hashing one-way
			digest = MessageDigest.getInstance("SHA-256");
			hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		if (hash!=null) {
			//creo la stringa ottenuta dal byte array restituito dal digest
			StringBuffer hexString = new StringBuffer();
		    for (int i = 0; i < hash.length; i++) {
		    	String hex = Integer.toHexString(0xff & hash[i]);
		    	if(hex.length() == 1) hexString.append('0');
		        hexString.append(hex);
		    }
		    return hexString.toString();
		}
		else return null;
	}
	
	public static String getFileStringy(String path) throws IOException {
		//creo un channel per la lettura del file
		FileChannel inChannel = FileChannel.open(Paths.get(path), StandardOpenOption.READ);
		ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
        boolean stop = false;
        String tmp = "";
        while (!stop) {
        	int bytesRead = inChannel.read(buffer);
        	if (bytesRead==-1) {
        		stop=true;
        	}
        	//flippo il puntatore all'inizio per la lettura decodificata
        	buffer.flip();
            while (buffer.hasRemaining()) {
            	tmp = tmp + StandardCharsets.UTF_8.decode(buffer).toString();
            }
            //riflippo per scrittura
            buffer.flip();
        }
        inChannel.close();
        return tmp;
	}
	
	
	//metodo che si occuopa di aggiornare il JSON delle password (database delle password + nickname)
	public void updatePJSON() {
		Gson gson = new Gson();
		String passjson = gson.toJson(passwords);
		try {
			//creo ByteBuffer dove inserisco il mio JSONArray
			ByteBuffer buf = ByteBuffer.wrap(passjson.getBytes("UTF-8"));
			try {
				Files.deleteIfExists(Paths.get(ppath)); //cancello il file precedentemente creato se esite
				Files.createFile(Paths.get(ppath)); //creo nuovo file
			} catch(Exception e) {
				e.printStackTrace();
			}
			//creo channel per la scrittura su file tramite NIO
			FileChannel outChannel = FileChannel.open(Paths.get(ppath), StandardOpenOption.WRITE);
			
			while(buf.hasRemaining()) {
				outChannel.write(buf);
			}
			outChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//metodo che si occupa di aggiornare il JSON degli utenti (database dei dati)
	public void updateUJSON() {
		Gson gson = new Gson();
		String userjson = gson.toJson(users);
		try {
			//creo ByteBuffer dove inserisco il mio JSONArray
			ByteBuffer buf = ByteBuffer.wrap(userjson.getBytes("UTF-8"));
			try {
				Files.deleteIfExists(Paths.get(upath)); //cancello il file precedentemente creato se esite
				Files.createFile(Paths.get(upath)); //creo nuovo file
			} catch(Exception e) {
				e.printStackTrace();
			}
			//creo channel per la scrittura su file tramite NIO
			FileChannel outChannel = FileChannel.open(Paths.get(upath), StandardOpenOption.WRITE);
			
			while(buf.hasRemaining()) {
				outChannel.write(buf);
			}
			outChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
