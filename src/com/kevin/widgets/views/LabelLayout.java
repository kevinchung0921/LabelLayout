package com.kevin.widgets.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kevin.widgets.R;


/*
 *  This widget constructed by 10 views, but only [label] and [custom] could 
 *  be configured. Other views are just decorate.  
 * 			[up_left_corner]  [+/-] [label]    [up_right_corner]
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
	static final int INDEX_SHOW_CONTROL = 1;
	static final int INDEX_TEXT = 2;
	static final int INDEX_RIGHT_UP = 3;
	static final int INDEX_LEFT = 4;
	static final int INDEX_RIGHT = 5;
	static final int INDEX_LEFT_BOTTOM = 6;
	static final int INDEX_RIGHT_BOTTOM = 7;
	static final int INDEX_CONTENT = 8;
	static final int INDEX_CLOSED = 9;	
	static final int DEFAULT_VIEWS_NUM = 10;

	float dpToPx = 1;

	int mTvHeight = 0;
	int mTvWidth = 0;

	int mCustomHeight = 0;
	int mCustomWidth = 0;


	int mDefaultWidth = 12; // at least 12dp to have better looking
	int mDefaultTopHeight = 0;
	int mDefaultBottomHeight = 0;
	int mDefaultContentHeight = 0;
	int mLabelSize = 18;
	static final int ANIMATION_DURATION = 150; 
	
	boolean mContentIsOpen = true;
	TextView mTvLabel = null;
	TextView mTvHide = null;
	ImageView mShowControl = null;
	LinearLayout mContent = null;

	
	// unit dip
	static final int SIDE_WIDTH = 2;
	static final int ROW_TOP_HEIGHT = 6;
	static final int ROW_BOTTOM_HEIGHT = 12;
	static final int ROW_HIDE_HEIGHT = 15;
	
	int mLabelPos;
	int mLabelOffset;
	String mLabel = "";
	
	
	boolean debug = false;
	
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
		Log.d(TAG,"LabelLayout()");
		mTvLabel = new TextView(context, attrs, defStyle);
		mShowControl = new ImageView(context);
		mContent = new LinearLayout(context, attrs, defStyle);
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OutlineLayoutConfig);
		mLabelPos = a.getInt(R.styleable.OutlineLayoutConfig_label_pos, LABEL_MIDDLE);
        mLabelOffset = (int)a.getDimensionPixelSize(R.styleable.OutlineLayoutConfig_label_offset, 0);
        debug("mLabelOffset:"+mLabelOffset);
        mEnableAnimation = a.getBoolean(R.styleable.OutlineLayoutConfig_animation, true);
        mLabelSize *= dpToPx;
        canHideContent(a.getBoolean(R.styleable.OutlineLayoutConfig_canHideContent, true));        
        a.recycle();     
		
		dpToPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
				getResources().getDisplayMetrics());
		
		mDefaultWidth = (int) (mDefaultWidth * dpToPx);
		mDefaultTopHeight = (int) (ROW_TOP_HEIGHT * dpToPx);
		mDefaultBottomHeight = (int) (ROW_BOTTOM_HEIGHT * dpToPx);;
		mDefaultContentHeight = (int)(ROW_HIDE_HEIGHT*dpToPx);
				
		mTvLabel.setSingleLine(true);
		mTvLabel.setEllipsize(TruncateAt.END);
				
//		mTvLabel.setText(mLabel);
//		mTvLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, mLabelSize);
        
		  
		if(!mTvLabel.getText().toString().equals(""))
			mTvLabel.setPadding(4, 0, 4, 0); // reset padding to prevent it got impacted from parent ViewGroup
		else
			mTvLabel.setPadding(0, 0, 0, 0);
		mContent.setPadding(0, 0, 0, 0); // clean content layout's padding
//		mContent.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
		mTvLabel.setGravity(Gravity.CENTER);
        
		
		mShowControl.setBackgroundResource(R.drawable.collapse_selector);		
		mShowControl.setScaleType(ScaleType.FIT_XY);
		// default is opened, set it selected
		mShowControl.setSelected(true);
		
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
		
		super.addView(ul, INDEX_LEFT_UP);
		super.addView(mShowControl, INDEX_SHOW_CONTROL);
		super.addView(mTvLabel, INDEX_TEXT);
		super.addView(ur, INDEX_RIGHT_UP);
		super.addView(l, INDEX_LEFT);
		super.addView(r, INDEX_RIGHT);
		super.addView(bl, INDEX_LEFT_BOTTOM);
		super.addView(br, INDEX_RIGHT_BOTTOM);
		super.addView(mContent, INDEX_CONTENT);
		
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

		Log.d(TAG, "child count:"+count);
		// Measurement will ultimately be computing these values.
		int contentHeight = 0;
		int contentWidth = 0;
		int childState = 0;

		int givenWidth = MeasureSpec.getSize(widthMeasureSpec);
		int givenHeight = MeasureSpec.getSize(heightMeasureSpec);
		
		int reportHeight = givenWidth;
		int reportWidth = givenHeight;
		
		View v = getChildAt(INDEX_TEXT);
		debug(String.format("measure spec w:%s h:%s", MeasureSpec.toString(widthMeasureSpec),MeasureSpec.toString(heightMeasureSpec)));
		if(v != null) {
			v.measure(0, 0);
			mTvHeight = v.getMeasuredHeight();
			mTvWidth = v.getMeasuredWidth();
		}
		if(mDefaultTopHeight < mTvHeight)
			mDefaultTopHeight = mTvHeight;		
		contentWidth = mTvWidth + 2*mDefaultWidth;
		contentHeight = mDefaultTopHeight + mDefaultBottomHeight + mDefaultContentHeight;
		if(mEnableHideContent) {
			mShowControl.getLayoutParams().height = mDefaultTopHeight;
			mShowControl.getLayoutParams().width = mDefaultTopHeight;
		}
		int childCount = count-DEFAULT_VIEWS_NUM;
		if(childCount > 0) {
			View childs[] = new View[childCount];
			for (int i = 0; i <childCount; i++) 
				childs[i] = getChildAt(i+DEFAULT_VIEWS_NUM);			
			for (int i=0;i <childs.length;i++) {
				this.removeView(childs[i]);
				mContent.addView(childs[i]);
			}
		}
		LayoutParams params = getLayoutParams();
		int specW = MeasureSpec.EXACTLY;
		int specH = MeasureSpec.EXACTLY;
		if(params.height == LayoutParams.WRAP_CONTENT)
			specH = MeasureSpec.AT_MOST;
		if(params.width == LayoutParams.WRAP_CONTENT)
			specH = MeasureSpec.AT_MOST;
		
		mContent.measure(MeasureSpec.makeMeasureSpec(givenWidth-2*mDefaultWidth-getPaddingLeft()-getPaddingRight(), MeasureSpec.getMode(widthMeasureSpec)), 
						MeasureSpec.makeMeasureSpec(givenHeight-mDefaultTopHeight-mDefaultBottomHeight-getPaddingTop()-getPaddingBottom(), MeasureSpec.getMode(heightMeasureSpec)));
		
		contentHeight = mContent.getMeasuredHeight();
		contentWidth = mContent.getMeasuredWidth();


		if(mContentIsOpen) {
			mCustomHeight = contentHeight;
			mCustomWidth = contentWidth;
		} else {
			mCustomHeight = mDefaultContentHeight;
			mCustomWidth = contentWidth;
		}

		contentHeight = mCustomHeight+ mDefaultTopHeight + mDefaultBottomHeight + getPaddingTop()+getPaddingBottom();
		contentWidth = mCustomWidth + 2*mDefaultWidth+ getPaddingLeft()+getPaddingRight();

		debug(getLabel()+" maxHeight:" + contentHeight + " maxWidth:" + contentWidth);
		
		if(params.height == LayoutParams.WRAP_CONTENT)
			reportHeight = contentHeight;
		if(params.width == LayoutParams.WRAP_CONTENT)
			reportWidth = contentWidth;
		// Report our final dimensions.
		setMeasuredDimension(
				resolveSizeAndState(reportWidth, widthMeasureSpec, childState),
				resolveSizeAndState(reportHeight, heightMeasureSpec,
						childState << MEASURED_HEIGHT_STATE_SHIFT));
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		debug(String.format("%s Layout size l:%d, t:%d, r:%d, b:%d",getLabel(), l, t, r, b));
		
		int icon_size = 0;
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
		
		if(mEnableHideContent)
			icon_size = mDefaultTopHeight;
		
		// check text view wider than total width
		if(mTvWidth > childWidth - 2 *(mDefaultWidth))
			mTvWidth = childWidth - 2 *(mDefaultWidth);
		
		switch (mLabelPos) {
			case LABEL_MIDDLE:
				ul_width = (childWidth - mTvWidth) /2+mLabelOffset;			
				ur_width = childWidth - ul_width -mTvWidth;
				ul_width -= icon_size/2;
				ur_width -= icon_size/2;
				break;
			case LABEL_LEFT:
				ul_width = mDefaultWidth+mLabelOffset;
				ur_width = childWidth -ul_width - mTvWidth - icon_size;				
				break;
			case LABEL_RIGHT:
				ur_width = mDefaultWidth+mLabelOffset;
				ul_width = childWidth -ur_width - mTvWidth - icon_size;
				break;
		}
		
		View v = getChildAt(INDEX_LEFT_UP);

		childLayout(v, 0, mDefaultTopHeight/2, ul_width, mTvHeight);
		
		v = getChildAt(INDEX_RIGHT_UP);
		childLayout(v, childWidth - ur_width, mDefaultTopHeight/2, childWidth, mTvHeight);
		
		v = getChildAt(INDEX_SHOW_CONTROL);
		if(mEnableHideContent) {
			v.setVisibility(View.VISIBLE);
			childLayout(v, ul_width, 0, icon_size+ul_width, icon_size);
		} else {
			v.setVisibility(View.GONE);
		}
		
		v = getChildAt(INDEX_TEXT);
		childLayout(v, ul_width+icon_size, 0, ul_width+mTvWidth+icon_size, mTvHeight);
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
		if(mContentIsOpen) {
			// TODO this should according how many child added here
//			v = getChildAt(DEFAULT_VIEWS_NUM);	
			v = getChildAt(INDEX_CONTENT);
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
			v = getChildAt(INDEX_CONTENT);
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
		if(v != null)
			v.layout(l+mChildLeft, t+mChildTop, r+mChildLeft, b+mChildTop);
		else {
			Log.e(TAG, "Child is null!");
		}
	}
	
	void fadeInAnimation(final View ll) {
		if(mEnableAnimation) {
			AnimationSet set = new AnimationSet(true);
			ScaleAnimation anim1 = new ScaleAnimation(0,1,0,1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.01f);
			anim1.setDuration(ANIMATION_DURATION);
			anim1.setZAdjustment(TranslateAnimation.ZORDER_BOTTOM);
//			anim1.setInterpolator(new AccelerateInterpolator());		
			Animation anim2 = AnimationUtils.loadAnimation(
					getContext(), android.R.anim.fade_in);
			anim2.setDuration(ANIMATION_DURATION);
//			anim2.setInterpolator(new AccelerateInterpolator());
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
//			anim1.setInterpolator(new DecelerateInterpolator());
			Animation anim2 = AnimationUtils.loadAnimation(
					getContext(), android.R.anim.fade_out);
			anim2.setDuration(ANIMATION_DURATION);
//			anim2.setInterpolator(new DecelerateInterpolator());
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
		mContentIsOpen = enable;
		if(mShowControl != null)
			mShowControl.setSelected(mContentIsOpen);		
		requestLayout();
	}
	
	public boolean getContentOpen() {
		return mContentIsOpen;
	}
	
	public void canHideContent(boolean enable) {
		mEnableHideContent = enable;
		if(mEnableHideContent) {
        	mTvLabel.setBackgroundResource(android.R.drawable.list_selector_background);
        	mTvLabel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {				
					mContentIsOpen = ! mContentIsOpen;
					if(mShowControl != null)
						mShowControl.setSelected(mContentIsOpen);
					requestLayout();
				}
				
			});
        	mShowControl.setOnClickListener(new OnClickListener() {
        		@Override
				public void onClick(View v) {				
					mContentIsOpen = ! mContentIsOpen;
					if(mShowControl != null)
						mShowControl.setSelected(mContentIsOpen);
					requestLayout();
				}
        	});
//        	mTvLabel.setPaintFlags(mTvLabel.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
		} else {
			mTvLabel.setOnClickListener(null);
//			mTvLabel.setPaintFlags(mTvLabel.getPaintFlags() &   ~Paint.UNDERLINE_TEXT_FLAG);
		}
	}
	// override remove all views to prevent outline views remvoed
	@Override
	public void removeAllViews() {
		if(mContent != null) {
			mContent.removeAllViews();
		}
	}
	
	@Override
	public void addView(View v) {
		if(mContent != null)
			mContent.addView(v);
	}
}
