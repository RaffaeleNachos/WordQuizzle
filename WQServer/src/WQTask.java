import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class WQTask implements Runnable{
	
	private WQDatabase db;
	private Socket clientTCPsocket;
	private DatagramSocket clientUDPsocket;
	
	public WQTask(WQDatabase db, Socket socket) {
		this.db=db;
		this.clientTCPsocket=socket;
	}

	@Override
	public void run() {
		System.out.println("New WQTask executed by: " + Thread.currentThread().getName());
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientTCPsocket.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientTCPsocket.getOutputStream()));
			String line = reader.readLine();
			System.out.println("WQTask read: " + line);
			//tokenizzo la stringa ricevuta dal client
			String[] tokens = line.split("\\s+");
			while(!tokens[0].equals("LOGOUT")) {
				if (tokens[0].equals("LOGIN") && tokens.length == 5) {
					int err = db.user_login(tokens[1], tokens[2], tokens[3], Integer.parseInt(tokens[4]));
					if(err==12) { //se login ok allora creo la socket per le notifiche, al logout la chiuderò
						clientUDPsocket = new DatagramSocket();
					}
					writer.write(Integer.toString(err));
					writer.newLine(); 
					writer.flush();
					//se il login non è corretto devo uscire dal thread
					if (err!=12) {
						System.out.println(Thread.currentThread().getName() + " WQTask Exiting...");
						return;
					}
				}
				else if (tokens[0].equals("ADD") && tokens.length == 3) {
					writer.write(Integer.toString(db.add_friend(tokens[1], tokens[2])));
					writer.newLine();
					writer.flush();
				}
				else if (tokens[0].equals("REMOVE") && tokens.length == 3) {
					writer.write(Integer.toString(db.remove_friend(tokens[1], tokens[2])));
					writer.newLine(); 
					writer.flush();
				}
				else if (tokens[0].equals("POINTS") && tokens.length == 2) {
					writer.write(Integer.toString(db.show_points(tokens[1])));
					writer.newLine(); 
					writer.flush();
				}
				else if (tokens[0].equals("LIST") && tokens.length == 2) {
					writer.write(db.friend_list(tokens[1]).toJSONString());
					writer.newLine();
					writer.flush();
				}
				else if (tokens[0].equals("RANK") && tokens.length == 2) {
					writer.write(db.show_ranking(tokens[1]).toJSONString());
					writer.newLine();
					writer.flush();
				}
				//messaggio ricevuto dallo sfidante
				else if (tokens[0].equals("CHALL") && tokens.length == 3) {
					//creo la randomicamente porta TCP per la sfida (thread con selector)
					int TCPport = (int) ((Math.random() * ((65535 - 1024) + 1)) + 1024);
					//faccio partire il thread della challenge, così da essere già pronto in caso di accettazione
					WQChallenge wqc = new WQChallenge(TCPport, db);
					wqc.start();
					//chiamata al database che si occuperà di mandare sulla socket UDP le notifiche di richiesta sfida 
					//il messaggio conterrà l'username dello sfidante e la porta TCP del server dove avverrà la sfida
					int err = db.challenge(tokens[1], tokens[2], clientUDPsocket, TCPport);
					//se c'è stato qualche problema allora mi occupo di chiudere il thread della challenge e mandare il messaggio di errore al client su TCP
					if (err!=21 && wqc.isAlive()) {
						if (wqc.isAlive()) wqc.interrupt();
						writer.write(Integer.toString(err));
						writer.newLine();
						writer.flush();
					} else {
						//altrimenti mando messaggio di richiesta inviata al client
						writer.write(Integer.toString(err));
						writer.newLine();
						writer.flush();
						byte[] buffer = new byte[1024];
						DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
						//imposto TIMER T1 PER ACCETAZIONE SFIDA 30 secondi (se non mi arriva la risposta entro 30 secondi...)
						clientUDPsocket.setSoTimeout(30000);
						try {
							clientUDPsocket.receive(receivedPacket);
						} catch (SocketTimeoutException e) {
							System.out.println("WQTask clientUDPsocket timeout");
							//se dovesse scadere il timer T1
							//allo sfidante mando "DECLINED"
							db.challengedeclined(tokens[1], clientUDPsocket);
							//allo sfidato mando "TIMEOUT username" per eliminare la notifica
							db.timeout(tokens[1], tokens[2], clientUDPsocket);
							//unico metodo per fermare il thread bloccato sulla select
							if (wqc.isAlive()) wqc.interrupt();
						}
						//leggo il datagramma ricevuto
						String byteToString = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
						System.out.println("WQTask received on UDP conn: " + byteToString);
						String [] tokens2 = byteToString.split("\\s+");
						if (tokens2[0].equals("ACCEPT")) {
							//mando allo sfidante la porta del server dove avverrà la sfida (al client dello sfidato l'ho inviata tempo precedente)
							db.challengeaccepted(tokens[1], clientUDPsocket, TCPport);
						}
						if (tokens2[0].equals("DECLINE")) {
							//ho ricevuto dallo sfidato decline quindi invio allo sfidante DECLINED
							db.challengedeclined(tokens[1], clientUDPsocket);
							//mi occupo della chiusura del thread della challenge preparato
							if (wqc.isAlive()) wqc.interrupt();
						}
					}
				}
				else {
					writer.write(Integer.toString(9));
					writer.newLine(); 
					writer.flush();
				}
				line = reader.readLine();
				System.out.println("WQTask read: " + line);
				tokens = line.split("\\s+");
			}
			writer.write(Integer.toString(db.user_logout(tokens[1])));
			writer.newLine(); 
			writer.flush();
			//al logout chiudo le socket
			clientUDPsocket.close();
			clientTCPsocket.close();
			System.out.println(Thread.currentThread().getName() + " WQTask Exiting...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
