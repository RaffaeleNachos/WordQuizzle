import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class User implements Comparable<User>{
	private transient boolean online = false;
	private String username;
	private int points = 0;
	private transient ArrayList<User> friends;
	private ArrayList<String> stringfriends;
	
	public User(String username) {
		this.username = username;
		friends = new ArrayList<>();
		stringfriends = new ArrayList<>();
	}
	
	public int getPoints() {
		return points;
	}
	
	public boolean isOnline() {
		return online;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setPoints(int value) {
		points = value;
	}
	
	public void setOnline() {
		online = true;
	}
	
	public void setOffline() {
		online = false;
	}
	
	public int addFriend(User friend) {
		//necessario nel caso in cui il server deve ripartire dai dati su file json
		if (friends == null) {
			friends = new ArrayList<>();
		}
		if (friends.contains(friend)) return 0;
		else {
			friends.add(friend);
			stringfriends.add(friend.getUsername());
			return 1;
		}
	}
	
	public int removeFriend(User friend) {
		if (!friends.contains(friend)) return 0;
		else {
			friends.remove(friend);
			stringfriends.remove(friend.getUsername());
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
        int comparePoints= u.getPoints();
        return comparePoints-this.points;
    }
	
}
