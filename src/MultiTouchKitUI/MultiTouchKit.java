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
import blobDetection.*;
import gab.opencv.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.Size;

public class MultiTouchKit {

	private PApplet parent; // the "Processing Object"

	private int[][] rawValues; // stores the latest values for every tx/rx intersection
	private int[][] values; // stores the difference of the rawValues and the baseline after calibration is done (or adjusted values if adjustment is turned on)

	
	//variables that have to be set by the user
	private int rx; // number of receiver lines
	private int tx; // number of transmission lines
	private int anyHeight, anyWidth; // dimensions of scaledbc

	//variables for thresholds and to enable/disable features

	//drawing to screen
	private boolean autoDraw = false; // false: will not draw anything to the screen, true: will draw to the screen
										// (what exactly depends on some of the other options)

	//variables for amplification/suppression of the input
	// those 4 values can be used to scale the input up or down
	private int minInputRange = 10;
	private int maxInputRange = 600;
	private int minOutputRange = 0;
	private int maxOutputRange = 255;
	private int colorrange = 255; // colorrange of setColors (255 should be best), can be used to scale the input
	private boolean adjust = false; // enable(true) or disable(false) adjustment
	
	//variables for adjustment
	private int maxNoiseThreshold = 60; // in adjust: if a value higher than this is detected it is considered a touch and will get adjusted
	private int noiseThreshold = 10; // everything below this is considered noise
	

	//draw blobs
	private boolean drawBlobs = false; // false: will not draw blobs, true: will draw blobs
	private boolean drawBlobCenters = true; //true: blob centers will be drawn (if drawBlobs is also true)
	private boolean drawBlobEdges = true; //true: blob edges will be drawn (if drawBlobs is also true)
	private float thresh = 0.85f; // threshold for blob detection, 1.0 is max
	private boolean cubic = true; // false: interpolation is nearest neighbors (blocky), true: interpolation is cubic (smooth)

	//calibration
	private long waittime = 2000000000; // how much time the the sketch should take at least to set the Baseline (2 seconds = 2000000000)


	//Variables for the Serial connection
	private Serial myPort; // Serial Object
	private boolean init = true; //auxiliary variable, true indicates that no values have been recorded yet and the starttime has not been set yet
	private boolean connected = false; // true if the serial connection to the arduino is established
	private boolean changes = false; // auxiliary variable, is true if readSerial has actually received something


	//variables needed to set the Baseline
	private long[][] bt_average; // helps to set the BaseLine
	private long[][] BaseLine; // used to set the Baseline and then used to remove noise from input values (rawValues) coming from the arduino
	private long starttime; //auxiliary variable, needed to set the Baseline
	private boolean baseLineSet = false; //auxiliary variable, false while the Baseline is not set yet
	private boolean calibrationError = false; //auxiliary variable, true if the values for the calibration are lower than the calibrationErrorThreshold
	private int calibrationErrorThreshold = -40; // if the values for inital calibration are lower than this a warning will be printed

	//variables/objects for interpolation and blobdetection
	private OpenCV opencv; // openCV object for interpolation
	private BlobDetection theBlobDetection; // BlobDetection object for BlobDetection
	private PImage img; // image that will be interpolated
	private PImage scaledbc; // image that results from interpolation and that will be drawn to the screen if  "autodraw" = true
	

	/**
	 * Constructor, creates the MultiTouchKit object, assigns several variables, initiallizes arrays,
	 * and calls connect() to establish the serial connection
	 * 
	 * @param parent the serialPort of the arduino
	 * @param tx the serialPort of the arduino
	 * @param rx the serialPort of the arduino
	 * @param serialPort the serialPort of the arduino
	 * @param anyHeight the serialPort of the arduino
	 * @param anyHeight the serialPort of the arduino
	 */

	public MultiTouchKit(PApplet parent, int tx, int rx, int serialPort, int anyHeight, int anyWidth) {

		this.parent = parent;
		this.rx = rx;
		this.tx = tx;
		this.anyHeight = anyHeight;
		this.anyWidth = anyWidth;

		// some of the preset options, look above for meaning
		this.autoDraw = false;
		this.drawBlobs = true;
		this.cubic = true;

		rawValues = new int[tx][rx];
		values = new int[tx][rx];

		img = parent.createImage(rx, tx, parent.RGB);
		scaledbc = parent.createImage(anyHeight, anyWidth, parent.ALPHA);
		theBlobDetection = new BlobDetection(scaledbc.width, scaledbc.height);
		theBlobDetection.setThreshold(thresh);

		bt_average = new long[rx][tx];

		BaseLine = new long[rx][tx];
		
		//creating the font to draw on screen later
		PFont f;
		f = parent.createFont("Arial",16,true); // Arial, 16 point, anti-aliasing on
		parent.textFont(f,36); 

		// try connect to arduino via serial
		connect(serialPort);

		parent.registerMethod("draw", this);
		parent.registerMethod("dispose", this);
	}

