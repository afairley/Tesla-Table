package com.avftech.TeslaTable;

import android.util.DisplayMetrics;

/**
 * Encapsulate conversions from PhysicsEngine units
 * to Screen Units
 * @author afairley
 *
 */
public class PhysicsEngineConvertor {
	
	private SystemDimensions mDimensions;
	private float mMetersToPixelsX;
	private float mMetersToPixelsY;
    

	PhysicsEngineConvertor(DisplayMetrics metrics, SystemDimensions dimensions){
		mDimensions = dimensions;
        mMetersToPixelsX = metrics.xdpi / 0.0254f;
        mMetersToPixelsY = metrics.ydpi / 0.0254f;
	}
	
	public float convertToInertialFrameX(float x){
		return x/mMetersToPixelsX;
	}
	public float convertToInertialFrameY(float y){
		return y/mMetersToPixelsY;
	}
	public float convertToScreenX(float x){
		return x*mMetersToPixelsX;
	}
	public float convertToScreenY(float y){
		return y*mMetersToPixelsY;
	}
}
