//
// Copyright 2016 by Garmin Ltd. or its subsidiaries.
// Subject to Garmin SDK License Agreement and Wearables
// Application Developer Agreement.
//

using Toybox.WatchUi as Ui;
using Toybox.System as Sys;
using Toybox.Communications as Comm;
using Toybox.ActivityMonitor as Act;
using Toybox.Time as Tim;
using Toybox.SensorHistory as Sen;
using Toybox.Application as App;

class CommListener extends Comm.ConnectionListener {
    function initialize() {
        Comm.ConnectionListener.initialize();
    }

    function onComplete() {
        Sys.println("Transmit Complete");
//        App.getApp().setProperty("lastSync", Tim.today().value() - 86400);
    }

    function onError() {
        Sys.println("Transmit Failed");
    }
}

class CommInputDelegate extends Ui.BehaviorDelegate {
    function initialize() {
        Ui.BehaviorDelegate.initialize();
    }

    function onMenu() {
        var menu = new Ui.Menu();
        var delegate;

        menu.addItem("Send Data", :sendData);
//        menu.addItem("Set Listener", :setListener);
        delegate = new BaseMenuDelegate();
        Ui.pushView(menu, delegate, SLIDE_IMMEDIATE);

        return true;
    }
    
    //basically just returns to watchface screen after a set amount of time
    function returnHome() {
    	page = 0;
   // 	System.println("RETURNED HOME");
    	retTimer.stop();
    	Ui.requestUpdate();
    }

    function onTap(event) {
    	System.println("Tap");
        if(page == 0) {
        	retTimer.start(method(:returnHome), 30000, false);
            page = 1;
        } else if(page == 1){
        	retTimer.stop();
        	retTimer.start(method(:returnHome), 30000, false);
            page = 2;
        }
        else {
        	retTimer.stop();
        	page = 0;
        }
        Ui.requestUpdate();
    }
}

class BaseMenuDelegate extends Ui.MenuInputDelegate {
    function initialize() {
        Ui.MenuInputDelegate.initialize();
    }

    function onMenuItem(item) {
        var menu = new Ui.Menu();
        var delegate = null;

        if(item == :sendData) {
            menu.addItem("Sync", :sync);
    //        menu.addItem("New Day", :newDay);
    //        menu.addItem("ClearAll", :clear);
    //        menu.addItem("Print", :print);
    //        menu.addItem("ClearUserData", :clearU);
            delegate = new SendMenuDelegate();
        } 
        /*else if(item == :setListener) {
            menu.setTitle("Listner Type");
            menu.addItem("Mailbox", :mailbox);
            if(Comm has :registerForPhoneAppMessages) {
                menu.addItem("Phone App", :phone);
            }
            menu.addItem("None", :none);
            menu.addItem("Crash if 'Hi'", :phoneFail);
            delegate = new ListnerMenuDelegate();
        }
		*/
        Ui.pushView(menu, delegate, SLIDE_IMMEDIATE);
    }
}

//Transmit here
class SendMenuDelegate extends Ui.MenuInputDelegate {
    function initialize() {
        Ui.MenuInputDelegate.initialize();
    }

