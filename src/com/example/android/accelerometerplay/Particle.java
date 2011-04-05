package com.example.android.accelerometerplay;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

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
	private float mMass;

    Particle(ParticleSystem particleSystem, Bitmap ball) {
        mParticleSystem = particleSystem;
		// make each particle a bit different by randomizing its
        // coefficient of friction
        final float r1 = ((float) Math.random() - 0.5f) * 0.2f;
        final float r2 = (float) (Math.random() + 0.5f);
        mOneMinusFriction = 1.0f - SimulationView.sFriction + r1;
        mMass = 500.0f + 500 * r2;
        final float scaleFactor = mMass/1000.0f;
        final int dstWidth = (int) ( (Particle.sBallDiameter * mParticleSystem.mMetersToPixelsX + 0.5f)
        							 * scaleFactor)	;
        final int dstHeight = (int)(  (Particle.sBallDiameter * mParticleSystem.mMetersToPixelsY + 0.5f)
        		                      * scaleFactor)	;
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
        				                           (int) Math.max(
        				            Color.blue(color_orig) + Math.floor(255 *((r1/0.2f) + 0.5f)),
        				            		       255
        				            		       )
        				                           );
        		
        	}
        }		
	}

	public void computePhysics(float sx, float sy, float dT, float dTC) {
        
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
}