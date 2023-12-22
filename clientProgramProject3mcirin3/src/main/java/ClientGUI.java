/*
 *  ClientGUI.java
 *  Author: Mark Cirineo
 *  System: Intellij IDEA
 *  Course: CS 342
 *
 *
 *
 * GuiServer.java was inspired from the Gui Server Client
 * code created by Prof. Mark Hallenbeck
 *
 *
 *
 * */
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class ClientGUI extends Application {

	private Button clientChoice;
	private HashMap<String, Scene> sceneMap;
	private HBox buttonBox;
	private BorderPane startPane;
	private HangmanClient clientConnection;
	private ImageView hangmanImageView;
	private Text dashedLineText;

	private ListView<String> listItems2;

	private HBox dashedLineBox;
	private List<Label> dashedLineLabels;

	private TextField wordToGuessTF, c1;

	private String wordToGuess;

	private Button sendWordGuessButton;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Welcome to Hangman!");

		this.clientChoice = createStyledButton("Start Game");

		this.clientChoice.setOnAction(e -> {
			primaryStage.setScene(sceneMap.get("client"));
			primaryStage.setTitle("Hangman Game (Client-Side)");
			clientConnection = new HangmanClient(data -> {
				Platform.runLater(() -> {
					listItems2.getItems().add(data.toString());
					handleServerMessage(data.toString());

					// Autoscroll to the last item
					listItems2.scrollTo(listItems2.getItems().size() - 1);
				});
			});

			clientConnection.start();
		});

		this.buttonBox = new HBox(10, clientChoice);
		this.buttonBox.setPadding(new Insets(20));
		this.buttonBox.setAlignment(Pos.CENTER);
		startPane = new BorderPane();
		startPane.setTop(createTitle());
		startPane.setCenter(buttonBox);

		Scene startScene = new Scene(startPane, 500, 500);

		listItems2 = new ListView<>();
		listItems2.setPrefHeight(200); // Set an appropriate height for the ListView

		sceneMap = new HashMap<>();
		sceneMap.put("client", createClientGui());

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});

		primaryStage.setScene(startScene);
		primaryStage.show();
	}

	private Button createStyledButton(String text) {
		Button button = new Button(text);
		button.setStyle("-fx-pref-width: 200px; -fx-pref-height: 50px; -fx-background-radius: 10;");
		return button;
	}

	private VBox createTitle() {
		VBox titleBox = new VBox();
		titleBox.setStyle("-fx-background-color: lightgray; -fx-padding: 10;");
		javafx.scene.text.Text titleText = new javafx.scene.text.Text("Welcome to Hangman!");
		titleText.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
		titleBox.getChildren().add(titleText);
		return titleBox;
	}

	public Scene createClientGui() {
		VBox clientBox = new VBox(10, createMenuBar(false), createServerClientLogs(), createQwertyKeyboard(), createHangmanImageView(), createSendField(), createWordGuessSend());
		clientBox.setStyle("-fx-background-color: lightblue");
		clientBox.setAlignment(Pos.CENTER);

		// Create TextField for word guesses
//		TextField wordGuessTextField = new TextField();

//		// Create Button for sending word guesses
//		Button sendWordGuessButton = new Button("Send");
//		sendWordGuessButton.setOnAction(e -> {
//			String wordGuess = wordGuessTextField.getText();
//			// Send the word guess to the server
//			clientConnection.send("wordGuess:" + wordGuess);
//			// Optionally, clear the TextField after sending the guess
//			wordGuessTextField.clear();
//		});

		// Add TextField and Button to the clientBox
//		clientBox.getChildren().addAll(wordGuessTextField, sendWordGuessButton);

		return new Scene(clientBox, 800, 800);
	}

	private MenuBar createMenuBar(boolean isServer) {
		MenuBar menuBar = new MenuBar();
		Menu menu = new Menu("Game");
		MenuItem newGameItem = new MenuItem("New Game");
		MenuItem exitItem = new MenuItem("Exit");

		newGameItem.setOnAction(e -> handleNewGame());



		exitItem.setOnAction(e -> handleExit(isServer));



		menu.getItems().addAll(newGameItem, exitItem);


		//Category menu
		Menu categoryMenu = new Menu("Category");

		MenuItem nflQbs = new MenuItem("NFL Qbs");
		MenuItem commonPhrases = new MenuItem("Common Phrases");
		MenuItem csTerms = new MenuItem("CS Terms");
		MenuItem defaultWords = new MenuItem("Single Words");

		nflQbs.setOnAction(e -> handleCategorySelection("nfl.txt"));
		commonPhrases.setOnAction(e -> handleCategorySelection("commonPhrases.txt"));
		csTerms.setOnAction(e -> handleCategorySelection("CsTerms.txt"));
		defaultWords.setOnAction(e-> handleCategorySelection("wordlist.txt"));


		categoryMenu.getItems().addAll(nflQbs, commonPhrases, csTerms, defaultWords);

		menuBar.getMenus().addAll(menu, categoryMenu);

		return menuBar;
	}

	private TextField createSendField(){
		 wordToGuessTF = new TextField();


		return wordToGuessTF;
	}

	private Button createWordGuessSend(){

	 sendWordGuessButton = new Button("Send");

		sendWordGuessButton.setOnAction(e->{

			clientConnection.send(wordToGuessTF.getText());
			wordToGuessTF.clear();

		});


		return sendWordGuessButton;
	}

	private void handleCategorySelection(String fileName) {
		// Add logic to handle the selection of a category
		clientConnection.send("changeCat:" + fileName);
		System.out.println("Client: Starting a new game with category " + fileName);
		resetHangman();
	}


	private void handleNewGame() {
		// Add logic for starting a new game

			clientConnection.send("newGame");
			System.out.println("Client: Starting a new game...");
			resetHangman();

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

	private void resetHangman() {
		// Reset the hangman image to the initial state
		hangmanImageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("0.jpg"))));
	}

	private void handleExit(boolean isServer) {
		// Add logic for exiting
		if (!isServer) {
			// Add client-specific exit logic
			System.out.println("Client: Exiting...");
		}
		Platform.exit();
		System.exit(0);
	}

	private ImageView createHangmanImageView() {
		// Load the initial hangman image
		hangmanImageView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("0.jpg"))));
		hangmanImageView.setFitWidth(100); // Adjust the width as needed
		hangmanImageView.setPreserveRatio(true);
		return hangmanImageView;
	}

	private void updateHangmanImage(int incorrectGuessCount) {
		// Check if the incorrect guess count is within the limit (in this case, 6)
		if (incorrectGuessCount <= 6) {
			// Update the Hangman image source
			hangmanImageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(incorrectGuessCount + ".jpg"))));
		} else {
			// Display an alert or perform other actions when the limit is exceeded
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setTitle("Game Over");
			alert.setHeaderText(null);
			alert.setContentText("Maximum incorrect guesses reached. Game over!");
			alert.showAndWait();
		}
	}

