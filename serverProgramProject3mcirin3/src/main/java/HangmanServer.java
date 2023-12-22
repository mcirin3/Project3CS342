/*
 *  HangmanServer.java
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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class HangmanServer {

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();
	TheServer server;
	private Consumer<Serializable> callback;

	String wordToGuess;
	HashMap<Integer, Boolean> guessedLetters;
	int incorrectGuesses = 0;
	int incorrectWordGuesses = 3;

	StringBuilder updatedDashedLine = new StringBuilder();

	HangmanServer(Consumer<Serializable> call) {
		callback = call;
		server = new TheServer();
		server.start();

		wordToGuess = generateRandomWordFromFile("src/main/resources/wordlist.txt").toUpperCase();
		guessedLetters = new HashMap<>();

		for (int i = 0; i < wordToGuess.length(); i++) {
			guessedLetters.put(i, false);
		}

		// Print out the word to guess on the console for testing purposes
		System.out.println("Word to guess: " + wordToGuess);
	}

	public String generateRandomWordFromFile(String filePath) {
		ArrayList<String> words = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				words.add(line.trim());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (words.isEmpty()) {
			throw new RuntimeException("Word list is empty");
		}

		return words.get(new Random().nextInt(words.size()));
	}

	class TheServer extends Thread {
		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555)) {
				System.out.println("Server is waiting for a client!");

				while (true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					callback.accept("client has connected to server: " + "client #" + count);
					clients.add(c);
					c.start();

					count++;
				}
			} catch (Exception e) {
				callback.accept("Server socket did not launch");
			}
		}
	}

	class ClientThread extends Thread {
		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;

		int wordGuessAttempts = 0;
		final int MAX_WORD_GUESS_ATTEMPTS = 3;

		ClientThread(Socket s, int count) {
			this.connection = s;
			this.count = count;
		}

		public void updateClients(String message) {
			for (int i = 0; i < clients.size(); i++) {
				ClientThread t = clients.get(i);
				try {
					t.out.writeObject(message);
				} catch (Exception e) {
				}
			}
		}

		void handleWordGuess(String wordGuess) {
			// Check if the number of attempts has reached the limit
			if (wordGuessAttempts >= MAX_WORD_GUESS_ATTEMPTS) {
				updateClients("client #" + count + " has used all word guess attempts. Game over. The word was: " + wordToGuess);
				endGame(false, this);
				return;
			}

			// Increment the number of word guess attempts
			wordGuessAttempts++;

			// Convert the word guess to uppercase
			wordGuess = wordGuess.toUpperCase();

			// Check if the word guess is correct
			if (wordToGuess.equals(wordGuess)) {
				updateClients("client #" + count + " guessed the word correctly: " + wordToGuess);
				endGame(true, this);
			} else {
				updateClients("client #" + count + " guessed the word incorrectly: " + wordGuess);
				// Handle incorrect word guess (provide feedback, update UI, etc.)
				// Optionally, you may send feedback to the client about remaining attempts.
			}
		}

		public void run() {
			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				System.out.println("Streams not open");
			}
			updateClients("Welcome to the Game of Hangman! Developed by Mark Cirineo");
			updateClients("To play the game, click on one of the letters to send a letter guess.");
			updateClients("Each incorrect guess will result in a part of hangman being added. Once");
			updateClients("the Hangman is complete, that means you have used all your guesses! Otherwise");
			updateClients("you have won! To change a category, click Categories drop down to select a new Category");
			updateClients("Clicking on new game will result in a new default game of the single common 5 letter words.");
			updateClients("Enjoy playing this classic word guessing game! :)");
			updateClients("new client on server: client #" + count);
			updateClients("The word list contains some fairly commonly used 5 - letter english words.");
			send("Word to guess is of length:  " + wordToGuess.length());
			updateClients(createDashedLine());

			while (true) {
				try {
					String data = in.readObject().toString();
					callback.accept("client: " + count + " sent: " + data);

					if (data.startsWith("guess:")) {
						// existing code for handling letter guesses
						handleSingleLetterGuess(data.charAt(7));
					} else if (data.startsWith("newGame")) {
						// Handle new game request
						resetGame();
					} else if (data.startsWith("changeCat:")) {
						// Handle category change request
						String categoryFileName = data.substring(10);
						handleCategoryChange(categoryFileName);
					}
					else if (data.equals("resetGame")) {
						// Handle reset game request
						resetGame();
					} else if (data.charAt(0) == wordToGuess.charAt(0)) {
						// Handle word guesses
						handleWordGuess(data);
					} else if (data.charAt(0) != wordToGuess.charAt(0)) {
						// Handle incorrect word guesses
						incorrectWordGuesses--;
						updateClients("Remaining guesses: " + incorrectWordGuesses);
						if(incorrectWordGuesses == 0){
							updateClients("You have used all your word attempts! The word was " + wordToGuess);
						}

					}
					else if (data.startsWith("endGame ")) {
						boolean win = Boolean.parseBoolean(data.substring(8));
						if (win) {
							updateClients("Congratulations! You won!");
						} else {
							updateClients("Sorry, you lost. The word was: " + wordToGuess);
						}
						break; // exit the loop as the game has ended
					}
					else {
						updateClients("Invalid command from client #" + count);
					}
				} catch (Exception e) {
					callback.accept("OOOOPPs...Something wrong with the socket from client: " + count +
							"....closing down!");
					updateClients("Client #" + count + " has left the server!");
					clients.remove(this);
					break;
				}
			}
		}

		void handleCategoryChange(String categoryFileName) {
			// Reset game state on the client side
			guessedLetters.clear();
			incorrectGuesses = 0;

			String properFilePath = "src/main/resources/"+categoryFileName;

			// Generate a new word to guess based on the selected category
			wordToGuess = generateRandomWordFromFile(properFilePath).toUpperCase();

			for (int i = 0; i < wordToGuess.length(); ++i) {
				guessedLetters.put(i, false);
			}

			// Notify clients about the new word and category
			updateClients("New word is of length:  " + wordToGuess.length());
			updateClients(createDashedLine());

			// Testing that we have a new word to guess
			System.out.println("The word to guess is: " + wordToGuess);
		}

		void handleSingleLetterGuess(char letterGuess) {
			boolean correctGuess = false;

			for (int i = 0; i < wordToGuess.length(); i++) {
				if (Character.toUpperCase(wordToGuess.charAt(i)) == Character.toUpperCase(letterGuess) &&
						!guessedLetters.get(i)) {
					guessedLetters.put(i, true);
					correctGuess = true;
				}
			}

			if (correctGuess) {
				updateClients("client #" + count + " guessed correctly: " + letterGuess);

				// Update the dashed line with correct guesses
				String updatedDashedLine = getUpdatedDashedLine();
				updateClients(updatedDashedLine);

				if (wordGuessed()) {
					updateClients("client #" + count + " has guessed the word! It was: " + wordToGuess);
					endGame(true, this);
				}
			} else {
				updateClients("client #" + count + " guessed incorrectly: " + letterGuess);
				incorrectGuesses++;

				// Update the hangman image for all clients
				updateHangmanImage();

				if (incorrectGuesses >= 6) {
					updateClients("client #" + count + " has reached the maximum incorrect guesses. Game over. The word was: " + wordToGuess);
					endGame(false, this);
				}
			}
		}

		String createDashedLine() {
			updatedDashedLine.setLength(0); // Clear the StringBuilder before updating

			for (int i = 0; i < wordToGuess.length(); i++) {
				char currentChar = wordToGuess.charAt(i);

				if (Character.isWhitespace(currentChar)) {
					// If the character is a space, append the space
					updatedDashedLine.append(' ');
				} else {
					// If the character is not a space, append '-'
					updatedDashedLine.append('-');
				}
			}

			return updatedDashedLine.toString();
		}

		String getUpdatedDashedLine() {
			updatedDashedLine.setLength(0); // Clear the StringBuilder before updating

			for (int i = 0; i < wordToGuess.length(); i++) {
				char currentChar = wordToGuess.charAt(i);

				if (Character.isWhitespace(currentChar)) {
					// If the character is a space, append the space
					updatedDashedLine.append(' ');
				} else if (guessedLetters.get(i)) {
					// If the letter is guessed, append the actual letter
					updatedDashedLine.append(currentChar);
				} else {
					// If the letter is not guessed, append '-'
					updatedDashedLine.append('-');
				}
			}

			return updatedDashedLine.toString();
		}

		void updateHangmanImage() {
			// Notify all clients to update the Hangman image
			updateClients("updateImage " + incorrectGuesses);
			if(incorrectGuesses == 1) {
				updateClients("Hangman now has a head! :O");

			}else if(incorrectGuesses == 2){
				updateClients("Hangman now has a body but no limbs! :O");
			}else if(incorrectGuesses == 3){
				updateClients("Hangman now has 1/4 limbs! (This is getting bad :/ )");

			}else if(incorrectGuesses == 4){
				updateClients("DANGER! DANGER!");
			}else if(incorrectGuesses == 5){
				updateClients("........");

			}
		}

		private String getClientWord() {
			StringBuilder clientWord = new StringBuilder();
			for (int i = 0; i < wordToGuess.length(); i++) {
				if (guessedLetters.get(i)) {
					clientWord.append(wordToGuess.charAt(i));
				} else {
					clientWord.append('_');
				}
			}
			return clientWord.toString();
		}

		boolean wordGuessed() {
			for (boolean guessed : guessedLetters.values()) {
				if (!guessed) {
					return false;
				}
			}
			return true;
		}

		public void resetGame() {
			// Reset game state on the client side
//			send("resetGame");

			guessedLetters.clear();
			incorrectGuesses = 0;

			// Generate new word to guess
			wordToGuess = generateRandomWordFromFile("src/main/resources/wordlist.txt").toUpperCase();

			for (int i = 0; i < wordToGuess.length(); ++i) {
				guessedLetters.put(i, false);
			}

			// Notify clients about the new word
			updateClients("New word is of length:  " + wordToGuess.length());
			updateClients(createDashedLine());

			// Testing that we have a new word to guess
			System.out.println("The word to guess is: " + wordToGuess);
		}

		private List<ClientThread> clientsToClose = new ArrayList<>();

		void endGame(boolean win, ClientThread client) {
			// Notify all clients about the end of the game and whether it's a win or lose
			updateClients("endGame " + win);

			// Optionally, you can perform additional actions based on the game result
			if (win) {
				callback.accept("client #" + count + " has won the game!");
			} else {
				callback.accept("client #" + count + " has lost the game.");
			}

			// Add the client to the list of clients to close
			clientsToClose.add(client);
		}

		// Method to close connections for clients who want to exit
		public void closeConnections() {
			for (ClientThread client : clientsToClose) {
				try {
					client.in.close();
					client.out.close();
					client.connection.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Clear the list after closing connections
			clientsToClose.clear();
		}

		public void send(String data) {
			for (ClientThread client : clients) {
				try {
					client.out.writeObject(data);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		new HangmanServer(msg -> System.out.println(msg));
	}
}
