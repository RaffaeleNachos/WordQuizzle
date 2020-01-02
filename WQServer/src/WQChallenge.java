import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class WQChallenge extends Thread{
	
	private int Aport;
	private int Bport;
	private InetAddress Aia;
	private InetAddress Bia;
	private Socket a;
	private Socket b;
	
	public WQChallenge(int port1, int port2, InetAddress ia1, InetAddress ia2) {
		Aport = port1;
		Bport = port2;
		Aia = ia1;
		Bia = ia2;
		try {
			System.out.println("A " + Aia + Aport);
			System.out.println("B " + Bia + Bport);
			//TODO vedere se c'è un modo per non aspettare che i due client creino la socket
			Thread.sleep(2000);
			a = new Socket(Aia, Aport);
			b = new Socket(Bia, Bport);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			BufferedWriter Bwriter = new BufferedWriter(new OutputStreamWriter(b.getOutputStream()));
			BufferedReader Areader = new BufferedReader(new InputStreamReader(a.getInputStream()));
			BufferedWriter Awriter = new BufferedWriter(new OutputStreamWriter(a.getOutputStream()));
			BufferedReader Breader = new BufferedReader(new InputStreamReader(b.getInputStream()));
			Bwriter.write("ciao");
			Bwriter.newLine(); 
			Bwriter.flush();
			Awriter.write("ciao");
			Awriter.newLine(); 
			Awriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
