

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javafx.application.Platform;

public class WQNotify extends Thread{
	
	private int port;
	private MainViewController masterContr;
	private DatagramSocket mys;
	private InetAddress destia;
	private int destport;
	private WQClient client_master;
	
	public WQNotify (int port, WQClient client_master) {
		this.port = port;
		this.client_master = client_master;		
	}
	
	public void run() {
		System.out.println("Thread Notify attivo");
		try {
			mys= new DatagramSocket(port);
			byte[] buffer = new byte[1024];
			DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
			while (true) {
				System.out.println("sono in attesa UDP");
				mys.receive(receivedPacket);
				String byteToString = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
				String [] tokens = byteToString.split("\\s+");
				//sfidato
				if (tokens[0].equals("CH")) {
					System.out.println("Client ho ricevuto CH");
					client_master.setTCPport(Integer.parseInt(tokens[2]));
					destia = receivedPacket.getAddress();
					destport = receivedPacket.getPort();
					Platform.runLater(new Runnable() {
						@Override
			            public void run() {
			            	masterContr.setNotifyTabVisible(tokens[1]);
			            }
			          });
				}
				//sfidante
				if (tokens[0].equals("ACCEPTED")) {
					client_master.setTCPport(Integer.parseInt(tokens[1]));
					System.out.println("Client ho ricevuto ACCEPTED");
					//serve per aggiornare la usi nei thread javafx altrimenti non si può aggiornare.
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							client_master.gotoGame();
			            }
			        });
				}
				if (tokens[0].equals("DECLINED")) {
					//client_master.gotoGame();
				}
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setController (MainViewController contr) {
		this.masterContr = contr;
	}
	
	public void accept() {
		String tmp = "ACCEPT";
		byte[] buffer=tmp.getBytes();
		DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, destia, destport);
		try {
			mys.send(mypacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void decline() {
		String tmp = "DECLINE";
		byte[] buffer=tmp.getBytes();
		DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, destia, destport);
		try {
			mys.send(mypacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		masterContr.setNotifyTabInvisible();
	}
}
