//
// Copyright 2015-2016 by Garmin Ltd. or its subsidiaries.
// Subject to Garmin SDK License Agreement and Wearables
// Application Developer Agreement.
//

using Toybox.Application as App;
using Toybox.Communications as Comm;
using Toybox.WatchUi as Ui;
using Toybox.System as Sys;
using Toybox.ActivityMonitor as Act;
using Toybox.Time as Tim;

var page = 0;
var strings = ["","","","",""];
var stringsSize = 5;
var userID = "0000";
var begDate = "";
var mailMethod;
var phoneMethod;
var crashOnMessage = false;
var app;
var retTimer;

class CommExample extends App.AppBase {

    function initialize() {
        App.AppBase.initialize();
		app = App.getApp(); // this app variable works just like the one in commview, this allows me to do setProperty from this commApp.mc
		
		retTimer = new Timer.Timer();
		
        mailMethod = method(:onMail);
        phoneMethod = method(:onPhone);
        if(Comm has :registerForPhoneAppMessages) {
            Comm.registerForPhoneAppMessages(phoneMethod);
        } else {
            Comm.setMailboxListener(mailMethod);
        }
    }

    // onStart() is called on application start up
    function onStart(state) {
    }

    // onStop() is called when your application is exiting
    function onStop(state) {
    }

    // Return the initial view of your application here
    function getInitialView() {
        return [new CommView(), new CommInputDelegate()];
    }

    function onMail(mailIter) {
        var mail;

        mail = mailIter.next();

        while(mail != null) {
            var i;
            for(i = (stringsSize - 1); i > 0; i -= 1) {
                strings[i] = strings[i-1];
            }
            strings[0] = mail.toString();
            page = 1;
            mail = mailIter.next();
        }

        Comm.emptyMailbox();
        Ui.requestUpdate();
    }

    function onPhone(msg) {
        var i;

        if((crashOnMessage == true) && msg.data.equals("Hi")) {
            foo = bar;
        }

        for(i = (stringsSize - 1); i > 0; i -= 1) {
            strings[i] = strings[i-1];
        }
        
        var temp = msg.data.toString();
        
        if(temp.equals("success")){
        	app.setProperty("lastSync", Tim.today().value()); // updates the last sync data to today
        }else{
        
	        userID = temp.substring(0,4);
	        begDate = temp.substring(4,temp.length());
			
			app.setProperty("lastSync",begDate.toNumber());
			        
	        // we set property only when we receive a message
	        // look at the initialize function for commApp befcause I added some stuff there
	        app.setProperty("begDate", begDate);
	        app.setProperty("userID",userID);
	        
	        userID = app.getProperty("userID");
	        begDate = app.getProperty("begDate");
        }
        
        strings[0] = temp;
        //page = 1;

        Ui.requestUpdate();
    }

}