import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

public class GameViewController {
	
	@FXML
	private Label itawordlabel;
	@FXML
	private TextField engwordfield;
	@FXML
	public Button btnSend;
	@FXML
	public Button btnExit;
	@FXML
	private ProgressBar progressBar;
	@FXML
	public Label labelStatus;
	
	
	private WQClient client_master;
	private SocketAddress socket;
	private SocketChannel socketChannel;
	private ByteBuffer byteBuffer;
	
	public void setClient(WQClient client) {
        this.client_master = client;
    }
	
	public void setSocket(int port, InetAddress ia) {
		socket = new InetSocketAddress(ia, port);
		try {
			socketChannel = SocketChannel.open();
			socketChannel.connect(socket);
			System.out.println("Collegato a: " + socketChannel);
			readStatus();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendbtnAction(ActionEvent event) {
		//TODO check che abbia scritto meno di 512 caratteri! prendo la sottostringa
		String tosend = client_master.user + " " + engwordfield.getText();
		byteBuffer = ByteBuffer.wrap(tosend.getBytes());
		try {
	    	while (byteBuffer.hasRemaining()) {
	    		System.out.println("Client | scrivo: " + socketChannel.write(byteBuffer) + " bytes");
	    	}
	    	byteBuffer.clear();
	    	byteBuffer.flip();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		engwordfield.clear();
		readStatus();
	}
	
	public void readStatus() {
		System.out.println("Sono in lettura");
		byteBuffer = ByteBuffer.allocate(1024);
    	boolean stop = false;
        String tmp = "";
		try {
	        while (!stop) {
	        	byteBuffer.clear();
	        	int bytesRead;
				bytesRead = socketChannel.read(byteBuffer);
	        	byteBuffer.flip();
	        	tmp = tmp + StandardCharsets.UTF_8.decode(byteBuffer).toString();
	        	byteBuffer.flip();
	    		System.out.println("Client | leggo: " + bytesRead + " bytes");
	    		if (bytesRead < 1024) {
	        		stop=true;
        		}
        	}
    		byteBuffer.flip();
    		String token[] = tmp.split("\\s+");
    		if (!token[0].equals("CHEND")) {
    			itawordlabel.setText(token[0]);
    			progressBar.setProgress(Double.parseDouble(token[1]));
    		}
    		else {
    			labelStatus.setText("Challenge ended. Your score: " + token[1] + " Correct Words: " + token[2] + " Wrong Words: " + token[3]);
    			btnSend.setStyle("-fx-background-color: #DEDEE0");
				btnSend.setDisable(true);
    		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void exitbtnAction(ActionEvent event) {
		String tosend = "CHEXITED";
		byteBuffer = ByteBuffer.wrap(tosend.getBytes());
		try {
	    	while (byteBuffer.hasRemaining()) {
	    		System.out.println("Client | scrivo: " + socketChannel.write(byteBuffer) + " bytes");
	    	}
	    	byteBuffer.clear();
	    	byteBuffer.flip();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		client_master.gotoMain();
	}
}
