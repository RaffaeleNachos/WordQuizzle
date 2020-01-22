import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;

public class MainViewController {
	
	@FXML
	public Label ch_error;
	@FXML
	private Label username;
	@FXML
	private Label points;
	@FXML
	private Label chnotificationlabel;
	@FXML
	private ToggleButton toggleRanking;
	@FXML
	private Button btnLogout;
	@FXML
	private Button addbntFriend;
	@FXML
	private Button delbtnFriend;
	@FXML
	public Button btnChallenge;
	@FXML
	private Button btnAccept;
	@FXML
	private Button btnDecline;
	@FXML
	private TextField nickChall;
	@FXML
	private Tab tabInfo;
	@FXML 
	private Tab tabNotif;
	@FXML
	private ListView<String> listview;
	@FXML
	private AnchorPane notifyPane;

	private WQClient client_master;
	private WQNotify notify_master;
	
	//arraylist di notifiche
	private LinkedList<Notify> notifications = new LinkedList<>();
	
	public void setClient(WQClient client, WQNotify notifier) {
        this.client_master = client;
        this.notify_master = notifier;
    }
	
	public void setPoints(int num_points) {
		points.setText(Integer.toString(num_points));
	}
	
	public void setUsername(String g_username) {
		username.setText(g_username);
	}
	
	//rendono visibile ed invisibile il contenuto della tab delle notifiche 
	public void setNotifyTabVisible() {
		Notify peek = notifications.peekLast();
		chnotificationlabel.setText(peek.username);
		notifyPane.setVisible(true);
	}
	
	public void setNotifyTabInvisible() {
		notifyPane.setVisible(false);
	}
	
	//popola la listview della GUI
	public void populateList(ArrayList<String> people) {
		ObservableList<String> items = FXCollections.observableArrayList(people);
		listview.setItems(items);
	}
	
	public void logoutbtnAction(ActionEvent event) {
		int err;
		err = client_master.logout_handler();
		ch_error.setText(WQClient.codetoString(err));
		if (err==16) client_master.gotoLogin();
	}
	
	public void addbtnAction(ActionEvent event) {
		int err;
		err = client_master.add_handler(nickChall.getText());
		ch_error.setText(WQClient.codetoString(err));
		populateList(client_master.list_handler());
	}
	
	public void delbtnAction(ActionEvent event) {
		int err;
		err = client_master.del_handler(nickChall.getText());
		ch_error.setText(WQClient.codetoString(err));
		populateList(client_master.list_handler());
	}
	
	public void togglebtnAction(ActionEvent event) {
		if (toggleRanking.isSelected()) {
			populateList(client_master.list_handler());
		} else {
			populateList(client_master.rank_handler());
		}
	}
	
	//per lo sfidato nella tab delle notifiche accettazione
	public void declinebtnAction(ActionEvent event) {
		Notify removed = notifications.removeLast();
		notify_master.decline(removed.destIA, removed.UDPport);
		if (notifications.isEmpty()) setNotifyTabInvisible();
		else setNotifyTabVisible();
	}
	
	//per lo sfidato nella tab delle notifoiche accettazione
	public void acceptbtnAction(ActionEvent event) {
		Notify removed = notifications.removeLast();
		notify_master.accept(removed.destIA, removed.UDPport);
		client_master.TCPport = removed.TCPport;
		setNotifyTabInvisible();
		notifications.clear();
		client_master.gotoGame();
	}
	
	//per lo sfidante nel main controller
	public void challengebtnAction(ActionEvent event) {
		int err;
		err = client_master.chall_handler(nickChall.getText());
		ch_error.setText(WQClient.codetoString(err));
		//se la richiesta è inviata non permetto di inviarne un'altra fino a che non scada il timeout T1
		if (err==21) {
			btnChallenge.setStyle("-fx-background-color: #DEDEE0");
			btnChallenge.setDisable(true);
		}
	}
	
	public void addNotification(String username, InetAddress destIA, int TCPport, int UDPport) {
		notifications.addFirst(new Notify(username, destIA, TCPport, UDPport));
		System.out.println("aggiunto");
		setNotifyTabVisible();
	}
	
	public void removeNotification(String username) {
		for (int i = 0; i< notifications.size(); i++) {
			if (notifications.get(i).username.equals(username)) {
				notifications.remove(i);
			}
		}
		setNotifyTabVisible();
	}
	
	public class Notify{
		InetAddress destIA;
		int TCPport;
		int UDPport;
		String username;
		
		public Notify(String username, InetAddress destIA, int TCPport, int UDPport) {
			this.destIA = destIA;
			this.TCPport = TCPport;
			this.UDPport = UDPport;
			this.username = username;
		}
	}
	
}
