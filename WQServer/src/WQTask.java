import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class WQTask implements Runnable{
	
	private WQDatabase db;
	private Socket socket;
	
	public WQTask(WQDatabase db, Socket socket) {
		this.db=db;
		this.socket=socket;
	}

	@Override
	public void run() {
		System.out.println("Nuovo client eseguito da " + Thread.currentThread().getName());
		DatagramSocket clientsocket = null;
		try {
			clientsocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			String line = reader.readLine();
			System.out.println("Server ho letto " + line);
			//tokenizzo la stringa ricevuta dal client
			String[] tokens = line.split("\\s+");
			while(!tokens[0].equals("LOGOUT")) {
				if (tokens[0].equals("LOGIN") && tokens.length == 5) {
					writer.write(Integer.toString(db.user_login(tokens[1], tokens[2], tokens[3], Integer.parseInt(tokens[4]))));
					writer.newLine(); 
					writer.flush();
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
				//need to be checked
				else if (tokens[0].equals("CHALL")) {
					int TCPport = (int) ((Math.random() * ((65535 - 1024) + 1)) + 1024);
					WQChallenge wqc = new WQChallenge(TCPport, db);
					wqc.start();
					int err = db.challenge(tokens[1], tokens[2], clientsocket, TCPport);
					if (err!=21 && wqc.isAlive()) {
						wqc.firealarm.incrementAndGet();
						writer.write(Integer.toString(err));
						writer.newLine();
						writer.flush();
					} else {
						writer.write(Integer.toString(err));
						writer.newLine();
						writer.flush();
						byte[] buffer = new byte[1024];
						DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
						//TIMER T1 PER ACCETAZIONE SFIDA 30 secondi
						clientsocket.setSoTimeout(30000);
						try {
							clientsocket.receive(receivedPacket);
						} catch (SocketTimeoutException e) {
							//allo sfidante mando non accettata
							db.challengedeclined(tokens[1], clientsocket);
							//unico metodo per fermare il thread bloccato sulla select
							if (wqc.isAlive()) wqc.interrupt();
							//allo sfidato mando timeout per eliminare la notifica
							db.timeout(tokens[2], clientsocket);
						}
						String byteToString = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
						String [] tokens2 = byteToString.split("\\s+");
						System.out.println(byteToString);
						if (tokens2[0].equals("ACCEPT")) {
							db.challengeaccepted(tokens[1], clientsocket, TCPport);
						}
						if (tokens2[0].equals("DECLINE")) {
							db.challengedeclined(tokens[1], clientsocket);
							if (wqc.isAlive()) wqc.firealarm.incrementAndGet();
							System.out.println("killed");
						}
					}
				}
				else {
					writer.write(Integer.toString(9));
					writer.newLine(); 
					writer.flush();
				}
				line = reader.readLine();
				System.out.println("Server ho letto " + line);
				tokens = line.split("\\s+");
			}
			writer.write(Integer.toString(db.user_logout(tokens[1])));
			writer.newLine(); 
			writer.flush();
			System.out.println("Thread Exiting...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