//	private Text createDashedLineText() {
//		dashedLineText = new Text();
//		dashedLineText.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
//		return dashedLineText;
//	}

	private void handleServerMessage(String message) {
        String wordToGuess = null;
        if (message.startsWith("updateImage ")) {
            // Extract the incorrect guess count from the message
            int incorrectGuessCount = Integer.parseInt(message.substring(12));

            // Update the Hangman image based on the incorrect guess count
            updateHangmanImage(incorrectGuessCount);
        } else if (message.startsWith("wordToGuess ")) {
            // Extract the word to guess from the message
            wordToGuess = message.substring(12);

            // Update the dashed line with underscores for each letter in the word
            updateDashedLine(wordToGuess, Collections.emptyList());
        } else if (message.startsWith("correctGuess ")) {
            // Extract the correct letter and its position from the message
            String[] parts = message.substring(13).split(",");
            char correctLetter = parts[0].charAt(0);
            int correctPosition = Integer.parseInt(parts[1]);

//            // Update the dashed line with the correct letter at the correct position
//            updateDashedLine(wordToGuess, Collections.singletonList(correctPosition));
        } else if(message.startsWith("resetGame")){
			handleNewGame();



		}
		else {
            // Handle other messages if needed
        }
    }



	private void updateDashedLine(String wordToGuess, List<Integer> correctPositions) {
		char[] dashedLineArray = new char[wordToGuess.length() * 2];

		// Fill the dashed line array with underscores
		Arrays.fill(dashedLineArray, '_');

		// Replace underscores with correct letters at correct positions
		for (int position : correctPositions) {
			int index = position * 2;
			char correctLetter = wordToGuess.charAt(position);
			dashedLineArray[index] = correctLetter;
		}

		dashedLineText.setText(new String(dashedLineArray));
	}

	private VBox createServerClientLogs() {
		VBox logsBox = new VBox(10, listItems2);
		logsBox.setStyle("-fx-background-color: lightgray; -fx-padding: 10;");
		return logsBox;
	}

	private TilePane createQwertyKeyboard() {
		TilePane qwertyKeyboard = new TilePane();
		qwertyKeyboard.setPrefColumns(10);

		String qwertyLayout = "QWERTYUIOP\nASDFGHJKL\nZXCVBNM";
		for (char letter : qwertyLayout.toCharArray()) {
			if (letter != '\n') {
				Button letterButton = new Button(String.valueOf(letter));
				letterButton.setOnAction(event -> handleLetterButtonClick(letter));
				qwertyKeyboard.getChildren().add(letterButton);
			}
		}

		return qwertyKeyboard;
	}

	private void handleLetterButtonClick(char letter) {
		clientConnection.send("guess: " + letter);
	}

}


