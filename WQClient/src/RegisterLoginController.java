import java.rmi.RemoteException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
	
public class RegisterLoginController {
	@FXML
	private Button loginbtn;
	@FXML
	private Button registerbtn;
	@FXML
	private PasswordField passfield;
	@FXML
	private TextField userfield;
	@FXML
	private Label errors;
	
	private WQClient client_master;
	
	public void setClient(WQClient client) {
        this.client_master = client;
    }
	
	@FXML
	private void loginbtnAction(ActionEvent event) {
		int err;
		//chiama le funzioni handler definite nella classe WQClient associata al client che sta gestendo
		err = client_master.login_handler(userfield.getText(), passfield.getText());
		errors.setText(WQClient.codetoString(err));
		if (err==12) client_master.gotoMain();
	}
	
	@FXML
	private void registerbtnAction(ActionEvent event) {
		try {
			int err;
			err = WQClient.regGest.user_registration(userfield.getText(), passfield.getText());
			errors.setText(WQClient.codetoString(err));
		} catch (RemoteException | NullPointerException e) {
			e.printStackTrace();
		}
	}
}
