package com.yuyan.imemodule.view.widget.seekbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.IntDef;
import com.yuyan.imemodule.utils.DevicesUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

public class SignSeekBar extends View {
    static final int NONE = -1;

    @IntDef({NONE, TextPosition.SIDES, TextPosition.BOTTOM_SIDES, TextPosition.BELOW_SECTION_MARK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextPosition {
        int SIDES = 0, BOTTOM_SIDES = 1, BELOW_SECTION_MARK = 2;
    }

    private float mMin; // min
    private float mMax; // max
    private float mProgress; // real time value
    private boolean isFloatType; // support for float type output
    private int mTrackSize; // height of right-track(on the right of thumb)
    private int mSecondTrackSize; // height of left-track(on the left of thumb)
    private int mThumbRadius; // radius of thumb
    private int mThumbRadiusOnDragging; // radius of thumb when be dragging
    private int mTrackColor; // color of right-track
    private int mSecondTrackColor; // color of left-track
    private int mThumbColor; // color of thumb
    private int mSectionCount; // shares of whole progress(max - min)
    private boolean isShowSectionMark; // show demarcation points or not
    private boolean isAutoAdjustSectionMark; // auto scroll to the nearest section_mark or not
    private boolean isShowSectionText; // show section-text or not
    private int mSectionTextSize; // text size of section-text
    private int mSectionTextColor; // text color of section-text
    @TextPosition
    private int mSectionTextPosition = NONE; // text position of section-text relative to track
    private int mSectionTextInterval; // the interval of two section-text
    private boolean isShowThumbText; // show real time progress-text under thumb or not
    private int mThumbTextSize; // text size of progress-text
    private int mThumbTextColor; // text color of progress-text
    private boolean isShowProgressInFloat; // show sign-progress in float or not
    private boolean isTouchToSeek; // touch anywhere on track to quickly seek
    private boolean isSeekBySection; // seek by section, the progress may not be linear
    private long mAnimDuration; // duration of animation

    private int mSignBorderSize; // border size
    private boolean isShowSignBorder; // show sign border
    private int mSignBorderColor;// color of border color
    private int mSignColor;// color of sign
    private int mSignTextSize; // text size of sign-progress
    private int mSignTextColor; // text color of sign-progress
    private int mSignHeight; //sign Height
    private int mSignWidth; //sign width

    private float mDelta; // max - min
    private float mSectionValue; // (mDelta / mSectionCount)
    private float mThumbCenterX; // X coordinate of thumb's center
    private float mTrackLength; // pixel length of whole track
    private float mSectionOffset; // pixel length of one section
    private boolean isThumbOnDragging; // is thumb on dragging or not
    private boolean triggerSeekBySection;

    private OnProgressChangedListener mProgressListener; // progress changing listener
    private float mLeft; // space between left of track and left of the view
    private float mRight; // space between right of track and left of the view
    private final Paint mPaint;
    private final Rect mRectText;

    private boolean isTouchToSeekAnimEnd = true;
    private float mPreSecValue; // previous SectionValue
    private SignConfigBuilder mConfigBuilder; // config attributes
    private String[] mSidesLabels;
    private boolean isSidesLabels;
    private float mThumbBgAlpha; //  alpha of thumb shadow
    private float mThumbRatio; // ratio of thumb shadow
    private boolean isShowThumbShadow;
    private boolean isShowSign;
    private boolean isSignArrowAutofloat;

    private final Rect valueSignBounds;
    private final RectF roundRectangleBounds;
    private int mSignArrowHeight;   //提示框箭头的高度
    private int mSignArrowWidth;   //提示框箭头的宽度
    private int mSignRound;      //提示框的圆角大小
    private final Point point1;
    private final Point point2;
    private final Point point3;
    private Paint signPaint;
    private Paint signborderPaint;
    private StaticLayout valueTextLayout;
    private final Path trianglePath;
    private final Path triangleboderPath;
    private String unit;
    private boolean mReverse;
    private TextPaint valueTextPaint;  //滑块数值文本
    private NumberFormat mFormat;

    public SignSeekBar(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setTextAlign(Paint.Align.CENTER);

        mRectText = new Rect();
        isSidesLabels = mSidesLabels != null && mSidesLabels.length > 0;

        //init sign
        roundRectangleBounds = new RectF();
        valueSignBounds = new Rect();

        point1 = new Point();
        point2 = new Point();
        point3 = new Point();

        trianglePath = new Path();
        trianglePath.setFillType(Path.FillType.EVEN_ODD);

        triangleboderPath = new Path();

        init();

        initConfigByPriority();
    }

    private void init() {
        signPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        signPaint.setStyle(Paint.Style.FILL);
        signPaint.setAntiAlias(true);
        signPaint.setColor(mSignColor);

        signborderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        signborderPaint.setStyle(Paint.Style.STROKE);
        signborderPaint.setStrokeWidth(mSignBorderSize);
        signborderPaint.setColor(mSignBorderColor);
        signborderPaint.setAntiAlias(true);

        valueTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        valueTextPaint.setStyle(Paint.Style.FILL);
        valueTextPaint.setTextSize(mSignTextSize);
        valueTextPaint.setColor(mSignTextColor);
    }

    private void initConfigByPriority() {
        if (mMin == mMax) {
            mMin = 0.0f;
            mMax = 100.0f;
        }
        if (mMin > mMax) {
            float tmp = mMax;
            mMax = mMin;
            mMin = tmp;
        }
        if (mProgress < mMin) {
            mProgress = mMin;
        }
        if (mProgress > mMax) {
            mProgress = mMax;
        }
        if (mSecondTrackSize < mTrackSize) {
            mSecondTrackSize = mTrackSize + DevicesUtils.dip2px(2);
        }
        if (mThumbRadius <= mSecondTrackSize) {
            mThumbRadius = mSecondTrackSize + DevicesUtils.dip2px(2);
        }
        if (mThumbRadiusOnDragging <= mSecondTrackSize) {
            mThumbRadiusOnDragging = mSecondTrackSize * 2;
        }
        if (mSectionCount <= 0) {
            mSectionCount = 10;
        }
        mDelta = mMax - mMin;
        mSectionValue = mDelta / mSectionCount;

        if (mSectionValue < 1) {
            isFloatType = true;
        }
        if (isFloatType) {
            isShowProgressInFloat = true;
        }
        if (mSectionTextPosition != NONE) {
            isShowSectionText = true;
        }
        if (isShowSectionText) {
            if (mSectionTextPosition == NONE) {
                mSectionTextPosition = TextPosition.SIDES;
            }
            if (mSectionTextPosition == TextPosition.BELOW_SECTION_MARK) {
                isShowSectionMark = true;
            }
        }
        if (mSectionTextInterval < 1) {
            mSectionTextInterval = 1;
        }
        if (isAutoAdjustSectionMark && !isShowSectionMark) {
            isAutoAdjustSectionMark = false;
        }
        if (isSeekBySection) {
            mPreSecValue = mMin;
            if (mProgress != mMin) {
                mPreSecValue = mSectionValue;
            }
            isShowSectionMark = true;
            isAutoAdjustSectionMark = true;
            isTouchToSeek = false;
        }

        setProgress(mProgress);

        mThumbTextSize = isFloatType || isSeekBySection || (isShowSectionText && mSectionTextPosition ==
                TextPosition.BELOW_SECTION_MARK) ? mSectionTextSize : mThumbTextSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = mThumbRadiusOnDragging * 2; // 默认高度为拖动时thumb圆的直径
        if (isShowThumbText) {
            mPaint.setTextSize(mThumbTextSize);
            mPaint.getTextBounds("j", 0, 1, mRectText); // “j”是字母和阿拉伯数字中最高的
            height += mRectText.height(); // 如果显示实时进度，则原来基础上加上进度文字高度和间隔
        }
        if (isShowSectionText && mSectionTextPosition >= TextPosition.BOTTOM_SIDES) { // 如果Section值在track之下显示，比较取较大值
            //测量节点文字的高度，如果有lable，测量真实的lable高度，如果没有表示显示的进度是数字，就用“j”代替测量高度
            String measuretext = isSidesLabels ? mSidesLabels[0] : "j";
            mPaint.setTextSize(mSectionTextSize);
            mPaint.getTextBounds(measuretext, 0, measuretext.length(), mRectText);
            height = Math.max(height, mThumbRadiusOnDragging * 2 + mRectText.height());
        }
        height += mSignHeight;//加上提示框的高度
        if (isShowSignBorder) height += mSignBorderSize;//加上提示框边框高度
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), height);

        mLeft = getPaddingLeft() + mThumbRadiusOnDragging;
        mRight = getMeasuredWidth() - getPaddingRight() - mThumbRadiusOnDragging;

        if (isShowSectionText) {
            mPaint.setTextSize(mSectionTextSize);

            if (mSectionTextPosition == TextPosition.SIDES) {
                String text = getMinText();
                mPaint.getTextBounds(text, 0, text.length(), mRectText);
                mLeft += (mRectText.width());

                text = getMaxText();
                mPaint.getTextBounds(text, 0, text.length(), mRectText);
                mRight -= (mRectText.width());
            } else if (mSectionTextPosition >= TextPosition.BOTTOM_SIDES) {
                String text = isSidesLabels ? mSidesLabels[0] : getMinText();
                mPaint.getTextBounds(text, 0, text.length(), mRectText);
                float max = Math.max(mThumbRadiusOnDragging, mRectText.width() / 2f);
                mLeft = getPaddingLeft() + max;

                text = isSidesLabels ? mSidesLabels[mSidesLabels.length - 1] : getMaxText();
                mPaint.getTextBounds(text, 0, text.length(), mRectText);
                max = Math.max(mThumbRadiusOnDragging, mRectText.width() / 2f);
                mRight = getMeasuredWidth() - getPaddingRight() - max;
            }
        } else if (isShowThumbText && mSectionTextPosition == NONE) {
            mPaint.setTextSize(mThumbTextSize);

            String text = getMinText();
            mPaint.getTextBounds(text, 0, text.length(), mRectText);
            float max = Math.max(mThumbRadiusOnDragging, mRectText.width() / 2f);
            mLeft = getPaddingLeft() + max;

            text = getMaxText();
            mPaint.getTextBounds(text, 0, text.length(), mRectText);
            max = Math.max(mThumbRadiusOnDragging, mRectText.width() / 2f);
            mRight = getMeasuredWidth() - getPaddingRight() - max;
        }

        if (isShowSign && !isSignArrowAutofloat) {//提示框 三角指示是否自动移动
            mLeft = Math.max(mLeft, getPaddingLeft() + mSignWidth / 2f + mSignBorderSize);
            mRight = Math.min(mRight, getMeasuredWidth() - getPaddingRight() - mSignWidth / 2f - mSignBorderSize);
        }

        mTrackLength = mRight - mLeft;
        mSectionOffset = mTrackLength / mSectionCount;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float xLeft = getPaddingLeft();
        float xRight = getMeasuredWidth() - getPaddingRight();
        float yTop = getPaddingTop() + mThumbRadiusOnDragging;
        if (isShowSign) {//加上提示框高度
            yTop += mSignHeight;
        }
        if (isShowSignBorder) {//加上提示框边框高度
            yTop += mSignBorderSize;
        }
        if (isShowSign && !isSignArrowAutofloat) {//是否浮动显示提示框三角指示，默认浮动，否则居中显示
            xLeft += (mSignWidth / 2f + mSignBorderSize);
            xRight -= (mSignWidth / 2f + mSignBorderSize);
        }
        // draw sectionText SIDES or BOTTOM_SIDES
        if (isShowSectionText) {
            mPaint.setTextSize(mSectionTextSize);
            if(isEnabled())mPaint.setColor(mSectionTextColor);

            if (mSectionTextPosition == TextPosition.SIDES) {
                float y_ = yTop + mRectText.height() / 2f;

                //String text = getMinText();
                String text = isSidesLabels ? mSidesLabels[0] : getMinText();
                mPaint.getTextBounds(text, 0, text.length(), mRectText);
                canvas.drawText(text, xLeft + mRectText.width() / 2f, y_, mPaint);
                xLeft += mRectText.width();

                //text = getMaxText();
                text = isSidesLabels && mSidesLabels.length > 1 ? mSidesLabels[mSidesLabels.length - 1] : getMaxText();
                mPaint.getTextBounds(text, 0, text.length(), mRectText);
                canvas.drawText(text, xRight - mRectText.width() / 2f, y_, mPaint);
                xRight -= (mRectText.width());

            } else if (mSectionTextPosition >= TextPosition.BOTTOM_SIDES) {
                float y_ = yTop + mThumbRadiusOnDragging;

                // String text = getMinText();
                String text = isSidesLabels ? mSidesLabels[0] : getMinText();
                mPaint.getTextBounds(text, 0, text.length(), mRectText);
                y_ += mRectText.height();
                xLeft = mLeft;
                if (mSectionTextPosition == TextPosition.BOTTOM_SIDES) {
                    canvas.drawText(text, xLeft, y_, mPaint);
                }

                //text = getMaxText();
                text = isSidesLabels && mSidesLabels.length > 1 ? mSidesLabels[mSidesLabels.length - 1] : getMaxText();
                mPaint.getTextBounds(text, 0, text.length(), mRectText);
                xRight = mRight;
                if (mSectionTextPosition == TextPosition.BOTTOM_SIDES) {
                    canvas.drawText(text, xRight, y_, mPaint);
                }
            }
        } else if (isShowThumbText && mSectionTextPosition == NONE) {
            xLeft = mLeft;
            xRight = mRight;
        }

        if ((!isShowSectionText && !isShowThumbText) || mSectionTextPosition == TextPosition.SIDES) {
            xLeft += mThumbRadiusOnDragging;
            xRight -= mThumbRadiusOnDragging;
        }

        boolean isShowTextBelowSectionMark = isShowSectionText && mSectionTextPosition ==
                TextPosition.BELOW_SECTION_MARK;
        //boolean conditionInterval = mSectionCount % 2 == 0;
        boolean conditionInterval = true;

        // draw sectionMark & sectionText BELOW_SECTION_MARK
        if (isShowTextBelowSectionMark || isShowSectionMark) {
            drawMark(canvas, xLeft, yTop, isShowTextBelowSectionMark, conditionInterval);
        }

        if (!isThumbOnDragging) {
            mThumbCenterX = mTrackLength / mDelta * (mProgress - mMin) + xLeft;
        }

        // draw thumbText
        if (isShowThumbText && !isThumbOnDragging && isTouchToSeekAnimEnd) {
            drawThumbText(canvas, yTop);
        }

        // draw track
        mPaint.setColor(mSecondTrackColor);
        mPaint.setStrokeWidth(mSecondTrackSize);
        canvas.drawLine(xLeft, yTop, mThumbCenterX, yTop, mPaint);

        // draw second track
        mPaint.setColor(mTrackColor);
        mPaint.setStrokeWidth(mTrackSize);
        canvas.drawLine(mThumbCenterX, yTop, xRight, yTop, mPaint);

        // draw thumb
        mPaint.setColor(mThumbColor);
        //draw thumb shadow
        if (isShowThumbShadow) {
            canvas.drawCircle(mThumbCenterX, yTop, isThumbOnDragging ? mThumbRadiusOnDragging * mThumbRatio : mThumbRadius * mThumbRatio, mPaint);
            mPaint.setColor(getColorWithAlpha(mThumbColor, mThumbBgAlpha));
        }
        //Paint paint = new Paint();
        //Shader shader = new RadialGradient(mThumbCenterX, yTop, isThumbOnDragging ? mThumbRadiusOnDragging : mThumbRadius, mThumbColor, getColorWithAlpha(mThumbColor, mThumbBgAlpha), Shader.TileMode.CLAMP);
        //paint.setShader(shader);
        canvas.drawCircle(mThumbCenterX, yTop, isThumbOnDragging ? mThumbRadiusOnDragging : mThumbRadius, mPaint);

        //draw progress text
        //drawProgressText(canvas);

        //draw sign
        if (!isShowSign) return;
        drawValueSign(canvas, mSignHeight, (int) mThumbCenterX);
    }

