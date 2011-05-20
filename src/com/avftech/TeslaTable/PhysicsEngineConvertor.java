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

import android.util.DisplayMetrics;

/**
 * Encapsulate conversions from PhysicsEngine units
 * to Screen Units
 * @author afairley
 *
 */
public class PhysicsEngineConvertor {
	
	private float mMetersToPixelsX;
	private float mMetersToPixelsY;
    

	PhysicsEngineConvertor(DisplayMetrics metrics){
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
