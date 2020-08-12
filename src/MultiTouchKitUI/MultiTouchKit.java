/***************************************************
 This is a library for the Multi-Touch Kit
 Designed and tested to work with Arduino Uno, MEGA2560, LilyPad(ATmega 328P)
 Note: Please remind to disconnect AREF pin from AVCC for Lilypad

 For details on using this library see the tutorial at:
 ----> https://hci.cs.uni-saarland.de/multi-touch-kit/

 Written by Jan Dickmann, Narjes Pourjafarian, Juergen Steimle (Saarland University), Anusha Withana (University of Sydney), Joe Paradiso (MIT)
 MIT license, all text above must be included in any redistribution
 ****************************************************/

package MultiTouchKitUI;

import processing.core.*;
import processing.serial.*;
import processing.data.Table;
import processing.data.TableRow;
import blobDetection.*;
import gab.opencv.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.Size;

public class MultiTouchKit {

	private PApplet parent; // the "Processing Object"
	private PSurface psurface; // the "Surface" Object, required for resizing the window

	private int[][] rawValues; // stores the latest values for every tx/rx intersection
	private int[][] values; // stores the difference of the rawValues and the baseline after calibration is
							// done (or adjusted values if adjustment is turned on)

	// variables that have to be set by the user
	private int rx; // number of receiver lines
	private int tx; // number of transmission lines

	// variables for thresholds and to enable/disable features

	// drawing to screen
	private boolean autoDraw = false; // false: will not draw anything to the screen, true: will draw to the screen
										// (what exactly depends on some of the other options)

	// information and control
	private boolean calibrationDone = false; // true if the indication is done
	private boolean setInterpolate = true;
	private boolean setComputeBlobs = true;
	private boolean enableUI = false;
	private boolean recordReplay = false;
	private boolean playReplay = false;
	private int currentRow = 0;

	// Replays
	private Table replay; // table for replays
	private String replayName = "Replay"; // name of the replay

	// variables for amplification/suppression of the input
	// those 4 values can be used to scale the input up or down
	private int minInputRange = 10;
	private int maxInputRange = 600;
	private int minOutputRange = 0;
	private int maxOutputRange = 255;
	private int colorrange = 255; // colorrange of setColors (255 should be best), can be used to scale the input
	private boolean adjust = false; // enable(true) or disable(false) adjustment

	// variables for adjustment
	private int maxNoiseThreshold = 60; // in adjust: if a value higher than this is detected it is considered a touch
										// and will get adjusted
	private int noiseThreshold = 10; // everything below this is considered noise

	// draw blobs
	private boolean drawBlobs = false; // false: will not draw blobs, true: will draw blobs
	private boolean drawBlobCenters = true; // true: blob centers will be drawn (if drawBlobs is also true)
	private boolean drawBlobEdges = true; // true: blob edges will be drawn (if drawBlobs is also true)
	private float thresh = 0.85f; // threshold for blob detection, 1.0 is max
	private boolean cubic = true; // false: interpolation is nearest neighbors (blocky), true: interpolation is
									// cubic (smooth)
	private int sizePerIntersection = 50;// rx*sizePerIntersection will be the width and tx*sizePerIntersection will be
											// the height of scaledbc if the height is set dynamically
	private int bgColor;

	// calibration
	private long waittime = 2000000000; // how much time the the sketch should take at least to set the Baseline (2
										// seconds = 2000000000)

	// Variables for the Serial connection
	private Serial myPort; // Serial Object
	private boolean init = true; // auxiliary variable, true indicates that no values have been recorded yet and
									// the starttime has not been set yet
	private boolean connected = false; // true if the serial connection to the arduino is established
	private boolean changes = false; // auxiliary variable, is true if readSerial has actually received something

	// variables needed to set the Baseline
	private long[][] bt_average; // helps to set the BaseLine
	private long[][] BaseLine; // used to set the Baseline and then used to remove noise from input values
								// (rawValues) coming from the arduino
	private long starttime; // auxiliary variable, needed to set the Baseline
	private boolean baseLineSet = false; // auxiliary variable, false while the Baseline is not set yet
	private boolean calibrationError = false; // auxiliary variable, true if the values for the calibration are lower
												// than the calibrationErrorThreshold
	private int calibrationErrorThreshold = -70; // if the values for inital calibration are lower than this a warning
													// will be printed

