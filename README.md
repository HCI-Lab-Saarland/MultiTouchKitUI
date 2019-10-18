# Multi Touch Kit Processing Library

This is a Processing library for Multi Touch Kit. For more information on the project and tutorial, visit our [website](https://hci.cs.uni-saarland.de/multi-touch-kit/).


### Dependencies

- [blobDetection](http://www.v3ga.net/processing/BlobDetection/)
- [OpenCV for Processing](https://github.com/atduskgreg/opencv-processing)

### Installing
Download the zip folder and unpack it in your Processing Libraries folder. Make sure to restart processing after doing so.

( ../Documents/Processing/Libraries/MultiTouchKitUI)


### Setting the index of serial port

In Arduino IDE go to “Tools → Port“. Find the name of the port that your Arduino is connected to.
In Processing IDE, get a list of all available serial ports, by writing:
`println(Serial.list());`
Find the Arduino port among the printed ports. Use it’s index (starting from 0) to set `serialPort`.


### Documentation

After the MultiTouchKit object is created, it will execute code at the End of your `draw()` method!
In the first 2 seconds, it will create a "baseline", so for each intersection it receives the values from the sensor, averages them, and will then always subtract this average from the incoming values, to eliminate noise. So make sure to not touch the sensor while this is happening!

If you use the standard settings it will continuously read the data that is coming from the Arduino via serial.

To increase the brightness of touchpoints (blobs), reduce `maxInputRange`.
To change the threshold for blob detection, set `threshold`.


#### Important methods:

`int[][] getRawValues();`
Will return the latest raw values.



`int[][] getAdjustedValues();`
Returns the latest raw Values - Baseline.



`void autoDraw(boolean draw);`
If draw = true, the library will automatically draw the picture that results from the interpolated input values to the screen.


`void setWaittime(long t);`
Sets the time that the library takes to set the Baseline, if you have problems with this you can increase this, it takes nanoseconds as input, the standard value is 2000000000 (=2 seconds);


`void setThresh(float newThresh);`
Sets the threshold for the blobdetection, should be a value between 0 and 1. Look at the documentation of the BlobDetection library for more information.



