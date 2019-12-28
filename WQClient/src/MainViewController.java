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

public class MainViewController {
	
	@FXML
	private Label ch_error;
	@FXML
	private Label username;
	@FXML
	private Label points;
	@FXML
	private ToggleButton toggleRanking;
	@FXML
	private Button btnLogout;
	@FXML
	private Button addbntFriend;
	@FXML
	private Button delbtnFriend;
	@FXML
	private Button btnChallenge;
	@FXML
	private TextField nickChall;
	@FXML
	private Tab tabInfo;
	@FXML 
	private Tab tabNotif;
	@FXML
	private ListView<String> listview;

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
		//if (err==16) client_master.gotoLogin();
	}
	
	public void delbtnAction(ActionEvent event) {
		int err;
		err = client_master.del_handler(nickChall.getText());
		ch_error.setText(WQClient.codetoString(err));
		populateList(client_master.list_handler());
		//if (err==16) client_master.gotoLogin();
	}
	
	public void togglebtnAction(ActionEvent event) {
		if (toggleRanking.isSelected()) {
			populateList(client_master.rank_handler());
		} else {
			populateList(client_master.list_handler());
		}
	}
	
}