	// variables/objects for interpolation and blobdetection
	private OpenCV opencv; // openCV object for interpolation
	private BlobDetection theBlobDetection; // BlobDetection object for BlobDetection
	private PImage img; // image that will be interpolated
	private PImage scaledbc; // image that results from interpolation and that will be drawn to the screen if
								// "autodraw" = true

	// positions/sizes of UI Elements

	// ui position
	private int uiX;
	private int uiY;
	// ui size
	private int uiWidth = 300;
	private int uiHeight = 50;
	// threshold number position
	private int numberX;
	private int numberY;
	// button positions
	private int plusButtonX;
	private int plusButtonY;
	private int minusButtonX;
	private int minusButtonY;
	private int plusButtonSmallX;
	private int plusButtonSmallY;
	private int minusButtonSmallX;
	private int minusButtonSmallY;
	private int anyHeight, anyWidth; // dimensions of scaledbc
	// button dimensions
	private int buttonWidth = 40;
	private int buttonHeight = 40;
	private int buttonMiddle = 20;
	// dimensions of the rects for the plus/minus signs in the UI
	private int minusWidth = 30;
	private int minusHeight = 10;
	private int minusWidthSmall = 20;
	private int minusHeightSmall = 6;
	private int imgX;
	private int imgY;
	// creating the fonts to draw on screen
	private PFont f; // font for the text while calibrating
	private PFont fUI; // font for the number in the UI

	private boolean mousepressed = false;

	/**
	 * 
	 * creates the MultiTouchKit object, assigns several variables, initiallizes
	 * arrays, and calls connect() to establish the serial connection will adjust
	 * the window size if necessary
	 * 
	 * @param parent     the parent processing sketch, so just "this" when you call
	 *                   the constructor from processing
	 * @param tx         number of transmission lines
	 * @param rx         number of receiver lines
	 * @param serialPort the serialPort of the arduino
	 */

	public MultiTouchKit(PApplet parent, int tx, int rx, int serialPort) {

		this.parent = parent;
		this.psurface = this.parent.getSurface();
		this.rx = rx;
		this.tx = tx;

		this.anyHeight = tx * sizePerIntersection;
		this.anyWidth = rx * sizePerIntersection;
		// resize screen if it's too small
		if (parent.height < this.anyHeight) {
			this.psurface.setSize(parent.width, this.anyHeight);
		}
		if (parent.width < this.anyWidth) {
			this.psurface.setSize(this.anyWidth, parent.height);
		}

		// position of scaledbc
		imgX = parent.width / 2;
		imgY = parent.height / 2;

		// some of the preset options, look above for meaning
		this.autoDraw = false;
		this.drawBlobs = true;
		this.cubic = true;

		rawValues = new int[tx][rx];
		values = new int[tx][rx];

		img = parent.createImage(rx, tx, PConstants.RGB);
		scaledbc = parent.createImage(anyWidth, anyHeight, PConstants.ALPHA);
		theBlobDetection = new BlobDetection(scaledbc.width, scaledbc.height);
		theBlobDetection.setThreshold(thresh);

		bgColor = parent.color(100);

		bt_average = new long[rx][tx];

		BaseLine = new long[rx][tx];

		// setting the fonts
		f = parent.createFont("Arial", 16, true);
		parent.textFont(f, 36);
		fUI = parent.createFont("Arial", 20, true);
		parent.textFont(f, 30);

		// set the modes of all objects that can be drawn to CENTER for easier/more
		// intuitiv positioning
		parent.rectMode(PConstants.CENTER);
		parent.textAlign(PConstants.CENTER, PConstants.CENTER);
		parent.imageMode(PConstants.CENTER);
		
		
		// try connect to arduino via serial
		connect(serialPort);

		parent.registerMethod("draw", this);
		parent.registerMethod("dispose", this);

	}

