/*
 * Copyright (C) 2011, T-Mobile USA, Inc.
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

package com.tmobile.themechooser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Generic layout which creates a glass reflection look. Measuring has many
 * side-effects and works best with a single ImageView child but should be
 * flexible enough to display any other type of view.
 */
public class ReflectionLayout extends FrameLayout {
    private static final String TAG = "ReflectionLayout";

    private static final boolean DEBUG_DRAWING_TIME = false;

    /**
     * Desired reflection layout size (including the child). This should be
     * configurable at runtime somehow. It may be smaller than this depending on
     * layout constraints.
     */
    private static final float REFLECTION_SIZE = 1.20f;

    /* Drawing tools used to create the reflection pool effect. */
    private final Paint mDarkPaint = new Paint();
    private final Paint mReflectionPaint = new Paint();
    private final Matrix mMatrix = new Matrix();
    private final Shader mShader;

    public ReflectionLayout(Context context) {
        this(context, null);
    }

    public ReflectionLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReflectionLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setWillNotDraw(false);

        mDarkPaint.setColor(0x98000000);

        mShader = new LinearGradient(0, 0, 0, 1, 0x00000000, 0xFF000000, Shader.TileMode.CLAMP);
        mReflectionPaint.setShader(mShader);
        mReflectionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }

    @Override
    protected void onMeasure(int wspec, int hspec) {
        super.onMeasure(wspec, hspec);

        if (getChildCount() > 0) {
            View child = getChildAt(0);
            int childw = child.getMeasuredWidth();
            int childh = child.getMeasuredHeight();

            /* Enlarge the child's height by 33% for the reflection. */
            setMeasuredDimension(resolveSize(childw, wspec),
                    resolveSize((int)(childh * REFLECTION_SIZE), hspec));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now;
        if (DEBUG_DRAWING_TIME) {
            now = System.currentTimeMillis();
        }

        /* Magic magic magic... */
        if (getChildCount() > 0) {
            drawReflection(canvas);
        }

        if (DEBUG_DRAWING_TIME) {
            long elapsed = System.currentTimeMillis() - now;
            Log.d(TAG, "Drawing took " + elapsed + " ms");
        }
    }

    private void drawReflection(Canvas canvas) {
        View child = getChildAt(0);
        int childw = child.getWidth();
        int childh = child.getHeight();
        int selfh = getHeight();
        int poolh = selfh - childh;

        /*
         * Save a layer so that we can render off screen initially in order to
         * achieve the DST_OUT xfer mode.  This allows us to have a non-solid
         * background.
         */
        canvas.saveLayer(child.getLeft(), child.getBottom(), child.getRight(),
                child.getBottom() + getBottom(),
                null, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);

        /* Draw the flipped child. */
        canvas.save();
        canvas.scale(1, -1);
        canvas.translate(0, -(childh * 2));
        child.draw(canvas);
        canvas.restore();

        /* Saturate the flipped image with a dark color. */
        canvas.drawRect(0, childh, childw, selfh, mDarkPaint);

        /* Carve out the reflection area's alpha channel. */
        mMatrix.setScale(1, poolh);
        mMatrix.postTranslate(0, childh);
        mShader.setLocalMatrix(mMatrix);
        canvas.drawRect(0, childh, childw, selfh, mReflectionPaint);

        /* Apply the canvas layer. */
        canvas.restore();
    }
}
