package com.example.android.accelerometerplay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.BitmapFactory.Options;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;

class SimulationView extends View implements SensorEventListener {
    /**
	 * 
	 */
	private final AccelerometerPlayActivity accelerometerPlayActivity;
    private Sensor mAccelerometer;
    long mLastT;
    float mLastDeltaT;

	int mWidth;
	int mHeight;
    private Bitmap mWood;
    private float mXOrigin;
    private float mYOrigin;
    private float mSensorX;
    private float mSensorY;
    private long mSensorTimeStamp;
    private long mCpuTimeStamp;
    private ParticleSystem mParticleSystem;
    private DisplayMetrics mDisplayMetrics;

	private PhysicsEngineConvertor mConvertor;

    public void startSimulation() {
        
    	// Using SENSOR_DELAY_UI serves as alow-pass filter to 
    	// eliminate gravity from sensor readings
        this.accelerometerPlayActivity.mSensorManager.registerListener(this, mAccelerometer, 
        		SensorManager.SENSOR_DELAY_UI);
        
        if (this.mParticleSystem == null){
            mParticleSystem = new ParticleSystem(this, accelerometerPlayActivity, mConvertor);
        }
    }

    public void stopSimulation() {
        this.accelerometerPlayActivity.mSensorManager.unregisterListener(this);
    }

    public SimulationView(AccelerometerPlayActivity accelerometerPlayActivity, Context context,
    		              PhysicsEngineConvertor convertor, SystemDimensions systemDimensions) {
        super(context);
		this.accelerometerPlayActivity = accelerometerPlayActivity;
        mAccelerometer = this.accelerometerPlayActivity.mSensorManager.getDefaultSensor(
        																Sensor.TYPE_ACCELEROMETER);
        Options opts = new Options();
        opts.inDither = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        mWood = BitmapFactory.decodeResource(getResources(), R.drawable.wood, opts);
        mConvertor = convertor;
    }



	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // compute the origin of the screen relative to the origin of
        // the bitmap
    	mWidth = w;
    	mHeight = h;
        mParticleSystem.onSizeChanged(w,h);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        /*
         * record the accelerometer data, the event's timestamp as well as
         * the current time. The latter is needed so we can calculate the
         * "present" time during rendering. In this application, we need to
         * take into account how the screen is rotated with respect to the
         * sensors (which always return data in a coordinate space aligned
         * to with the screen in its native orientation).
         */
       
        switch (this.accelerometerPlayActivity.mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                mSensorX = event.values[0];
                mSensorY = event.values[1];
                break;
            case Surface.ROTATION_90:
                mSensorX = -event.values[1];
                mSensorY = event.values[0];
                break;
            case Surface.ROTATION_180:
                mSensorX = -event.values[0];
                mSensorY = -event.values[1];
                break;
            case Surface.ROTATION_270:
                mSensorX = event.values[1];
                mSensorY = -event.values[0];
                break;
        }

        mSensorTimeStamp = event.timestamp;
        mCpuTimeStamp = System.nanoTime();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //draw the background
        canvas.drawBitmap(mWood, 0, 0, null);

        //compute the new position of our object, based on accelerometer
        //data and present time.
        final ParticleSystem particleSystem = mParticleSystem;
        final long now = mSensorTimeStamp + (System.nanoTime() - mCpuTimeStamp);
        final float sx = mSensorX;
        final float sy = mSensorY;

        particleSystem.update(sx, sy, now);
        final int count = particleSystem.getParticleCount();
        for (int i = 0; i < count; i++) {
            /*
             * We transform the canvas so that the coordinate system matches
             * the sensors coordinate system with the origin in the center
             * of the screen and the unit is the meter.
             */
            final Bitmap bitmap = mParticleSystem.getBitmap(i);
            final float xc = (mWidth - bitmap.getWidth()) * 0.5f;
            final float yc = (mHeight - bitmap.getHeight()) * 0.5f;
            final float x = xc + mConvertor.convertToScreenX( mParticleSystem.getPosX(i) );
            final float y = yc - mConvertor.convertToScreenY( mParticleSystem.getPosY(i) );
            canvas.drawBitmap(bitmap, x, y, null);
        }

        // and make sure to redraw asap
        invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    	
    @Override
    public boolean onTouchEvent(MotionEvent event){
    	switch(event.getAction()){
    	case(MotionEvent.ACTION_DOWN):
    	    handleActionDown(event);
    		break;
    	case(MotionEvent.ACTION_UP):
    	    handleActionUp(event);
    		break;
    	case(MotionEvent.ACTION_MOVE):
    	    handleActionMove(event);
    		break;
    	default:
    		return false;
    	}
    	invalidate();
    	return true;
    }

	
    private void handleActionDown(MotionEvent event) {
		final int numPointers = event.getPointerCount();
		for(int pointerIndex = 0;  pointerIndex < numPointers; pointerIndex++){
			final int pointerId = event.getPointerId(pointerIndex);
			final float pointer_x  = event.getX(pointerIndex);
			final float pointer_y  = event.getY(pointerIndex);
			for (Particle p : mParticleSystem.getParticles()){
				if( p.intersects(pointer_x, pointer_y) ){
					p.handleActionDownPointer(pointerId);
				}
			}
		}
		
	}
    
	private void handleActionUp(MotionEvent event) {
		final int numPointers = event.getPointerCount();
		for(int pointerIndex = 0;  pointerIndex < numPointers; pointerIndex++){
			final int pointerId = event.getPointerId(pointerIndex);
			for (Particle p : mParticleSystem.getParticles()){
				if( p.touchedBy(pointerId) ){
					p.handleActionUp();
				}
			}
		}
		
	}

	
	private void handleActionMove(MotionEvent event) {
		final int numPointers = event.getPointerCount();
		for(int pointerIndex = 0;  pointerIndex < numPointers; pointerIndex++){
			final int pointerId = event.getPointerId(pointerIndex);
			for (Particle p : mParticleSystem.getParticles()){
				if( p.touchedBy(pointerId) ){
					p.handleActionMove(event);
				}
			}
		}
		
	}
	
	public DisplayMetrics getDisplayMetrics() {
		return mDisplayMetrics;
	}
}