	/**
	 * Method that's called at the end of the draw() method in the parent sketch, so
	 * if autoDraw == true, it will draw over whatever you do in the processing
	 * sketch it reads from the serial port, sets the colors if necessary, creates
	 * the baseline and draws to the screen if enabled
	 */
	public void draw() {
		// printing a warning when it seems that something went wrong during
		// calibration:
		// (generally this happens when the calibrated values are very negative, so
		// smaller than "calibrationErrorThreshold", but they should be close to 0)

		if (calibrationError) {
			System.out.println("===========================================================");
			System.out.println("There seems to be a problem with the calibration,please start again");
			System.out.println("===========================================================");
		}
		
		

		// play Replay or get input from Serial
		if (playReplay) {
			if(!(currentRow >= replay.getRowCount())) {
				// read the latest data from the SerialPort
			// read the next line from the replay csv
			readReplay();
			currentRow++;
			}else {
				System.out.println("Replay is over!");
			}

		} else {
				readSerial();
			
		}

		// if the SerialPort actually received something, set the Colors
		if (changes) {
			setColors();
		}

		// the Baseline is set here
		if ((System.nanoTime() - starttime) > waittime && !baseLineSet) {
			baseLineSet = true;

			// check that bt_average is not 0, so that we can not accidentally divide by 0
			// if it is 0, the sketch will take more time to collect values to get a proper
			// Baseline
			for (int i = 0; i < rx; i++) {
				for (int j = 0; j < tx; j++) {
					if (bt_average[i][j] == 0) {
						baseLineSet = false;
						starttime = System.nanoTime();
						break;
					}
				}
			}
			// calculate the averages of the collected values and save them as Baseline
			if (baseLineSet) {
				for (int i = 0; i < rx; i++) {
					for (int j = 0; j < tx; j++) {
						BaseLine[i][j] = BaseLine[i][j] / bt_average[i][j];
					}
				}
				calibrationDone = true;
				System.out.println("Calibration done, ready to go");
			}

		}

		if (setInterpolate && calibrationDone) {
			interpolate();
		}

		if (setComputeBlobs && calibrationDone) {
			// computing the blobs on scaledbc
			// (imageForBlobDetection() will replace the edges of scaledbc with black pixels
			// as workaround for a bug in BlobDetection)
			theBlobDetection.computeBlobs(imageForBlobDetection(scaledbc).pixels);
		}

		// drawing to the screen if autoDraw == true
		if (autoDraw) {

			// if the baseline is not set yet, the sensor should not be touched, therefore
			// "Calibrating!" will be printed on the screen to indicate this
			if (calibrationDone) {
				parent.background(bgColor);
				parent.image(scaledbc, imgX, imgY);
				if (enableUI) { // detect if ui buttons are pressed
					drawUI();
					if (parent.mousePressed && !mousepressed) {
						if (parent.mouseX > plusButtonSmallX - buttonMiddle
								&& parent.mouseX < plusButtonSmallX + buttonMiddle
								&& parent.mouseY > plusButtonSmallY - buttonMiddle
								&& parent.mouseY < plusButtonSmallY + buttonMiddle) {
							maxInputRange++;
						}
						if (parent.mouseX > minusButtonSmallX - buttonMiddle
								&& parent.mouseX < minusButtonSmallX + buttonMiddle
								&& parent.mouseY > minusButtonSmallY - buttonMiddle
								&& parent.mouseY < minusButtonSmallY + buttonMiddle) {
							maxInputRange--;
						}
					} else {
						if (parent.mousePressed) {
							if (parent.mouseX > plusButtonX - buttonMiddle && parent.mouseX < plusButtonX + buttonMiddle
									&& parent.mouseY > plusButtonY - buttonMiddle
									&& parent.mouseY < plusButtonY + buttonMiddle) {
								maxInputRange++;
							}
							if (parent.mouseX > minusButtonX - buttonMiddle
									&& parent.mouseX < minusButtonX + buttonMiddle
									&& parent.mouseY > minusButtonY - buttonMiddle
									&& parent.mouseY < minusButtonY + buttonMiddle) {
								maxInputRange--;
							}
						}
					}
					mousepressed = parent.mousePressed;
					// updateUI(); updateUI is not working yet
				}
			} else {
				parent.background(bgColor);
				parent.fill(255);
				parent.text("Calibrating!", imgX, imgY);
			}

			if (drawBlobs && calibrationDone) {
				// draw the blobs
				drawBlobsAndEdges(drawBlobEdges, drawBlobCenters); // (Edge,Blob)
			}
		}

	}

	// TODO remove this? I dont think it is used/needed
	void mouseClicked() {
		if (parent.mouseX > plusButtonSmallX - buttonMiddle && parent.mouseX < plusButtonSmallX + buttonMiddle
				&& parent.mouseY > plusButtonSmallY - buttonMiddle && parent.mouseY < plusButtonSmallY + buttonMiddle) {
			maxInputRange++;
		}
		if (parent.mouseX > minusButtonSmallX - buttonMiddle && parent.mouseX < minusButtonSmallX + buttonMiddle
				&& parent.mouseY > minusButtonSmallY - buttonMiddle
				&& parent.mouseY < minusButtonSmallY + buttonMiddle) {
			maxInputRange--;
		}
	}