	/**
	 * Method that's called at the end of the draw() method in the parent sketch, so if autoDraw == true,
	 * it will draw over whatever you do in the processing sketch
	 * it reads from the serial port, sets the colors if necessary, creates the baseline and draws to the screen if enabled
	 */
	public void draw() {
		//printing a warning when it seems that something went wrong during calibration:
		//(generally this happens when the calibrated values are very negative, so smaller than "calibrationErrorThreshold", but they should be close to 0)
		if(calibrationError) {
			System.out.println("===========================================================");
			System.out.println("There seems to be a problem with the calibration,please start again");
			System.out.println("===========================================================");
		}
		// read the latest data from the SerialPort
		readSerial();

		// if the SerialPort actually received something, set the Colors
		if (changes) {
			setColors();
		}

		// the Baseline is set here
		if ((System.nanoTime() - starttime) > waittime && !baseLineSet) {
			baseLineSet = true;
			parent.noFill();
			
			//check that bt_average is not 0, so that we can not accidentally divide by 0
			//if it is 0, the sketch will take more time to collect values to get a proper Baseline
			for (int i = 0; i < rx; i++) {
				for (int j = 0; j < tx; j++) {
					if(bt_average[i][j] == 0) {
						baseLineSet = false;
						starttime = System.nanoTime();
						System.out.println("Calibration not done yet, need more time! Don't touch please");
						break;
						}
				}
			}
			//calculate the averages of the collected values and save them as Baseline
			if(baseLineSet) {
				for (int i = 0; i < rx; i++) {
					for (int j = 0; j < tx; j++) {
						System.out.println("Debug, j: "+j+"  i: "+i);
						BaseLine[i][j] = BaseLine[i][j] / bt_average[i][j];
					}
				}
				System.out.println("Calibration done, ready to go");
			}
			
		}

		// drawing to the screen if autoDraw == true
		if (autoDraw) {
			interpolate();
			//if the baseline is not set yet, the sensor should not be touched, therefore "Calibrating!" will be printed on the screen to indicate this
			if(baseLineSet) {
				parent.image(scaledbc, 0, 0);
			}else {
				parent.background(parent.color(0,0,0));
				parent.fill(255);
				parent.text("Calibrating!",anyWidth/3,anyHeight/2);
			}
			
			//computing the blobs on scaledbc 
			// (imageForBlobDetection() will replace the edges of scaledbc with black pixels as workaround for a bug in BlobDetection)
			theBlobDetection.computeBlobs(imageForBlobDetection(scaledbc).pixels);
			if (drawBlobs) {
				//draw the blobs
				drawBlobsAndEdges(drawBlobEdges, drawBlobCenters); // (Edge,Blob)
			}
		}

	}

