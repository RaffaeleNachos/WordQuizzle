import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javafx.application.Platform;

public class WQNotify extends Thread{

	private MainViewController mainController;
	private DatagramSocket myUDPsocket;
	private WQClient clientMaster;
	private InetAddress destIA;
	private int UDPport;
	
	public WQNotify (WQClient clientMaster, DatagramSocket myUDPsocket) {
		this.clientMaster = clientMaster;	
		this.myUDPsocket = myUDPsocket;
	}
	
	public void run() {
		System.out.println("WQNotify | Thread Notify running...");
		try {
			byte[] buffer = new byte[1024];
			DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
			while (true) {
				System.out.println("WQNotify | waiting for UDP datagrams...");
				myUDPsocket.receive(receivedPacket);
				String byteToString = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
				System.out.println("WQNotify | received: " + byteToString);
				String [] tokens = byteToString.split("\\s+");
				
				//messaggi ricevuti dallo sfidato
				if (tokens[0].equals("CH") && tokens.length == 3) {
					destIA = receivedPacket.getAddress();
					UDPport = receivedPacket.getPort();
					//serve per aggiornare la gui nei thread javafx altrimenti non è possibile aggiornare la GUI da altri thread
					Platform.runLater(new Runnable() {
						@Override
			            public void run() {
							//aggiungo la notifica alla coda
							mainController.addNotification(tokens[1], destIA, Integer.parseInt(tokens[2]), UDPport);
			            }
			          });
				}
				//se non rispondo in tempo ricevo timeout
				if (tokens[0].equals("TIMEOUT") && tokens.length == 2) {
					Platform.runLater(new Runnable() {
						@Override
			            public void run() {
							//rimuovo la notifica dalla coda
			            	mainController.removeNotification(tokens[1]);
			            }
			          });
				}
				
				//sfidante nel caso di sfida accettata
				if (tokens[0].equals("ACCEPTED") && tokens.length == 2) {
					//setto la porta TCP per la sfida
					clientMaster.ChallengeTCPport = Integer.parseInt(tokens[1]);
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							//mando la GUI del client dello sfidante in modalità gioco
							clientMaster.gotoGame();
			            }
			        });
				}
				//ricevuto dallo sfindante nel caso di challenge non accettata o timer scaduto
				if (tokens[0].equals("DECLINED")) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							//rendo nuovamente cliccabile il tasto di challenge
							mainController.ch_error.setText("Challenge Not Accepted");
							mainController.btnChallenge.setStyle("-fx-background-color: #FF9800");
							mainController.btnChallenge.setDisable(false);
			            }
			        });
				}
			}
		} catch (SocketException e) {
			System.out.println("WQNotify | socket is closed, exiting thread...");
			//e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("WQNotify thread shutdown");
	}
	
	public void setController (MainViewController contr) {
		this.mainController = contr;
	}
	
	//inviato dallo sfidato al server
	public void accept(InetAddress destIA, int destport) {
		String tmp = "ACCEPT";
		byte[] buffer=tmp.getBytes();
		System.out.println("WQNotify | sending: " + tmp);
		DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, destIA, destport);
		try {
			myUDPsocket.send(mypacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mainController.setNotifyTabInvisible();
	}
	
	//inviato dallo sfidato al server
	public void decline(InetAddress destIA, int destport) {
		String tmp = "DECLINE";
		byte[] buffer=tmp.getBytes();
		System.out.println("WQNotify | sending: " + tmp);
		DatagramPacket mypacket = new DatagramPacket(buffer, buffer.length, destIA, destport);
		try {
			myUDPsocket.send(mypacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mainController.setNotifyTabInvisible();
	}
}
