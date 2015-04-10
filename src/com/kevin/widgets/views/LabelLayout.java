package com.kevin.widgets.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.kevin.widgets.R;


/*
 *  This widget constructed by 9 views, but only [label] and [custom] could 
 *  be configured. Other views are just decorate.  
 * 			[up_left_corner]   [label]    [up_right_corner]
 * 			[left]      [custom layout/empty]       [right]
 * 			[down_left]                        [down_right]
 *  
 */

public class LabelLayout extends ViewGroup {
	
	public static final int LABEL_MIDDLE = 0;
	public static final int LABEL_LEFT = 1;	
	public static final int LABEL_RIGHT = 2;
	
	private static final String TAG = "LabelLayout";

	static final int INDEX_LEFT_UP = 0;
	static final int INDEX_TEXT = 1;
	static final int INDEX_RIGHT_UP = 2;
	static final int INDEX_LEFT = 3;
	static final int INDEX_RIGHT = 4;
	static final int INDEX_LEFT_BOTTOM = 5;
	static final int INDEX_RIGHT_BOTTOM = 6;
	static final int INDEX_CLOSED = 7;
	static final int DEFAULT_VIEWS_NUM = 8;

	float dpToPx = 1;

	int mTvHeight = 0;
	int mTvWidth = 0;

	int mCustomHeight = 0;
	int mCustomWidth = 0;

	int mDefaultWidth = 4;
	int mDefaultTopHeight = 0;
	int mDefaultBottomHeight = 0;
	int mDefaultContentHeight = 0;
	int mLabelSize = 18;
	static final int ANIMATION_DURATION = 300; 
	
	boolean mShowContent = true;
	TextView mTvLabel = null;
	TextView mTvHide = null;
	

	
	// unit dip
	static final int SIDE_WIDTH = 4;
	static final int ROW_TOP_HEIGHT = 6;
	static final int ROW_BOTTOM_HEIGHT = 12;
	static final int ROW_HIDE_HEIGHT = 15;
	
	int mLabelPos;
	int mLabelOffset;
	String mLabel = "";
	
	
	boolean debug = true;
	
	boolean mEnableAnimation = true;
	boolean mEnableHideContent = true;
	
	int mChildTop, mChildLeft, mChildRight, mChildBottom;
		
	public LabelLayout(Context context) {
		this(context, null);
	}

	public LabelLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LabelLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		mTvLabel = new TextView(context, attrs, defStyle);
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OutlineLayoutConfig);
		mLabelPos = a.getInt(R.styleable.OutlineLayoutConfig_label_pos, LABEL_MIDDLE);
        mLabelOffset = (int)a.getDimensionPixelSize(R.styleable.OutlineLayoutConfig_label_offset, 0);
        debug("mLabelOffset:"+mLabelOffset);
        mEnableAnimation = a.getBoolean(R.styleable.OutlineLayoutConfig_animation, true);
        mLabelSize *= dpToPx;
        enableHideContent(a.getBoolean(R.styleable.OutlineLayoutConfig_canHideContent, true));
        
        a.recycle();     
		
		dpToPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
				getResources().getDisplayMetrics());
		
		mDefaultWidth = (int) (SIDE_WIDTH * dpToPx);
		mDefaultTopHeight = (int) (ROW_TOP_HEIGHT * dpToPx);
		mDefaultBottomHeight = (int) (ROW_BOTTOM_HEIGHT * dpToPx);;
		mDefaultContentHeight = (int)(ROW_HIDE_HEIGHT*dpToPx);
				
		mTvLabel.setSingleLine(true);
		mTvLabel.setEllipsize(TruncateAt.END);
				
