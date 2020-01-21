import java.util.Timer;
import java.util.TimerTask;

public class WQChallTimer{
	private Timer timer;
	private WQChallenge chTh;
	
	public WQChallTimer(int seconds, WQChallenge chTh) {
		this.chTh = chTh;
		timer = new Timer();
		timer.schedule(new RemindTask(), seconds * 1000);
	}

	class RemindTask extends TimerTask {
		public void run() {
			if (chTh.isAlive()) {
				System.out.println("var aumentata");
				chTh.to.incrementAndGet();
			}
		}
	}
}
