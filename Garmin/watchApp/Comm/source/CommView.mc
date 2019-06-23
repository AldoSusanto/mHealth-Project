//
// Copyright 2016 by Garmin Ltd. or its subsidiaries.
// Subject to Garmin SDK License Agreement and Wearables
// Application Developer Agreement.
//

using Toybox.WatchUi as Ui;
using Toybox.Graphics as Gfx;
using Toybox.Communications as Comm;
using Toybox.System as Sys;
using Toybox.Application as App;
using Toybox.Time as Tim;
using Toybox.ActivityMonitor as Act;

class CommView extends Ui.View {
    var screenShape;
    var hTimer;
    var tTimer;

    
    var clockTime;
    var curHRCount;//counts of heartrate, goes up every minute worn
    var curHRTotal;//total of heartrates, adds curHR every minute
    var curHRAvg;
    var curHR;
    var curBattery;
    var lastAvgTaken;
    
    var timeString;
	var app;
	var hist;
	var isPM;
	
	var diff = -1; // TESTING
	var now = -1;
	var sampleTime = -1;
	
	
    function initialize() {
        View.initialize();
        app = App.getApp();
//        var testString = "" + (Tim.today().value() - 86400);
//        app.setProperty("lastSync", testString);
//		app.clearProperties();
    }
    
    
    //new day: Day 1 will ALWAYS be today, day 1 = 1 day ago, etc...up to day 8 = 7 days ago
    //so a new day will essentially push the array back. day8 info gets lost, and day 1 gets new information
    function newDay() {
    	app.setProperty("heartDay8", app.getProperty("heartDay7"));
    	app.setProperty("heartNums8", app.getProperty("heartNums7"));
    	
    	app.setProperty("heartDay7", app.getProperty("heartDay6"));
    	app.setProperty("heartNums7", app.getProperty("heartNums6"));
    	
    	app.setProperty("heartDay6", app.getProperty("heartDay5"));
    	app.setProperty("heartNums6", app.getProperty("heartNums5"));
    	
    	app.setProperty("heartDay5", app.getProperty("heartDay4"));
    	app.setProperty("heartNums5", app.getProperty("heartNums4"));
    	
    	app.setProperty("heartDay4", app.getProperty("heartDay3"));
    	app.setProperty("heartNums4", app.getProperty("heartNums3"));
    	
    	app.setProperty("heartDay3", app.getProperty("heartDay2"));
    	app.setProperty("heartNums3", app.getProperty("heartNums2"));
    	
    	app.setProperty("heartDay2", app.getProperty("heartDay1"));
    	app.setProperty("heartNums2", app.getProperty("heartNums1"));
    	
    	app.setProperty("heartDay1", Tim.today().value());
    	var defaultNums = "(0-0-0)";
    	var emptyHeartNums = new [48];
        for(var i = 0; i < 48; i++) {
        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
        }
    	app.setProperty("heartNums1", emptyHeartNums);
    }
    
    
    //CHECK::::
    //e.g. HRCount reaches 30 at say, 12:00 midnight, will 48 be enough?
    //12:00 AM...all the way to 11:30 PM, for start times
    //check for timestamps > last sy
    //remove sensor
    
    //Time, will be exactly 48.
    //from 12:00 AM to 11:30 PM.
    //Signifies start of the 30 minute period(e.g. 11:30PM will be 11:30 to 12:00, and will be the 48th one, next one is next day)
    
    //Sample run-through
    //Start using app. Day 1.
    //Get .today() value, set timeArray for the 30 minute marks, in Unix Epoch time
    //if days 2-3-4-5-6-7 are unset(Unix-time for .today()), set them.
    //initialize heart rates for the 48 spots in array as 0, 0, 0( 0 Total, 0 Count, 0 Average).
    //note: check substrings for , in heartNums
    //get a smaller hist array
    
    
    //note: check if current day already exists, so if someone skips a day or two of not using the app, it'll check if the day already exists. oherwise, just +1 day
    //DIFF HR's display for BPM and added to total
    
