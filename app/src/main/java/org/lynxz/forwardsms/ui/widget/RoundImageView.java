package org.lynxz.forwardsms.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import org.lynxz.forwardsms.R;

/**
 * 自定义圆角iamgeview,默认为直角
 * https://blog.csdn.net/qq_26287435/article/details/79162186
 *
 * <pre>
 * 1. 若设定 "app:riv_clipCircle=true" ,则raidus等参数不起作用, 会在控件中间位置裁切一个圆形;
 * 2. 若同时设置 "app:riv_radius" 及 各圆角半径,则各圆角半径优先起作用;
 * 3. 其他属性设置同 ImageView;
 * </pre>
 */
public class RoundImageView extends AppCompatImageView {
    float width, height;
    private static final int defaultRadius = 0;
    private int leftTopRadius;
    private int rightTopRadius;
    private int rightBottomRadius;
    private int leftBottomRadius;
    private Path mPath;
    private int mMinWidth;
    private int mMinHeight;
    private boolean mShouldClipCanvas = false; // 是否需要裁剪圆角
    private boolean mClipToCircle;

    public RoundImageView(Context context) {
        this(context, null);
    }

    public RoundImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (Build.VERSION.SDK_INT < 18) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RoundImageView);
        int radius = array.getDimensionPixelOffset(R.styleable.RoundImageView_riv_radius, defaultRadius);
        leftTopRadius = array.getDimensionPixelOffset(R.styleable.RoundImageView_riv_left_top_radius, radius);
        rightTopRadius = array.getDimensionPixelOffset(R.styleable.RoundImageView_riv_right_top_radius, radius);
        rightBottomRadius = array.getDimensionPixelOffset(R.styleable.RoundImageView_riv_right_bottom_radius, radius);
        leftBottomRadius = array.getDimensionPixelOffset(R.styleable.RoundImageView_riv_left_bottom_radius, radius);
        mClipToCircle = array.getBoolean(R.styleable.RoundImageView_riv_clipCircle, false);

        // 是否需要裁切
        mShouldClipCanvas = mClipToCircle || (leftTopRadius != defaultRadius) || (rightTopRadius != defaultRadius)
                || (rightBottomRadius != defaultRadius) || (leftBottomRadius != defaultRadius);

        array.recycle();
        mPath = new Path();

        int maxLeft = Math.max(leftTopRadius, leftBottomRadius);
        int maxRight = Math.max(rightTopRadius, rightBottomRadius);
        mMinWidth = maxLeft + maxRight;
        int maxTop = Math.max(leftTopRadius, rightTopRadius);
        int maxBottom = Math.max(leftBottomRadius, rightBottomRadius);
        mMinHeight = maxTop + maxBottom;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        width = getWidth();
        height = getHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 只有图片的宽高大于设置的圆角距离的时候才进行裁剪
        if (mShouldClipCanvas) {
            mPath.reset();

            float size = Math.min(width, height);
            if (mClipToCircle) {
                mPath.addCircle(width / 2, height / 2, size / 2, Path.Direction.CCW);
            } else {
                if (width < mMinWidth || height < mMinHeight) {
                    leftTopRadius = (int) (size / 2);
                    rightTopRadius = (int) (size / 2);
                    leftBottomRadius = (int) (size / 2);
                    rightBottomRadius = (int) (size / 2);
                }

                // 四个角：右上，右下，左下，左上
                mPath.moveTo(leftTopRadius, 0);
                mPath.lineTo(width - rightTopRadius, 0);
                mPath.quadTo(width, 0, width, rightTopRadius);

                mPath.lineTo(width, height - rightBottomRadius);
                mPath.quadTo(width, height, width - rightBottomRadius, height);

                mPath.lineTo(leftBottomRadius, height);
                mPath.quadTo(0, height, 0, height - leftBottomRadius);

                mPath.lineTo(0, leftTopRadius);
                mPath.quadTo(0, 0, leftTopRadius, 0);
            }

            canvas.clipPath(mPath);
        }
        super.onDraw(canvas);
    }
}