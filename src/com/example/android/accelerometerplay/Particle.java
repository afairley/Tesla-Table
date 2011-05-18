package com.example.android.accelerometerplay;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.MotionEvent;

class Particle {
    private final ParticleSystem mParticleSystem;
	float mPosX;
    float mPosY;
    private float mAccelX;
    private float mAccelY;
    private float mLastPosX;
    private float mLastPosY;
    private float mOneMinusFriction;
    Bitmap mBitmap;
    
    // diameter of the balls in meters
    static final float sBallDiameter = 0.004f;
    static final float sBallDiameter2 = sBallDiameter * sBallDiameter;
    // friction of the virtual table and air
    static final float sFriction = 0.1f;
    
    static PhysicsEngineConvertor sConvertor = null;
    
	private float mMass;
	private int mTouchedBy;
	private float scaleFactor;
	private float radius;
	public float getRadius(){
		return radius;
	}
	
    Particle(ParticleSystem particleSystem, Bitmap ball, PhysicsEngineConvertor convertor) {
        mParticleSystem = particleSystem;
        if(sConvertor == null) sConvertor = convertor;
        initializeConstants(ball);
    }

    private void initializeConstants(Bitmap ball) {
		// make each particle a bit different by randomizing its
        // coefficient of friction and it's mass
        final float r1 = ((float) Math.random() - 0.5f) * 0.2f;
        final float r2 = (float) (Math.random() + 0.5f);
        mOneMinusFriction = 1.0f - sFriction + r1;
        mMass = 500.0f + 500 * r2;
        scaleFactor = mMass/1000.0f;
        radius = (Particle.sBallDiameter * scaleFactor)/2;
        final int dstWidth = (int) Math.ceil(
        				sConvertor.convertToScreenX(Particle.sBallDiameter * scaleFactor) );
        final int dstHeight =(int) Math.ceil(
        		 sConvertor.convertToScreenY(Particle.sBallDiameter * scaleFactor) );
        mBitmap = Bitmap.createScaledBitmap(ball, dstWidth, dstHeight, true);
        colorize_bitmap_based_on_friction(r1);		
	}

	private void colorize_bitmap_based_on_friction(float r1) {
    	for ( int h = 0; h < mBitmap.getHeight(); h++) {
        	for ( int w = 0; w < mBitmap.getWidth(); w++) {
        		final int color_orig = mBitmap.getPixel(w, h);
        		final int color_shade = Color.argb(Color.alpha(color_orig),
        				                           Color.red(color_orig),
        				                           Color.green(color_orig),
        				                           (int) Math.min(
        				                        		   Color.blue(color_orig) +
        				                        		    Math.floor(255 *((r1/0.2f) + 0.5f)),
        				                        		   255
        				            		       )
        				                           );
        		mBitmap.setPixel(w, h, color_shade);
        		
        	}
        }		
	}

	public void computePhysics(float sx, float sy, float dT, float dTC) {
        if ( mTouchedBy == -1){
         final float gx = -sx * mMass;
         final float gy = -sy * mMass;
         /*
          * �F = mA <=> A = �F / m We could simplify the code by
          * completely eliminating "m" (the mass) from all the equations,
          * but it would hide the concepts from this sample code.
          */
         final float invm = 1.0f / mMass;
         final float ax = gx * invm;
         final float ay = gy * invm;
         /*
          * Time-corrected Verlet integration The position Verlet
          * integrator is defined as x(t+�t) = x(t) + x(t) - x(t-�t) +
          * a(t)�t�2 However, the above equation doesn't handle variable
          * �t very well, a time-corrected version is needed: x(t+�t) =
          * x(t) + (x(t) - x(t-�t)) * (�t/�t_prev) + a(t)�t�2 We also add
          * a simple friction term (f) to the equation: x(t+�t) = x(t) +
          * (1-f) * (x(t) - x(t-�t)) * (�t/�t_prev) + a(t)�t�2
          */
         final float dTdT = dT * dT;
         final float x = mPosX + mOneMinusFriction * dTC * (mPosX - mLastPosX) + mAccelX
                * dTdT;
         final float y = mPosY + mOneMinusFriction * dTC * (mPosY - mLastPosY) + mAccelY
                * dTdT;
         mLastPosX = mPosX;
         mLastPosY = mPosY;
         mPosX = x;
         mPosY = y;
         mAccelX = ax;
         mAccelY = ay;
        }
    }

    /*
     * Resolving constraints and collisions with the Verlet integrator
     * can be very simple, we simply need to move a colliding or
     * constrained particle in such way that the constraint is
     * satisfied.
     */
    public void resolveCollisionWithBounds() {
        final float xmax = mParticleSystem.mHorizontalBound;
        final float ymax = mParticleSystem.mVerticalBound;
        final float x = mPosX;
        final float y = mPosY;
        if (x > xmax) {
            mPosX = xmax;
        } else if (x < -xmax) {
            mPosX = -xmax;
        }
        if (y > ymax) {
            mPosY = ymax;
        } else if (y < -ymax) {
            mPosY = -ymax;
        }
    }

	public boolean intersects(float screen_x, float screen_y) {
        final Bitmap bitmap = mBitmap;
        final float xc = (mParticleSystem.mSimulationView.mWidth - bitmap.getWidth()) * 0.5f;
        final float yc = (mParticleSystem.mSimulationView.mHeight - bitmap.getHeight()) * 0.5f;
        final float x = xc + sConvertor.convertToScreenX(mPosX) ;
        final float y = yc - sConvertor.convertToScreenY(mPosY) ;
        if( screen_x >= x && screen_x <= x + bitmap.getWidth() ){
        	if( screen_y >= y && screen_y <= y + bitmap.getHeight() ){
        		return true;
        	}
        }
        return false;
	}

	public boolean touchedBy(int pointerId) {
		return mTouchedBy == pointerId;
	}

	public void handleActionDownPointer(int pointerId) {
		mTouchedBy = pointerId;
	}
	public void handleActionUp() {
		mTouchedBy = -1;
	}
	public void handleActionMove(MotionEvent event){
		final int idx = event.findPointerIndex(mTouchedBy);
		//for now, let's just see how making the thing track to your finger works,
		//then we'll do some weird stuff with integrating forces once the kinks are worked out
		//prob also need to do the coord transform baloney homie is doing out at the wrong level
		mPosX = sConvertor.convertToInertialFrameX( event.getX(mTouchedBy) );
		mPosY = sConvertor.convertToInertialFrameY( event.getY(mTouchedBy) );
	}
}