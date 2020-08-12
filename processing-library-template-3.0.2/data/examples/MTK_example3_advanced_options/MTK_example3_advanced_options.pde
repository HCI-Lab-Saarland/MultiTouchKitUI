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

This example contains advanced options  of the library and tries to explain what they do.

Make sure that you read the comments in the sourcecode/the full documentation, before you use any of those, as some may have unwanted side effects.

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
  
  
  //here are the options, uncomment them to use them, also fill in the parameters, as the letters are just placeholders without meaning, all of them are int (even the parameter from setBGColor() is actually an int)
  //I will also add some usecases for some of them
  
  
  //mtk.setInterpolate(true)          // set to false to disable interpolation
  //mtk.setComputeBlobs(true)         // set to false to disable blob computation 
  //you might want to disable both of those if you just want to get the raw/adjusted values and process them yourself, then you don't need those things to happen in the library
  
  //mtk.setWindowSize(w,h)            // use this to resize the window while the sketch is running, otherwise just use the size() method at the beginning of setup();
  //mtk.setSbcSize(w,h)               // use this to resize Scaledbc (the output picture), if the dimensions don't line up with your rx/tx numbers it will not look/work right
  //mtk.setBGColor(c)                 // use this to set a different backgroundcolor, the parameter has to be a processing color, use processings color() method for that
  //mtk.setWaittime(t)                // use this to modify the time the library will take at the beginning to calibrate
  //mtk.setAdjust(true)               // use this to enable "adjustment" which will set the MaxInputRange value to scale the input automatically (but not necessarily good)
  //mtksetMaxNoiseThreshhold(t)       // every input value bigger than this will be considered a touch by adjust() 
  //mtk.setNoiseThreshhold(t)         // every input value that is smaller than this threshold will be considered noise by adjust()
  
  //mtk.setCalErrThreshold(t)         // if the calibration error is bigger (actually smaller since it is a negative number), than this, you will get the error message printed that the calibration might have gone wrong
  //you might want to set this to a lower value if everything works well but the message is printed anyways
  
  
  //old commands, I'm not sure if those are usefull anymore, they were at some point and are just still in the code, so I will include them here
  
  //mtk.setColorrange(c);              //use this to change the colorrange of the sketch (look at Processings colorMode())
  
  //mtk.setMinInputRange(r);
  //mtk.setMinOutputRange(r);
  //mtk.setMaxOutputRange(r);
  //those can in theory all be used to scale the input, they are used when the input values are beeing mapped to greyvalues with Processings map() method
  //but MaxInputRange is the only one that is beeing used in the library normally
  
  
  
  
}

void draw(){
  
  
  
  
}
