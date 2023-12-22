/*
 *  GuiServer.java
 *  Author: Mark Cirineo
 *  System: Intellij IDEA
 *  Course: CS 342
 *
 *
 *
 * GuiServer.java was borrowed from the Gui Server Client
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
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.HashMap;

public class GuiServer extends Application {

	private Button serverChoice;
	private HashMap<String, Scene> sceneMap;
	private HBox buttonBox;
	private BorderPane startPane;
	private HangmanServer serverConnection;
	private ListView<String> listItems;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Welcome to Hangman!");

		this.serverChoice = createStyledButton("Start Server");

		this.serverChoice.setOnAction(e -> {
			primaryStage.setScene(sceneMap.get("server"));
			primaryStage.setTitle("Hangman Game Data");
			serverConnection = new HangmanServer(data -> {
				Platform.runLater(() -> {
					listItems.getItems().add(data.toString());
					listItems.scrollTo(listItems.getItems().size() - 1);
				});
			});
		});

		this.buttonBox = new HBox(10, serverChoice);
		this.buttonBox.setPadding(new Insets(20));
		this.buttonBox.setAlignment(Pos.CENTER);
		startPane = new BorderPane();
		startPane.setTop(createTitle());
		startPane.setCenter(buttonBox);

		Scene startScene = new Scene(startPane, 500, 500);

		listItems = new ListView<>();

		sceneMap = new HashMap<>();
		sceneMap.put("server", createServerGui());

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

	public Scene createServerGui() {
		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(70));
		pane.setStyle("-fx-background-color: lightgreen");
		pane.setTop(createMenuBar(true));
		pane.setCenter(listItems);
		return new Scene(pane, 500, 400);
	}

	private MenuBar createMenuBar(boolean isServer) {
		MenuBar menuBar = new MenuBar();
		Menu menu = new Menu("Game");
		MenuItem newGameItem = new MenuItem("New Game");
		MenuItem exitItem = new MenuItem("Exit");

		newGameItem.setOnAction(e -> handleNewGame(isServer));
		exitItem.setOnAction(e -> handleExit(isServer));

		menu.getItems().addAll(newGameItem, exitItem);
		menuBar.getMenus().add(menu);

		return menuBar;
	}

	private void handleNewGame(boolean isServer) {
		// Add logic for starting a new game
		if (isServer) {
			// Add server-specific new game logic
			System.out.println("Server: Starting a new game...");
		}
	}

	private void handleExit(boolean isServer) {
		// Add logic for exiting
		if (isServer) {
			// Add server-specific exit logic
			System.out.println("Server: Exiting...");
		}
		Platform.exit();
		System.exit(0);
	}
}