	/**
	 * setupUI will (re-)calculate the position of all UI elements, depending on the
	 * current window height and width
	 */
	private void setupUI() {
		// img position depends on the UI beeing enabled or not
		if (enableUI) {
			imgY = (parent.height - 50) / 2;

			uiX = parent.width / 2;
			uiY = parent.height - 25;
			plusButtonX = uiX + 120;
			plusButtonY = uiY;
			minusButtonX = uiX - 120;
			minusButtonY = uiY;
			plusButtonSmallX = uiX + 70;
			plusButtonSmallY = uiY;
			minusButtonSmallX = uiX - 70;
			minusButtonSmallY = uiY;
			numberX = uiX;
			numberY = uiY;
		} else {
			imgY = parent.height / 2;
		}
		imgX = parent.width / 2;

	}

	/**
	 * draws the UI
	 */
	private void drawUI() {
		parent.stroke(255);
		parent.fill(parent.color(100));
		parent.strokeWeight(1);
		parent.rect(uiX, uiY, uiWidth, uiHeight);

		parent.fill(parent.color(0));
		parent.strokeWeight(1);

		parent.rect(plusButtonX, plusButtonY, buttonWidth, buttonHeight);
		parent.rect(minusButtonX, minusButtonY, buttonWidth, buttonHeight);

		parent.rect(plusButtonSmallX, plusButtonSmallY, buttonWidth, buttonHeight);
		parent.rect(minusButtonSmallX, minusButtonSmallY, buttonWidth, buttonHeight);

		parent.fill(parent.color(255));
		parent.strokeWeight(0);
		parent.stroke(255);
		parent.rect(plusButtonX, plusButtonY, minusWidth, minusHeight);
		parent.rect(plusButtonX, plusButtonY, minusHeight, minusWidth);
		parent.rect(minusButtonX, minusButtonY, minusWidth, minusHeight);

		parent.rect(plusButtonSmallX, plusButtonSmallY, minusWidthSmall, minusHeightSmall);
		parent.rect(plusButtonSmallX, plusButtonSmallY, minusHeightSmall, minusWidthSmall);
		parent.rect(minusButtonSmallX, minusButtonSmallY, minusWidthSmall, minusHeightSmall);

		// writing the updated number
		parent.fill(parent.color(0));
		parent.text(maxInputRange, numberX, numberY);

	}

	/**
	 * WORK IN PROGESS this method could in the future be used so that the full ui
	 * doesn't have to be redrawn every frame, and instead only the bit of it that
	 * changes gets updated for a potential performance boost, it is not finished
	 * because it's not clear if this will actually increase performance by a
	 * significant amount
	 */
	private void updateUI() {
		// Erasing the Number
		parent.fill(parent.color(100));
		parent.noStroke();
		parent.rect(uiX, uiY, 70, 45);

		// writing the updated number
		parent.fill(parent.color(0));
		parent.text(maxInputRange, numberX, numberY);

	}

	/**
	 * read from the Serial Port if anything has been read: changes = true else
	 * changes = false from the input, everything that is not "," or a number is
	 * removed, than the string is split at the commas the numbers are saved into
	 * the "rawValues[][]" array
	 * 
	 */
	public void readSerial() {
		changes = false;
		String inputString = "";
		if (connected) {
			while (myPort.available() > 0) {
				String myString = myPort.readStringUntil('\n');
				if (myString == null) {
					break;
				}

				if (init) { // at the beginning of the sketch, start the timer for setting the Baseline
					starttime = System.nanoTime();
					init = false;
				}
				if (myString != null) {

					// remove unnecessary parts of the string (potential corrupted parts too)
					myString = myString.replaceAll("[^0-9,]+", "");

					// if there is no "," something broke or the input was corrupted
					if (!myString.contains(",")) {
						break;
					}

					inputString = inputString + myString + "\n"; // TODO check this

					// split the string into rx+1 numbers (still string type though)
					String[] splitString = myString.split(",");

					// check if the correct amount of strings are in the array
					if (splitString.length != rx + 1) {
						break;
					}

					// TODO remove this, it's probably unnecessary
					if (!splitString[0].matches("[0-9]+")) {
						break;
					}

					int t = Integer.parseInt(splitString[0]); // TX of the received input

					for (int i = 0; i < splitString.length; i++) {
						if (i > 0) {
							// TODO remove this, it's probably unnecessary
							if (!splitString[i].matches("[0-9]+")) {
								break;
							}
							changes = true;

							// save the new values
							rawValues[t][i - 1] = Integer.parseInt(splitString[i]);
						}
					}
				}
			}

			if (recordReplay) {
				TableRow newRow = replay.addRow();
				newRow.setString("Input", inputString);
				parent.saveTable(replay, replayName + ".csv");
			}
		}
	}

