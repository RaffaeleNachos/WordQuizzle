
public class WQCheckWord extends Thread{

	private WQDatabase db;
	private String word;
	private String cTransl;
	private String username;
	private static int correctPoint = 3;
	private static int wrongPoint = -1;
	
	public WQCheckWord(WQDatabase db, String word, String username, String cTransl) {
		this.db = db;
		this.word = word;
		this.username = username;
		this.cTransl = cTransl;
	}
	
	public void run() {
		User u = db.getUser(username);
		if (u!=null) {
			if(word.equals(cTransl)) {
				u.points = u.points + correctPoint;
			} else {
				u.points = u.points + wrongPoint;
			}
		}
	}
	
}
