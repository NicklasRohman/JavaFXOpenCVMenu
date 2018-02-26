package message;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Messages {
	String menuAnswer;
	public Messages() {	}
	public int menuMessage() {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setMinWidth(250);
		stage.setMinHeight(250);

		ChoiceBox<String> menuBox = new ChoiceBox<>();
		menuBox.getItems().addAll("Gray Scale", "Image transformation", "Face Detector/Tracker", "Edge Detection", "Object Detection",
				"Calibrate Camera","Quit program");
		menuBox.setValue("Gray Scale");

		Button okButton = new Button("Submit");
		okButton.setOnAction(e -> getChose(menuBox, stage));

		VBox layout = new VBox(10);
		layout.getChildren().addAll(menuBox, okButton);
		layout.setAlignment(Pos.CENTER);

		Scene scene = new Scene(layout, 500, 500);
		stage.setScene(scene);
		stage.showAndWait();
		return convertAnswerToInteger(menuAnswer);

	}

	private void getChose(ChoiceBox<String> menuBox, Stage stage) {
		menuAnswer = menuBox.getValue();
		stage.close();
	}

	private int convertAnswerToInteger(String menuAnswer){
		int result;
		
		if (menuAnswer.equalsIgnoreCase("Gray Scale")) {
			result = 2;
		}
		else if (menuAnswer.equalsIgnoreCase("Image transformation")) {
			result = 3;
		}
		else if (menuAnswer.equalsIgnoreCase("Face Detector/Tracker")) {
			result = 4;
		}else if (menuAnswer.equalsIgnoreCase("Edge Detection")) {
			result = 5;
		}else if (menuAnswer.equalsIgnoreCase("Object Detection")) {
			result = 6;
		}else if (menuAnswer.equalsIgnoreCase("Calibrate Camera")) {
			result = 7;
		}else  {
			result = 0;
		}
		return result;
	}
	
	
}

