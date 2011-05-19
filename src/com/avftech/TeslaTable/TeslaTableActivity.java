/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.avftech.TeslaTable;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * This is an example of using the accelerometer to integrate the device's
 * acceleration to a position using the Verlet method. This is illustrated with
 * a very simple particle system comprised of a few iron balls freely moving on
 * an inclined wooden table. The inclination of the virtual table is controlled
 * by the device's accelerometer.
 * 
 * @see SensorManager
 * @see SensorEvent
 * @see Sensor
 */

public class TeslaTableActivity extends Activity {

    private SimulationView mSimulationView;
    SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager;
    Display mDisplay;
    private WakeLock mWakeLock;
	private MediaPlayer mMediaPlayer;
	private AssetFileDescriptor mAssetFileDescriptor;
	private AlertDialog mAlert;
	private boolean mFirstResume = true;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
   
        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get an instance of the PowerManager
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Get an instance of the WindowManager
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();

        // Create a bright wake lock
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass()
                .getName());
        
        mMediaPlayer = new MediaPlayer();
        
        try {
        	mAssetFileDescriptor = getAssets().openFd("music.mp3");
			mMediaPlayer.setDataSource(mAssetFileDescriptor.getFileDescriptor(),
									   mAssetFileDescriptor.getStartOffset(),
					                   mAssetFileDescriptor.getLength());
		} catch (IllegalArgumentException e) {
			Log.e("TeslaTable", "Couldn't read music data due to IllegalArgumentException" + e);
		} catch (IllegalStateException e) {
			Log.e("TeslaTable", "Couldn't read music data due to IllegalStateException" + e);
		} catch (IOException e) {
			Log.e("TeslaTable", "Couldn't read music data due to IOException" + e);
		}	
        
        DisplayMetrics displayMetrics = initializeDisplayMetrics();
        SystemDimensions systemDimensions = new SystemDimensions();
        PhysicsEngineConvertor convertor = new PhysicsEngineConvertor(displayMetrics,
        															  systemDimensions );
        mSimulationView = new SimulationView(this, this, convertor, systemDimensions);
        setContentView(mSimulationView);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Welcome to Tesla's Table.  The yellow orbs are sensitive to magnetism,"+
        		" all the orbs respond to touch.  Music is \"Sleep Well\" from Trans Alp's Silizium," +
        		"available at http://j.mp/jxAfo4.  Source is available on Github" +
        		"at http://j.mp/lSJdbU?r=bb ");
        builder.setTitle("About");
        builder.setPositiveButton("Ok", null);
        mAlert = builder.create();
        
    }

    private DisplayMetrics initializeDisplayMetrics(){
		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        /*
         * when the activity is resumed, we acquire a wake-lock so that the
         * screen stays on, since the user will likely not be fiddling with the
         * screen or buttons.
         */
        
        mWakeLock.acquire();
        if(mFirstResume){
        	mAlert.show();
        	mFirstResume = false; //Probably doesn't work all the time, but good enough for today
        }
        // Start the simulation
        mSimulationView.startSimulation();
        
        try {
			mMediaPlayer.prepare();
		} catch (IllegalStateException e) {
			Log.e("TeslaTable", "Couldn't read music data due to IllegalStateException" + e);
		} catch (IOException e) {
			Log.e("TeslaTable", "Couldn't read music data due to IOException" + e);
		}
        mMediaPlayer.start();
        mMediaPlayer.setLooping(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*
         * When the activity is paused, we make sure to stop the simulation,
         * release our sensor resources and wake locks
         */

        // Stop the simulation
        mSimulationView.stopSimulation();
        mMediaPlayer.stop();
        // and release our wake-lock
        mWakeLock.release();
    }
}
