import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class WQChallenge extends Thread{
	
	//porta del server a cui si collegheranno i due client
	private int port;
	private Selector selector;
	private ServerSocket serverSocket;
	private ServerSocketChannel serverChannel;
	//array per le parole in italiano contenute in un file json
	private JSONArray jarr;
	private JSONParser parser;
	private ArrayList<String> selectedWords;
	private ArrayList<String> translatedWords;
	//numero di parole
	private static int K = 5;
	//riferimento al database per poter aggiornare i punti
	private WQDatabase db;
	//variabile atomica in memoria principale che si occupa di capire quando entrambi i client hanno finito
	private volatile AtomicInteger endusers;
	//variabile atomica che mi gestisce il timer della sfida
	public volatile AtomicInteger timeover;
	//punti da assegnare
	private static int correctPoint = 3;
	private static int wrongPoint = -1;
	private static int bonusPoint = 5;
	//dove mi salvo le chiavi per il recap finale
	private ArrayList<SelectionKey> finalkeys;
	
	public WQChallenge(int port, WQDatabase db) {
		this.port = port;
		this.db = db;
		finalkeys = new ArrayList<>();
		endusers = new AtomicInteger(0);
		timeover = new AtomicInteger(0);
		selectedWords = new ArrayList<>();
		parser = new JSONParser();
	}
	
	public void run() {
		String strjson = null;
		try {
			//leggo il file json delle parole
			strjson = WQDatabase.getFileStringy("./words.json");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			jarr = (JSONArray) parser.parse(strjson);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		//scelgo le K parole
		for (int i = 0; i < K; i++) {
			//System.out.println("size: " + (int) (Math.random() * ((jarr.size()))) + " prima parola " + (String) jarr.get(0) + " parola scelta " + (String) jarr.get((int) (Math.random() * ((jarr.size())))));
			selectedWords.add((String) jarr.get((int) ((Math.random() * ((jarr.size()))))));
		}
		System.out.println("Parole italiane scelte: " + selectedWords.toString());
		try {
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
		} catch (IOException ex) { 
			ex.printStackTrace();
		}
		//finchè entrambi non hanno finito o il thread non è interrotto
		while (endusers.get() != 2 && !Thread.currentThread().isInterrupted()) { 
			try {
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
						finalkeys.add(clientkey);
						//appena qualcuno si collega mi occupo di interrogare le API e faccio partire il timer
						if (translatedWords == null) {
							translatedWords = new ArrayList<>();
							translateWords();
							System.out.println("Traduzioni: " + translatedWords.toString());
							//timer della sfida
							new WQChallTimer(30, this);
						}
					}
					else if (key.isWritable()) {
						System.out.println("Server | pronta una chiave in scrittura");
						SocketChannel client = (SocketChannel) key.channel();
						WQWord myWord = (WQWord) key.attachment();
						//se la parola è null vuol dire che è stata scritta tutta e se ne può inviare una nuova
						if (myWord.getWord() == null) {
							//mando nuova parola e double che indica la percentuale della progressbar da settare
							//caso in cui entrambi i giocatori stanno ancora giocando e il timer non è scaduto
							if (myWord.getIndex() < K && endusers.get() != 1 && timeover.get()==0) {
								//percentuale che invia il client al server per la progressbar
								double perc = (double) myWord.getIndex() * (double) ( 1.0 / (double) (K - 1));
								System.out.println(perc);
								//metto la parola in italiano seguita dalla percentuale da assegnare alla progressbar
								myWord.setWord(selectedWords.get(myWord.getIndex()) + " " + perc);
							}
							//nel caso in cui uno dei due termina (enduser è 1) oppure le parole sono terminate oppure il timer è scaduto
							else {
								//System.out.println("mando chend");
								//recap finale per inviare le statistiche finali + hai vinto/hai perso
								String firsttoken = null;
								if (timeover.get()==1) {
									firsttoken = "TIMEOUT ";
								} else {
									firsttoken = "CHEND ";
								}
								WQWord A = (WQWord) finalkeys.get(0).attachment();
								WQWord B = (WQWord) finalkeys.get(1).attachment();
								if (A!=null) System.out.println(A.stat.username);
								if (B!=null) System.out.println(B.stat.username);
								//se i punteggi sono uguali parità
								if (A.stat.chPoints == B.stat.chPoints) {
									myWord.setWord(firsttoken + myWord.stat.chPoints + " " + myWord.stat.correctWords + " " + myWord.stat.wrongWords + " DRAW");
								}
								else if (A.stat.chPoints > B.stat.chPoints && myWord.stat.username.equals(A.stat.username)) {
									//System.out.println("ha vinto " + A.stat.username + " con punti " + A.stat.chPoints + " contro " + B.stat.username + " con punti " + B.stat.chPoints);
									myWord.setWord(firsttoken + myWord.stat.chPoints + " " + myWord.stat.correctWords + " " + myWord.stat.wrongWords + " WIN");
									//aggiungo punti bonus
									addPointsToWinner(myWord.stat.username);
								}
								else if (B.stat.chPoints > A.stat.chPoints && myWord.stat.username.equals(B.stat.username)) {
									//System.out.println("ha vinto " + B.stat.username + " con punti " + B.stat.chPoints + " contro " + A.stat.username + " con punti " + A.stat.chPoints);
									myWord.setWord(firsttoken + myWord.stat.chPoints + " " + myWord.stat.correctWords + " " + myWord.stat.wrongWords + " WIN");
									addPointsToWinner(myWord.stat.username);
								}
								else myWord.setWord(firsttoken + myWord.stat.chPoints + " " + myWord.stat.correctWords + " " + myWord.stat.wrongWords + " LOSE");
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
							//setto la restante parola e non cambio interesse della chiave
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
								//tokenizzo la stringa di risposta e controllo la correttezza della traduzione
								String token[] = read.split("\\s+");
								//se è la prima volta che manda la parola mi salvo l'username nell'oggetto statistiche (associato alla chiave)
								//per poter gestire la classifica finale
								if (myWord.stat.username == null) myWord.stat.username = token[0];
								//parolainglese - username - parolaitaliana - classe per le statistiche finali
								//conto il punteggio solo se l'altro utente non ha finito oppure il timer non è scaduto
								if (endusers.get()==0 && timeover.get()==0 ) checkWords(token[1], token[0], translatedWords.get(myWord.getIndex()), myWord.stat);
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
		db.updateUJSON();
		System.out.println("Challenge Thread Shutdown...");
	}
	
	public class Statistics{
		public String username;
		public int chPoints;
		public int correctWords;
		public int wrongWords;
		
		public Statistics() {
			chPoints = 0;
			correctWords = 0;
			wrongWords = 0;
			username = null;
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
	
	public void addPointsToWinner(String username) {
		WQUser u = db.getUser(username);
		if (u!=null) {
			u.points = u.points + bonusPoint;
		}
	}
	
	public void checkWords(String word, String username, String cTransl, Statistics stats) {
		//costo O(1) per avere l'istanza dell'utente per aggiornare il punteggio direttamente nel database
		WQUser u = db.getUser(username);
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
