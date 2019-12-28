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
import java.util.HashMap;
import java.util.Iterator;
import java.lang.reflect.Type;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class WQDatabase extends RemoteServer implements RegistrationInterface{
	
	private HashMap<String, String> passwords;
	private HashMap<String, User> users;
	
	private static MessageDigest digest;
	
	private String ppath = "./passwords.json";
	private String upath = "./users.json";
	
	public WQDatabase(boolean exist) {
		if (exist == false) {
			System.out.println("Persistencies files not present");
			passwords = new HashMap<>();
			users = new HashMap<>();
		} else {
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
			passwords = (HashMap<String, String>) gson.fromJson(pjson, passwords.getClass());
			
			//users file
			String ujson = null;
			try {
				ujson = getFileStringy(upath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Type type = new TypeToken<HashMap<String, User>>(){}.getType();
	        users = gson.fromJson(ujson, type);
	        //creo la lista di amicizie reali per ogni utente
	        JSONParser parser = new JSONParser();
	        Object obj = null;
			try {
				obj = parser.parse(ujson);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        JSONObject jsonObject = (JSONObject) obj;
	        Iterator<String> iterator = jsonObject.keySet().iterator();
	        while(iterator.hasNext()) {
	            String key = (String) iterator.next();
	            JSONObject fr = (JSONObject) jsonObject.get(key);
	            JSONArray list = (JSONArray) fr.get("stringfriends");
	            //System.out.println(list);
	            Iterator<String> itr = list.iterator();
				while (itr.hasNext()) {
					System.out.println(users.get(key).addFriend(users.get(itr.next())));
				}
	        }
		}
	}
	
	public String getFileStringy(String path) throws IOException {
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
	
	//nella regsitrazione aggiungo i dati nel database degli utenti per il login poi inserisco una istanza di user nel grafo
	@Override
	public synchronized int user_registration(String nickname, String password) throws RemoteException, NullPointerException{
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (password == null) throw new NullPointerException("Invalid password (NULL)");
		if (passwords.containsKey(nickname)) return 11;
		if (passwords.put(nickname, hashMyPass(nickname + password)) == null) {
			if (!users.containsKey(nickname)) {
				users.put(nickname, new User(nickname));
				updatePJSON();
				updateUJSON();
				return 10;
			}
			else return 11;
		}
		else return 11;
	}
	
	public synchronized int user_login(String nickname, String password) throws NullPointerException{
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (password == null) throw new NullPointerException("Invalid password (NULL)");
		if (passwords.containsKey(nickname)) {
			if (passwords.get(nickname).equals(hashMyPass(nickname + password))) {
				if (users.get(nickname).isOnline() == true) return 15;
				users.get(nickname).setOnline();
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
				users.get(nickname).setOffline();
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
				if (users.get(nickname).addFriend(users.get(nickfriend)) == 0) {
					return 17;
				};
				if (users.get(nickfriend).addFriend(users.get(nickname)) == 0) {
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
				if (users.get(nickname).removeFriend(users.get(nickfriend)) == 0) {
					return 19;
				};
				if (users.get(nickfriend).removeFriend(users.get(nickname)) == 0) {
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
	
	public JSONArray friend_list(String nickname) {
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (users.containsKey(nickname)) {
			Iterator<String> itr = users.get(nickname).getFriends().iterator();
			JSONArray lista = new JSONArray();
			while(itr.hasNext()) {
				lista.add(itr.next());
			}
			return lista;
		}
		return null;
	}
	
	public int show_points(String nickname) {
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (users.containsKey(nickname)) {
			return users.get(nickname).getPoints();
		} else {
			return -1;
		}
	} 
	
	public JSONArray show_ranking(String nickname) {
		if (nickname == null) throw new NullPointerException("Invalid nickname (NULL)");
		if (users.containsKey(nickname)) {
			Iterator<User> itr = users.get(nickname).getOrderedFriends().iterator();
			JSONArray lista = new JSONArray();
			while(itr.hasNext()) {
				lista.add(itr.next().getUsername());
			}
			return lista;
		}
		return null;
	}
	
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
	
	private void updateUJSON() {
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
