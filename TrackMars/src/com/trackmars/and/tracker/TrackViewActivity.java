package com.trackmars.and.tracker;

import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import com.google.android.gms.maps.SupportMapFragment;
import com.trackmars.and.tracker.dataUtils.EntityHelper;
import com.trackmars.and.tracker.model.Track;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

public class TrackViewActivity extends FragmentActivity {
	
	private GoogleMap map;
	Integer id;
	Long created;
	String title;
    private TrackRecorderService trackRecorderService;
    private Rectangle shapeRectangle;

	EntityHelper entityHelper;
	Track track;
    
    
	class MyTask extends AsyncTask<Void, Void, Void> {

	    @Override
	    protected void onPreExecute() {
	      super.onPreExecute();
	      //tvInfo.setText("Begin");
	    }

	    @Override
	    protected Void doInBackground(Void... params) {
	      //TimeUnit.SECONDS.sleep(2);
	      setLocation();
	      return null;
	    }

	    @Override
	    protected void onPostExecute(Void result) {
	      super.onPostExecute(result);
	      //tvInfo.setText("End");
	    }
	  }	
	
	
    @SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_track);

		Bundle extras = getIntent().getExtras();
		id = extras.getInt("id");
		created = extras.getLong("created");
		title = extras.getString("title");

        try {
	        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
	                .getMap();
	        
        } finally {}
        
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.intoFrame);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        ListTracksItemTrack listTracksItemTrack = new ListTracksItemTrack();
        
        Bundle args = new Bundle();
        
        args.putString("title", title);
        args.putLong("created", created);
        args.putInt("id", id);
        
        listTracksItemTrack.setArguments(args);
     
        ft.replace(R.id.intoFrame, listTracksItemTrack);
        ft.commit();         
        
    }
    
    
    private Rectangle getRectangle(Track track) {
    	Rectangle rectangle = new Rectangle();
    	
    	if (track.LEFT != null && track.RIGHT != null && track.TOP != null && track.BOTTOM != null) {
    		rectangle.create(track.LEFT, track.RIGHT, track.TOP, track.BOTTOM);
    	} else {
    		rectangle = null;
    	}
    	
    	return rectangle;
    }
    
    
    private void setLocation() {
    	try {
    		
    		entityHelper = new EntityHelper(getApplicationContext(), Track.class);
    		track = (Track) entityHelper.getRow(id);
    		
    		this.shapeRectangle = getRectangle(track);
    		
    		
			List<LatLng> latLngs = trackRecorderService.getAllTrackPoint(id);
			
	        Message msg = new Message();
	        msg.obj = latLngs;
	        
	        handler.sendMessage(msg);
	        
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
        	trackRecorderService = ((TrackRecorderService.ManagerBinder) binder).getMe();
    		MyTask mt = new MyTask();
    	    mt.execute();		
        }

        public void onServiceDisconnected(ComponentName className) {
        }
        
    };    

    
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			final List<LatLng> latLngs = (List<LatLng>)msg.obj;
			Boolean updateShape = TrackViewActivity.this.shapeRectangle == null;
			
			if (updateShape) {
				TrackViewActivity.this.shapeRectangle = new Rectangle();
			}

	    	PolylineOptions polylineOptions = new PolylineOptions();
	    	polylineOptions.geodesic(true);
	    	
	    	for (LatLng latLng : latLngs) {
	    		Log.d(MainActivity.class.getName(), "latLng " + latLng.latitude + " " + latLng.longitude);
	    		polylineOptions.add(latLng);
	    		
	    		if (updateShape) {
	    			TrackViewActivity.this.shapeRectangle.shape(latLng);
	    		}
	    		
	    	}

	    	if (map != null) {
		    	map.addPolyline(polylineOptions);
	    	}
			
	    	if (updateShape) {
	    		track.TOP = shapeRectangle.getTop();
	    		track.BOTTOM = shapeRectangle.getBottom();
	    		track.LEFT = shapeRectangle.getLeft();
	    		track.RIGHT = shapeRectangle.getRight();
	    		entityHelper.save(track);
	    	}
	    	
	    	if (map != null) {
	        	LatLng myCurrentPosition = new LatLng((track.TOP + track.BOTTOM) / 2, (track.LEFT + track.RIGHT) / 2); 
	            map.moveCamera(CameraUpdateFactory.newLatLng(myCurrentPosition));
		        map.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
	    	}
			
		}
	};		
    
    
    @Override
    protected void onResume() {
        super.onResume();
        
        Log.d(MainActivity.class.getName(), "Main activity onResume started");

        Intent intent = new Intent(this, TrackRecorderService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    	
    }
    
	public void onClick(View view) {
 	}

	
}
