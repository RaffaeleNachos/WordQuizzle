import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class GameViewController {
	
	@FXML
	private Label itawordlabel;
	@FXML
	private TextField engwordfield;
	@FXML
	private Button btnsend;
	
	
	private WQClient client_master;
	private ServerSocket ss;
	private Socket s_socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private String currentword;
	
	public void setClient(WQClient client) {
        this.client_master = client;
    }
	
	public void setSocket(int port) {
		try {
			ss = new ServerSocket(port);
			System.out.println(client_master.user + " aspetto sulla porta " + port);
			s_socket = ss.accept();
			System.out.println(client_master.user + " accettato");
			reader = new BufferedReader(new InputStreamReader(s_socket.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(s_socket.getOutputStream()));
			currentword = reader.readLine();
			itawordlabel.setText(currentword);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendbtnAction(ActionEvent event) {
		try {
			writer.write(engwordfield.getText());
			writer.newLine(); 
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
