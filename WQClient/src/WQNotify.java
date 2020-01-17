import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javafx.application.Platform;

public class WQNotify extends Thread{

	private MainViewController masterContr;
	private DatagramSocket mys;
	private InetAddress destia;
	private int destport;
	private WQClient client_master;
	
	public WQNotify (WQClient client_master, DatagramSocket mys) {
		this.client_master = client_master;	
		this.mys = mys;
	}
	
	public void run() {
		System.out.println("Thread Notify attivo");
		try {
			byte[] buffer = new byte[1024];
			DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
			while (true) {
				System.out.println("sono in attesa UDP");
				mys.receive(receivedPacket);
				String byteToString = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
				String [] tokens = byteToString.split("\\s+");
				
				//messaggi ricevuti dallo sfidato
				if (tokens[0].equals("CH") && tokens.length == 3) {
					//System.out.println("Client ho ricevuto CH");
					//setto al client la porta TCP (mi servirà per poi quando parte il gioco)
					client_master.setTCPport(Integer.parseInt(tokens[2]));
					//prendo ip e porta UDP del server dal pacchetto per potergli spedire la risposta
					destia = receivedPacket.getAddress();
					destport = receivedPacket.getPort();
					//questo thread verrà eseguito sui thread associati a javafx, non è possibile aggiornare la GUI da thread non appartenenti a javafx
					Platform.runLater(new Runnable() {
						@Override
			            public void run() {
							//rendo visibile la tab delle notifiche, passo come paramentro il username dello sfidante
			            	masterContr.setNotifyTabVisible(tokens[1]);
			            }
			          });
				}
				//se non rispondo in tempo ricevo timeout
				if (tokens[0].equals("TIMEOUT")) {
					//System.out.println("Client ho ricevuto TIMEOUT");
					Platform.runLater(new Runnable() {
						@Override
			            public void run() {
							//rendo invisibile la tab delle notifiche
			            	masterContr.setNotifyTabInvisible();
			            }
			          });
				}
				
				//sfidante nel caso di sfida accettata
				if (tokens[0].equals("ACCEPTED") && tokens.length == 2) {
					//setto la porta TCP per la sfida
					client_master.setTCPport(Integer.parseInt(tokens[1]));
					//System.out.println("Client ho ricevuto ACCEPTED");
					//serve per aggiornare la gui nei thread javafx altrimenti non si può aggiornare.
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							//mando la GUI del client dello sfidante in modalità gioco
							client_master.gotoGame();
			            }
			        });
				}
				//ricevuto nel caso di challenge non accettata o timer scaduto
				if (tokens[0].equals("DECLINED")) {
					//System.out.println("Client ho ricevuto DECLINED");
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							//rendo nuovamente cliccabile il tasto di challenge
							masterContr.ch_error.setText("Challenge Not Accepted");
							masterContr.btnChallenge.setStyle("-fx-background-color: #FF9800");
							masterContr.btnChallenge.setDisable(false);
			            }
			        });
				}
			}
		} catch (SocketException e) {
			System.out.println("la socket è stata chiusa, termino il thread");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Thread Notify Shutdown");
	}
	
	public void setController (MainViewController contr) {
		this.masterContr = contr;
	}
	
	//inviato dallo sfidato al server
	public void accept() {
		String tmp = "ACCEPT";
		byte[] buffer=tmp.getBytes();
		DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, destia, destport);
		try {
			mys.send(mypacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//masterContr.setNotifyTabInvisible();
	}
	
	//inviato dallo sfidato al server
	public void decline() {
		String tmp = "DECLINE";
		byte[] buffer=tmp.getBytes();
		DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, destia, destport);
		try {
			mys.send(mypacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//masterContr.setNotifyTabInvisible();
	}
}
