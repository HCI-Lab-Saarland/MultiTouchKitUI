/***************************************************
 This is a library for the Multi-Touch Kit
 Designed and tested to work with Arduino Uno, MEGA2560, LilyPad(ATmega 328P)
 
 For details on using this library see the tutorial at:
 ----> https://hci.cs.uni-saarland.de/multi-touch-kit/
 
 Written by Jan Dickmann, Narjes Pourjafarian, Juergen Steimle (Saarland University), Anusha Withana (University of Sydney), Joe Paradiso (MIT)
 MIT license, all text above must be included in any redistribution
 ****************************************************/


/*
This example shows how to use "Multi-Touch Kit Processing library" in your code.

This example contains the most important options of the library and tries to explain what they do.

It is recommended that you also read the comments in the sourcecode/the documentation.

Please insert the correct Serial Port and number of RX and TX based on the size of the sensor
*/


import gab.opencv.*;
import MultiTouchKitUI.*;
import processing.serial.*;
import blobDetection.*;


//Here you will have to set the tx/rx numbers, as well as the serial port
int tx = 4;               //number of transmitter lines (rx)
int rx = 4;               //number of receiver lines (rx)
int serialPort = 0;       //serial port that the Arduino is connected to


Serial myPort;
MultiTouchKit mtk;

int maxInputRange = 100;  // set the brightness of touch points
float threshold = 0.75f;  // set the threshold for blob detection


void setup(){
  //here you can set the window size, the window will be resized automatically when it is too small
  size(400,400);
  
  //the MultiTouchKit is created here
  mtk = new MultiTouchKit(this,tx,rx,serialPort);      // instantiate the MultiTouchKit  
  
  
  //here are the options, uncomment them to use them
  
  //mtk.autoDraw(true);                         // visualize the touch points, this will take full control of the window, you cant draw yourself anymore
  //mtk.drawBlobs(true);                        // set this to false to not draw the blobs, will be true by default
  //mtk.setDrawBlobCenters(true);               // set this to false to not draw the blob centers (touchpoints), will be true by default
  //mtk.setDrawBlobEdges(true);                 // set this to false to not draw the blob edges, will be true by default
  
  //mtk.interpolationCubic(true);               // set this to false to use nearest neighbour interpolation ("blocky" output image), default will be Cubic interpolation, which is smooth
  //mtk.setMaxInputRange(maxInputRange);        // set the brightness of touch points, decreasing this value will make everything "brigther", this is also the value that is beeing changed by the buttons
  //mtk.setThresh(threshold);                   // set the threshold for blob detection
  //mtk.enableUI(true);                         // enable the ui (buttons to change maxInputRange on the fly)
  
  //mtk.setRecordReplay(true);                  // from the moment this is set to true a replay will be recorded (mutually exclusive with playReplay()), until you terminate the sketch or set it to false
  //mtk.setReplayName("Replay");                // set the name of the recorded replay, the default name is "Replay", you can also use this to create several replays without restarting the sketch
  //mtk.playReplay(true,"Replay.csv");          // use this to play a replay, replace "Replay.csv" with whatever your replay is called
  
  //mtk.setSizePerIntersection(50);             // use this to scale the size of the output picture (with small sensors you might want to increase the number, with big sensors maybe you want to decrease it)
  
  
}

void draw(){
  
  //here are the getter methods of the library
  
  //boolean calibrationDone = mtk.calibrationDone();                     // if this is true, the calibration is done                                    
  
  //int[][] values = mtk.getAdjustedValues();                            // raw values recieved from serial port
  //int[][] rawvalues = mtk.getRawValues();                              // values used for visualization (max((raw values - baseline = adjusted values),0))
  //long[][] baseline = mtk.getBaseLine();                               // baseline values saved for calibartion
  
  //BlobDetection bd = mtk.getBlobDetection();                           // the blobDetection object, which contains the blobs
  //PImage sbc = mtk.getScaledbc();                                      // the output image that is displayed when autoDrawing is enabled
}