    function timeTimer() {
    	clockTime = Sys.getClockTime();
    	var nonMilHour = clockTime.hour;
    	isPM = false;
    	if(nonMilHour >= 12) {
    		isPM = true;
    		nonMilHour -= 12;
    	}
    	if(nonMilHour == 0) {
    		nonMilHour = 12;//basically 0 o clock = 12 o clock
    	}
        timeString = Lang.format("$1$:$2$", [nonMilHour, clockTime.min.format("%02d")]);
        
       	
       	//GET HISTORY, RATHER THAN SENSOR, AND GET MOST RECENT ONE
       	//USING DURATION, +- A MINUTE
       	//null = entire history, true = newest first.
        hist = Act.getHeartRateHistory(null,true);
        var curSample = hist.next();
        //gets the MOST RECENT, regardless of whether or not it's "null", if it's "null" it's just 255, or INVALID_HR_SAMPLE
        if(curSample != null && curSample.heartRate != Act.INVALID_HR_SAMPLE) {
        	
        	curHR = curSample.heartRate;
    //    	System.println("curHR: " + curHR);
        }
        else {
        	curHR = null;
        }
        
        
        
        if(curHR != null) {//i.e. the subject is actually wearing the watch, so a HR is gotten
        	curHRTotal += curHR;
        	curHRCount += 1;
        }
        
        var curDay = Tim.today().value();
        
        
        
        
        
        //i.e. a new day
        if(app.getProperty("currentDayTime") != curDay) {	
        	
        	//it may not necessarily be just one day has passed(i.e. user has left app off for more than a day)
        	var dayDiff = curDay - app.getProperty("currentDayTime");
        	var numDaysPassed = dayDiff / 86400;
        	
        	app.setProperty("currentDayTime", curDay);
        	
        	//there are only 8 "days" stored so if more than 8, just do it 8 times
        	if(numDaysPassed > 8) {
        		numDaysPassed = 8;
        	}
        	for(var i = 0; i < numDaysPassed; i++) {
        		newDay();
        	}
        	
        }
        
        var curTime = Tim.now().value();
        var index = getHeartIndex(curTime);//index in the array of the current time frame
        
        
        
        //will be 'heartNums'+curDayIndex;
        
        
        
        var heartNums = new [48];
        var heartString = "";
        //parsing heartNums
        if(curHR != null) {
        	/*
	        if(curDayIndex == 1) {
	        	heartNums = app.getProperty("heartNums1", heartNums);
	        	lastHeartSet = "heartNums1";
	        }
	        else if(curDayIndex == 2) {
	        	heartNums = app.getProperty("heartNums2", heartNums);
	        	lastHeartSet = "heartNums2";
	        }
	        else if(curDayIndex == 3) {
	        	heartNums = app.getProperty("heartNums3", heartNums);
	        	lastHeartSet = "heartNums3";
	        }
	        else if(curDayIndex == 4) {
	        	heartNums = app.getProperty("heartNums4", heartNums);
	        	lastHeartSet = "heartNums4";
	        }
	        else if(curDayIndex == 5) {
	        	heartNums = app.getProperty("heartNums5", heartNums);
	        	lastHeartSet = "heartNums5";
	        }
	        else if(curDayIndex == 6) {
	        	heartNums = app.getProperty("heartNums6", heartNums);
	        	lastHeartSet = "heartNums6";
	        }
	        else if(curDayIndex == 7) {
	        	heartNums = app.getProperty("heartNums7", heartNums);
	        	lastHeartSet = "heartNums7";
	        }
	        */
	        heartNums = app.getProperty("heartNums1");
	        
	//        System.println(heartNums);
	//        System.println(index);
	        heartString = heartNums[index];
	        	
	    	heartString = heartString.substring(1,heartString.length()-1);
	//		System.println(heartString);
			
			var commaPlace = heartString.find("-");
			
			var totalString = heartString.substring(0, commaPlace);
	//		System.println(totalString);
			var heartTotal = totalString.toNumber();
			
			heartString = heartString.substring(commaPlace+1,heartString.length());
			commaPlace = heartString.find("-");
	//		System.println(heartString);
			var countString = heartString.substring(0, commaPlace);
	//		System.println(countString);
			var heartCount = countString.toNumber();
			
			heartString = heartString.substring(commaPlace+1,heartString.length());
			//at this point the string is just the average
			var heartAvg = heartString.toNumber();
	//		System.println(heartString);
			
			
			
	//		System.println(heartTotal);
	//		System.println(heartCount);
	//		System.println(heartAvg);
			
			heartTotal += curHR;
			heartCount += 1;
			heartAvg = heartTotal / heartCount;
			
			curHRTotal = heartTotal;
			curHRCount = heartCount;
			curHRAvg = heartAvg;
			
			var newHeartString = "(" + heartTotal + "-" + heartCount + "-" + heartAvg + ")";
			heartNums[index] = newHeartString;
			

	        //if it's not [1,7]
	        app.setProperty("heartNums1", heartNums);
        }
        
        Ui.requestUpdate();
    }

