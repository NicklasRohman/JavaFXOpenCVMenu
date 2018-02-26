package application;

import org.opencv.core.*;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import message.Messages;

public class MainClass extends Application {
	boolean runInit;
	Messages mess = new Messages();
	FXMLLoader loader;
	@Override
	public void start(Stage primaryStage) {
		try {int menuMessageAnswer = 0;
			try {
				menuMessageAnswer = mess.menuMessage();
			} catch (Exception e) {
				System.exit(0);
			}
			
			System.out.println(menuMessageAnswer);
			
			fXMLresourceLoader(menuMessageAnswer);
			
			
			// store the root element so that the controllers can use it
			BorderPane rootElement = (BorderPane) loader.load();
			// create and style a scene
			Scene scene = new Scene(rootElement, 1800, 1000);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			// create the stage with the given title and the previously created
			// scene
			primaryStage.setTitle("Video processing");
			primaryStage.setScene(scene);
			// show the GUI
			primaryStage.show();
			// set the proper behavior on closing the application
			if (menuMessageAnswer == 2) {
				SecoundController controller = loader.getController();	
				primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
					public void handle(WindowEvent we) {
						controller.setClosed();
					}
				}));
			}
			else if (menuMessageAnswer==3) {
			ThirdController thirdController = loader.getController();
			thirdController.init();
			primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we) {
				}
			}));
			}
			else if (menuMessageAnswer==4) {
				ForthController forthController = loader.getController();
				forthController.init();
				primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
					public void handle(WindowEvent we) {
					}
				}));	
			}
			else if (menuMessageAnswer==5) {
				FifthController fifthController = loader.getController();
				fifthController.init();
				primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
					public void handle(WindowEvent we) {
						fifthController.setClosed();
					}
				}));	
			}
			else if (menuMessageAnswer==6) {
				primaryStage.setX(1000);
				primaryStage.setY(0);			

				SixController sixController = loader.getController();
				primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
					public void handle(WindowEvent we) {
						sixController.setClosed();
					}
				}));
			}
			
			
			
			
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * For launching the application...
	 * 
	 * @param args
	 *            optional params
	 */
	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		launch(args);
	}
	
	private void fXMLresourceLoader(int menuMessageAnswer){
		// load the FXML resource
		if (menuMessageAnswer == 2) {
			loader = new FXMLLoader(getClass().getResource("SecoundJFX.fxml"));
		}
		else if (menuMessageAnswer == 3) {
			loader = new FXMLLoader(getClass().getResource("ThirdJFX.fxml"));
		}
		else if (menuMessageAnswer == 4) {
			loader = new FXMLLoader(getClass().getResource("ForthFX.fxml"));
		}
		else if (menuMessageAnswer == 5) {
			loader = new FXMLLoader(getClass().getResource("FifthFX.fxml"));
		}
		else if (menuMessageAnswer == 6) {
			loader = new FXMLLoader(getClass().getResource("SixFX.fxml"));
		}
		else if (menuMessageAnswer == 7) {
			loader = new FXMLLoader(getClass().getResource("SevenFX.fxml"));
		}
		else {
			System.exit(0);;
		}
	}
	
}
