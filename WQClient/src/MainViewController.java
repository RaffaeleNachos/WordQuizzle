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

	private WQClient clientMaster;
	private WQNotify notifyMaster;
	
	//arraylist di notifiche
	private LinkedList<Notify> notifications = new LinkedList<>();
	
	public void setClient(WQClient client, WQNotify notifier) {
        this.clientMaster = client;
        this.notifyMaster = notifier;
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
		//controllo se non ci sono più notifiche (mi serve nel caso in cui ritorni da una partita ma qualcuno nel frattempo mi ha chiesto di sfidarlo)
		if(notifications.isEmpty()) notifyPane.setVisible(false);
		else setNotifyTabVisible();
	}
	
	//popola la listview della GUI
	public void populateList(ArrayList<String> people) {
		ObservableList<String> items = FXCollections.observableArrayList(people);
		listview.setItems(items);
	}
	
	public void logoutbtnAction(ActionEvent event) {
		int err;
		err = clientMaster.logout_handler();
		ch_error.setText(WQClient.codetoString(err));
		if (err==16) clientMaster.gotoLogin();
	}
	
	public void addbtnAction(ActionEvent event) {
		int err;
		err = clientMaster.add_handler(nickChall.getText());
		ch_error.setText(WQClient.codetoString(err));
		populateList(clientMaster.list_handler());
	}
	
	public void delbtnAction(ActionEvent event) {
		int err;
		err = clientMaster.del_handler(nickChall.getText());
		ch_error.setText(WQClient.codetoString(err));
		populateList(clientMaster.list_handler());
	}
	
	public void togglebtnAction(ActionEvent event) {
		if (toggleRanking.isSelected()) {
			populateList(clientMaster.list_handler());
		} else {
			populateList(clientMaster.rank_handler());
		}
	}
	
	//per lo sfidato nella tab delle notifiche accettazione
	public void declinebtnAction(ActionEvent event) {
		Notify removed = notifications.removeLast();
		notifyMaster.decline(removed.destIA, removed.UDPport);
		if (notifications.isEmpty()) setNotifyTabInvisible();
		else setNotifyTabVisible();
	}
	
	//per lo sfidato nella tab delle notifoiche accettazione
	public void acceptbtnAction(ActionEvent event) {
		Notify removed = notifications.removeLast();
		notifyMaster.accept(removed.destIA, removed.UDPport);
		clientMaster.ChallengeTCPport = removed.TCPport;
		notifications.clear();
		setNotifyTabInvisible();
		clientMaster.gotoGame();
	}
	
	//per lo sfidante nel main controller
	public void challengebtnAction(ActionEvent event) {
		int err;
		err = clientMaster.chall_handler(nickChall.getText());
		ch_error.setText(WQClient.codetoString(err));
		//se la richiesta è inviata non permetto di inviarne un'altra fino a che non scada il timeout T1
		if (err==21) {
			btnChallenge.setStyle("-fx-background-color: #DEDEE0");
			btnChallenge.setDisable(true);
		}
	}
	
	public void addNotification(String username, InetAddress destIA, int TCPport, int UDPport) {
		notifications.addFirst(new Notify(username, destIA, TCPport, UDPport));
		System.out.println("MainController | new notification added");
		setNotifyTabVisible();
	}
	
	public void removeNotification(String username) {
		for (int i = 0; i< notifications.size(); i++) {
			if (notifications.get(i).username.equals(username)) {
				notifications.remove(i);
			}
		}
		System.out.println("MainController | notification removed");
		setNotifyTabVisible();
	}
	
	//classe delle notifiche, per ogni notifica mi salvo l'indirizzo di destinazione (se ci sono più server so a quale mandarlo), la porta TCP della challenge 
	//e la sua porta UDP (del server)
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