    function onLayout(dc) {
        screenShape = Sys.getDeviceSettings().screenShape;
        
        initProperties();
        
        var curDay = Tim.today().value();

        //i.e. a new day, checks right when the app is loaded
        if(app.getProperty("currentDayTime") != curDay) {	
        	
        	//it may not necessarily be just one day has passed(i.e. user has left app off for more than a day)
        	var dayDiff = curDay - app.getProperty("currentDayTime");
        	var numDaysPassed = dayDiff / 86400;
        	
        	app.setProperty("currentDayTime", curDay);
        	
        	//there are only 8 "days" stored so if more than 8, just do it 8 times
        	if(numDaysPassed > 8) {
        		numDaysPassed = 8;
        	}
        	for(var i = 0; i < numDaysPassed; i++) {
        		newDay();
        	}
        	
        }
        
        tTimer = new Timer.Timer();
        
        
        clockTime = Sys.getClockTime();
        var nonMilHour = clockTime.hour;
    	isPM = false;
    	if(nonMilHour >= 12) {
    		isPM = true;
    		nonMilHour -= 12;
    	}
    	if(nonMilHour == 0) {
    		nonMilHour = 12;//basically 0 o clock = 12 o clock
    	}
        timeString = Lang.format("$1$:$2$", [nonMilHour, clockTime.min.format("%02d")]);
		
//        tTimer.start(method(:timeTimer), 60000, true);
		tTimer.start(method(:timeTimer), 60000, true);
        
        
        //current day index, 1 through 7, to keep track of which "day" it's storing information on
        //still need heartArrays, and heartTimestamps
        
        
     //   var testHist = 0;
        
     //   System.println(app.getProperty("heartNums1"));
        //(2497-27-92), (2254-25-90), (2359-27-87), (2201-26-84), (2229-27-82), (1040-13-80)
        //(2497-27-92), (2386-26-91), (2311-26-88), (2347-27-86), (2178-26-83), (2185-27-80), (245-3-81),
        Ui.requestUpdate();
    }
    
