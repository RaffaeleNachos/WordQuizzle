import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

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
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			String line = reader.readLine();
			System.out.println("Server ho letto " + line);
			String[] tokens = line.split("\\s+");
			while(!tokens[0].equals("LOGOUT")) {
				if (tokens[0].equals("LOGIN")) {
					writer.write(Integer.toString(db.user_login(tokens[1], tokens[2], tokens[3], Integer.parseInt(tokens[4]))));
					writer.newLine(); 
					writer.flush();
				}
				if (tokens[0].equals("ADD")) {
					writer.write(Integer.toString(db.add_friend(tokens[1], tokens[2])));
					writer.newLine(); 
					writer.flush();
				}
				if (tokens[0].equals("REMOVE")) {
					writer.write(Integer.toString(db.remove_friend(tokens[1], tokens[2])));
					writer.newLine(); 
					writer.flush();
				}
				if (tokens[0].equals("POINTS")) {
					writer.write(Integer.toString(db.show_points(tokens[1])));
					writer.newLine(); 
					writer.flush();
				}
				if (tokens[0].equals("LIST")) {
					writer.write(db.friend_list(tokens[1]).toJSONString());
					writer.newLine();
					writer.flush();
				}
				if (tokens[0].equals("RANK")) {
					writer.write(db.show_ranking(tokens[1]).toJSONString());
					writer.newLine();
					writer.flush();
				}
				if (tokens[0].equals("CHALL")) {
					int TCPport = (int) ((Math.random() * ((65535 - 1024) + 1)) + 1024);
					WQChallenge wqc = new WQChallenge(TCPport);
					wqc.start();
					writer.write(Integer.toString(db.challenge(tokens[1], tokens[2], clientsocket, TCPport)));
					writer.newLine();
					writer.flush();
					byte[] buffer = new byte[1024];
					DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
					//clientsocket.setSoTimeout(3000);
					clientsocket.receive(receivedPacket);
					String byteToString = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
					String [] tokens2 = byteToString.split("\\s+");
					System.out.println(byteToString);
					if (tokens2[0].equals("ACCEPT")) {
						db.challengeaccepted(tokens[1], clientsocket, TCPport);
					}
					if (tokens2[0].equals("DECLINE")) {
						if (wqc.isAlive()) wqc.interrupt();
						//db.challengedeclined(tokens[2]);
					}
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
