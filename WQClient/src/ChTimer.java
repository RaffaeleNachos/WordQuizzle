import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.scene.effect.GaussianBlur;

public class ChTimer{
	private Timer timer;
	private GameViewController masterContr;
	
	public ChTimer(int seconds, GameViewController masterContr) {
		this.masterContr = masterContr;
		timer = new Timer();
		timer.schedule(new RemindTask(), seconds * 1000);
	}

	class RemindTask extends TimerTask {
		public void run() {
			System.out.println("Time is out");
			Platform.runLater(new Runnable() {
				@Override
	            public void run() {
					masterContr.btnSend.setStyle("-fx-background-color: #DEDEE0");
					masterContr.btnSend.setEffect(new GaussianBlur());
					masterContr.btnSend.setDisable(true);
					masterContr.labelTimeOver.setVisible(true);
	            }
	          });
		}
	}
}
