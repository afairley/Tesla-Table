/*
 * Portions Copyright (C) 2011 Alexander Vegas Fairley
 * Portions Copyright (C) 2010 The Android Open Source Project
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
	private float mScaleFactor;
	private float mRadius;
	private boolean mCharged;
	private float mCharge;
	
	public float getRadius(){
		return mRadius;
	}
	
    public Particle(ParticleSystem particleSystem, Bitmap ball, PhysicsEngineConvertor convertor) {
        mParticleSystem = particleSystem;
        if(sConvertor == null) sConvertor = convertor;
        initializeConstants(ball);
    }

    private void initializeConstants(Bitmap ball) {
		// make each particle a bit different by randomizing its
        // coefficient of friction and it's mass
        final float r1 = ((float) Math.random() - 0.5f) * 0.2f;
        final float r2 = (float) (Math.random() + 0.5f);
        final float r3 = ((float) Math.random() - 0.5f) * 0.2f;
        mCharged = (Math.random() > 0.5) ? true : false;
        mOneMinusFriction = 1.0f - sFriction + r1;
        mMass = 500.0f + 500 * r2;
        mCharge = r3;
        mScaleFactor = mMass/1000.0f;
        mRadius = (Particle.sBallDiameter * mScaleFactor)/2;
        final int dstWidth = (int) Math.ceil(
        				sConvertor.convertToScreenX(Particle.sBallDiameter * mScaleFactor) );
        final int dstHeight =(int) Math.ceil(
        		 sConvertor.convertToScreenY(Particle.sBallDiameter * mScaleFactor) );
        mBitmap = Bitmap.createScaledBitmap(ball, dstWidth, dstHeight, true);
        
		colorize_bitmap_based_on_details(r1, r3,mCharged);		
	}

    /**
     * Colorize the ball based on either its mass or its charge
     * @param r1
     * @param r3
     * @param mCharged
     */
	private void colorize_bitmap_based_on_details(float r1, float r3, boolean mCharged) {
    	for ( int h = 0; h < mBitmap.getHeight(); h++) {
        	for ( int w = 0; w < mBitmap.getWidth(); w++) {
        		final int color_orig = mBitmap.getPixel(w, h);
        		final int alpha = Color.alpha(color_orig);
        		final int red = Color.red(color_orig);
        		final int green = Color.green(color_orig);
        		final int blue = Color.blue(color_orig);
        		int newAlpha = alpha, newRed = red, newGreen = green, newBlue = blue;
        		if( ! mCharged ){
        		   newBlue = (int) Math.min(
        				                Color.blue(color_orig) +
        				                           Math.floor(255 *((r3/0.2f) + 0.5f)),
        				                        	255
        				            		       );
        		}	else {
        			/* We turn positively charged particles yellow, negative ones green */
        			if( r3 > 0){
        					newRed = (int) Math.max( Math.min(
	                        		   Color.red(color_orig) +
	                        		    Math.floor(255 *((r3/0.2f) + 0.5f)),
	                        		   255),
	                        		   0);
        			}
	                newGreen = (int) Math.max( Math.min(
	                        		   Color.green(color_orig) +
	                        		    Math.floor(255 *((Math.abs(r3)/0.2f) + 0.5f)),
	                        		   255),
	                        		   0);
	                     
	                           
        		}
        		final int colorShade = Color.argb(newAlpha, newRed, newGreen, newBlue);
        		mBitmap.setPixel(w, h, colorShade);
        		
        	}
        }		
	}

	public void computePhysics(float sx, float sy, float mx, float my, float dT, float dTC) {
        if ( mTouchedBy == -1){
         final float gx = -sx * mMass;
         final float gy = -sy * mMass;
         float cx = 0 , cy = 0;
         if (mCharged){
        	 cx = (mx * mMass) * mCharge;
        	 cy = my * mMass * mCharge;
         }
         /*
          * �F = mA <=> A = �F / m We could simplify the code by
          * completely eliminating "m" (the mass) from all the equations,
          * but it would hide the concepts from this sample code.
          */
         final float invm = 1.0f / mMass;
         float ax = gx * invm;
         float ay = gy * invm;
         if(mCharged){
        	 ax += cx * invm;
        	 ay += cy * invm;
         }
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
        final float xmax = mParticleSystem.mHorizontalBound - mRadius;
        final float ymax = mParticleSystem.mVerticalBound - mRadius;
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
		//for now, let's just see how making the particle track to your finger works,
		//then we'll do some weird stuff with integrating forces once the kinks are worked out
		
        final float xc = (mParticleSystem.mSimulationView.mWidth - mBitmap.getWidth()) * 0.5f;
        final float yc = (mParticleSystem.mSimulationView.mHeight - mBitmap.getHeight()) * 0.5f;
		mPosX = sConvertor.convertToInertialFrameX( event.getX(mTouchedBy) - xc  );
		mPosY = sConvertor.convertToInertialFrameY( yc - event.getY(mTouchedBy) );
	}
}