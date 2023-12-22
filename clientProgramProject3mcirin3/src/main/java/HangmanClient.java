/*
 *  HangmanClient.java
 *  Author: Mark Cirineo
 *  System: Intellij IDEA
 *  Course: CS 342
 *
 *
 *
 *
 *
 *
 *
 * */
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;

public class HangmanClient extends Thread {

	private Socket socketClient;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Consumer<Serializable> callback;

	public HangmanClient(Consumer<Serializable> call) {
		callback = call;
		try {
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error connecting to the server. Check the server address and port.");
		}
	}

	public void startNewGame(){

		start();
		send("Create New GUI");
	}





	public void run() {
		try {
			while (true) {
				String message = in.readObject().toString();
				callback.accept(message);
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			// Indicate that the connection has been lost
			callback.accept("Connection lost");
		}
	}

	public void send(String data) {
		try {
			out.writeObject(data);
		} catch (IOException e) {
			e.printStackTrace();
			// Handle send error if needed
		}
	}

	public void close() {
		try {
			out.close();
			in.close();
			socketClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