    //draw mark
    private void drawMark(Canvas canvas, float xLeft, float yTop, boolean isShowTextBelowSectionMark, boolean conditionInterval) {
        float r = (mThumbRadiusOnDragging - DevicesUtils.dip2px(2)) / 2f;
        float junction = mTrackLength / mDelta * Math.abs(mProgress - mMin) + mLeft; // 交汇点
        mPaint.setTextSize(mSectionTextSize);
        mPaint.getTextBounds("0123456789", 0, "0123456789".length(), mRectText); // compute solid height

        float x_;
        float y_ = yTop + mRectText.height() + mThumbRadiusOnDragging;

        for (int i = 0; i <= mSectionCount; i++) {
            x_ = xLeft + i * mSectionOffset;
            mPaint.setColor(x_ <= junction ? mSecondTrackColor : mTrackColor);
            // sectionMark
            canvas.drawCircle(x_, yTop, r, mPaint);

            // sectionText belows section
            if (isShowTextBelowSectionMark) {
                float m = mMin + mSectionValue * i;
                //不可用，除了当前节点之外的其它节点用不可用颜色表示
                if(isEnabled()) mPaint.setColor(mSectionTextColor);
                if (mSectionTextInterval > 1) {
                    if (conditionInterval && i % mSectionTextInterval == 0) {
                        if (isSidesLabels) {
                            canvas.drawText(mSidesLabels[i], x_, y_, mPaint);
                        } else {
                            canvas.drawText(isFloatType ? float2String(m) : (int) m + "", x_, y_, mPaint);
                        }
                    }
                } else {
                    if (conditionInterval && i % mSectionTextInterval == 0) {
                        if (isSidesLabels && i / mSectionTextInterval <= mSidesLabels.length) {
                            canvas.drawText(mSidesLabels[i / mSectionTextInterval], x_, y_, mPaint);
                        } else {
                            canvas.drawText(isFloatType ? float2String(m) : (int) m + "", x_, y_, mPaint);
                        }
                    }
                }
            }
        }
    }