    function initProperties() {
        if(app.getProperty("currentDayTime") == null) {
        	app.setProperty("currentDayTime", Tim.today().value());
        }
        
        //will be timestamps of when the last average was taken(Not exact time, but rather, the day, so the UTC timestamp of 12:00 AM on that day)
        if(app.getProperty("lastAvg") == null) {
        	app.setProperty("lastAvg", -1);
        }
        
        //Days 1-7, epoch time for what would be days 1-7.
        //heartNums = (heartTotal,heartCount,heartAvg)
        //
        
        var defaultNums = "(0-0-0)";
        if(app.getProperty("heartDay1") == null) {
        	app.setProperty("heartDay1", Tim.today().value());
        	var emptyHeartNums = new [48];
	        for(var i = 0; i < 48; i++) {
	        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
	        }
        	app.setProperty("heartNums1", emptyHeartNums);
//        	var heartTime1 = new [48];
//        	heartTime1 = makeTimeArray(heartTime1, app.getProperty("heartDay1"));
//        	app.setProperty("heartTime1", heartTime1);
        }
        if(app.getProperty("heartDay2") == null) {
        	app.setProperty("heartDay2", app.getProperty("heartDay1") - 86400);
        	var emptyHeartNums = new [48];
	        for(var i = 0; i < 48; i++) {
	        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
	        }
        	app.setProperty("heartNums2", emptyHeartNums);
        }
        if(app.getProperty("heartDay3") == null) {
        	app.setProperty("heartDay3", app.getProperty("heartDay2") - 86400);
        	var emptyHeartNums = new [48];
	        for(var i = 0; i < 48; i++) {
	        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
	        }
        	app.setProperty("heartNums3", emptyHeartNums);
        }
        if(app.getProperty("heartDay4") == null) {
        	app.setProperty("heartDay4", app.getProperty("heartDay3") - 86400);
        	var emptyHeartNums = new [48];
	        for(var i = 0; i < 48; i++) {
	        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
	        }
        	app.setProperty("heartNums4", emptyHeartNums);
        }
        if(app.getProperty("heartDay5") == null) {
        	app.setProperty("heartDay5", app.getProperty("heartDay4") - 86400);
        	var emptyHeartNums = new [48];
	        for(var i = 0; i < 48; i++) {
	        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
	        }
        	app.setProperty("heartNums5", emptyHeartNums);
        }
        if(app.getProperty("heartDay6") == null) {
        	app.setProperty("heartDay6", app.getProperty("heartDay5") - 86400);
        	var emptyHeartNums = new [48];
	        for(var i = 0; i < 48; i++) {
	        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
	        }
        	app.setProperty("heartNums6", emptyHeartNums);
        }
        if(app.getProperty("heartDay7") == null) {
        	app.setProperty("heartDay7", app.getProperty("heartDay6") - 86400);
        	var emptyHeartNums = new [48];
	        for(var i = 0; i < 48; i++) {
	        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
	        }
        	app.setProperty("heartNums7", emptyHeartNums);
        }
        if(app.getProperty("heartDay8") == null) {
        	app.setProperty("heartDay8", app.getProperty("heartDay7") - 86400);
        	var emptyHeartNums = new [48];
	        for(var i = 0; i < 48; i++) {
	        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
	        }
        	app.setProperty("heartNums8", emptyHeartNums);
        }
        
        
        
        
      
        
        //actually accumulating HR data for the averages
        //total heart beats 
        if(app.getProperty("curHRTotal") == null) {
        	app.setProperty("curHRTotal", 0);
        }
        //total heart beat "counts", or ticks, would go up to 30, for 30 minutes, then average curHRTotal
        if(app.getProperty("curHRCount") == null) {
        	app.setProperty("curHRCount", 0);
        }
        curHRCount = app.getProperty("curHRCount");
        curHRTotal = app.getProperty("curHRTotal");
        
//        var curTime = Sys.getClockTime();

		//unix time of last date with full info(I.e. most likely NOT TODAY, so yesterday would be a "full day" of stored data)
		//0 if have not synced yet, so everything is new 
		if(app.getProperty("lastSync") == null) {
			app.setProperty("lastSync", 0);
		}
		//0 if not set
        if(app.getProperty("begDate") == null) {
        	app.setProperty("begDate", "0");
        }
        
        getPastData();
        
        hist = Act.getHeartRateHistory(null,true);
        var curSample = hist.next();
        //gets the MOST RECENT, regardless of whether or not it's "null", if it's "null" it's just 255, or INVALID_HR_SAMPLE
        if(curSample != null && curSample.heartRate != Act.INVALID_HR_SAMPLE) {
        	
        	curHR = curSample.heartRate;
   //     	System.println("curHR: " + curHR);
        }
        else {
        	curHR = null;
        }
    }
    
    function getPastData() {
    	var timeArray = new [48];
    	timeArray = makeTimeArray(timeArray, Tim.today().value());
    	var index = getHeartIndex(Tim.now().value());
    	
    	hist = Act.getHeartRateHistory(null,true);
    	var beginIndex = index - 7;
    	
    	if(beginIndex < 0) {
    		beginIndex = 0;
    	}
        var curSample = hist.next();
        var heartNums = app.getProperty("heartNums1");
        //gets the MOST RECENT, regardless of whether or not it's "null", if it's "null" it's just 255, or INVALID_HR_SAMPLE
//        for(var i = beginIndex; i <= index; i++) {
//        	heartNums[i] = "(0-0-0)";
//        }
        var curHRTotal = 0;
        var curHRCount = 0;
        var curHRAvg = 0;
        
//        var testCount = 0;
        while(curSample != null && index >= beginIndex) {
        	if(curSample.heartRate != Act.INVALID_HR_SAMPLE) {
        	
	        	curHR = curSample.heartRate;
	        	
	       // 	System.println("curHR: " + curHR);
		    }
		    else {
		    	curHR = null;
		    }
		    
		    var curTime = curSample.when.value();
		    
        	if(curTime > timeArray[index] && curSample.heartRate != Act.INVALID_HR_SAMPLE) {
        		curHRTotal += curHR;
        		curHRCount += 1;
        		curHRAvg = curHRTotal / curHRCount;
        	}
        	else if(curTime <= timeArray[index]){
        		var newHeartString = "(" + curHRTotal + "-" + curHRCount + "-" + curHRAvg + ")";
        		heartNums[index] = newHeartString;
        		index -= 1;
        		curHRTotal = 0;
        		curHRCount = 0;
        		curHRAvg = 0;
        		
        		if(curSample.heartRate != Act.INVALID_HR_SAMPLE) {
	        		curHRTotal += curHR;
	        		curHRCount += 1;
	        		curHRAvg = curHRTotal / curHRCount;
        		}
        	}
        	
        	curSample = hist.next();
        }
        
        app.setProperty("heartNums1",heartNums);
        
    }
    
