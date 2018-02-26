package application;

import java.io.ByteArrayInputStream;
import java.util.*;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SevenController {
	// FXML buttons
	@FXML
	private Button cameraButton;
	@FXML
	private Button applyButton;
	@FXML
	private Button snapshotButton;
	// the FXML area for showing the current frame (before calibration)
	@FXML
	private ImageView originalFrame;
	// the FXML area for showing the current frame (after calibration)
	@FXML
	private ImageView calibratedFrame;
	// info related to the calibration process
	@FXML
	private TextField numBoards;
	@FXML
	private TextField numHorCorners;
	@FXML
	private TextField numVertCorners;

	// a timer for acquiring the video stream
	private Timer timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture;
	// a flag to change the button behavior
	private boolean cameraActive;
	// the saved chessbooard image
	private Mat savedImage;
	// the calibrated camera frame
	private Image undistoredImage, CamStream;
	// various variables needed for the calibration
	private List<Mat> imagePoints;
	private List<Mat> objectPoints;
	private MatOfPoint3f obj;
	private MatOfPoint2f imageCorners;
	private int boardsNumber, numCornersHor, numCornersVer, successes;
	private Mat intrinsic, distCoeffs;
	private boolean isCalibrated;

	/**
	 * Init all the (global) variables needed in the controller
	 */
	protected void init() {
		this.capture = new VideoCapture();
		this.cameraActive = false;
		this.obj = new MatOfPoint3f();
		this.imageCorners = new MatOfPoint2f();
		this.savedImage = new Mat();
		this.undistoredImage = null;
		this.imagePoints = new ArrayList<>();
		this.objectPoints = new ArrayList<>();
		this.intrinsic = new Mat(3, 3, CvType.CV_32FC1);
		this.distCoeffs = new Mat();
		this.successes = 0;
		this.isCalibrated = false;

	}

	@FXML
	protected void updateSettings() {
		this.boardsNumber = Integer.parseInt(this.numBoards.getText());
		this.numCornersHor = Integer.parseInt(this.numHorCorners.getText());
		this.numCornersVer = Integer.parseInt(this.numVertCorners.getText());
		int numSquares = this.numCornersHor * this.numCornersVer;
		for (int j = 0; j < numSquares; j++) {
			obj.push_back(new MatOfPoint3f(new Point3(j / this.numCornersHor, j % this.numCornersVer, 0.0f)));
		}
		this.cameraButton.setDisable(false);
	}

	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	protected void startCamera() {

		if (!this.cameraActive) {
			// start the video capture
			this.capture.open(0);

			// is the video stream available?
			if (this.capture.isOpened()) {
				this.cameraActive = true;

				// Grab a frame every 33ms (30frame/sec)
				TimerTask frameGrabber = new TimerTask() {

					@Override
					public void run() {

						CamStream = grabFrame();
						//Show the original frames
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								originalFrame.setImage(CamStream);
								// set fixed width
								originalFrame.setFitWidth(680);
								// preserve image ratio
								originalFrame.setPreserveRatio(true);
								// show the original frames
								calibratedFrame.setImage(undistoredImage);
								// setfixed width
								calibratedFrame.setFitWidth(680);
								// preserve image ratio
								calibratedFrame.setPreserveRatio(true);
							}
						});
					}
				};
				
				this.timer = new Timer();
				this.timer.schedule(frameGrabber, 0,33);
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

		if (this.timer !=null) {
			this.timer.cancel();
			this.timer = null;
		}
		//release the camera
		this.capture.release();
		//clean the image areas
		originalFrame.setImage(null);
		calibratedFrame.setImage(null);
		}
	}

	/*
	 * Get a frame from the opened video stream(if any)
	 * 
	 * @return the{@link image} to show
	 */
	private Image grabFrame() {
		
		//init ecerything
		Image imageToShow = null;
		Mat frame = new Mat();
		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);
				// if the frame is not empty, process it
				if (!frame.empty()) {

					// show the chessboard pattern
					this.findAndDrawPoints(frame);
					if (this.isCalibrated) {
						//prepare the undistored image
						Mat undistored= new Mat();
						Imgproc.undistort(frame, undistored, intrinsic, distCoeffs);
						undistoredImage = mat2Image(undistored);
					}
					//convert the Mat object(openCV) to image(javaFX)
					imageToShow = mat2Image(frame);
					
					
				}

			} catch (Exception e) {
				// log the (full) error
				System.err.print("ERROR");
				e.printStackTrace();
		}
		}
		return imageToShow;
	}

	/**
	 * Take a snapshot to be used for the calibration process
	 */
	@FXML
	protected void takeSnapshot()
	{
		if (this.successes < this.boardsNumber)
		{
			// save all the needed values
			this.imagePoints.add(imageCorners);
			imageCorners = new MatOfPoint2f();
			this.objectPoints.add(obj);
			this.successes++;
		}
		
		// reach the correct number of images needed for the calibration
		if (this.successes == this.boardsNumber)
		{
			this.calibrateCamera();
		}
	}
	
	/**
	 * the effective camera calibration, to performed once in the program execution
	 */
	
		private void calibrateCamera() {
			//init needed variables according to opencv docs
			List<Mat>rvecs = new ArrayList<>();
			List<Mat>tvecs = new ArrayList<>();
			intrinsic.put(0, 0, 1);
			intrinsic.put(1, 1, 1);
			//calibrate!
			Calib3d.calibrateCamera(objectPoints, imagePoints, savedImage.size(), intrinsic, distCoeffs, rvecs, tvecs);
			this.isCalibrated = true;
			//you cannot take other snapshot, at this point..
			this.snapshotButton.setDisable(true);
	}

		/**
		 * Convert a MatObject(OpenCV) in the corresping Image for JavaFX
		 * @param frame
		 * 				the {@link Mat} representing the current frame
		 * @return the {@link image} to show
		 */
	private Image mat2Image(Mat frame) {

		//create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		//encode the frame in the buffer,according to the PNG format
		Imgcodecs.imencode(".png", frame, buffer);
		//build and return an Image created from the image encoded in the buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}


	/**
	 * Given a binary image containing one or more closed surfaces,use it as a
	 * mask to find and highlight the objects contours
	 * 
	 * @param maskedImage
	 *            the binary image to be used as a mask
	 * @param frame
	 *            the original{@link Mat} image to be used for drawing the
	 *            objects contours
	 */
	private void findAndDrawPoints(Mat frame) {

		// init
		Mat grayImage = new Mat();
		// I would perform this operation only before starting the calibration process
		
		if (this.successes <this.boardsNumber) {
			Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);
			//the size of the chessboard
			Size boardSize = new Size(this.numCornersHor,this.numCornersVer);
			//look for the inner chessboard corners
			boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners,Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE+Calib3d.CALIB_CB_FAST_CHECK);
			//all the required corners have been found..
			if (found) {
				//optimization
				TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER,30,0.1);
				Imgproc.cornerSubPix(grayImage, imageCorners, new Size(11,11),new Size(-1,-1),term);
				//save the current frame for further elaborations
				grayImage.copyTo(this.savedImage);
				//show the chessboard inner corner of screen
				Calib3d.drawChessboardCorners(frame, boardSize, imageCorners, found);
				
				//enable the option for taking a snapshot
				this.snapshotButton.setDisable(false);
			}
			else {
				this.snapshotButton.setDisable(true);
			}
		}
	}
}