    //draw thumb text
    private void drawThumbText(Canvas canvas, float yTop) {
        mPaint.setColor(mThumbTextColor);
        mPaint.setTextSize(mThumbTextSize);
        mPaint.getTextBounds("0123456789", 0, "0123456789".length(), mRectText); // compute solid height
        float y_ = yTop + mRectText.height() + mThumbRadiusOnDragging;

        if (isFloatType || (isShowProgressInFloat && mSectionTextPosition == TextPosition.BOTTOM_SIDES &&
                mProgress != mMin && mProgress != mMax)) {
            float progress = getProgressFloat();
            String value = String.valueOf(progress);
            if (mFormat != null) {
                value = mFormat.format(progress);
            }
            if (unit != null && !unit.isEmpty()) {
                if (!mReverse) {
                    value += String.format("%s", unit);
                } else {
                    value = String.format("%s", unit) + value;
                }
            }
            drawSignText(canvas, value, mThumbCenterX, y_, mPaint);
        } else {
            int progress = getProgress();
            String value = String.valueOf(progress);
            if (mFormat != null) {
                value = mFormat.format(progress);
            }
            if (unit != null && !unit.isEmpty()) {
                if (!mReverse) {
                    value += String.format("%s", unit);
                } else {
                    value = String.format("%s", unit) + value;
                }
            }
            drawSignText(canvas, value, mThumbCenterX, y_, mPaint);
        }
    }

