# Android - Free Fall Detector

This is a simple android library with a demo app to detect device's free falling using **accelerometer** sensor, it also uses a database to manage history of these events.

## Overview

This library is built using, it uses foreground service in order to receive events when the app is not in use by the user, it also enables you to:

 - to notify user when free falling event occures
 - It also shares real time updates to the app using local broadcast receiver, on event occurs
 - It stores event data in a local sqlite database to manage history of these events 

## Getting Started

### Installation
Following are the steps to install this library in an existing project ,

 - Download/clone this repository at `Path/To/The/Lib/`
 -  In your `settings.gradle`, add the following lines,
 ```
project(':free-fall-detector').projectDir = new File('Path/To/The/Lib/library')
```  
- And then add this in your app level build.gradle inside dependencies section,
```
implementation project(":free-fall-detector")
```  
Thats it, enjoy!
### Usage

#### Starting service
You need to add following piece of code in AndroidManifest.xml to register the foreground service first,
 ``
<service
	android:name="io.xbird.library.service.MotionDetectService"
	android:enabled="true"
	android:exported="false"></service>
	``
In order to start service programmitically, you can use the default startService methods offer by android sdk, an example of it is mentioned below,

	Intent(this, MotionDetectService::class.java).also {
	    it.action = Actions.START.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	        startForegroundService(it)
            return
        }
        startService(it)
	}
#### Fetching History
Fetching history of older records can be done from a single line of code which mentioned below,

    val readings = AppDatabase.getAppDataBase(applicationContext)
    	.freeFallReadingDao()?.getLatestReadings()
**NOTE:** The above code will executes a database call, so it should be handled from a background thread

#### Getting Realtime Updates
In order to get realtime updates on event triggers, you need to register a local broadcast receiver within your app like mentioned below,


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel?.updateReadings()
        }
    }
Then, use it from your activity/fragment's onCreate method like,

	//handle receiver
    val localBroadcastManager = LocalBroadcastManager.getInstance(this)
    localBroadcastManager.registerReceiver(
	    broadcastReceiver,
	    IntentFilter(MotionDetectService.ACTION_NEW_FALL_DETECTED)
    )
## Details about the Recipe
### Initial Thoughts
Initially, when I received this code challenge (requirements),  first two things which comes in my mind were accelerometre and foreground service. Laterly, after digging more into the requirements, I decided Room API (wrapper over SQLite database) to save/fetch data with RxAndroid. 
### Calculations
Moreover on the calculations side, I made some analysis and from those analysis, I have found following results,
acceleration about all 3 axes drops close to zero during free fall. Magnitude of total acceleration is given by 
![total acceleration](https://raw.githubusercontent.com/ArsalRaza/free-fall-detector-internal/master/readme.assets/formula.jpg)

Hence, if acceleration about all the 3 axes are closer to zero than the total acceleration would also be closer to zero. It is the idea behind detecting the free fall. At the end, I have add threshold ranges to find the acceleration.

## Author
ArsalImam