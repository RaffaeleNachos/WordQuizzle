import java.util.ArrayList;

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
	
	public void setClient(WQClient client) {
        this.client_master = client;
    }
	
	public void setPoints(int num_points) {
		points.setText(Integer.toString(num_points));
	}
	
	public void setUsername(String g_username) {
		username.setText(g_username);
	}
	
	public void setNotifyTabVisible(String nick) {
		chnotificationlabel.setText(nick);
		notifyPane.setVisible(true);
	}
	
	public void setNotifyTabInvisible() {
		notifyPane.setVisible(false);
	}
	
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
		client_master.decline_handler();
	}
	
	//per lo sfidato nella tab delle notifoiche accettazione
	public void acceptbtnAction(ActionEvent event) {
		client_master.accept_handler();
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
	
}