    //will always get a 48-size array, make it based on time. So 30 minute/1800 second intervals
    function makeTimeArray(arr, time) {
    	
    	for(var i = 0; i < 48; i++) {
    		arr[i] = time;
    		time += 1800;
    	}
    	return arr;
    }
	
	// this is the page == 0 and it only shows the watch stuff, nothing about heart Rate
    function drawIntroPage(dc) {
				// we update the userID and begDate variable everytime it wants to draw the intro page (this ensures that the userID is always checked when the app is resumed
        userID = app.getProperty("userID");
        begDate = app.getProperty("begDate");
        //var temp = app.getProperty("lastSync");
		
		var displayDay = "";
		//uninitialized
		
		var useDay = Tim.Gregorian.info(Tim.today(), Tim.FORMAT_MEDIUM);
		displayDay = "" + useDay.day_of_week + " " + useDay.month + " " + useDay.day;
		
		
		
    	
    	
    	
    	var displayBattery = "Battery: ";
    	var batteryNum = Sys.getSystemStats().battery.format("%0.2f");
    	var ID = "";
    	if(userID == null) {
    		ID += "No ID set";
    	}
    	else {
    		ID += userID;
    	}
    	
    	var displayAMPM = "";
    	if(isPM == true) {
    		displayAMPM = "PM";
    	}
    	else {
    		displayAMPM = "AM";
    	}
    	
    	displayBattery += batteryNum;
    	
    	dc.drawText(0,0, Gfx.FONT_SMALL, displayAMPM, Gfx.TEXT_JUSTIFY_LEFT);
    	dc.drawText(dc.getWidth()/3, 0, Gfx.FONT_SMALL, displayBattery, Gfx.TEXT_JUSTIFY_LEFT);
    	dc.drawText(dc.getWidth() / 2, dc.getHeight() / 4, Gfx.FONT_NUMBER_THAI_HOT, timeString, Gfx.TEXT_JUSTIFY_CENTER);
    	dc.drawText(dc.getWidth() / 2, 5 * dc.getHeight() / 8, Gfx.FONT_SMALL, displayDay, Gfx.TEXT_JUSTIFY_CENTER);
    	//dc.drawText(dc.getWidth() / 2, 6 * dc.getHeight() / 8, Gfx.FONT_SMALL, temp, Gfx.TEXT_JUSTIFY_CENTER);
    	dc.drawText(dc.getWidth()/2,7*dc.getHeight() / 8,Gfx.FONT_MEDIUM, ID, Gfx.TEXT_JUSTIFY_CENTER);

    	
    	//things to draw: User ID, Battery, cur HR, current HR counts, current HR averages

    }
    
