package com.example.android.accelerometerplay;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

/** A mathematical model of a system of particles.  
 * 
 * @author johannes
 *
 */
class ParticleSystem {

	private final SimulationView mSimulationView;

	static final int NUM_PARTICLES = 15;
    private Particle mBalls[] = new Particle[NUM_PARTICLES];
	float mHorizontalBound;
	float mVerticalBound;
	float mMetersToPixelsX;
	float mMetersToPixelsY;

    ParticleSystem(SimulationView simulationView, 
    		AccelerometerPlayActivity accelerometerPlayActivity) {
        this.mSimulationView = simulationView;
        
        DisplayMetrics metrics = simulationView.getDisplayMetrics();
        final float XDPI = metrics.xdpi;
        final float YDPI = metrics.ydpi;
		/*
         * Initially our particles have no speed or acceleration
         */
        mMetersToPixelsX = XDPI / 0.0254f;
        mMetersToPixelsY = YDPI / 0.0254f;
        Bitmap ball = BitmapFactory.decodeResource( accelerometerPlayActivity.getResources(),
        											R.drawable.ball);
        for (int i = 0; i < mBalls.length; i++) {
            mBalls[i] = new Particle(this, ball);
        }
    }

    /*
     * Update the position of each particle in the system using the
     * Verlet integrator.
     */
    private void updatePositions(float sx, float sy, long timestamp) {
        final long t = timestamp;
        if (this.mSimulationView.mLastT != 0) {
            final float dT = (float) (t - this.mSimulationView.mLastT) * (1.0f / 1000000000.0f);
            if (this.mSimulationView.mLastDeltaT != 0) {
                final float dTC = dT / this.mSimulationView.mLastDeltaT;
                final int count = mBalls.length;
                for (int i = 0; i < count; i++) {
                    Particle ball = mBalls[i];
                    ball.computePhysics(sx, sy, dT, dTC);
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
    public void update(float sx, float sy, long now) {
        // update the system's positions
        updatePositions(sx, sy, now);
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
        final int count = mBalls.length;
        for (int k = 0; k < NUM_MAX_ITERATIONS && more; k++) {
            more = false;
            for (int i = 0; i < count; i++) {
                Particle curr = mBalls[i];
                for (int j = i + 1; j < count; j++) {
                    Particle ball = mBalls[j];
                    float dx = ball.mPosX - curr.mPosX;
                    float dy = ball.mPosY - curr.mPosY;
                    float dd = dx * dx + dy * dy;
                    // Check for collisions
                    if (dd <= Particle.sBallDiameter2) {
                        /*
                         * add a little bit of entropy, after all nothing is
                         * perfect in the universe.
                         */
                        dx += ((float) Math.random() - 0.5f) * 0.0001f;
                        dy += ((float) Math.random() - 0.5f) * 0.0001f;
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
        return mBalls.length;
    }

    public float getPosX(int i) {
        return mBalls[i].mPosX;
    }

    public float getPosY(int i) {
        return mBalls[i].mPosY;
    }

	public void onSizeChanged(int w, int h) {
        //Calculate the new walls of the Particle System
        float horizontalBound = ((w / mMetersToPixelsX - Particle.sBallDiameter) * 0.5f);
        float verticalBound = ((h / mMetersToPixelsY - Particle.sBallDiameter) * 0.5f);
        updateBounds(horizontalBound,verticalBound);
		
	}
	
	private void updateBounds(float horizontalBound, float verticalBound) {
		mHorizontalBound = horizontalBound;
		mVerticalBound = verticalBound;	
	}

	public Bitmap getBitmap(int i) {
		// TODO Auto-generated method stub
		return mBalls[i].mBitmap;
	}
}