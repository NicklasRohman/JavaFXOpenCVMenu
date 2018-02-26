package application;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import utilsPackage.Utils;

public class SixController {

	// FXML buttons
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	@FXML
	private ImageView originalFrame;
	// the FXML area for showing the mask
	@FXML
	private ImageView maskImage;
	// the slider for setting HSV ranges
	@FXML
	private ImageView morphImage;
	@FXML
	private Slider hueStart;
	@FXML
	private Slider hueStop;
	@FXML
	private Slider saturationStart;
	@FXML
	private Slider saturationStop;
	@FXML
	private Slider valueStart;
	@FXML
	private Slider valueStop;
	//FXML label to show the current values set whit the sliders
	@FXML
	private Label hsvCurrentValues;
	
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive;

	//property for object binding
	private ObjectProperty<String>hsvValuesProp;
	
	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	protected void startCamera() {

		//bind a tect property with the string containg the current range
		// of HSV values for object detection
		hsvValuesProp = new SimpleObjectProperty<>();
		this.hsvCurrentValues.textProperty().bind(hsvValuesProp);
		
//set a fixed width for all the image to show and preserve image ratio
		this.imageViewProperties(this.originalFrame,800);
		this.imageViewProperties(this.maskImage,300);
		this.imageViewProperties(this.morphImage, 300);
		
		
		if (!this.cameraActive) {
			// start the video capture
			this.capture.open(0);

			// is the video stream available?
			if (this.capture.isOpened()) {
				this.cameraActive = true;

				// Grab a frame every 33ms (30frame/sec)
				Runnable frameGrabber = new Runnable() {

					@Override
					public void run() {

						// effectively grab and process a single frame
						Mat frame = grabFrame();
						// convert and show the frame
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(originalFrame, imageToShow);
					}
				};
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				// update the button content
				this.cameraButton.setText("Stop Camera");
			} else {
				// log the error
				System.err.println("Failed to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.cameraButton.setText("Start Camera");
			// stop the timer
			this.stopAcquisition();
		}

	}


	/*
	 * Get a frame from the opened video stream(if any)
	 * 
	 * @return the{@link image} to show
	 */
	private Mat grabFrame() {
		Mat frame = new Mat();
		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);
				// if the frame is not empty, process it
				if (!frame.empty()) {
					
					//init
					Mat blurredImage = new Mat();
					Mat hsvImage = new Mat();
					Mat mask = new Mat();
					Mat morphOutput = new Mat();
					
					//remove some noise
					Imgproc.blur(frame, blurredImage, new Size(7,7));
					//convert the frame to HSV
					Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);
					//get thresholding values from the UI
					//remember: H ranges 0-180, S and V range 0-255
					Scalar minValues = new Scalar(this.hueStart.getValue(),this.saturationStart.getValue(),this.valueStart.getValue());
					Scalar maxValues = new Scalar(this.hueStop.getValue(),this.saturationStop.getValue(),this.valueStop.getValue());
					
					//show the current selected HSV range
					String  valuesToPoint = "Hue range: " + minValues.val[0] + "-"+ maxValues.val[0] +
							"\tSaturation range: "+minValues.val[1]+"-"+maxValues.val[1]+
							"\tValue range " + minValues.val[2] +"-"+ maxValues.val[2];
					Utils.onFXThread(this.hsvValuesProp, valuesToPoint);

					//threshold HSV image to select objects
					Core.inRange(hsvImage, minValues, maxValues, mask);
					//show the partial output
					this.updateImageView(this.maskImage, Utils.mat2Image(mask));
					//morphological operators
					//dilate whit large element, erode with small ones
					Mat dileateElemet = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24,24));
					Mat erodeElemet = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12,12));
					
					Imgproc.erode(mask, morphOutput, erodeElemet);
					Imgproc.erode(morphOutput, morphOutput, erodeElemet);
					
					Imgproc.dilate(morphOutput, morphOutput, dileateElemet);
					Imgproc.dilate(morphOutput, morphOutput, dileateElemet);
					
					//show the partial output
					this.updateImageView(this.morphImage, Utils.mat2Image(morphOutput));
					//find the object(s) contours and show them
					frame = this.findAndDrawObjects(morphOutput,frame);
					}
				
			} catch (Exception e) {
				// log the (full) error
				System.err.print("Exception during the image elaboration: ");
				e.printStackTrace();
			}
		}

		return frame;
	}

/**
 * Given a binary image containing one or more closed surfaces,use it as a mask to find and highlight the objects contours
 * @param maskedImage 
 * 				the binary image to be used as a mask
 * @param frame
 * 				the original{@link Mat} image to be used for drawing the objects contours
 * @return	the {@link Mat} image with the objects contours framed
 */
	private Mat findAndDrawObjects(Mat maskedImage, Mat frame) {

		//init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		
		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		
		//if any contour exist...
		if (hierarchy.size().height>0 && hierarchy.size().width>0) {
			//for each contour, display it in blue
			for (int idx = 0; idx >= 0;idx=(int) hierarchy.get(0, idx)[0]) {
				Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
			}
		}
		
		return frame;
	}

	/**
	 * Set typical {@link ImageView} properties: a fixed width and the information to preserve the original image ration
	 * @param image
	 * 				the {@link ImageView} to use
	 * @param dimension
	 * 				the width of the image to set
	 */
	private void imageViewProperties(ImageView image, int dimension) {

		//set a fixed width for the given ImageView
		image.setFitWidth(dimension);
		//preserve the image ratio
		image.setPreserveRatio(true);
	}


	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition() {
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

		if (this.capture.isOpened()) {
			// release the camera
			this.capture.release();
		}
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image) {
		Utils.onFXThread(view.imageProperty(), image);
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed() {
		this.stopAcquisition();
	}

}
