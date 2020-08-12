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
Check out the other examples to see all the options, as this example will stick to the absolute basics.

You can change these two variables to get the optimum results:
"maxInputRange" : If the touch points are not bright enough, decrease this value.
"threshold"     : If the touch points are not detected as blobs, decrease this value.

Please insert the correct Serial Port and number of RX and TX based on the size of the sensor
*/


import gab.opencv.*;
import MultiTouchKitUI.*;
import processing.serial.*;
import blobDetection.*;



int tx = 4;               //number of transmitter lines (rx)
int rx = 4;               //number of receiver lines (rx)
int serialPort = 0;       //serial port that the Arduino is connected to
Serial myPort;
MultiTouchKit mtk;

int maxInputRange = 100;  // set the brightness of touch points
float threshold = 0.75f;  // set the threshold for blob detection

int[][] values;           // values used for visualization
int[][] rawvalues;        // raw values recieved from serial port
long[][] baseline;        // baseline values saved for calibartion

Table tableRaw;           // table for recording raw values 
Table tableAdj;           // table for recording values used for visualization
Table tableBas;           // table for recording baseline values 



void setup(){
  size(500,500);
  background(255);
  mtk = new MultiTouchKit(this,tx,rx,serialPort);      		   // instantiate the MultiTouchKit  
  mtk.autoDraw(true);                                          // visualize the touch points
  mtk.setMaxInputRange(maxInputRange);                         // set the brightness of touch points
  mtk.setThresh(threshold);                                    // set the threshold for blob detection
  
  //mtk.enableUI(true);										   // uncomment this to enable the UI
  //mtk.setRecordReplay(true);								   // uncomment this to record a replay (default: saved as "Replay.csv" in your sketch folder)
  //mtk.playReplay(true,"Replay.csv");						   // uncomment this to play the Replay "Replay.csv" from your sketch folder
  
}

void draw(){
  
  values = mtk.getAdjustedValues();                            // raw values recieved from serial port
  rawvalues = mtk.getRawValues();                              // values used for visualization
  baseline = mtk.getBaseLine();                                // baseline values saved for calibartion
  
  
  // -------------- Uncomment this part, if you want to see the raw values, and calibrated values for debugging --------------
  /*
 
  println("values: ");
  for(int i = 0; i < tx; i++){
    print("tx "+i+" :");
    for(int j = 0; j < rx; j++){
      print(" "+values[i][j]);
    }
    println();
  }
  println("------------------------------------------");
  println("rawvalues: ");
  for(int i = 0; i < tx; i++){
    print("tx "+i+" :");
    for(int j = 0; j < rx; j++){
      print(" "+rawvalues[i][j]);
    }
    println();
  }
  println("------------------------------------------");
  println("BaseLine: ");
  for(int i = 0; i < tx; i++){
    print("tx "+i+" :");
    for(int j = 0; j < rx; j++){
      print(" "+baseline[i][j]);
    }
    println();
  }
  println("------------------------------------------");
  */
  // ---------------------------------------------------------------------------------------------------------------------------
  
  
  
}
