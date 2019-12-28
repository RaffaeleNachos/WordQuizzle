import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class WQTask implements Runnable{
	
	private WQDatabase db;
	private Socket socket;
	
	public WQTask(WQDatabase db, Socket socket) {
		this.db=db;
		this.socket=socket;
	}

	@Override
	public void run() {
		System.out.println("Nuovo client eseguito da " + Thread.currentThread().getName());
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			String line = reader.readLine();
			System.out.println("Server ho letto " + line);
			String[] tokens = line.split("\\s+");
			while(!tokens[0].equals("LOGOUT")) {
				if (tokens[0].equals("LOGIN")) {
					writer.write(Integer.toString(db.user_login(tokens[1], tokens[2])));
					writer.newLine(); 
					writer.flush();
				}
				if (tokens[0].equals("ADD")) {
					writer.write(Integer.toString(db.add_friend(tokens[1], tokens[2])));
					writer.newLine(); 
					writer.flush();
				}
				if (tokens[0].equals("REMOVE")) {
					writer.write(Integer.toString(db.remove_friend(tokens[1], tokens[2])));
					writer.newLine(); 
					writer.flush();
				}
				if (tokens[0].equals("POINTS")) {
					writer.write(Integer.toString(db.show_points(tokens[1])));
					writer.newLine(); 
					writer.flush();
				}
				if (tokens[0].equals("LIST")) {
					writer.write(db.friend_list(tokens[1]).toJSONString());
					writer.newLine();
					writer.flush();
				}
				if (tokens[0].equals("RANK")) {
					writer.write(db.show_ranking(tokens[1]).toJSONString());
					writer.newLine();
					writer.flush();
				}
				line = reader.readLine();
				System.out.println("Server ho letto " + line);
				tokens = line.split("\\s+");
			}
			writer.write(Integer.toString(db.user_logout(tokens[1])));
			writer.newLine(); 
			writer.flush();
			System.out.println("Thread Exiting...");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
