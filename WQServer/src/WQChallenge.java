import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class WQChallenge extends Thread{
	
	private int port;
	private Selector selector;
	private ServerSocket serverSocket;
	private ServerSocketChannel serverChannel;
	
	public WQChallenge(int port) {
		this.port = port;
	}
	
	public void run() {
		try {
			//creo ServerSocketChannel per poterlo settare in modalità non bloccante
			//il SocketChannel è creato implicitamente
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			//ottengo la socket del server associata al channel
			serverSocket = serverChannel.socket();
			//bindo la socket del server sulla porta *port*
			//come indirizzo usa una wildcard standard che mi indica tutti gli indirizzi IP della macchina
			InetSocketAddress address = new InetSocketAddress(port);
			serverSocket.bind(address);
			//creo il mio selector e lo associo al channel
			//per ora sto in ascolto solo sulla key (della socket del server) che identifica la accept
			selector = Selector.open(); 
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException ex) { 
			ex.printStackTrace();
		}
		while (true) { 
			try {
				//System.out.println(selector.keys());
				//System.out.println(selector.selectedKeys());
				System.out.println("Server | Aspetto sulla select");
				selector.select();
			} catch (IOException ex) {
				ex.printStackTrace(); 
				break;
			}
			//selectionkey identifica i tipi delle operazioni da gestire
			//readyKeys identifica i canali pronti
			Set <SelectionKey> readyKeys = selector.selectedKeys();
			Iterator <SelectionKey> iterator = readyKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = (SelectionKey) iterator.next();
				//esplicita rimozione chiave dal SelectedSet
				iterator.remove();
				try {
					if (key.isAcceptable()) {
						System.out.println("Server | pronta una chiave di accettazione");
						ServerSocketChannel server = (ServerSocketChannel) key.channel(); 
						SocketChannel client = server.accept(); 
						System.out.println("Connessione accettata verso: " + client); 
						client.configureBlocking(false);
						//creo una nuova chiave associata alla socket client
						SelectionKey clientkey = client.register(selector, SelectionKey.OP_WRITE, null);
					}
					else if (key.isWritable()) {
						System.out.println("Server | pronta una chiave in scrittura");
						SocketChannel client = (SocketChannel) key.channel();
						String write;
						if (key.attachment() == null) write = "prova";
						else write = (String) key.attachment();
						ByteBuffer end = ByteBuffer.wrap(write.getBytes());
						int bWrite = client.write(end);
						//se ho scritto tutto rimetto la chiave in read
						if (bWrite == write.length()) {
							key.attach(null);
							key.interestOps(SelectionKey.OP_READ);
							System.out.println("Server | key impostata su read");
						}
						//se il client chiude la socket o termina la write restituisce -1
						else if (bWrite == -1) {
							key.cancel();
							key.channel().close();
							System.out.println("Server | socket chiusa dal client");
						}
						//se non ha scritto tutto
						else {
							System.out.println("Server | scrivo: " + bWrite + " bytes");
							//la flip server per la decodifica
							end.flip();
							key.attach(StandardCharsets.UTF_8.decode(end).toString());
						}
					}
					else if (key.isReadable()) {
						System.out.println("Server | pronta una chiave in lettura");
						SocketChannel client = (SocketChannel) key.channel();
						String read = (String) key.attachment();
						ByteBuffer input = ByteBuffer.allocate(1024);
						input.clear();
						int bRead = client.read(input);
						//se il buffer è pieno ritorna a leggere al ciclo dopo
						if (bRead == 1024){
							System.out.println("Server | leggo: " + bRead + " bytes");
							input.flip();
							read = read + StandardCharsets.UTF_8.decode(input).toString();;
							key.attach(read);
						}
						//se ho letto meno della dimensione ha letto tutto
						else if (bRead < 1024) {
							System.out.println("Server | leggo: " + bRead + " bytes");
							input.flip();
							read = read + StandardCharsets.UTF_8.decode(input).toString();
							System.out.println(read);
							key.attach(null);
							key.interestOps(SelectionKey.OP_WRITE);
							System.out.println("Server | key impostata su write");
						}
						//se il client chiude la socket o termina, la read restituisce -1
						else if (bRead == -1) {
							key.cancel();
							key.channel().close();
							System.out.println("Server | socket chiusa dal client");
						}
					}
				} catch (IOException ex) {
					//in caso di exception cancello la chiave e chiudo il channel associato alla chiave
					key.cancel();
					try {
						key.channel().close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

}