	/**
	 * read from the Serial Port if anything has been read: changes = true else
	 * changes = false from the input, everything that is not "," or a number is
	 * removed, than the string is split at the commas the numbers are saved into
	 * the "rawValues[][]" array
	 * 
	 */
	public void readReplay() {
		String inputString = replay.getString(currentRow, "Input");
		String[] splitInputString = inputString.split("\n");

		for (int n = 0; n < splitInputString.length; n++) {

			String myString = splitInputString[n];
			if (myString == null) {
				break;
			}

			if (init) { // at the beginning of the sketch, start the timer for setting the Baseline
				starttime = System.nanoTime();
				init = false;
			}
			if (myString != null) {

				// split the string into rx+1 numbers (still string type though)
				String[] splitString = myString.split(",");
				// check if the correct amount of strings are in the array
				if (splitString.length != rx + 1) {
					break;
				}
				// TODO remove this, it's probably unnecessary
				if (!splitString[0].matches("[0-9]+")) {
					break;
				}
				int t = Integer.parseInt(splitString[0]); // TX of the received input

				for (int i = 0; i < splitString.length; i++) {
					if (i > 0) {
						if (!splitString[i].matches("[0-9]+")) {
							break;
						}

						changes = true;

						// save the new values
						rawValues[t][i - 1] = Integer.parseInt(splitString[i]);

					}
				}
			}
		}
	}

	/**
	 * connects the library to the arduino via serial
	 * 
	 * @param serialPort the serialPort of the arduino
	 */
	private void connect(int serialPort) {
		if (serialPort <= Serial.list().length && Serial.list().length > 0) { //TODO check if this is correct 
			myPort = new Serial(parent, Serial.list()[serialPort], 115200);
			myPort.clear(); // clear the buffer
			myPort.bufferUntil(10); // always buffer until the newline symbol
			connected = true;
		} else {
			PApplet.println("Error: serial Port not found");
			connected = false;
		}

	}

	/**
	 * disconnect the library from the arduino
	 */
	private void disconnect() {
		myPort.clear(); // clear the serial buffer
		myPort.stop(); // stop the connection
	}

	/**
	 * called if the parentsketch shuts down, makes sure that it disconnects from
	 * the arduino
	 */
	public void dispose() {
		disconnect();
	}

	/**
	 * 
	 * @return true if calibration is done, else false
	 */
	public boolean calibrationDone() {
		return calibrationDone;
	}

	/**
	 * 
	 * @return latest raw values
	 */
	public int[][] getRawValues() {
		return rawValues;
	}

	/**
	 * 
	 * @return values (raw - BaseLine) OR adjusted values (when adjustment is
	 *         enabled)
	 */
	public int[][] getAdjustedValues() {
		return values;
	}

	/**
	 * 
	 * @return returns the array with all BaseLines
	 */
	public long[][] getBaseLine() {
		return BaseLine;
	}

	/**
	 * option to draw the blobs
	 * 
	 * @param drawBlobs true: draw the blobs, false: don't draw them
	 */
	public void drawBlobs(boolean drawBlobs) {
		this.drawBlobs = drawBlobs;
	}

	/**
	 * set the interpolation mode
	 * 
	 * @param cubic true: use cubic interpolation (smooth), false: use "nearest"
	 *              (blocky)
	 */
	public void interpolationCubic(boolean cubic) {
		this.cubic = cubic;
	}

	/**
	 * enable the UI
	 * 
	 * @param b, true = ui enabled, false = ui disabled
	 */
	public void enableUI(boolean b) {
		this.enableUI = true;
		if (parent.height < this.anyHeight + 50) {
			this.psurface.setSize(parent.width, this.anyHeight + 50);
		}
		if (parent.width < 300) {
			this.psurface.setSize(300, parent.height);
		}
		setupUI();
		drawUI();
	}

	/**
	 * This can be used to start recording a replay
	 * 
	 * @param record, true -> start recording, false -> stop recording
	 */
	public void setRecordReplay(boolean record) {
		this.recordReplay = record;
		if (this.recordReplay) {
			replay = new Table();
			replay.addColumn("Input");
		}
	}