    public void drawSignText(Canvas canvas, String text, float x, float y, Paint paint) {
        canvas.drawText(text, x, y, paint);
    }

    //draw value sign
    private void drawValueSign(Canvas canvas, int valueSignSpaceHeight, int valueSignCenter) {
        valueSignBounds.set(valueSignCenter - mSignWidth / 2, getPaddingTop(), valueSignCenter + mSignWidth / 2, mSignHeight - mSignArrowHeight + getPaddingTop());

        int bordersize = isShowSignBorder ? mSignBorderSize : 0;
        // Move if not fit horizontal
        if (valueSignBounds.left < getPaddingLeft()) {
            int difference = -valueSignBounds.left + getPaddingLeft() + bordersize;
            roundRectangleBounds.set(valueSignBounds.left + difference, valueSignBounds.top, valueSignBounds.right +
                    difference, valueSignBounds.bottom);
        } else if (valueSignBounds.right > getMeasuredWidth() - getPaddingRight()) {
            int difference = valueSignBounds.right - getMeasuredWidth() + getPaddingRight() + bordersize;
            roundRectangleBounds.set(valueSignBounds.left - difference, valueSignBounds.top, valueSignBounds.right -
                    difference, valueSignBounds.bottom);
        } else {
            roundRectangleBounds.set(valueSignBounds.left, valueSignBounds.top, valueSignBounds.right,
                    valueSignBounds.bottom);
        }

        canvas.drawRoundRect(roundRectangleBounds, mSignRound, mSignRound, signPaint);
        if (isShowSignBorder) {
            roundRectangleBounds.top = roundRectangleBounds.top + mSignBorderSize / 2f;
            canvas.drawRoundRect(roundRectangleBounds, mSignRound, mSignRound, signborderPaint);
        }
        int barRoundingRadius = isThumbOnDragging ? mThumbRadiusOnDragging : mThumbRadius;
        int difference = 0;
        if (valueSignCenter - mSignArrowWidth / 2 < barRoundingRadius + getPaddingLeft() + bordersize) {
            difference = barRoundingRadius - valueSignCenter + getPaddingLeft() + bordersize;
        } else if (valueSignCenter + mSignArrowWidth / 2 > getMeasuredWidth() - barRoundingRadius - getPaddingRight() - bordersize) {
            difference = (getMeasuredWidth() - barRoundingRadius) - valueSignCenter - getPaddingRight() - bordersize;
        }

        point1.set(valueSignCenter - mSignArrowWidth / 2 + difference, valueSignSpaceHeight - mSignArrowHeight + getPaddingTop());
        point2.set(valueSignCenter + mSignArrowWidth / 2 + difference, valueSignSpaceHeight - mSignArrowHeight + getPaddingTop());
        point3.set(valueSignCenter + difference, valueSignSpaceHeight + getPaddingTop());

        drawTriangle(canvas, point1, point2, point3, signPaint);
        if (isShowSignBorder) {
            drawTriangleBoder(canvas, point1, point2, point3, signborderPaint);
        }

        createValueTextLayout();
        // Draw value text
        if (valueTextLayout != null) {
            canvas.translate(roundRectangleBounds.left, roundRectangleBounds.top + roundRectangleBounds.height() / 2 - valueTextLayout.getHeight() / 2);
            valueTextLayout.draw(canvas);
        }
    }

