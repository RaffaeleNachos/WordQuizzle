import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class WQUser implements Comparable<WQUser>{
	//transient serve a Gson per non serializzare la variabile
	public transient boolean online = false;
	public String username;
	public int points;
	private ArrayList<String> stringfriends;
	//inet address associato quando esegue il login
	private transient InetAddress ia;
	//porta udp del client per le notifiche, viene associata al login
	private transient int UDPport;
	
	public WQUser(String username) {
		this.username = username;
		this.points = 0;
		stringfriends = new ArrayList<>();
	}
	
	public void setIA(String i) {
		try {
			ia = InetAddress.getByName(i);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public InetAddress getIA() {
		return ia;
	}
	
	public void setPort(int p) {
		UDPport = p;
	}
	
	public int  getPort() {
		return UDPport;
	}
	
	public int addFriend(String friend) {
		if (stringfriends.contains(friend) || friend.equals(username)) return 0;
		stringfriends.add(friend);
		return 1;
	}
	
	public int removeFriend(String friend) {
		if (!stringfriends.contains(friend)) return 0;
		stringfriends.remove(friend);
		return 1;
	}
	
	public ArrayList<String> getFriends(){
		return stringfriends;
	}
	
	@Override
    public int compareTo(WQUser u) {
        int comparePoints = u.points;
        return comparePoints - this.points;
    }
	
}
