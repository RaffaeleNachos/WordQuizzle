import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class User implements Comparable<User>{
	public transient boolean online = false;
	public String username;
	public int points = 0;
	private transient ArrayList<User> friends;
	private ArrayList<String> stringfriends;
	private transient InetAddress ia;
	private transient int UDPport;
	
	public User(String username) {
		this.username = username;
		friends = new ArrayList<>();
		stringfriends = new ArrayList<>();
	}
	
	public void setIA(String i) {
		try {
			ia = InetAddress.getByName(i);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public void setPort(int p) {
		UDPport = p;
	}
	
	public int  getPort() {
		return UDPport;
	}
	
	public InetAddress getIA() {
		return ia;
	}
	
	public int addFriend(User friend) {
		//TODO aggiungere se stesso non si può fare
		//necessario nel caso in cui il server deve ripartire dai dati su file json
		if (friends == null) {
			friends = new ArrayList<>();
		}
		if (friends.contains(friend)) return 0;
		else {
			friends.add(friend);
			if (!stringfriends.contains(friend.username)) stringfriends.add(friend.username);
			return 1;
		}
	}
	
	public int removeFriend(User friend) {
		//necessario nel caso in cui il server deve ripartire dai dati su file json
		if (friends == null) {
			friends = new ArrayList<>();
		}
		if (!friends.contains(friend)) return 0;
		else {
			friends.remove(friend);
			if (stringfriends.contains(friend.username)) stringfriends.remove(friend.username);
			return 1;
		}
	}
	
	public ArrayList<String> getFriends(){
		return stringfriends;
	}
	
	public ArrayList<User> getOrderedFriends(){
		Collections.sort(friends);
		return friends;
	}
	
	@Override
    public int compareTo(User u) {
        int comparePoints= u.points;
        return comparePoints-this.points;
    }
	
}
