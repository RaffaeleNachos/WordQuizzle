

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class WQNotify extends Thread{
	
	private int port;
	private MainViewController masterContr;
	
	public WQNotify (int port, WQClient client_master) {
		this.port = port;
	}
	
	public void run() {
		System.out.println("Thread Notify attivo");
		DatagramSocket mys;
		try {
			mys= new DatagramSocket(port);
			byte[] buffer = new byte[1024];
			DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
			while (true) {
				System.out.println("sono in attesa UDP");
				mys.receive(receivedPacket);
				String byteToString = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
				String [] tokens = byteToString.split("\\s+");
				masterContr.setNotifyTabVisible();
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
}
