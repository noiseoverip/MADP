package com.madp.meetme;

import com.madp.maps.GPSFindLocationFromStringOnMap;
import com.madp.meetme.webapi.WebService;
import com.madp.meetme.common.entities.LatLonPoint;
import com.madp.meetme.common.entities.Meeting;
import com.madp.meetme.common.entities.User;
import com.madp.utils.Logger;
import com.madp.utils.SerializerHelper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

/* Background process that sends the user coordinates to server during the meeting. It also display  
 *  a notification about the meeting. 
 */

public class BackgroundMeetingManager extends Service implements LocationListener {

	private WebService ws;
	private User user;
	private LocationManager lm;
	private NotificationManager notificationManager;
	private Meeting meeting;
	private static final int NOTIFICATION_ID = 654321;
	
	@Override
	public IBinder onBind(Intent arg0) {return null;}

	/* Inits */
	@Override
	public void onCreate() {
		ws = new WebService(new Logger());
		lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		
		/* HARDCODED */
		user = new User(1989, "name", "noiseoverip@gmail.com");
		/** **/
		notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		Toast.makeText(this, "The meeting is now started", Toast.LENGTH_LONG).show();	
	}

	/* When the meetup is over */
	@Override
	public void onDestroy() {
		Toast.makeText(this, "The Meeting has now stopped. Your position is not displayed in the meeting anymore!", Toast.LENGTH_LONG).show();
		lm.removeUpdates(this);
		notificationManager.cancel(NOTIFICATION_ID);
	}

	/* During the meeting */
	@Override
	public void onStart(Intent intent, int startid) {
		
		/* Get the id and the meeting object from the MeetingAlarmManager when the meeting should start
		 * 1) Get the meeting object
		 * 2) Create notification manager
		 * 3) Start look for GPS position updates 
		 *   */
		int meetingId;
		Bundle extras = intent.getExtras();
		meetingId = extras.getInt("meetingid");
		meeting = ws.getMeeting(meetingId); //Get meeting object from server
				
		/*Start look for locationupdates */
		Toast.makeText(this, "Your position is now displayed to other participants", Toast.LENGTH_LONG).show();
		int icon = R.drawable.user;
		CharSequence ntext = "MeetMe is started";
		CharSequence contentTitle = "MeetMe - Click To View";
		long when = System.currentTimeMillis();
		
		
		/* HARDCODED SHIT REMOVE LATER 
		 * 
		 * Hardcoded coordinates as we do not set that into the new meetingactivity
		 * 
		 * */
		meeting.setCoordinates(new LatLonPoint(100, 100));
		/* ***************************/
		
		Intent mapIntent = new Intent(this, GPSFindLocationFromStringOnMap.class);
		mapIntent.putExtra("meeting", SerializerHelper.serializeObject(meeting));
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, mapIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		CharSequence contentText = meeting.getTitle() + " " + meeting.getAddress();
		Notification notification = new Notification(icon, ntext, when);
		
		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
		notificationManager.notify(NOTIFICATION_ID, notification);
		
		/* Start request updates */
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 10.0f, this);		
	}

	/*	When loocation has changed, update the user object and send it to the server
	 * 
	 * */
	@Override
	public void onLocationChanged(Location location) {
	
		/* Update and send the new position to the server */
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		
		Toast.makeText(this, "Moved to latitude: " + latitude * 1000000 + " longitude: " + longitude*1000000, Toast.LENGTH_LONG).show();
		
		user.setLatitude( latitude);
		user.setLongitude(longitude);
		
		/* Broadcast user position to gps activity */
		/*
		Intent userCordinates = new Intent(this, GPSLocationFinderActivity.class); //OR WHAT EVER?
		Bundle b=new Bundle();
		b.putByteArray("user", SerializerHelper.serializeObject(user));
		userCordinates.putExtras(b);
		sendBroadcast(userCordinates);
		
		*/
		
		// Update user position on the server
		new Thread(new Runnable(){
			@Override
			public void run() {
				ws.updateUser(user);
			}
		}).start();

		/* Broadcast update to mapview activity */
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(getApplicationContext(),"GPS Disabled", Toast.LENGTH_LONG);
	}

	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(getApplicationContext(),"GPS Enabled", Toast.LENGTH_LONG);

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
}