	/**
	 * This can be used to play a replay
	 * 
	 * @param play, true -> start playing, false -> stop playing
	 */
	public void playReplay(boolean play, String replayPath) {
		this.playReplay = play;
		baseLineSet = false;
		calibrationDone = false;
		init = true;
		replay = parent.loadTable(replayPath, "header");

	}

	/**
	 * This can be used to set the replay name
	 * 
	 * @param name, the new replay name
	 */
	public void setReplayName(String name) {
		this.replayName = name;
	}

	/**
	 * option to activate/deactivate autodrawing
	 * 
	 * @param draw true: draw to the screen, false: don't
	 */
	public void autoDraw(boolean draw) {
		this.autoDraw = draw;
	}

	/**
	 * with this, interpolation can be disabled (e.g. for performance reasons) but
	 * autodraw will then not work anymore and scaledbc will be null or won't be
	 * updated anymore also the blobs will not be properly computed anymore
	 * 
	 * @param t true = enable interpolation, false = disable interpolation
	 */
	public void setInterpolate(boolean t) {
		this.setInterpolate = t;
	}

	/**
	 * set the new size of the sketch window manually
	 * 
	 * @param w, new window width
	 * @param h, new window height
	 */
	public void setWindowSize(int w, int h) {
		this.psurface.setSize(w, h);

		// make sure that the window is still big enough
		if (enableUI) {
			enableUI(true);
		} else {
			if (parent.height < this.anyHeight) {
				this.psurface.setSize(parent.width, this.anyHeight);
			}
			if (parent.width < this.anyWidth) {
				this.psurface.setSize(this.anyWidth, parent.height);
			}
		}

		setupUI(); // adjust the position of UI elements

	}

	/**
	 * this basically controls the overall size of scaledbc, if it is not resized
	 * manually with setSbcSize() this value can be set very high if you only have
	 * very few intersections (e.g. with a 4x4 sensor = 16 intersections)
	 * 
	 * @param t new size that each intersection will have in scaledbc
	 */
	public void setSizePerIntersection(int t) {
		this.sizePerIntersection = t;
		setSbcSize(rx * t, tx * t); // the standard values get overwritten
		// make sure that the window is still big enough
		if (enableUI) {
			enableUI(true);
		} else {
			if (parent.height < this.anyHeight) {
				this.psurface.setSize(parent.width, this.anyHeight);
			}
			if (parent.width < this.anyWidth) {
				this.psurface.setSize(this.anyWidth, parent.height);
			}
		}

		setupUI(); // adjust the position of UI elements

	}

	/**
	 * with this you can manually set a size for scaledbc
	 * 
	 * @param w, width of scaledbc
	 * @param h, height of scaledbc
	 */
	public void setSbcSize(int w, int h) {

		this.anyHeight = h;
		this.anyWidth = w;
		// resize screen if it's too small
		if (parent.height < this.anyHeight) {
			this.psurface.setSize(parent.width, this.anyHeight);
		}
		if (parent.width < this.anyWidth) {
			this.psurface.setSize(this.anyWidth, parent.height);
		}
		setupUI();
		scaledbc = parent.createImage( anyWidth, anyHeight, PConstants.ALPHA);
		theBlobDetection = new BlobDetection(scaledbc.width, scaledbc.height);
		theBlobDetection.setThreshold(thresh);
	}

	/**
	 * set the backgroundcolor
	 * 
	 * @param c, new color (use processings color() function)
	 */
	public void setBGColor(int c) {

		this.bgColor = c;
	}

	/**
	 * with this, blob computation can be disabled (e.g. for performance reasons)
	 * with this disabled, blobDetection will not contain the most recent blobs
	 * anymore
	 * 
	 * @param t true = enable blob computation, false = disable blob computation
	 */
	public void setComputeBlobs(boolean t) {
		this.setComputeBlobs = t;
	}

	/**
	 * set the timeframe to collect values for the Baseline, even with a 16x16
	 * sensor, 2 seconds should always be sufficient set this in "setup()" if you
	 * want to change it
	 * 
	 * @param t time to collect values for Baseline at the beginning
	 */
	public void setWaittime(long t) {
		waittime = t;
	}

	/**
	 * set the threshold for BlobDetection
	 * 
	 * @param newThresh the threshold
	 */
	public void setThresh(float newThresh) {
		this.thresh = newThresh;
		theBlobDetection.setThreshold(thresh);
	}

	/**
	 * set the colorrange used in setColors(), look at the documentation of the
	 * processing method colorMode() to understand what is happening
	 * (https://processing.org/reference/colorMode_.html)
	 * 
	 * @param colorange
	 */
	public void setColorrange(int colorange) {
		this.colorrange = colorange;
	}