    // this is the page == 1, this page displays all you need to know about the heart Rate
    function drawHRpage(dc){
    	var displayHRText = "Heart Rate: "; // notice how the display HR is changed to just an empty string
    	var displayHR = "";
    	//var displayIndex = "Index: ";
    	var displayHRTotal = "HRTotal: ";
    	var displayHRCount ="HRCount: ";
    	var displayHRAvg = "HRAvg: ";
    	//var displayBattery = "Battery: ";
    	//var batteryNum = Sys.getSystemStats().battery.format("%0.2f");
    	//displayBattery += batteryNum;
    	
    	if(curHR == null) {
    		displayHR += "N/A";
//			displayHR += Tim.today().value();
    	}
    	else {
    		displayHR += curHR;
    	}
    	if(curHRCount == null) {
    		displayHRCount += "N/A";
    	}
    	else {
    		displayHRCount += curHRCount;
    	}
    	if(curHRTotal == null) {
    		displayHRTotal += "N/A";
    	}
    	else {
    		displayHRTotal += curHRTotal;
    	}
    	if(curHRAvg == null) {
    		displayHRAvg += "N/A";
    	}
    	else {
    		displayHRAvg += curHRAvg;
    	}
    	
    	var lastSync = app.getProperty("lastSync");
		var lastSyncNum = lastSync.toNumber();
		
		var lastSyncMoment = new Tim.Moment(lastSyncNum);
//		lastSyncMoment = lastSyncMoment.initialize(lastSyncNum);
		var displayDay = "Last Sync: ";
		System.println(lastSyncMoment.value());
		//uninitialized
		if(lastSyncNum == 0) {
			displayDay += "Never";
		}
		else {
			var useDay = Tim.Gregorian.info(lastSyncMoment, Tim.FORMAT_MEDIUM);
			displayDay += " " + useDay.month + " " + useDay.day;
		}
    	
    	
    	dc.drawText(dc.getWidth()/2, 0, Gfx.FONT_SMALL, displayHRText, Gfx.TEXT_JUSTIFY_CENTER);
    	dc.drawText(dc.getWidth()/2, dc.getHeight()/8 , Gfx.FONT_NUMBER_HOT, displayHR, Gfx.TEXT_JUSTIFY_CENTER);
    	dc.drawText(0, 6*dc.getHeight() /8, Gfx.FONT_SMALL, displayDay, Gfx.TEXT_JUSTIFY_LEFT);
    	dc.drawText(0, 5* dc.getHeight() /8, Gfx.FONT_SMALL, displayHRCount, Gfx.TEXT_JUSTIFY_LEFT);
    	dc.drawText(0, 4* dc.getHeight() / 8, Gfx.FONT_MEDIUM, displayHRAvg, Gfx.TEXT_JUSTIFY_LEFT);
    	//dc.drawText(0, 6* dc.getHeight() / 8, Gfx.FONT_SMALL, displayIndex, Gfx.TEXT_JUSTIFY_LEFT);
    }
    
    //will get index in the 48-size array based on the time and day.
    //i.e. it will get the correct 30-minute interval to put a heartNum into based on the time
    //time is epoch time at this moment, day is day 1 through 7
    //it will loop through the array, finding the first instance which time >= 30-minute-interval
    //returns -1 if day isnt a valid number
    function getHeartIndex(time) {
    	var index = 0;
    	var heartTime = new [48];
    	
    	heartTime =  makeTimeArray(heartTime, Tim.today().value());
    	
    	for(var i = 0; i < 48; i++) {
    		if(time >= heartTime[i]) {
    			index = i;
    		}
    		
    		
    		if(heartTime[i] > time) {
    			break;
    		}
    	}
    	
    	return index;
    }
	
	function drawStepsPage(dc) {
		var displayStepsText = "Steps:";
		var displaySteps = "";
		displaySteps += Act.getInfo().steps;
		
		var displayIntensityText = "Activity Minutes:";
		var displayIntensity = "";
		displayIntensity += Act.getInfo().activeMinutesDay.total;
			
		dc.drawText(dc.getWidth()/2, 0, Gfx.FONT_SMALL, displayStepsText, Gfx.TEXT_JUSTIFY_CENTER);
		dc.drawText(dc.getWidth()/2, dc.getHeight()/8 , Gfx.FONT_NUMBER_HOT, displaySteps, Gfx.TEXT_JUSTIFY_CENTER);
		dc.drawText(dc.getWidth()/2, dc.getHeight()/2, Gfx.FONT_SMALL, displayIntensityText, Gfx.TEXT_JUSTIFY_CENTER);
		dc.drawText(dc.getWidth()/2, dc.getHeight()/2 + dc.getHeight()/8 , Gfx.FONT_NUMBER_HOT, displayIntensity, Gfx.TEXT_JUSTIFY_CENTER);
		
	}
	
    function onUpdate(dc) {
    	
        dc.setColor(0xFFFFFF, 0x000000);
        dc.clear();
        dc.setColor(0xFFFFFF, 0x000000); // set the font to be white and the background to be black

        if(page == 0) {
            drawIntroPage(dc);
        } else if(page == 1){
      //  	System.println(page);
        	drawHRpage(dc);
        
          //  var i;
           // var y = 50;

           // dc.drawText(dc.getWidth() / 2, 20,  Gfx.FONT_MEDIUM, "Strings Received:", Gfx.TEXT_JUSTIFY_CENTER);
           // for(i = 0; i < stringsSize; i += 1) {
           //     dc.drawText(dc.getWidth() / 2, y,  Gfx.FONT_SMALL, strings[i], Gfx.TEXT_JUSTIFY_CENTER);
          //      y += 20;
         //   }
        }
        else  {
        	drawStepsPage(dc);
        }
    }


}