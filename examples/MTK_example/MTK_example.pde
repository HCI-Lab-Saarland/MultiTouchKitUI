/***************************************************
 This is a library for the Multi-Touch Kit
 Designed and tested to work with Arduino Uno, MEGA2560, LilyPad(ATmega 328P)
 
 For details on using this library see the tutorial at:
 ----> https://hci.cs.uni-saarland.de/multi-touch-kit/
 
 Written by Jan Dickmann, Narjes Pourjafarian, Juergen Steimle (Saarland University),
 Anusha Withana (University of Sydney), Joe Paradiso (MIT)
 MIT license, all text above must be included in any redistribution
 ****************************************************/


/*
This example shows how to use "Multi-Touch Kit Processing library" in your code.
With this code you can visualize the touch points and/or save the raw capacitive data in a CSV file.

You can change these two variables to get the optimum results:
"maxInputRange" : If the touch points are not bright enough, decrease this value.
"threshold"     : If the touch points are not detected as blobs, decrease this value.

Please insert the correct Index of Serial Port and number of RX and TX based on the size of the sensor
*/


import gab.opencv.*;
import MultiTouchKitUI.*;
import processing.serial.*;
import blobDetection.*;



int tx = 16;               //number of transmitter lines (rx)
int rx = 16;               //number of receiver lines (rx)
int serialPort = 1;       //serial port that the Arduino is connected to
Serial myPort;
MultiTouchKit mtk;

int maxInputRange = 80;  // set the brightness of touch points (blobs)
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
  mtk = new MultiTouchKit(this,tx,rx,serialPort,500,500);      // instantiate the MultiTouchKit  
  mtk.autoDraw(true);                                          // visualize the touch points
  mtk.setMaxInputRange(maxInputRange);                         // set the brightness of touch points
  mtk.setThresh(threshold);                                    // set the threshold for blob detection
  
  
  // -------------- Uncomment this part, if you want to save the data in a CSV file --------------
  /*
  tableRaw = new Table(); 
  tableAdj = new Table(); 
  tableBas = new Table(); 
  
  tableRaw.addColumn("Tline");
  for(int i = 0; i<rx; i++){
    tableRaw.addColumn(Integer.toString(i));
  }
  tableAdj.addColumn("Tline");
  for(int i = 0; i<rx; i++){
    tableAdj.addColumn(Integer.toString(i));
  }
  tableBas.addColumn("Tline");
  for(int i = 0; i<rx; i++){
    tableBas.addColumn(Integer.toString(i));
  }
  */
  // --------------------------------------------------------------------------------------------
  
}

void draw(){
  
  values = mtk.getAdjustedValues();                            // values used for visualization
  rawvalues = mtk.getRawValues();                              // raw values recieved from serial port
  baseline = mtk.getBaseLine();                                // baseline values saved for calibartion
  
  // -------------- Uncomment this part, if you want to save the data in a CSV file --------------
  /*
  for(int i = 0; i < tx; i++){
    TableRow newRow = tableBas.addRow();
    newRow.setInt("Tline", i);
    for(int j = 0; j < rx; j++){
      newRow.setLong(Integer.toString(j),baseline[i][j]);
    }
  }
  
  for(int i = 0; i < tx; i++){
    TableRow newRow = tableRaw.addRow();
    newRow.setInt("Tline", i);
    for(int j = 0; j < rx; j++){
      newRow.setInt(Integer.toString(j),rawvalues[i][j]);
    }
  }


 for(int i = 0; i < tx; i++){
    TableRow newRow = tableAdj.addRow();
    newRow.setInt("Tline", i);
    for(int j = 0; j < rx; j++){
      newRow.setInt(Integer.toString(j),values[i][j]);
    }
  } 
  saveTable(tableRaw, "tableRaw.csv");
  saveTable(tableAdj, "tableAdj.csv");
  saveTable(tableBas, "tableBas.csv");
  */
  
  // ---------------------------------------------------------------------------------------------------------------------------

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