	/**
	 * 
	 * @param minInputRange the minimum raw input value that can be received from
	 *                      the Arduino
	 */
	public void setMinInputRange(int minInputRange) {
		this.minInputRange = minInputRange;
	}

	/**
	 * this is supposed to be the maximum raw input value that can be received from
	 * the Arduino but you can use this to suppress or amplify the input signal, so
	 * if your input is too weak, choose a smaller maxInputRange
	 *
	 * @param maxInputRange the maximum raw input value that can be received from
	 *                      the Arduino
	 */
	public void setMaxInputRange(int maxInputRange) {
		this.maxInputRange = maxInputRange;
	}

	/**
	 * 
	 * @param minOutputRange the minimum grey value that can be displayed by the
	 *                       Processing sketch (so gernally 0)
	 */
	public void setMinOutputRange(int minOutputRange) {
		this.minOutputRange = minOutputRange;
	}

	/**
	 * 
	 * @param maxOutputRange the maximum grey value that can be displayed by the
	 *                       Processing sketch (so gernally 255)
	 */
	public void setMaxOutputRange(int maxOutputRange) {
		this.maxOutputRange = maxOutputRange;
	}

	/**
	 * 
	 * @param maxNoiseThreshhold everything bigger than this will be considered a
	 *                           touch by adjust()
	 */
	public void setMaxNoiseThreshhold(int maxNoiseThreshhold) {
		this.maxNoiseThreshold = maxNoiseThreshhold;
	}

	/**
	 * turn adjustment on/off
	 * 
	 * @param adjust true: adjustment on, false: calibration off
	 */
	public void setAdjust(boolean adjust) {
		this.adjust = adjust;
	}

	/**
	 * get the BlobDetection object use
	 * "theBlobDetection.computeBlobs(scaledbc.pixels);" to get the blobs
	 * 
	 * @return theBlobDetection
	 */
	public BlobDetection getBlobDetection() {
		return theBlobDetection;
	}

	/**
	 * 
	 * 
	 * @return noiseThreshhold everything lower than this will be considered noise
	 *         by adjust()
	 */
	public void setNoiseThreshhold(int noiseThreshhold) {
		this.noiseThreshold = noiseThreshhold;
	}

	/**
	 * option to enable/disable drawing of blob centers
	 * 
	 * @param dBC true: draw blob centers, false: don't draw blob centers
	 */
	public void setDrawBlobCenters(boolean dBC) {
		drawBlobCenters = dBC;
	}

	/**
	 * option to enable/disable drawing of blob edges
	 * 
	 * @param dBE true: draw blob edges, false: don't draw blob edges
	 */
	public void setDrawBlobEdges(boolean dBE) {
		drawBlobCenters = dBE;
	}

	/**
	 * Useful with theBlobDetection for
	 * "theBlobDetection.computeBlobs(scaledbc.pixels)" to get the blobs
	 * 
	 * @return the interpolated image
	 */
	public PImage getScaledbc() {
		return scaledbc;
	}

	/**
	 * set the calibrationErrorThreshold lower in case that everything is actually
	 * correct or the message bothers you
	 * 
	 * @param t new threshold
	 */
	public void setCalErrThreshold(int t) {
		calibrationErrorThreshold = t;
	}

	/**
	 * rescales "img" to "scaledbc", either using cubic or nearest neighbor
	 * interpolation
	 */
	public void interpolate() {
		opencv = new OpenCV(parent, img); // load image
		Mat mat1 = opencv.getGray(); // get grayscale matrix
		
		Mat mat2 = new Mat(anyWidth, anyHeight, mat1.type()); // new matrix to store resize image
		Size sz = new Size(anyWidth, anyHeight); // size to be resized

		if (cubic) {
			Imgproc.resize(mat1, mat2, sz, 0, 0, Imgproc.INTER_CUBIC); // resize
		} else {
			Imgproc.resize(mat1, mat2, sz, 0, 0, Imgproc.INTER_NEAREST); // resize
		}

		opencv.toPImage(mat2, scaledbc); // store in Pimage for drawing later
	}

