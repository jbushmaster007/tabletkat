/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.exalm.tabletkat.recent;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.AnimatorSet.Builder;
import android.animation.ObjectAnimator;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;

import org.exalm.tabletkat.TkR;

class Choreographer implements Animator.AnimatorListener {
    // should group this into a multi-property animation
    private static final int OPEN_DURATION = 136;
    private static final int CLOSE_DURATION = 230;
    private static final int SCRIM_DURATION = 400;
    private static final String TAG = "Choreographer";
    private static final boolean DEBUG = false;

    boolean mIsTablet;
    boolean mVisible;
    int mPanelHeight;
    ViewGroup mRootView;
    View mScrimView;
    View mContentView;
    View mNoRecentAppsView;
    AnimatorSet mContentAnim;
    Animator.AnimatorListener mListener;

    // the panel will start to appear this many px from the end
    final int HYPERSPACE_OFFRAMP = 200;
    public boolean mHideRecentsAfterThumbnailScaleUpStarted;
    public boolean mThumbnailScaleUpStarted;

    public Choreographer(ViewGroup root, View scrim, View content,
            View noRecentApps, Animator.AnimatorListener listener, boolean tablet) {
        mRootView = root;
        mScrimView = scrim;
        mContentView = content;
        mListener = listener;
        mNoRecentAppsView = noRecentApps;
        mIsTablet = tablet;
    }

    void createAnimation(boolean appearing) {
        float start, end;

        // 0: on-screen
        // height: off-screen
        float y = mContentView.getTranslationY();
        if (appearing) {
            // we want to go from near-the-top to the top, unless we're half-open in the right
            // general vicinity
            start = (y < HYPERSPACE_OFFRAMP) ? y : HYPERSPACE_OFFRAMP;
            end = 0;
        } else {
            start = y;
            end = y;
        }

        Animator posAnim = ObjectAnimator.ofFloat(mContentView, "translationY",
                start, end);
        posAnim.setInterpolator(appearing
                ? new android.view.animation.DecelerateInterpolator(2.5f)
                : new android.view.animation.AccelerateInterpolator(2.5f));
        posAnim.setDuration(appearing ? OPEN_DURATION : CLOSE_DURATION);

        Animator fadeAnim = ObjectAnimator.ofFloat(mContentView, "alpha",
                mContentView.getAlpha(), appearing ? 1.0f : 0.0f);
        fadeAnim.setInterpolator(appearing
                ? new android.view.animation.AccelerateInterpolator(1.0f)
                : new android.view.animation.AccelerateInterpolator(2.5f));
        fadeAnim.setDuration(appearing ? OPEN_DURATION : CLOSE_DURATION);

        Animator noRecentAppsFadeAnim = null;
        if (mNoRecentAppsView != null &&  // doesn't exist on large devices
                mNoRecentAppsView.getVisibility() == View.VISIBLE) {
            noRecentAppsFadeAnim = ObjectAnimator.ofFloat(mNoRecentAppsView, "alpha",
                    mContentView.getAlpha(), appearing ? 1.0f : 0.0f);
            noRecentAppsFadeAnim.setInterpolator(appearing
                    ? new android.view.animation.AccelerateInterpolator(1.0f)
                    : new android.view.animation.DecelerateInterpolator(1.0f));
            noRecentAppsFadeAnim.setDuration(appearing ? OPEN_DURATION : CLOSE_DURATION);
        }

        mContentAnim = new AnimatorSet();
        final Builder builder = mContentAnim.play(fadeAnim).with(posAnim);

        if (noRecentAppsFadeAnim != null) {
            builder.with(noRecentAppsFadeAnim);
        }

        if (appearing) {
            Drawable background = mScrimView.getBackground();
            if (background != null) {
                Animator bgAnim = ObjectAnimator.ofInt(background,
                    "alpha", appearing ? 0 : 255, appearing ? 255 : 0);
                bgAnim.setDuration(appearing ? SCRIM_DURATION : CLOSE_DURATION);
                builder.with(bgAnim);
            }
        } else {
            if (!mIsTablet) {
                View recentsTransitionBackground =
                        mRootView.findViewById(TkR.id.recents_transition_background);
                recentsTransitionBackground.setVisibility(View.VISIBLE);
                Drawable bgDrawable = new ColorDrawable(0xFF000000);
                recentsTransitionBackground.setBackground(bgDrawable);
                Animator bgAnim = ObjectAnimator.ofInt(bgDrawable, "alpha", 0, 255);
                bgAnim.setDuration(CLOSE_DURATION);
                bgAnim.setInterpolator(new android.view.animation.AccelerateInterpolator(1f));
                builder.with(bgAnim);
            }
        }
        mContentAnim.addListener(this);
        if (mListener != null) {
            mContentAnim.addListener(mListener);
        }
    }

    void startAnimation(boolean appearing) {
        if (DEBUG) Slog.d(TAG, "startAnimation(appearing=" + appearing + ")");

        createAnimation(appearing);

        // isHardwareAccelerated() checks if we're attached to a window and if that
        // window is HW accelerated-- we were sometimes not attached to a window
        // and buildLayer was throwing an IllegalStateException
        if (mContentView.isHardwareAccelerated()) {
            mContentView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mContentView.buildLayer();
        }
        mContentAnim.start();

        mVisible = appearing;
    }

    void jumpTo(boolean appearing) {
        mContentView.setTranslationY(appearing ? 0 : mPanelHeight);
        if (mScrimView.getBackground() != null) {
            mScrimView.getBackground().setAlpha(appearing ? 255 : 0);
        }
        View recentsTransitionBackground =
                mRootView.findViewById(TkR.id.recents_transition_background);
        recentsTransitionBackground.setVisibility(View.INVISIBLE);
        mRootView.requestLayout();
    }

    public void setPanelHeight(int h) {
        if (DEBUG) Slog.d(TAG, "panelHeight=" + h);
        mPanelHeight = h;
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        if (DEBUG) Slog.d(TAG, "onAnimationCancel");
        // force this to zero so we close the window
        mVisible = false;
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (DEBUG) Slog.d(TAG, "onAnimationEnd");
        if (!mVisible) {
            hideWindow();
        }
        mContentView.setLayerType(View.LAYER_TYPE_NONE, null);
        mContentView.setAlpha(1f);
        mContentAnim = null;
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }

    protected void hideWindow(){
        if (!mThumbnailScaleUpStarted) {
            mHideRecentsAfterThumbnailScaleUpStarted = true;
        } else {
            mRootView.setVisibility(View.GONE);
            View mTransitionBg =
                    mRootView.findViewById(TkR.id.recents_transition_background);
            View mPlaceholderThumbnail =
                    mRootView.findViewById(TkR.id.recents_transition_placeholder_icon);
            mTransitionBg.setVisibility(View.INVISIBLE);
            mPlaceholderThumbnail.setVisibility(View.INVISIBLE);
            mHideRecentsAfterThumbnailScaleUpStarted = false;
        }
    }
}