	/**
	 * read from the Serial Port if anything has been read: changes = true else				
	 * changes = false 
	 * from the input, everything that is not "," or a number is
	 * removed, than the string is split at the commas the numbers are saved into
	 * the "rawValues[][]" array
	 * 
	 */
	public void readSerial() {
		changes = false;
		if (connected) {
			
			while (myPort.available() > 0) {
				String myString = myPort.readStringUntil('\n');
				if(myString == null) {
					break;
				}
				
				if (init) { //at the beginning of the sketch, start the timer for setting the Baseline
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

					//split the string into rx+1 numbers (still string type though)
					String[] splitString = myString.split(",");

					//check if the correct amount of strings are in the array
					if (splitString.length != rx + 1) {
						break;
					}

					
					if(!splitString[0].matches("[0-9]+")) {break;}

					int t = Integer.parseInt(splitString[0]); // TX of the received input

					for (int i = 0; i < splitString.length; i++) {
						if (i > 0) {
							if(!splitString[i].matches("[0-9]+")) {break;}

							changes = true;

							//save the new values
							rawValues[t][i - 1] = Integer.parseInt(splitString[i]);
						}
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
		if (serialPort <= Serial.list().length) {
			myPort = new Serial(parent, Serial.list()[serialPort], 115200);
			myPort.clear(); // clear the buffer
			myPort.bufferUntil(10); // always buffer until the newline symbol
			connected = true;
		} else {
			parent.println("Error: serial Port not found");
			connected = false;
		}

	}

	/**
	 * disconnect the library from the arduino
	 */
	private void disconnect() {
		myPort.clear(); //clear the serial buffer
		myPort.stop(); //stop the connection
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
	 * @return latest raw values 
	 */
	public int[][] getRawValues() {
		return rawValues;
	}

	/**
	 * 
	 * @return values (raw - BaseLine) OR adjusted values (when
	 *         adjustment is enabled)
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
	 * option to activate/deactivate autodrawing
	 * 
	 * @param draw true: draw to the screen, false: don't
	 */
	public void autoDraw(boolean draw) {
		this.autoDraw = draw;
	}

	/**
	 * set the timeframe to collect values for the Baseline, even with a 16x16
	 * sensor, 2 seconds should always be sufficient 
	 * set this in "setup()" if you want to change it
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
	 * @param minInputRange the minimum raw input value that can be received from the Arduino
	 */
	public void setMinInputRange(int minInputRange) {
		this.minInputRange = minInputRange;
	}

	/** 
	 * this is supposed to be the maximum raw input value that can be received from the Arduino
	 * but you can use this to suppress or amplify the input signal, so if your input is too weak, choose a smaller maxInputRange
	 *
	 * @param maxInputRange the maximum raw input value that can be received from the Arduino
	 */
	public void setMaxInputRange(int maxInputRange) {
		this.maxInputRange = maxInputRange;
	}

	/**  
	 * 
	 * @param minOutputRange the minimum grey value that can be displayed by the Processing sketch (so gernally 0)
	 */
	public void setMinOutputRange(int minOutputRange) {
		this.minOutputRange = minOutputRange;
	}

	/** 
	 * 
	 * @param maxOutputRange the maximum grey value that can be displayed by the Processing sketch (so gernally 255)
	 */
	public void setMaxOutputRange(int maxOutputRange) {
		this.maxOutputRange = maxOutputRange;
	}

	/** 
	 * 
	 * @param maxNoiseThreshhold everything bigger than this will be considered a touch by adjust()
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
	 * @return noiseThreshhold everything lower than this will be considered noise by adjust()
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
	 * set the calibrationErrorThreshold lower in case that everything is actually correct or the message bothers you
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

		Mat mat2 = new Mat(anyHeight, anyWidth, mat1.type()); // new matrix to store resize image
		Size sz = new Size(anyHeight, anyWidth); // size to be resized

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
							parent.line(eA.x * parent.width, eA.y * parent.height, eB.x * parent.width,
									eB.y * parent.height);
					}
					scaledbc.loadPixels();
				}

				// Blobs
				if (drawBlobs) {
					parent.strokeWeight(5);
					parent.point(b.x * parent.width, b.y * parent.height);
					
					scaledbc.loadPixels();
				}
			}
		}
	}

	/**
	 * sets Baseline in the beginning 
	 * uses the Baseline to remove noise form input
	 * might scale up/down or adjust depending on how "adjust" "minInputrange"
	 * "minOutputrange" "maxInputRange" "maxOUtputRange" are set, 
	 * look at the processing method map() for better understanding of how the scaling works
	 */
	public void setColors() {

		parent.colorMode(parent.RGB, colorrange); 
		img.loadPixels();

		for (int i = 0; i < rx; i++) {
			for (int j = 0; j < tx; j++) {

				if (baseLineSet == false) {
					BaseLine[i][j] = (BaseLine[i][j] + rawValues[j][i]);
					bt_average[i][j] = bt_average[i][j] + 1;
				} else {
					if(calibrationErrorThreshold > rawValues[j][i] - BaseLine[i][j]) {
						calibrationError = true;
					}
					float value = parent.max(0, rawValues[j][i] - BaseLine[i][j]);

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

				//some rescaling of the input, amplification/suppression can happen here, depending on how the variables are set
				value = parent.map(value, minInputRange, maxInputRange, minOutputRange, maxOutputRange); //

				img.pixels[pix] = parent.color(value);
			}
		}
		img.updatePixels();

	}

	/**
	 * adjusts if a touch is detected (so if there is a input > maxNoiseThreshold), 
	 * then it adjusts the values in values[][] that are > noiseThreshold by mapping them 
	 * like this: map(values[i][j], 0, maxValue, minInputRange, maxInputRange);
	 */
	private void adjust() {

		//first looking for the current maximum input
		int maxValue = 0; 

		for (int i = 0; i < tx; i++) {
			for (int j = 0; j < rx; j++) {
				if (values[i][j] > maxValue) {
					maxValue = values[i][j];
				}
			}
		}
		//check if the current maximum input can be considered a touch
		if (maxValue > maxNoiseThreshold) {
			for (int i = 0; i < tx; i++) {
				for (int j = 0; j < rx; j++) {
					//all inputs that are not considered noise get adjusted
					if (values[i][j] > noiseThreshold) {
						values[i][j] = (int) parent.map(values[i][j], 0, maxValue, minInputRange, maxInputRange);
					}
				}
			}
		}
	}
	
	/*
	 * this method will set the color of the pixels at the edge of the given picture to 0 (black)
	 * this is so that BlobDetection will handle blobs near edges properly, so just a workaround for a bug in the BlobDetection library
	 * 
	 */
	public PImage imageForBlobDetection(PImage img) { 
		img.loadPixels();
		for(int i = 0; i < img.width; i++) {
			for(int j = 0; j < img.height; j++) {
				int p = i + j*(img.width);
				
				if(j == 0 || j == img.height-1 || i == 0 || i == img.width-1 ) {
					img.pixels[p] = parent.color(0);
				}
			}
		}
		img.updatePixels();
		return img;
	}
	
	
}
