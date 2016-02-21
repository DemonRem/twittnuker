/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.graphic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.util.Property;
import android.view.Gravity;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import de.vanita5.twittnuker.graphic.iface.DoNotWrapDrawable;

import java.lang.ref.WeakReference;

public class LikeAnimationDrawable extends LayerDrawable implements Animatable, DoNotWrapDrawable {

    private static final Property<IconLayer, Float> ICON_SCALE = new Property<IconLayer, Float>(Float.class, "icon_scale") {
        @Override
        public void set(IconLayer object, Float value) {
            object.setScale(value);
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public Float get(IconLayer object) {
            return object.getScale();
        }
    };
    private static final Property<Layer, Float> LAYER_PROGRESS = new Property<Layer, Float>(Float.class, "layer_progress") {
        @Override
        public void set(Layer object, Float value) {
            object.setProgress(value);
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public Float get(Layer object) {
            return object.getProgress();
        }
    };
    private final int mDefaultColor, mLikeColor;
    @Style
    private final int mStyle;
    private long mDuration = 500;

    private AnimatorSet mCurrentAnimator;
    private WeakReference<OnLikedListener> mListenerRef;

    public LikeAnimationDrawable(final Context context, final int likeIcon, final int defaultColor,
                                 final int likeColor, @Style final int style) {
        super(createLayers(context, likeIcon, defaultColor, style));
        mDefaultColor = defaultColor;
        mLikeColor = likeColor;
        mStyle = style;
    }

    @Override
    public void start() {
        if (mCurrentAnimator != null) return;

        final AnimatorSet animatorSet = new AnimatorSet();

        final AbsLayer particleLayer = getParticleShineLayer();
        final AbsLayer circleLayer = getCircleLayer();
        final IconLayer iconLayer = getIconLayer();

        switch (mStyle) {
            case Style.LIKE: {
                setupLikeAnimation(animatorSet, particleLayer, circleLayer, iconLayer);
                break;
            }
            case Style.FAVORITE: {
                setupFavoriteAnimation(animatorSet, particleLayer, circleLayer, iconLayer);
                break;
            }
        }


        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                resetState();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
                if (mListenerRef == null) return;
                final OnLikedListener listener = mListenerRef.get();
                if (listener == null) return;
                if (!listener.onLiked()) {
                    resetState();
                }
            }

            private void resetState() {
                iconLayer.setColorFilter(mDefaultColor, PorterDuff.Mode.SRC_ATOP);
                particleLayer.setProgress(-1);
            }
        });
        animatorSet.start();
        mCurrentAnimator = animatorSet;
    }


    private void setupFavoriteAnimation(final AnimatorSet animatorSet, final Layer particleLayer,
                                        final Layer circleLayer, final IconLayer iconLayer) {
        setupLikeAnimation(animatorSet, particleLayer, circleLayer, iconLayer);
    }

    private void setupLikeAnimation(final AnimatorSet animatorSet, final Layer particleLayer,
                                    final Layer circleLayer, final IconLayer iconLayer) {
        final long scaleDownDuration = Math.round(1f / 24f * mDuration);
        final long ovalExpandDuration = Math.round(4f / 24f * mDuration);
        final long iconExpandOffset = Math.round(6f / 24f * mDuration);
        final long iconExpandDuration = Math.round(8f / 24f * mDuration);
        final long iconNormalDuration = Math.round(4f / 24f * mDuration);
        final long particleExpandDuration = Math.round(12f / 24f * mDuration);
        final long circleExplodeDuration = Math.round(5f / 24f * mDuration);

        final ObjectAnimator iconScaleDown = ObjectAnimator.ofFloat(iconLayer, ICON_SCALE, 1, 0);
        iconScaleDown.setDuration(scaleDownDuration);
        iconScaleDown.setInterpolator(new AccelerateInterpolator(2));
        iconScaleDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                iconLayer.setColorFilter(mDefaultColor, PorterDuff.Mode.SRC_ATOP);
            }

        });

        final ObjectAnimator ovalExpand = ObjectAnimator.ofFloat(circleLayer, LAYER_PROGRESS, 0, 0.5f);
        ovalExpand.setDuration(ovalExpandDuration);


        final ObjectAnimator iconExpand = ObjectAnimator.ofFloat(iconLayer, ICON_SCALE, 0, 1.25f);
        iconExpand.setDuration(iconExpandDuration);
        iconExpand.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                iconLayer.setColorFilter(mLikeColor, PorterDuff.Mode.SRC_ATOP);
            }

        });

        final ObjectAnimator particleExplode = ObjectAnimator.ofFloat(particleLayer, LAYER_PROGRESS, 0, 0.5f);
        particleExplode.setDuration(iconExpandDuration);

        final ObjectAnimator iconNormal = ObjectAnimator.ofFloat(iconLayer, ICON_SCALE, 1.25f, 1);
        iconNormal.setDuration(iconNormalDuration);
        final ObjectAnimator circleExplode = ObjectAnimator.ofFloat(circleLayer, LAYER_PROGRESS, 0.5f, 0.95f, 0.95f, 1);
        circleExplode.setDuration(circleExplodeDuration);
        circleExplode.setInterpolator(new DecelerateInterpolator());


        final ObjectAnimator particleFade = ObjectAnimator.ofFloat(particleLayer, LAYER_PROGRESS, 0.5f, 1);
        particleFade.setDuration(particleExpandDuration);


        animatorSet.play(iconScaleDown);
        animatorSet.play(ovalExpand).after(iconScaleDown);
        animatorSet.play(iconExpand).after(iconExpandOffset);
        animatorSet.play(particleExplode).after(iconExpandOffset);
        animatorSet.play(circleExplode).after(iconExpandOffset);

        animatorSet.play(iconNormal).after(iconExpand);
        animatorSet.play(particleFade).after(iconExpand);
    }

    private IconLayer getIconLayer() {
        return (IconLayer) getDrawable(2);
    }

    private AbsLayer getCircleLayer() {
        return (AbsLayer) getDrawable(0);
    }

    private AbsLayer getParticleShineLayer() {
        return (AbsLayer) getDrawable(1);
    }

    @Override
    public void stop() {
        if (mCurrentAnimator == null) return;
        mCurrentAnimator.cancel();
    }


    @Override
    public boolean isRunning() {
        return mCurrentAnimator != null && mCurrentAnimator.isRunning();
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    public void setOnLikedListener(OnLikedListener listener) {
        mListenerRef = new WeakReference<>(listener);
    }

    @Override
    public int getIntrinsicWidth() {
        return getIconLayer().getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return getIconLayer().getIntrinsicHeight();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        getIconLayer().setColorFilter(colorFilter);
    }

    private static Drawable[] createLayers(final Context context, final int likeIcon, int defaultColor, int style) {
        final IconLayer iconDrawable = new IconLayer(ContextCompat.getDrawable(context, likeIcon));
        iconDrawable.setColorFilter(defaultColor, PorterDuff.Mode.SRC_ATOP);
        final AbsLayer particleLayer;
        final Palette palette;
        switch (style) {
            case Style.FAVORITE: {
                palette = new FavoritePalette();
                particleLayer = new ShineLayer(iconDrawable.getIntrinsicWidth(),
                        iconDrawable.getIntrinsicHeight(), palette);
                break;
            }
            case Style.LIKE: {
                palette = new LikePalette();
                particleLayer = new ParticleLayer(iconDrawable.getIntrinsicWidth(),
                        iconDrawable.getIntrinsicHeight(), palette);
                break;
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
        particleLayer.setProgress(-1);
        final Drawable circleLayer = new CircleLayer(iconDrawable.getIntrinsicWidth(), iconDrawable.getIntrinsicHeight(), palette);
        return new Drawable[]{circleLayer, particleLayer, iconDrawable};
    }

    private interface Layer {

        float getProgress();

        void setProgress(float progress);
    }

    public interface OnLikedListener {
        boolean onLiked();
    }

    public interface Palette {
        int getParticleColor(int count, int index, float progress);

        int getCircleColor(float progress);
    }

    @IntDef({Style.LIKE, Style.FAVORITE})
    public @interface Style {
        int LIKE = 1;
        int FAVORITE = 2;
    }

    private static class ShineLayer extends AbsLayer {

        private static final int PARTICLES_PIVOTS_COUNT = 5;

        private final Paint mPaint;
        private int mFullRadius;
        private float mLineWidth;

        public ShineLayer(final int intrinsicWidth, final int intrinsicHeight, final Palette palette) {
            super(intrinsicWidth, intrinsicHeight, palette);
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            setProgress(-1);
        }

        @Override
        protected ConstantState createConstantState(final int intrinsicWidth,
                                                    final int intrinsicHeight,
                                                    final Palette palette) {
            return new AbsLayerState() {
                @Override
                public Drawable newDrawable() {
                    return new ShineLayer(intrinsicWidth, intrinsicHeight, palette);
                }

                @Override
                public int getChangingConfigurations() {
                    return ShineLayer.this.getChangingConfigurations();
                }
            };
        }

        @Override
        public void draw(Canvas canvas) {
            final float progress = getProgress();
            if (progress < 0) return;
            final int particleColor = palette.getParticleColor(0, 0, progress);
            final Rect bounds = getBounds();
            mPaint.setColor(particleColor);
            mPaint.setStrokeWidth(mLineWidth);
            final float[] startEnd = new float[2];
            mPaint.setAlpha(0xFF);
            if (progress < 0.25f) {
                calcPhase1(startEnd, progress);
            } else if (progress < 0.5f) {
                calcPhase2(startEnd, progress);
            } else if (progress < 0.75f) {
                calcPhase3(startEnd, progress);
            } else {
                calcPhase4(startEnd, progress);
                mPaint.setAlpha(Math.round(0xFF * (1 - (progress - 0.75f) * 4)));
            }

            for (int i = 0; i < PARTICLES_PIVOTS_COUNT; i++) {
                final double degree = 360.0 / PARTICLES_PIVOTS_COUNT * i;
                final double mainParticleAngle = Math.toRadians(degree + 18);
                final float startX = (float) (bounds.centerX() + startEnd[0] * Math.cos(mainParticleAngle));
                final float startY = (float) (bounds.centerY() + startEnd[0] * Math.sin(mainParticleAngle));
                final float stopX = (float) (bounds.centerX() + startEnd[1] * Math.cos(mainParticleAngle));
                final float stopY = (float) (bounds.centerY() + startEnd[1] * Math.sin(mainParticleAngle));
                if (startEnd[1] - startEnd[0] <= 0) {
                    canvas.drawPoint(startX, startY, mPaint);
                } else {
                    canvas.drawLine(startX, startY, stopX, stopY, mPaint);
                }
            }
        }

        private void calcPhase4(float[] startEnd, float progress) {
            calcPhase3(startEnd, 0.75f);
        }

        private void calcPhase3(float[] startEnd, float progress) {
            calcPhase2(startEnd, 0.5f);
            final float length = (startEnd[1] - startEnd[0]) * (1 - (progress - 0.5f) * 4);
            startEnd[0] = startEnd[1] - length;
        }

        private void calcPhase2(float[] startEnd, float progress) {
            calcPhase1(startEnd, 0.25f);
            final float length = startEnd[1] - startEnd[0];
            final float initialStart = startEnd[0];
            startEnd[0] = initialStart + mFullRadius / 3 * (progress - 0.25f) * 4;
            startEnd[1] = startEnd[0] + length;
        }

        private void calcPhase1(float[] startEnd, float progress) {
            // Start point: 1/4 of icon radius
            startEnd[0] = mFullRadius / 3;
            startEnd[1] = startEnd[0] + (mFullRadius / 4 * progress * 4);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            mFullRadius = Math.min(bounds.width(), bounds.height()) / 2;
            mLineWidth = mFullRadius / 10f;
        }

    }

    private static class ParticleLayer extends AbsLayer {

        private static final int PARTICLES_PIVOTS_COUNT = 7;
        private final Paint mPaint;
        private float mFullRadius;
        private float mParticleSize;

        public ParticleLayer(final int intrinsicWidth, final int intrinsicHeight,
                             final Palette palette) {
            super(intrinsicWidth, intrinsicHeight, palette);

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setStyle(Paint.Style.FILL);
            setProgress(-1);
        }

        @Override
        protected ConstantState createConstantState(final int intrinsicWidth,
                                                    final int intrinsicHeight,
                                                    final Palette palette) {
            return new AbsLayerState() {
                @Override
                public Drawable newDrawable() {
                    return new ParticleLayer(intrinsicWidth, intrinsicHeight, palette);
                }

                @Override
                public int getChangingConfigurations() {
                    return ParticleLayer.this.getChangingConfigurations();
                }
            };
        }

        @Override
        public void draw(final Canvas canvas) {
            final float progress = getProgress();
            if (progress < 0) return;
            final Rect bounds = getBounds();
            final float expandSpinProgress = Math.min(0.5f, progress);
            final float currentRadius = mFullRadius + (mFullRadius * expandSpinProgress);
            final float distance = mParticleSize + (mParticleSize * progress);
            final float mainStrokeWidth, subStrokeWidth;
            if (progress < 0.5) {
                // Scale factor: [1, 0.5)
                mainStrokeWidth = mParticleSize * (1 - progress);
                // Scale factor: [1, 1.25)
                subStrokeWidth = mParticleSize * (1 + progress / 2);
            } else {
                mainStrokeWidth = mParticleSize * (1 - progress);
                subStrokeWidth = mParticleSize * 1.25f * (1 - (progress - 0.5f) * 2);
            }

            for (int i = 0; i < PARTICLES_PIVOTS_COUNT; i++) {
                final double degree = 360.0 / PARTICLES_PIVOTS_COUNT * i;
                final int color = palette.getParticleColor(PARTICLES_PIVOTS_COUNT, i, progress);

                final double mainParticleAngle = Math.toRadians(degree - 115);
                final float mainParticleX = (float) (bounds.centerX() + currentRadius * Math.cos(mainParticleAngle));
                final float mainParticleY = (float) (bounds.centerY() + currentRadius * Math.sin(mainParticleAngle));

                mPaint.setColor(color);
                if (mainStrokeWidth > 0) {
                    canvas.drawCircle(mainParticleX, mainParticleY, mainStrokeWidth / 2, mPaint);
                }

                final double particleAngle = Math.toRadians(90.0 * -expandSpinProgress + degree + 15);
                final float subParticleX = (float) (mainParticleX + distance * Math.cos(particleAngle));
                final float subParticleY = (float) (mainParticleY + distance * Math.sin(particleAngle));
                mPaint.setAlpha(Math.round(255f * (1 - progress / 2f)));

                if (subStrokeWidth > 0) {
                    canvas.drawCircle(subParticleX, subParticleY, subStrokeWidth / 2, mPaint);
                }
            }

        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            mFullRadius = Math.min(bounds.width(), bounds.height()) / 2;
            mParticleSize = mFullRadius / 4f;
        }

    }

    private static class CircleLayer extends AbsLayer {
        private final Paint mPaint;

        private int mFullRadius;

        public CircleLayer(final int intrinsicWidth, final int intrinsicHeight,
                           final Palette palette) {
            super(intrinsicWidth, intrinsicHeight, palette);
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        @Override
        protected ConstantState createConstantState(final int intrinsicWidth,
                                                    final int intrinsicHeight,
                                                    final Palette palette) {
            return new AbsLayerState() {
                @Override
                public Drawable newDrawable() {
                    return new CircleLayer(intrinsicWidth, intrinsicHeight, palette);
                }

                @Override
                public int getChangingConfigurations() {
                    return CircleLayer.this.getChangingConfigurations();
                }
            };
        }

        @Override
        public void draw(final Canvas canvas) {
            final float progress = getProgress();
            final Rect bounds = getBounds();
            final float radius;
            if (progress < 0.5f) {
                mPaint.setStyle(Paint.Style.FILL);
                final float sizeProgress = Math.min(1, progress * 2);
                radius = sizeProgress * mFullRadius;
            } else {
                mPaint.setStyle(Paint.Style.STROKE);
                final float innerLeftRatio = 1 - (progress - 0.5f) * 2f;
                final float strokeWidth = mFullRadius * innerLeftRatio;
                mPaint.setStrokeWidth(strokeWidth);
                radius = mFullRadius - strokeWidth / 2;
                if (strokeWidth <= 0) return;
            }
            mPaint.setColor(palette.getCircleColor(progress));
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, mPaint);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            mFullRadius = Math.min(bounds.width(), bounds.height()) / 2;
        }

    }

    private static abstract class AbsLayer extends Drawable implements Layer {
        protected final int intrinsicWidth;
        protected final int intrinsicHeight;
        protected final Palette palette;
        private float mProgress;
        private ConstantState mState;

        public AbsLayer(final int intrinsicWidth, final int intrinsicHeight, final Palette palette) {
            this.intrinsicWidth = intrinsicWidth;
            this.intrinsicHeight = intrinsicHeight;
            this.palette = palette;
            mState = createConstantState(intrinsicWidth, intrinsicHeight, palette);
        }

        protected abstract ConstantState createConstantState(int intrinsicWidth, int intrinsicHeight, final Palette palette);

        @Override
        public void setAlpha(final int alpha) {

        }

        @Override
        public final float getProgress() {
            return mProgress;
        }

        @Override
        public final void setProgress(float progress) {
            mProgress = progress;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(final ColorFilter colorFilter) {

        }

        @Override
        public final int getIntrinsicHeight() {
            return intrinsicHeight;
        }

        @Override
        public final int getIntrinsicWidth() {
            return intrinsicWidth;
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public ConstantState getConstantState() {
            return mState;
        }

        static abstract class AbsLayerState extends ConstantState {

        }
    }

    private static class FavoritePalette implements Palette {

        private final ArgbEvaluator evaluator = new ArgbEvaluator();

        @Override
        public int getParticleColor(int count, int index, float progress) {
            return (Integer) evaluator.evaluate(progress, 0xFFFF7020, 0xFFFD9050);
        }

        @Override
        public int getCircleColor(float progress) {
            return (Integer) evaluator.evaluate(progress, 0xFFFF9C00, 0xFFFFB024);
        }
    }

    private static class LikePalette implements Palette {

        private final ArgbEvaluator evaluator = new ArgbEvaluator();
        private final float[] hsv = new float[3];

        @Override
        public int getParticleColor(int count, int index, float progress) {
            final double degree = 360.0 / count * index;
            hsv[0] = (float) degree;
            hsv[1] = 0.4f;
            hsv[2] = 1f;
            return Color.HSVToColor(hsv);
        }

        @Override
        public int getCircleColor(float progress) {
            return (Integer) evaluator.evaluate(progress, 0xFFDE4689, 0xFFCD8FF5);
        }
    }

    static class IconLayer extends Drawable implements Callback {
        private final Drawable mDrawable;
        private final Rect mTmpRect = new Rect();
        private float mScale;
        private boolean mMutated;
        private ConstantState mState;

        public IconLayer(Drawable drawable) {
            if (drawable == null) throw new NullPointerException();
            mState = new ScaleConstantState(drawable);
            mDrawable = drawable;
            drawable.setCallback(this);
            setScale(1);
        }

        /**
         * Returns the drawable scaled by this ScaleDrawable.
         */
        public Drawable getDrawable() {
            return mDrawable;
        }

        // overrides from Drawable.Callback
        @Override
        public void invalidateDrawable(Drawable who) {
            if (getCallback() != null) {
                getCallback().invalidateDrawable(this);
            }
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            if (getCallback() != null) {
                getCallback().scheduleDrawable(this, what, when);
            }
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            if (getCallback() != null) {
                getCallback().unscheduleDrawable(this, what);
            }
        }

        // overrides from Drawable
        @Override
        public void draw(Canvas canvas) {
            if (mScale <= 0) return;
            mDrawable.draw(canvas);
        }

        @Override
        public int getChangingConfigurations() {
            return super.getChangingConfigurations()
                    | mDrawable.getChangingConfigurations();
        }

        @Override
        public boolean getPadding(Rect padding) {
            // XXX need to adjust padding!
            return mDrawable.getPadding(padding);
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            mDrawable.setVisible(visible, restart);
            return super.setVisible(visible, restart);
        }

        @Override
        public void setAlpha(int alpha) {
            mDrawable.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            mDrawable.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return mDrawable.getOpacity();
        }

        @Override
        public boolean isStateful() {
            return mDrawable.isStateful();
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean changed = mDrawable.setState(state);
            onBoundsChange(getBounds());
            return changed;
        }

        @Override
        protected boolean onLevelChange(int level) {
            mDrawable.setLevel(level);
            onBoundsChange(getBounds());
            invalidateSelf();
            return true;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            updateBounds(bounds);
        }

        @Override
        public int getIntrinsicWidth() {
            return mDrawable.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return mDrawable.getIntrinsicHeight();
        }

        @Override
        public Drawable mutate() {
            if (!mMutated && super.mutate() == this) {
                mDrawable.mutate();
                mMutated = true;
            }
            return this;
        }

        public float getScale() {
            return mScale;
        }

        public void setScale(float scale) {
            mScale = scale;
            updateBounds(getBounds());
        }

        @Override
        public ConstantState getConstantState() {
            return mState;
        }


        static class ScaleConstantState extends ConstantState {

            private final Drawable mIcon;

            public ScaleConstantState(Drawable icon) {
                mIcon = icon;
            }

            @Override
            public Drawable newDrawable() {
                return new IconLayer(mIcon.mutate());
            }

            @Override
            public int getChangingConfigurations() {
                return mIcon.getChangingConfigurations();
            }
        }

        private void updateBounds(Rect bounds) {
            final Rect r = mTmpRect;
            final int w = Math.round(mDrawable.getIntrinsicWidth() * mScale);
            final int h = Math.round(mDrawable.getIntrinsicHeight() * mScale);
            Gravity.apply(Gravity.CENTER, w, h, bounds, r);

            if (w > 0 && h > 0) {
                mDrawable.setBounds(r.left, r.top, r.right, r.bottom);
            }
            invalidateSelf();
        }

    }
}