    function onMenuItem(item) {
        var listener = new CommListener();
       	var toTest = 5;
       	var app = App.getApp();
   //     System.println(Act.getHistory().size());
        //Will be a 2-dimensional array, 3 rows(or however many needed), one for each set of data.
        //i.e. arr[0] will be an array of steps, arr[1] will be an array of heartrates, arr[2] will be an array of activity minutes
        var arrayToSend = new [14];
        //3 arrays for steps, intensity minutes, timestamps
        //7*2 for daily heart-rate averages(96 of them max, for 15 minute intervals), and timestamps for averages
        
        //FORMAT:
        //array[0] = steps, starting from Today all the way to up to 7 days ago.
        //array[1] = intensity minutes = moderate Intensity + 2*vigorous Intensity(It's just how they calculate it)
        //array[2] = time in epoch since unix...just use a converter, it should be the start of a day(e.g. May 9, 2017, 12:00 AM)
        
        //size = histSize(last 7 days) + 1(Today)
       	var actHistArray = Act.getHistory();//information from history(up to 7 days ago)
        var histSize = actHistArray.size();
        
        var userIDArr = new [1];
        userIDArr[0] = app.getProperty("userID");
        arrayToSend[0] = userIDArr;
        
        var lastSyncArr = new [1];
        lastSyncArr[0] = app.getProperty("lastSync");
        arrayToSend[1] = lastSyncArr; // 0 if never synced
        
        var begDateArr = new [1];
        begDateArr[0] = app.getProperty("begDate");
        arrayToSend[2] = begDateArr;// 0 if never set
        
        arrayToSend[3] = new [histSize + 1];
        arrayToSend[4] = new [histSize + 1];
        arrayToSend[5] = new [histSize + 1];
        var actInfo = Act.getInfo();//Information for TODAY
        var todayTime = Tim.today().value();
        var todayIntense = actInfo.activeMinutesDay.total;
        var todaySteps = actInfo.steps;
        
        arrayToSend[3][0] = todaySteps;
        arrayToSend[4][0] = todayIntense;
        arrayToSend[5][0] = todayTime;
     //   var testArr = app.getProperty("test3");
     //   var testArr2 = app.getProperty("test4");
        arrayToSend[6] = app.getProperty("heartNums1");
//        arrayToSend[4] = app.getProperty("heartTime1");
	
        arrayToSend[7] = app.getProperty("heartNums2");
//        arrayToSend[6] = app.getProperty("heartTime2");
	
        arrayToSend[8] = app.getProperty("heartNums3");
//        arrayToSend[8] = app.getProperty("heartTime3");
        
		arrayToSend[9] = app.getProperty("heartNums4");
//        arrayToSend[10] = app.getProperty("heartTime4");
        
		arrayToSend[10] = app.getProperty("heartNums5");
//        arrayToSend[12] = app.getProperty("heartTime5");
        
		arrayToSend[11] = app.getProperty("heartNums6");
//        arrayToSend[14] = app.getProperty("heartTime6");
        
		arrayToSend[12] = app.getProperty("heartNums7");
//        arrayToSend[16] = app.getProperty("heartTime7");
		arrayToSend[13] = app.getProperty("heartNums8");
	
        //i = 1 because i = 0 is today
        //gets i-1 from the array because the history starts yesterday, rather than today
        for(var i = 1; i <= histSize; i++) {
        	var curSteps = actHistArray[i-1].steps;
        	var curIntense = actHistArray[i-1].activeMinutes.total;
        	var curTime = actHistArray[i-1].startOfDay.value(); //time is in epoch
        	
        	arrayToSend[3][i] = curSteps;
        	arrayToSend[4][i] = curIntense;
        	arrayToSend[5][i] = curTime;
        }
        
       
		
        if(item == :sync) {
			Comm.transmit(arrayToSend, null, listener);
        } else if(item == :newDay) {
            newDay();
        } else if(item == :clear) {
            app.clearProperties();
            
        }
        else if(item == :print) {
        	System.println(arrayToSend);
        }
        else if(item == :clearU) {
        	app.deleteProperty("userID");
        	app.deleteProperty("lastSync");
        	app.deleteProperty("begDate");
        	
			app.setProperty("userID", null);
			app.setProperty("lastSync", 0);
			app.setProperty("begDate", "0");
        }
		
        Ui.popView(SLIDE_IMMEDIATE);
    }
   
    
    function newDay() {
    	var app = App.getApp();
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
    	var emptyHeartNums = new [48];
    	var defaultNums = "(0-0-0)";
        for(var i = 0; i < 48; i++) {
        	emptyHeartNums[i] = defaultNums;//initialized at 0 heart total, 0 heart count, 0 heart avg
        }
    	app.setProperty("heartNums1", emptyHeartNums);
    	
    	var testString = "heartNums";
    	var testString2 = "heartDay";
    	System.println("\nTESTING STUFF\n");
    	for(var i = 1; i <= 8; i++) {
    		System.println(i);
    		System.println(app.getProperty(testString2 + i));
    		System.println(app.getProperty(testString + i));
    		
    	}
    }
}


class ListnerMenuDelegate extends Ui.MenuInputDelegate {
    function initialize() {
        Ui.MenuInputDelegate.initialize();
    }

    function onMenuItem(item) {
        if(item == :mailbox) {
            Comm.setMailboxListener(mailMethod);
        } else if(item == :phone) {
            if(Comm has :registerForPhoneAppMessages) {
                Comm.registerForPhoneAppMessages(phoneMethod);
            }
        } else if(item == :none) {
            Comm.registerForPhoneAppMessages(null);
            Comm.setMailboxListener(null);
        } else if(item == :phoneFail) {
            crashOnMessage = true;
            Comm.registerForPhoneAppMessages(phoneMethod);
        }

        Ui.popView(SLIDE_IMMEDIATE);
    }
}