	/**
	 * draw the Blob centers/edges to the screen look at the documentation of
	 * BlobDetection for better understanding
	 * (http://www.v3ga.net/processing/BlobDetection/index-page-documentation.html)
	 * 
	 * @param drawBlobs true: draw centers, false: don't draw centers
	 * @param drawEdges true: draw edges, false: don't draw edges
	 */
	public void drawBlobsAndEdges(boolean drawBlobs, boolean drawEdges) {
		parent.noFill();
		Blob b;
		EdgeVertex eA, eB;
		for (int n = 0; n < theBlobDetection.getBlobNb(); n++) {
			b = theBlobDetection.getBlob(n);
			if (b != null) {
				// Edges
				if (drawEdges) {
					parent.strokeWeight(2);
					parent.stroke(0, 255, 0);
					for (int m = 0; m < b.getEdgeNb(); m++) {
						eA = b.getEdgeVertexA(m);
						eB = b.getEdgeVertexB(m);
						if (eA != null && eB != null)
							parent.line((eA.x * anyWidth) + imgX - anyWidth / 2,
									(eA.y * anyHeight) + imgY - anyHeight / 2, (eB.x * anyWidth) + imgX - anyWidth / 2,
									(eB.y * anyHeight) + imgY - anyHeight / 2);
					}
					scaledbc.loadPixels();
				}

				// Blobs
				if (drawBlobs) {
					parent.strokeWeight(5);
					parent.point((b.x * anyWidth) + imgX - anyWidth / 2, (b.y * anyHeight) + imgY - anyHeight / 2);

					scaledbc.loadPixels();
				}
			}
		}
	}

	/**
	 * sets Baseline in the beginning uses the Baseline to remove noise form input
	 * might scale up/down or adjust depending on how "adjust" "minInputrange"
	 * "minOutputrange" "maxInputRange" "maxOUtputRange" are set, look at the
	 * processing method map() for better understanding of how the scaling works
	 */
	public void setColors() {

		parent.colorMode(PConstants.RGB, colorrange);
		img.loadPixels();

		for (int i = 0; i < rx; i++) {
			for (int j = 0; j < tx; j++) {

				if (baseLineSet == false) {
					BaseLine[i][j] = (BaseLine[i][j] + rawValues[j][i]);
					bt_average[i][j] = bt_average[i][j] + 1;
				} else {
					if (calibrationErrorThreshold > rawValues[j][i] - BaseLine[i][j]) {
						calibrationError = true;
					}
					float value = PApplet.max(0, rawValues[j][i] - BaseLine[i][j]);

					values[j][i] = (int) value;

				}
			}
		}

		for (int i = 0; i < tx; i++) {
			for (int j = 0; j < rx; j++) {
				int pix = j + i * rx;
				if (adjust) {
					adjust();
				}

				float value = values[i][j];

				// some rescaling of the input, amplification/suppression can happen here,
				// depending on how the variables are set
				value = PApplet.map(value, minInputRange, maxInputRange, minOutputRange, maxOutputRange); //

				img.pixels[pix] = parent.color(value);
			}
		}
		img.updatePixels();

	}

	/**
	 * adjusts if a touch is detected (so if there is a input > maxNoiseThreshold),
	 * then it adjusts the values in values[][] that are > noiseThreshold by mapping
	 * them like this: map(values[i][j], 0, maxValue, minInputRange, maxInputRange);
	 */
	private void adjust() {

		// first looking for the current maximum input
		int maxValue = 0;

		for (int i = 0; i < tx; i++) {
			for (int j = 0; j < rx; j++) {
				if (values[i][j] > maxValue) {
					maxValue = values[i][j];
				}
			}
		}
		// check if the current maximum input can be considered a touch
		if (maxValue > maxNoiseThreshold) {
			for (int i = 0; i < tx; i++) {
				for (int j = 0; j < rx; j++) {
					// all inputs that are not considered noise get adjusted
					if (values[i][j] > noiseThreshold) {
						values[i][j] = (int) PApplet.map(values[i][j], 0, maxValue, minInputRange, maxInputRange);
					}
				}
			}
		}
	}

	/*
	 * this method will set the color of the pixels at the edge of the given picture
	 * to 0 (black) this is so that BlobDetection will handle blobs near edges
	 * properly, so just a workaround for a bug in the BlobDetection library
	 * 
	 */
	public PImage imageForBlobDetection(PImage img) {
		img.loadPixels();
		for (int i = 0; i < img.width; i++) {
			for (int j = 0; j < img.height; j++) {
				int p = i + j * (img.width);

				if (j == 0 || j == img.height - 1 || i == 0 || i == img.width - 1) {
					img.pixels[p] = parent.color(0);
				}
			}
		}
		img.updatePixels();
		return img;
	}

}
