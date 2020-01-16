import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class WQChallenge extends Thread{
	
	private int port;
	private Selector selector;
	private ServerSocket serverSocket;
	private ServerSocketChannel serverChannel;
	private JSONArray jarr;
	private ArrayList<String> selectedWords;
	private ArrayList<String> translatedWords;
	private static int K = 5;
	private WQDatabase db;
	private JSONParser parser;
	private volatile AtomicInteger endusers;
	public volatile AtomicInteger firealarm;
	private static int correctPoint = 3;
	private static int wrongPoint = -1;
	
	public WQChallenge(int port, WQDatabase db) {
		this.port = port;
		this.db = db;
		endusers = new AtomicInteger(0);
		firealarm = new AtomicInteger(0);
		selectedWords = new ArrayList<>();
		translatedWords = new ArrayList<>();
		parser = new JSONParser();
		String strjson = null;
		try {
			strjson = WQDatabase.getFileStringy("./words.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONParser parser = new JSONParser();
		try {
			jarr = (JSONArray) parser.parse(strjson);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < K; i++) {
			//System.out.println(jarr.toString());
			//System.out.println("size: " + (int) (Math.random() * ((jarr.size()))) + " prima parola " + (String) jarr.get(0) + " parola scelta " + (String) jarr.get((int) (Math.random() * ((jarr.size())))));
			selectedWords.add((String) jarr.get((int) ((Math.random() * ((jarr.size()))))));
		}
	}
	
	public void run() {
		try {
			System.out.println("Parole: " + selectedWords.toString());
			//creo ServerSocketChannel per poterlo settare in modalità non bloccante
			//il SocketChannel è creato implicitamente
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			//ottengo la socket del server associata al channel
			serverSocket = serverChannel.socket();
			//bindo la socket del server sulla porta *port*
			//come indirizzo usa una wildcard standard che mi indica tutti gli indirizzi IP della macchina
			InetSocketAddress address = new InetSocketAddress(port);
			serverSocket.bind(address);
			//creo il mio selector e lo associo al channel
			//per ora sto in ascolto solo sulla key (della socket del server) che identifica la accept
			selector = Selector.open(); 
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			translateWords();
			System.out.println("Traduzioni: " + translatedWords.toString());
		} catch (IOException ex) { 
			ex.printStackTrace();
		}
		while (firealarm.get() == 0 && endusers.get() != 2 && !Thread.currentThread().isInterrupted()) { 
			try {
				//System.out.println(selector.keys());
				//System.out.println(selector.selectedKeys());
				System.out.println("Server | Aspetto sulla select");
				selector.select();
			} catch (IOException ex) {
				ex.printStackTrace(); 
				break;
			}
			//selectionkey identifica i tipi delle operazioni da gestire
			//readyKeys identifica i canali pronti
			Set <SelectionKey> readyKeys = selector.selectedKeys();
			Iterator <SelectionKey> iterator = readyKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = (SelectionKey) iterator.next();
				//esplicita rimozione chiave dal SelectedSet
				iterator.remove();
				try {
					if (key.isAcceptable()) {
						System.out.println("Server | pronta una chiave di accettazione");
						ServerSocketChannel server = (ServerSocketChannel) key.channel(); 
						SocketChannel client = server.accept(); 
						System.out.println("Connessione accettata verso: " + client); 
						client.configureBlocking(false);
						//creo una nuova chiave associata alla socket client
						SelectionKey clientkey = client.register(selector, SelectionKey.OP_WRITE, new WQWord(null, 0));
					}
					else if (key.isWritable()) {
						System.out.println("Server | pronta una chiave in scrittura");
						SocketChannel client = (SocketChannel) key.channel();
						WQWord myWord = (WQWord) key.attachment();
						if (myWord.getWord() == null) {
							//mando nuova parola e double che indica la percentuale della progressbar da settare
							if (myWord.getIndex() < K) {
								//TODO aggiungere la parola iniziale e non partire da zero!
								double perc = ((double) myWord.getIndex() / (double) K);
								System.out.println(perc);
								myWord.setWord(selectedWords.get(myWord.getIndex()) + " " + perc);
							} else {
								System.out.println("mando chend");
								myWord.setWord("CHEND " + myWord.stat.chPoints + " " + myWord.stat.correctWords + " " + myWord.stat.wrongWords);
								endusers.incrementAndGet();
							}
						}
						ByteBuffer end = ByteBuffer.wrap(myWord.getWord().getBytes());
						int bWrite = client.write(end);
						//se ho scritto tutto rimetto la chiave in read
						if (bWrite == myWord.getWord().length()) {
							myWord.setWord(null);
							key.attach(myWord);
							key.interestOps(SelectionKey.OP_READ);
							System.out.println("Server | key impostata su read");
						}
						//se il client chiude la socket o termina la write restituisce -1
						else if (bWrite == -1) {
							key.cancel();
							key.channel().close();
							System.out.println("Server | socket chiusa dal client");
						}
						//se non ha scritto tutto
						else {
							System.out.println("Server | scrivo: " + bWrite + " bytes");
							//la flip server per la decodifica
							end.flip();
							myWord.setWord(StandardCharsets.UTF_8.decode(end).toString());
							key.attach(myWord);
						}
					}
					else if (key.isReadable()) {
						System.out.println("Server | pronta una chiave in lettura");
						SocketChannel client = (SocketChannel) key.channel();
						WQWord myWord = (WQWord) key.attachment();
						String read = "";
						if (myWord.getWord() != null) read = myWord.getWord();
						ByteBuffer input = ByteBuffer.allocate(1024);
						input.clear();
						int bRead = client.read(input);
						//se il buffer è pieno ritorna a leggere al ciclo dopo
						if (bRead == 1024){
							System.out.println("Server | leggo: " + bRead + " bytes");
							input.flip();
							read = read + StandardCharsets.UTF_8.decode(input).toString();
							myWord.setWord(read);
							key.attach(myWord);
						}
						//se ho letto meno della dimensione ha letto tutto
						else if (bRead < 1024) {
							System.out.println("Server | leggo: " + bRead + " bytes");
							input.flip();
							read = read + StandardCharsets.UTF_8.decode(input).toString();
							System.out.println(read);
							if (read.equals("CHEXITED")) {
								endusers.incrementAndGet();
								key.cancel();
								key.channel().close();
							} else {
								//tokenizzo la stringa di risposta e avvio un thread che mi controlla la correttezza della traduzione
								String token[] = read.split("\\s+");
								checkWords(token[1], token[0], translatedWords.get(myWord.getIndex()), myWord.stat);
								myWord.setWord(null);
								myWord.incIndex();
								key.attach(myWord);
								key.interestOps(SelectionKey.OP_WRITE);
								System.out.println("Server | key impostata su write");
							}
						}
						//se il client chiude la socket o termina, la read restituisce -1
						else if (bRead == -1) {
							key.cancel();
							key.channel().close();
							System.out.println("Server | socket chiusa dal client");
						}
					}
				} catch (IOException ex) {
					//in caso di exception cancello la chiave e chiudo il channel associato alla chiave
					key.cancel();
					try {
						key.channel().close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println("Challenge Thread Shutdown...");
	}
	
	public class Statistics{
		public int chPoints;
		public int correctWords;
		public int wrongWords;
		public int notAnsweredWords;
		
		public Statistics() {
			chPoints = 0;
			correctWords = 0;
			wrongWords = 0;
			notAnsweredWords = 0;
		}
		
	}
	
	public class WQWord {
		
		String word;
		int numWord;
		Statistics stat;
		
		public WQWord(String word, int numWord) {
			this.word = word;
			this.numWord = numWord;
			this.stat = new Statistics();
		}
		
		public String getWord() {
			return word;
		}
		
		public int getIndex() {
			return numWord;
		}
		
		public void setWord(String neww) {
			word = neww;
		}
		
		public void incIndex() {
			numWord ++;
		}
	}

	//chiedere se conviene chiamare ogni volta il server delle API oppure tutto insieme
	//nel mio caso ho un thread che si occupa del controllo della parola sarebbe meglio farlo direttamente in questo thread
	public void translateWords() throws IOException {
		//traduzione k parole
		for (int i = 0; i < selectedWords.size(); i++) {
			URL url = new URL("https://api.mymemory.translated.net/get?q=" + selectedWords.get(i) + "&langpair=it|en");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) 
            {
                throw new RuntimeException("Failed, error code " + conn.getResponseCode());
            }
 
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String apiOutput = br.readLine();
            try {
				JSONObject bigobj = (JSONObject) parser.parse(apiOutput);
				JSONObject smallobj = (JSONObject) bigobj.get("responseData");
				//toLowercase perchè spesso la traduzione ha delle lettere maiuscole e può dare problemi per il controllo di correttezza
				translatedWords.add(i, (String) smallobj.get("translatedText").toString().toLowerCase());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            conn.disconnect();
		}
	}
	
	public void checkWords(String word, String username, String cTransl, Statistics stats) {
		//costo O(1) per avere l'istanza dell'utente per aggiornare il punteggio direttamente nel database
		User u = db.getUser(username);
		if (u!=null) {
			if(word.equals(cTransl)) {
				u.points = u.points + correctPoint;
				stats.chPoints = stats.chPoints + correctPoint;
				stats.correctWords++;
			} else {
				u.points = u.points + wrongPoint;
				stats.chPoints = stats.chPoints + wrongPoint;
				stats.wrongWords++;
			}
		}
	}
}