    private void drawTriangle(Canvas canvas, Point point1, Point point2, Point point3, Paint paint) {
        trianglePath.reset();
        trianglePath.moveTo(point1.x, point1.y);
        trianglePath.lineTo(point2.x, point2.y);
        trianglePath.lineTo(point3.x, point3.y);
        trianglePath.lineTo(point1.x, point1.y);
        trianglePath.close();

        canvas.drawPath(trianglePath, paint);
    }

    /**
     * 将三角形的一条顶边用颜色给覆盖掉
     */
    private void drawTriangleBoder(Canvas canvas, Point point1, Point point2, Point point3, Paint paint) {
        triangleboderPath.reset();
        triangleboderPath.moveTo(point1.x, point1.y);
        triangleboderPath.lineTo(point2.x, point2.y);
        paint.setColor(signPaint.getColor());
        float value = mSignBorderSize / 6f;
        paint.setStrokeWidth(mSignBorderSize + 1f);
        canvas.drawPath(triangleboderPath, paint);
        triangleboderPath.reset();
        paint.setStrokeWidth(mSignBorderSize);
        triangleboderPath.moveTo(point1.x - value, point1.y - value);
        triangleboderPath.lineTo(point3.x, point3.y);
        triangleboderPath.lineTo(point2.x + value, point2.y - value);
        paint.setColor(mSignBorderColor);
        canvas.drawPath(triangleboderPath, paint);
    }

    /**
     * 设置单位
     */
    public void setUnit(String unit) {
        this.unit = unit;
        createValueTextLayout();
        invalidate();
        requestLayout();
    }

    private void createValueTextLayout() {
        String value = "";
        if (isShowProgressInFloat) {
            float progress = getProgressFloat();
            value = String.valueOf(progress);
            if (mFormat != null) {
                value = mFormat.format(progress);
            }
        } else {
            int progress = getProgress();
            value = String.valueOf(progress);
            if (mFormat != null) {
                value = mFormat.format(progress);
            }
        }
        if (unit != null && !unit.isEmpty()) {
            if (!mReverse) {
                value += String.format(" <small>%s</small> ", unit);
                //value += String.format("%s", unit);
            } else {
                value = String.format(" %s ", unit) + value;
            }
        }
        Spanned spanned = Html.fromHtml(value);
        valueTextLayout = new StaticLayout(spanned, valueTextPaint, mSignWidth, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        post(this::requestLayout);
    }

    float dx;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);

