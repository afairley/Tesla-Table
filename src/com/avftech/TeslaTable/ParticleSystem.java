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

import com.avftech.TeslaTable.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

/**
 *  A mathematical model of a system of particles.  
 */
class ParticleSystem {

	final SimulationView mSimulationView;

	static final int NUM_PARTICLES = 15;
    private Particle mBalls[] = new Particle[NUM_PARTICLES];
	float mHorizontalBound;
	float mVerticalBound;
	private PhysicsEngineConvertor mConvertor;

    ParticleSystem(SimulationView simulationView, 
    		       TeslaTableActivity accelerometerPlayActivity,
    		       PhysicsEngineConvertor convertor) {
        mSimulationView = simulationView;
        mConvertor = convertor;
		/*
         * Initially our particles have no speed or acceleration
         */

        Bitmap ball = BitmapFactory.decodeResource( accelerometerPlayActivity.getResources(),
        											R.drawable.ball);
        for (int i = 0; i < getParticles().length; i++) {
            getParticles()[i] = new Particle(this, ball, mConvertor);
        }
    }

    /*
     * Update the position of each particle in the system using the
     * Verlet integrator.
     */
    private void updatePositions(float sx, float sy, float mx, float my, long timestamp) {
        final long t = timestamp;
        if (this.mSimulationView.mLastT != 0) {
            final float dT = (float) (t - this.mSimulationView.mLastT) * (1.0f / 1000000000.0f);
            if (this.mSimulationView.mLastDeltaT != 0) {
                final float dTC = dT / this.mSimulationView.mLastDeltaT;
                final int count = getParticles().length;
                for (int i = 0; i < count; i++) {
                    Particle ball = getParticles()[i];
                    ball.computePhysics(sx, sy, mx, my, dT, dTC);
                }
            }
            this.mSimulationView.mLastDeltaT = dT;
        }
        this.mSimulationView.mLastT = t;
    }

    /*
     * Performs one iteration of the simulation. First updating the
     * position of all the particles, then resolving the constraints and
     * collisions.
     */
    public void update(float sx, float sy, float mx, float my,long now) {
        // update the system's positions
        updatePositions(sx, sy, mx, my, now);
        resolveCollisions();   
    }

    private void resolveCollisions() {
    	// We do no more than a limited number of iterations
        final int NUM_MAX_ITERATIONS = 10;

        /*
         * Resolve collisions, each particle is tested against every
         * other particle for collision. If a collision is detected the
         * particle is moved away using a virtual spring of infinite
         * stiffness.
         */
        boolean more = true;
        final int count = getParticles().length;
        for (int k = 0; k < NUM_MAX_ITERATIONS && more; k++) {
            more = false;
            for (int i = 0; i < count; i++) {
                Particle curr = getParticles()[i];
                for (int j = i + 1; j < count; j++) {
                    Particle ball = getParticles()[j];
                    float dx = ball.mPosX - curr.mPosX;
                    float dy = ball.mPosY - curr.mPosY;
                    float dd = dx * dx + dy * dy;
                    // Check for collisions
                    if (dd <= Particle.sBallDiameter2) {
                        /*
                         * add a little bit of entropy, after all nothing is
                         * perfect in the universe.
                         */
                        dx += ((float) Math.random() - 0.5f) * 0.00001f;
                        dy += ((float) Math.random() - 0.5f) * 0.00001f;
                        dd = dx * dx + dy * dy;
                        // simulate the spring
                        final float d = (float) Math.sqrt(dd);
                        final float c = (0.5f * (Particle.sBallDiameter - d)) / d;
                        curr.mPosX -= dx * c;
                        curr.mPosY -= dy * c;
                        ball.mPosX += dx * c;
                        ball.mPosY += dy * c;
                        more = true;
                    }
                }
                /*
                 * Finally make sure the particle doesn't intersect
                 * with the walls.
                 */
                curr.resolveCollisionWithBounds();
            }
        }
	}
    
    
	public int getParticleCount() {
        return getParticles().length;
    }

    public float getPosX(int i) {
        return getParticles()[i].mPosX;
    }

    public float getPosY(int i) {
        return getParticles()[i].mPosY;
    }

	public void onSizeChanged(int w, int h) {
        //Calculate the new walls of the Particle System
        float horizontalBound = mConvertor.convertToInertialFrameX(w) * 0.5f;
        float verticalBound = mConvertor.convertToInertialFrameY(h)  * 0.5f;
        updateBounds(horizontalBound,verticalBound);		
	}
	
	private void updateBounds(float horizontalBound, float verticalBound) {
		mHorizontalBound = horizontalBound;
		mVerticalBound = verticalBound;	
	}

	public Bitmap getBitmap(int i) {
		return getParticles()[i].mBitmap;
	}

	public Particle[] getParticles() {
		return mBalls;
	}
}