//		mTvLabel.setText(mLabel);
//		mTvLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, mLabelSize);
        
		mTvLabel.setPadding(0, 0, 0, 0);  // reset padding to prevent it got impacted from parent ViewGroup
		
        
        
        // setup and add decorate views
		View ul = new View(context);
		ul.setBackgroundResource(R.drawable.up_left_first);
		View ur = new View(context);
		ur.setBackgroundResource(R.drawable.up_right_first);
		View l = new View(context);
		l.setBackgroundResource(R.drawable.left);
		View r = new View(context);
		r.setBackgroundResource(R.drawable.right);
		View bl = new View(context);
		bl.setBackgroundResource(R.drawable.down_left);
		View br = new View(context);
		br.setBackgroundResource(R.drawable.down_right);
		
		addView(ul, INDEX_LEFT_UP);
		addView(mTvLabel, INDEX_TEXT);
		addView(ur, INDEX_RIGHT_UP);
		addView(l, INDEX_LEFT);
		addView(r, INDEX_RIGHT);
		addView(bl, INDEX_LEFT_BOTTOM);
		addView(br, INDEX_RIGHT_BOTTOM);
		
		mTvHide = new TextView(context);
		mTvHide.setTypeface(Typeface.DEFAULT_BOLD);
		mTvHide.setText("...");
		mTvHide.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		mTvHide.setVisibility(View.GONE);
		mTvHide.setGravity(Gravity.CENTER_HORIZONTAL);
		super.addView(mTvHide, INDEX_CLOSED);
		
		        
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int count = getChildCount();

		// Measurement will ultimately be computing these values.
		int maxHeight = 0;
		int maxWidth = 0;
		int childState = 0;

		int givenWidth = MeasureSpec.getSize(widthMeasureSpec);
		int givenHeight = MeasureSpec.getSize(heightMeasureSpec);
		
		View v = getChildAt(INDEX_TEXT);
		debug(String.format("measure spec w:%s h:%s", MeasureSpec.toString(widthMeasureSpec),MeasureSpec.toString(heightMeasureSpec)));
		v.measure(0, 0);
		mTvHeight = v.getMeasuredHeight();
		mTvWidth = v.getMeasuredWidth();
		if(mDefaultTopHeight < mTvHeight)
			mDefaultTopHeight = mTvHeight;
		maxWidth = mTvWidth + 2*mDefaultWidth;
		maxHeight = mDefaultTopHeight + mDefaultBottomHeight + mDefaultContentHeight;
		for (int i = DEFAULT_VIEWS_NUM; i < count; i++) {
			View child = getChildAt(i);
			if (child.getVisibility() != View.GONE) {
				// only allow child use the size which subtract outline size 
				child.measure(MeasureSpec.makeMeasureSpec(givenWidth-2*mDefaultWidth, MeasureSpec.EXACTLY), 
						MeasureSpec.makeMeasureSpec(givenHeight-mDefaultTopHeight-mDefaultBottomHeight, MeasureSpec.EXACTLY));
//				if (getOrientation() == LinearLayout.HORIZONTAL) {
//					maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
//					maxWidth = maxWidth + child.getMeasuredWidth();
//				} else {
//					maxHeight = maxHeight + child.getMeasuredHeight();
//					maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
//				}
				maxHeight = child.getMeasuredHeight();
				maxWidth = child.getMeasuredWidth();
				childState = combineMeasuredStates(childState, child.getMeasuredState());
				break;
			}
		}
		if(mShowContent) {
			mCustomHeight = maxHeight;
			mCustomWidth = maxWidth;
		} else {
			mCustomHeight = mDefaultContentHeight;
			mCustomWidth = maxWidth;
		}

		maxHeight = mCustomHeight+ mDefaultTopHeight + mDefaultBottomHeight + getPaddingTop()+getPaddingBottom();
		maxWidth = mCustomWidth + 2*mDefaultWidth+ getPaddingLeft()+getPaddingRight();

		debug(getLabel()+" maxHeight:" + maxHeight + " maxWidth:" + maxWidth);
		// Report our final dimensions.
		setMeasuredDimension(
				resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
				resolveSizeAndState(maxHeight, heightMeasureSpec,
						childState << MEASURED_HEIGHT_STATE_SHIFT));
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		debug(String.format("%s Layout size l:%d, t:%d, r:%d, b:%d",getLabel(), l, t, r, b));
		
		int width = r - l;
		int height = b - t;

		int ul_width, ur_width;
		ul_width = ur_width = width/2;
		
		
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
		
		mChildBottom = height - paddingBottom;
		mChildTop = paddingTop;
		mChildLeft = paddingLeft;
		mChildRight = width - paddingRight;
		
		debug(String.format("padding l:%d t:%d r:%d b:%d",  paddingLeft, paddingTop, paddingRight, paddingBottom));
		int childWidth = mChildRight - mChildLeft;
		int childHeight = mChildBottom - mChildTop;
		
		// check text view wider than total width
		if(mTvWidth > childWidth - 2 *(mDefaultWidth))
			mTvWidth = childWidth - 2 *(mDefaultWidth);
		
		switch (mLabelPos) {
			case LABEL_MIDDLE:
				ul_width = (childWidth - mTvWidth) /2+mLabelOffset;				
				ur_width = childWidth - ul_width -mTvWidth;
				break;
			case LABEL_LEFT:
				ul_width = mDefaultWidth+mLabelOffset;
				ur_width = childWidth -ul_width - mTvWidth;
				break;
			case LABEL_RIGHT:
				ur_width = mDefaultWidth+mLabelOffset;
				ul_width = childWidth -ur_width - mTvWidth;
				break;
		}
				
	
		View v = getChildAt(INDEX_LEFT_UP);

		childLayout(v, 0, mDefaultTopHeight/2, ul_width, mTvHeight);
		
		v = getChildAt(INDEX_RIGHT_UP);
		childLayout(v, childWidth - ur_width, mDefaultTopHeight/2, childWidth, mTvHeight);
		v = getChildAt(INDEX_TEXT);
		childLayout(v, ul_width, 0, ul_width+mTvWidth, mTvHeight);
		if (mCustomHeight > childHeight - mDefaultBottomHeight - mDefaultTopHeight)
			mCustomHeight = childHeight - mDefaultBottomHeight - mDefaultTopHeight;
		if (mCustomWidth > childWidth - 2 * mDefaultWidth)
			mCustomWidth = childWidth - 2 * mDefaultWidth;
	
		View left = getChildAt(INDEX_LEFT);

		childLayout(left, 0, mTvHeight, mDefaultWidth, mDefaultTopHeight + mCustomHeight);
		View right = getChildAt(INDEX_RIGHT);
		right.layout(childWidth - mDefaultWidth, mDefaultTopHeight, childWidth, mDefaultTopHeight
				+ mCustomHeight);
		childLayout(right, childWidth - mDefaultWidth, mDefaultTopHeight, childWidth, mDefaultTopHeight
				+ mCustomHeight);
		if(mShowContent) {
			// TODO this should according how many child added here
			v = getChildAt(DEFAULT_VIEWS_NUM);	
			if(v != null) {	
				childLayout(v, mDefaultWidth, mDefaultTopHeight, childWidth - mDefaultWidth,
						mDefaultTopHeight + mCustomHeight);
				debug(String.format(getLabel()+"content layout at l:%d t:%d, r:%d, b:%d",mDefaultWidth,mDefaultTopHeight, (r - mDefaultWidth), (mDefaultTopHeight + mCustomHeight)));
				if(v.getVisibility() != View.VISIBLE) {
					fadeInAnimation(v);
					v.setVisibility(View.VISIBLE);
				}
			}
			
		} else {
			v = getChildAt(DEFAULT_VIEWS_NUM);
			if(v != null) {
				if(v.getVisibility() == View.VISIBLE)
					fadeOutAnimation(v);
				v.setVisibility(View.INVISIBLE);
							
				childLayout(mTvHide, mDefaultWidth, mDefaultTopHeight-mDefaultContentHeight/2, childWidth - mDefaultWidth,
						mDefaultTopHeight + mCustomHeight-mDefaultContentHeight/2);
			}
		}

		v = getChildAt(INDEX_LEFT_BOTTOM);
		childLayout(v, 0, mDefaultTopHeight + mCustomHeight, childWidth / 2, childHeight);
		v = getChildAt(INDEX_RIGHT_BOTTOM);
		childLayout(v, childWidth / 2, mDefaultTopHeight + mCustomHeight, childWidth, childHeight);
		debug(String.format("%s bottom layout at l:%d t:%d, r:%d, b:%d",getLabel(),childWidth / 2, mDefaultTopHeight + mCustomHeight, r, b));
	}
	
	void childLayout(View v, int l, int t, int r, int b) {
		v.layout(l+mChildLeft, t+mChildTop, r+mChildLeft, b+mChildTop);
	}
	
	void fadeInAnimation(final View ll) {
		if(mEnableAnimation) {
			AnimationSet set = new AnimationSet(true);
			ScaleAnimation anim1 = new ScaleAnimation(0,1,0,1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.01f);
			anim1.setDuration(ANIMATION_DURATION);
			anim1.setZAdjustment(TranslateAnimation.ZORDER_BOTTOM);
			anim1.setInterpolator(new AccelerateInterpolator());		
			Animation anim2 = AnimationUtils.loadAnimation(
					getContext(), android.R.anim.fade_in);
			anim2.setDuration(ANIMATION_DURATION);
			anim2.setInterpolator(new AccelerateInterpolator());
			anim2.setAnimationListener(new AnimationListener() {
	
				@Override
				public void onAnimationEnd(Animation animation) {				
				}
	
				@Override
				public void onAnimationRepeat(Animation animation) {				
				}
	
				@Override
				public void onAnimationStart(Animation animation) {
					 mTvHide.setVisibility(View.GONE);
	
				}
				
			});
			set.addAnimation(anim1);
			set.addAnimation(anim2);
			ll.startAnimation(set);
		} else {
			mTvHide.setVisibility(View.GONE);
		}
	}
	
	void fadeOutAnimation(final View ll) {
		if(mEnableAnimation) {
			AnimationSet set = new AnimationSet(true);
			ScaleAnimation anim1 = new ScaleAnimation(1,0,1,0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.01f);
			anim1.setDuration(ANIMATION_DURATION);
			anim1.setZAdjustment(TranslateAnimation.ZORDER_BOTTOM);
			anim1.setInterpolator(new DecelerateInterpolator());
			Animation anim2 = AnimationUtils.loadAnimation(
					getContext(), android.R.anim.fade_out);
			anim2.setDuration(ANIMATION_DURATION);
			anim2.setInterpolator(new DecelerateInterpolator());
			set.addAnimation(anim1);
			set.addAnimation(anim2);
			anim1.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationEnd(Animation animation) {
					mTvHide.setVisibility(View.VISIBLE);				
				}
				@Override
				public void onAnimationRepeat(Animation animation) {
				}
				@Override
				public void onAnimationStart(Animation animation) {
				}
				
			});
			ll.startAnimation(set);
		} else {
			mTvHide.setVisibility(View.VISIBLE);
		}
	}
	
	public void enableAnimation(boolean enable) {
		mEnableAnimation = enable;
	}

	String getLabel() {
		return "["+mTvLabel.getText()+"]:";
	}
	
	void debug(String msg) {
		if(debug)
			Log.d(TAG,msg);
	}
	
	public void showContent(boolean enable) {
		mShowContent = enable;
		requestLayout();
	}
	
	public void enableHideContent(boolean enable) {
		mEnableHideContent = enable;
		if(mEnableHideContent) {
        	mTvLabel.setBackgroundResource(android.R.drawable.list_selector_background);
        	mTvLabel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {				
					mShowContent = ! mShowContent;				
					requestLayout();
				}
				
			});
		} else {
			mTvLabel.setOnClickListener(null);
		}
	}
}