                isThumbOnDragging = isThumbTouched(event);
                if (isThumbOnDragging) {
                    if (isSeekBySection && !triggerSeekBySection) {
                        triggerSeekBySection = true;
                    }
                    invalidate();
                } else if (isTouchToSeek && isTrackTouched(event)) {
                    isThumbOnDragging = true;
                    mThumbCenterX = event.getX();
                    if (mThumbCenterX < mLeft) {
                        mThumbCenterX = mLeft;
                    }
                    if (mThumbCenterX > mRight) {
                        mThumbCenterX = mRight;
                    }
                    mProgress = (mThumbCenterX - mLeft) * mDelta / mTrackLength + mMin;
                    invalidate();
                }

                dx = mThumbCenterX - event.getX();

                break;
            case MotionEvent.ACTION_MOVE:
                if (isThumbOnDragging) {
                    mThumbCenterX = event.getX() + dx;
                    if (mThumbCenterX < mLeft) {
                        mThumbCenterX = mLeft;
                    }
                    if (mThumbCenterX > mRight) {
                        mThumbCenterX = mRight;
                    }
                    mProgress = (mThumbCenterX - mLeft) * mDelta / mTrackLength + mMin;
                    invalidate();

                    if (mProgressListener != null) {
                        mProgressListener.onProgressChanged(this, getProgress(), getProgressFloat(), true);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);

                if (isAutoAdjustSectionMark) {
                    if (isTouchToSeek) {
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isTouchToSeekAnimEnd = false;
                                autoAdjustSection();
                            }
                        }, isThumbOnDragging ? 0 : 300);
                    } else {
                        autoAdjustSection();
                    }
                } else if (isThumbOnDragging || isTouchToSeek) {
                    animate()
                            .setDuration(mAnimDuration)
                            .setStartDelay(!isThumbOnDragging && isTouchToSeek ? 300 : 0)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    isThumbOnDragging = false;
                                    invalidate();

                                    if (mProgressListener != null) {
                                        mProgressListener.onProgressChanged(SignSeekBar.this,
                                                getProgress(), getProgressFloat(), true);
                                    }
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    isThumbOnDragging = false;
                                    invalidate();
                                }
                            })
                            .start();
                }

                if (mProgressListener != null) {
                    mProgressListener.getProgressOnActionUp(this, getProgress(), getProgressFloat());
                }

                break;
        }

        return isThumbOnDragging || isTouchToSeek || super.onTouchEvent(event);
    }

    /**
     * 计算新的透明度颜色
     *
     * @param color 旧颜色
     * @param ratio 透明度系数
     */
    public int getColorWithAlpha(int color, float ratio) {
        int newColor = 0;
        int alpha = Math.round(Color.alpha(color) * ratio);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        newColor = Color.argb(alpha, r, g, b);
        return newColor;
    }

    /**
     * Detect effective touch of thumb
     */
    private boolean isThumbTouched(MotionEvent event) {
        if (!isEnabled())
            return false;
        float mCircleR = isThumbOnDragging ? mThumbRadiusOnDragging : mThumbRadius;
        float x = mTrackLength / mDelta * (mProgress - mMin) + mLeft;
        float y = getMeasuredHeight() / 2f;
        return (event.getX() - x) * (event.getX() - x) + (event.getY() - y) * (event.getY() - y)
                <= (mLeft + mCircleR) * (mLeft + mCircleR);
    }

    /**
     * Detect effective touch of track
     */
    private boolean isTrackTouched(MotionEvent event) {
        return isEnabled() && event.getX() >= getPaddingLeft() && event.getX() <= getMeasuredWidth() - getPaddingRight()
                && event.getY() >= getPaddingTop() && event.getY() <= getMeasuredHeight() - getPaddingBottom();
    }

    /**
     * Auto scroll to the nearest section mark
     */
    private void autoAdjustSection() {
        int i;
        //计算最近节点位置，mSectionCount：节点个数，mSectionOffset：两个节点间隔距离，mThumbCenterX：滑块中心点位置
        float x = 0;
        for (i = 0; i <= mSectionCount; i++) {
            x = i * mSectionOffset + mLeft;
            if (x <= mThumbCenterX && mThumbCenterX - x <= mSectionOffset) {
                break;
            }
        }

        BigDecimal bigDecimal = BigDecimal.valueOf(mThumbCenterX);
        //BigDecimal setScale保留1位小数，四舍五入，2.35变成2.4
        float x_ = bigDecimal.setScale(1, RoundingMode.HALF_UP).floatValue();
        boolean onSection = x_ == x; // 就在section处，不作valueAnim，优化性能

        AnimatorSet animatorSet = new AnimatorSet();
        ValueAnimator valueAnim = null;
        if (!onSection) {
            if (mThumbCenterX - x <= mSectionOffset / 2f) {
                valueAnim = ValueAnimator.ofFloat(mThumbCenterX, x);
            } else {
                valueAnim = ValueAnimator.ofFloat(mThumbCenterX, (i + 1) * mSectionOffset + mLeft);
            }
            valueAnim.setInterpolator(new LinearInterpolator());
            valueAnim.addUpdateListener(animation -> {
                mThumbCenterX = (float) animation.getAnimatedValue();
                mProgress = (mThumbCenterX - mLeft) * mDelta / mTrackLength + mMin;
                invalidate();

                if (mProgressListener != null) {
                    mProgressListener.onProgressChanged(SignSeekBar.this, getProgress(), getProgressFloat(), true);
                }
            });
        }
        if (!onSection) {
            animatorSet.setDuration(mAnimDuration).playTogether(valueAnim);
        }
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgress = (mThumbCenterX - mLeft) * mDelta / mTrackLength + mMin;
                isThumbOnDragging = false;
                isTouchToSeekAnimEnd = true;
                invalidate();

                if (mProgressListener != null) {
                    mProgressListener.getProgressOnFinally(SignSeekBar.this, getProgress(), getProgressFloat(), true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mProgress = (mThumbCenterX - mLeft) * mDelta / mTrackLength + mMin;
                isThumbOnDragging = false;
                isTouchToSeekAnimEnd = true;
                invalidate();
            }
        });
        animatorSet.start();
    }

    private String getMinText() {
        return isFloatType ? float2String(mMin) : String.valueOf((int) mMin);
    }

    private String getMaxText() {
        return isFloatType ? float2String(mMax) : String.valueOf((int) mMax);
    }

    public float getMin() {
        return mMin;
    }

    public float getMax() {
        return mMax;
    }

    public void setProgress(float progress) {
        mProgress = progress;
        if (mProgressListener != null) {
            mProgressListener.onProgressChanged(this, getProgress(), getProgressFloat(), false);
            mProgressListener.getProgressOnFinally(this, getProgress(), getProgressFloat(), false);
        }
        postInvalidate();
    }

    public int getProgress() {
        if (isSeekBySection && triggerSeekBySection) {
            float half = mSectionValue / 2;

            if (mProgress >= mPreSecValue) { // increasing
                if (mProgress >= mPreSecValue + half) {
                    mPreSecValue += mSectionValue;
                    return Math.round(mPreSecValue);
                } else {
                    return Math.round(mPreSecValue);
                }
            } else { // reducing
                if (mProgress >= mPreSecValue - half) {
                    return Math.round(mPreSecValue);
                } else {
                    mPreSecValue -= mSectionValue;
                    return Math.round(mPreSecValue);
                }
            }
        }

        return Math.round(mProgress);
    }

    public float getProgressFloat() {
        return formatFloat(mProgress);
    }

    public void setOnProgressChangedListener(OnProgressChangedListener onProgressChangedListener) {
        mProgressListener = onProgressChangedListener;
    }

    void config(SignConfigBuilder builder) {
        mMin = builder.min;
        mMax = builder.max;
        mProgress = builder.progress;
        isFloatType = builder.floatType;
        mTrackSize = builder.trackSize;
        mSecondTrackSize = builder.secondTrackSize;
        mThumbRadius = builder.thumbRadius;
        mThumbRadiusOnDragging = builder.thumbRadiusOnDragging;
        mTrackColor = builder.trackColor;
        mSecondTrackColor = builder.secondTrackColor;
        mThumbColor = builder.thumbColor;
        mSectionCount = builder.sectionCount;
        isShowSectionMark = builder.showSectionMark;
        isAutoAdjustSectionMark = builder.autoAdjustSectionMark;
        isShowSectionText = builder.showSectionText;
        mSectionTextSize = builder.sectionTextSize;
        mSectionTextColor = builder.sectionTextColor;
        mSectionTextPosition = builder.sectionTextPosition;
        mSectionTextInterval = builder.sectionTextInterval;
        isShowThumbText = builder.showThumbText;
        mThumbTextSize = builder.thumbTextSize;
        mThumbTextColor = builder.thumbTextColor;
        isShowProgressInFloat = builder.showProgressInFloat;
        mAnimDuration = builder.animDuration;
        isTouchToSeek = builder.touchToSeek;
        isSeekBySection = builder.seekBySection;
        mSidesLabels = mConfigBuilder.bottomSidesLabels;
        isSidesLabels = mSidesLabels != null && mSidesLabels.length > 0;
        mThumbBgAlpha = mConfigBuilder.thumbBgAlpha;
        mThumbRatio = mConfigBuilder.thumbRatio;
        isShowThumbShadow = mConfigBuilder.showThumbShadow;
        unit = mConfigBuilder.unit;
        mReverse = mConfigBuilder.reverse;
        mFormat = mConfigBuilder.format;
        mSignColor = builder.signColor;
        mSignTextSize = builder.signTextSize;
        mSignTextColor = builder.signTextColor;
        isShowSign = builder.showSign;
        mSignArrowWidth = builder.signArrowWidth;
        mSignArrowHeight = builder.signArrowHeight;
        mSignRound = builder.signRound;
        mSignHeight = builder.signHeight;
        mSignWidth = builder.signWidth;
        isShowSignBorder = builder.showSignBorder;
        mSignBorderSize = builder.signBorderSize;
        mSignBorderColor = builder.signBorderColor;
        isSignArrowAutofloat = builder.signArrowAutofloat;

        init();
        initConfigByPriority();
        createValueTextLayout();
        if (mProgressListener != null) {
            mProgressListener.onProgressChanged(this, getProgress(), getProgressFloat(), false);
            mProgressListener.getProgressOnFinally(this, getProgress(), getProgressFloat(), false);
        }

        mConfigBuilder = null;

        requestLayout();
    }

    public SignConfigBuilder getConfigBuilder() {
        if (mConfigBuilder == null) {
            mConfigBuilder = new SignConfigBuilder(this);
        }
        mConfigBuilder.min = mMin;
        mConfigBuilder.max = mMax;
        mConfigBuilder.progress = mProgress;
        mConfigBuilder.floatType = isFloatType;
        mConfigBuilder.trackSize = mTrackSize;
        mConfigBuilder.secondTrackSize = mSecondTrackSize;
        mConfigBuilder.thumbRadius = mThumbRadius;
        mConfigBuilder.thumbRadiusOnDragging = mThumbRadiusOnDragging;
        mConfigBuilder.trackColor = mTrackColor;
        mConfigBuilder.secondTrackColor = mSecondTrackColor;
        mConfigBuilder.thumbColor = mThumbColor;
        mConfigBuilder.sectionCount = mSectionCount;
        mConfigBuilder.showSectionMark = isShowSectionMark;
        mConfigBuilder.autoAdjustSectionMark = isAutoAdjustSectionMark;
        mConfigBuilder.showSectionText = isShowSectionText;
        mConfigBuilder.sectionTextSize = mSectionTextSize;
        mConfigBuilder.sectionTextColor = mSectionTextColor;
        mConfigBuilder.sectionTextPosition = mSectionTextPosition;
        mConfigBuilder.sectionTextInterval = mSectionTextInterval;
        mConfigBuilder.showThumbText = isShowThumbText;
        mConfigBuilder.thumbTextSize = mThumbTextSize;
        mConfigBuilder.thumbTextColor = mThumbTextColor;
        mConfigBuilder.showProgressInFloat = isShowProgressInFloat;
        mConfigBuilder.animDuration = mAnimDuration;
        mConfigBuilder.touchToSeek = isTouchToSeek;
        mConfigBuilder.seekBySection = isSeekBySection;
        mConfigBuilder.bottomSidesLabels = mSidesLabels;
        mConfigBuilder.thumbBgAlpha = mThumbBgAlpha;
        mConfigBuilder.thumbRatio = mThumbRatio;
        mConfigBuilder.showThumbShadow = isShowThumbShadow;
        mConfigBuilder.unit = unit;
        mConfigBuilder.reverse = mReverse;
        mConfigBuilder.format = mFormat;
        mConfigBuilder.signColor = mSignColor;
        mConfigBuilder.signTextSize = mSignTextSize;
        mConfigBuilder.signTextColor = mSignTextColor;
        mConfigBuilder.showSign = isShowSign;
        mConfigBuilder.signArrowHeight = mSignArrowHeight;
        mConfigBuilder.signArrowWidth = mSignArrowWidth;
        mConfigBuilder.signRound = mSignRound;
        mConfigBuilder.signHeight = mSignHeight;
        mConfigBuilder.signWidth = mSignWidth;
        mConfigBuilder.showSignBorder = isShowSignBorder;
        mConfigBuilder.signBorderSize = mSignBorderSize;
        mConfigBuilder.signBorderColor = mSignBorderColor;
        mConfigBuilder.signArrowAutofloat = isSignArrowAutofloat;

        return mConfigBuilder;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("save_instance", super.onSaveInstanceState());
        bundle.putFloat("progress", mProgress);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle bundle) {
            mProgress = bundle.getFloat("progress");
            super.onRestoreInstanceState(bundle.getParcelable("save_instance"));
            setProgress(mProgress);
            return;
        }
        super.onRestoreInstanceState(state);
    }

    private String float2String(float value) {
        return String.valueOf(formatFloat(value));
    }

    private float formatFloat(float value) {
        BigDecimal bigDecimal = BigDecimal.valueOf(value);
        return bigDecimal.setScale(1, RoundingMode.HALF_UP).floatValue();
    }

    /**
     * Listen to progress onChanged, onActionUp, onFinally
     */
    public interface OnProgressChangedListener {

        void onProgressChanged(SignSeekBar signSeekBar, int progress, float progressFloat, boolean fromUser);

        void getProgressOnActionUp(SignSeekBar signSeekBar, int progress, float progressFloat);

        void getProgressOnFinally(SignSeekBar signSeekBar, int progress, float progressFloat, boolean fromUser);
    }
}
