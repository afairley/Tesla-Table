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
import android.view.Surface;
import android.view.View;

class SimulationView extends View implements SensorEventListener {
    /**
	 * 
	 */
	private final AccelerometerPlayActivity accelerometerPlayActivity;
	
    // friction of the virtual table and air
    static final float sFriction = 0.1f;

    private Sensor mAccelerometer;
    long mLastT;
    float mLastDeltaT;

    private float mXDpi;
    private float mYDpi;
    float mMetersToPixelsX;
    float mMetersToPixelsY;
    private Bitmap mBitmap;
    private Bitmap mWood;
    private float mXOrigin;
    private float mYOrigin;
    private float mSensorX;
    private float mSensorY;
    private long mSensorTimeStamp;
    private long mCpuTimeStamp;
    private final ParticleSystem mParticleSystem = new ParticleSystem(this);

    public void startSimulation() {
        /*
         * It is not necessary to get accelerometer events at a very high
         * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
         * automatic low-pass filter, which "extracts" the gravity component
         * of the acceleration. As an added benefit, we use less power and
         * CPU resources.
         */
        this.accelerometerPlayActivity.mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stopSimulation() {
        this.accelerometerPlayActivity.mSensorManager.unregisterListener(this);
    }

    public SimulationView(AccelerometerPlayActivity accelerometerPlayActivity, Context context) {
        super(context);
		this.accelerometerPlayActivity = accelerometerPlayActivity;
        mAccelerometer = this.accelerometerPlayActivity.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        DisplayMetrics metrics = new DisplayMetrics();
        this.accelerometerPlayActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mXDpi = metrics.xdpi;
        mYDpi = metrics.ydpi;
        mMetersToPixelsX = mXDpi / 0.0254f;
        mMetersToPixelsY = mYDpi / 0.0254f;

        //TODO 3.28.2011 This belongs in Particle's constructor
        // rescale the ball so it's about 0.5 cm on screen
        Bitmap ball = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
        final int dstWidth = (int) (Particle.sBallDiameter * mMetersToPixelsX + 0.5f);
        final int dstHeight = (int) (Particle.sBallDiameter * mMetersToPixelsY + 0.5f);
        mBitmap = Bitmap.createScaledBitmap(ball, dstWidth, dstHeight, true);

        Options opts = new Options();
        opts.inDither = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        mWood = BitmapFactory.decodeResource(getResources(), R.drawable.wood, opts);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // compute the origin of the screen relative to the origin of
        // the bitmap
        mXOrigin = (w - mBitmap.getWidth()) * 0.5f;
        mYOrigin = (h - mBitmap.getHeight()) * 0.5f;

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

        /*
         * draw the background
         */

        canvas.drawBitmap(mWood, 0, 0, null);

        /*
         * compute the new position of our object, based on accelerometer
         * data and present time.
         */

        final ParticleSystem particleSystem = mParticleSystem;
        final long now = mSensorTimeStamp + (System.nanoTime() - mCpuTimeStamp);
        final float sx = mSensorX;
        final float sy = mSensorY;

        particleSystem.update(sx, sy, now);

        final float xc = mXOrigin;
        final float yc = mYOrigin;
        final float xs = mMetersToPixelsX;
        final float ys = mMetersToPixelsY;
        final Bitmap bitmap = mBitmap;
        final int count = particleSystem.getParticleCount();
        for (int i = 0; i < count; i++) {
            /*
             * We transform the canvas so that the coordinate system matches
             * the sensors coordinate system with the origin in the center
             * of the screen and the unit is the meter.
             */

            final float x = xc + particleSystem.getPosX(i) * xs;
            final float y = yc - particleSystem.getPosY(i) * ys;
            canvas.drawBitmap(bitmap, x, y, null);
        }

        // and make sure to redraw asap
        invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}