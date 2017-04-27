/*
 * MIT License
 *
 * Copyright (c) 2017 K Sun <jcodeing@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.jcodeing.k_sonic;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class PlusMinusNum extends LinearLayout {

    // =========@View@=========
    private Button plusButton;
    private Button minusButton;
    private EditText editText;
    private LinearLayout centerLL;

    // =========@Config@=========
    private String plusButtonText = "＋";//"+"
    private String minusButtonText = "－";//"-"

    private int buttonWidth;
    private int ediTextMinimumWidth;
    // =========@Value@=========float/int
    public float minimumNumValue;
    public float maximumNumValue;
    private float num;

    private OnNumChangeListener onNumChangeListener;

    // ------------------------------K------------------------------@Construction
    public PlusMinusNum(Context context) {
        this(context, null);
    }

    public PlusMinusNum(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlusMinusNum(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //layout xmlns:app="http://schemas.android.com/apk/res-auto"
        //values attrs declare-styleable
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PlusMinusNum);
        minimumNumValue = typedArray.getFloat(R.styleable.PlusMinusNum_minimumNumValue, 0.1f);
        maximumNumValue = typedArray.getFloat(R.styleable.PlusMinusNum_maximumNumValue, 2.0f);
        num = typedArray.getFloat(R.styleable.PlusMinusNum_numValue, 1.0f);

        buttonWidth = typedArray.getLayoutDimension(R.styleable.PlusMinusNum_buttonWidth, dpToPx(38f));//getResources().getDimension(R.dimen.buttonWidthDefault)
        ediTextMinimumWidth = typedArray.getLayoutDimension(R.styleable.PlusMinusNum_ediTextMinimumWidth, dpToPx(45f));
        typedArray.recycle();

        initialize();
    }

    // ------------------------------K------------------------------@Initialize
    private void initialize() {
        initView();
        addAllView();
        setViewListener();
    }

    private void initView() {
        setOrientation(LinearLayout.HORIZONTAL);

        // =========@center Layout
        centerLL = new LinearLayout(getContext());
        editText = new EditText(getContext());
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);//TYPE_NULL
        editText.setText(String.valueOf(num));
        editText.setTextColor(0xff229EBF);
        editText.setBackgroundResource(R.drawable.num_edit);
        editText.setPadding(0, 0, 0, 0);
        editText.setGravity(Gravity.CENTER);
        editText.setMinimumWidth(ediTextMinimumWidth);
        LayoutParams ediTextLP = new LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        editText.setLayoutParams(ediTextLP);

        LayoutParams centerLP = new LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        centerLP.gravity = Gravity.CENTER;
        centerLL.setLayoutParams(centerLP);
        centerLL.setFocusable(true);
        centerLL.setFocusableInTouchMode(true);


        // =========@left/right Layout
        minusButton = new Button(getContext());
        minusButton.setText(minusButtonText);
        minusButton.setTextColor(0xff666666);
        minusButton.setTag(minusButtonText);
        minusButton.setBackgroundResource(R.drawable.num_minus);
        minusButton.setPadding(0, 0, 0, 0);

        plusButton = new Button(getContext());
        plusButton.setText(plusButtonText);
        plusButton.setTextColor(0xff666666);
        plusButton.setTag(plusButtonText);
        plusButton.setBackgroundResource(R.drawable.num_plus);
        plusButton.setPadding(0, 0, 0, 0);

        LayoutParams buttonLP = new LayoutParams(
                buttonWidth,
                LinearLayout.LayoutParams.MATCH_PARENT);
        plusButton.setLayoutParams(buttonLP);
        minusButton.setLayoutParams(buttonLP);
    }

    private void addAllView() {
        centerLL.addView(editText);

        addView(minusButton, 0);
        addView(centerLL, 1);
        addView(plusButton, 2);
    }

    private void setViewListener() {
        plusButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    stopTimer();
                return false;
            }
        });
        minusButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    stopTimer();
                return false;
            }
        });

        plusButton.setOnLongClickListener(new OnButtonClickLongListener());
        minusButton.setOnLongClickListener(new OnButtonClickLongListener());
        plusButton.setOnClickListener(new OnButtonClickListener());
        minusButton.setOnClickListener(new OnButtonClickListener());
        editText.addTextChangedListener(new OnTextChangeListener());
    }


    // ------------------------------K------------------------------@Set/Get
    public EditText getEditText() {
        return editText;
    }

    public void setText(CharSequence text) {
        editText.setText(text);
    }

    public void setNum(float num) {
        this.num = num;
        setText(String.valueOf(num));
    }

    public float getNum() {
        try {
            return Float.parseFloat(editText.getText().toString());
        } catch (Exception e) {//NumberFormat
            return 0;
        }
    }

    public void setEdiTextMinimumWidth(int ediTextMinimumWidth) {
        if (ediTextMinimumWidth > 0) {
            this.ediTextMinimumWidth = ediTextMinimumWidth;
            editText.setMinimumWidth(ediTextMinimumWidth);
        }

    }

    public void setButtonBgColor(int addBtnColor, int subBtnColor) {
        plusButton.setBackgroundColor(addBtnColor);
        minusButton.setBackgroundColor(subBtnColor);
    }

    public void setButtonBgResource(int addBtnResource, int subBtnResource) {
        plusButton.setBackgroundResource(addBtnResource);
        minusButton.setBackgroundResource(subBtnResource);
        plusButton.setText("");
        minusButton.setText("");
    }


    public void setOnNumChangeListener(OnNumChangeListener onNumChangeListener) {
        this.onNumChangeListener = onNumChangeListener;
    }

    public interface OnNumChangeListener {
        void onNumChange(View view, float num);
    }


    // ------------------------------K------------------------------@Calculate
    private void plusMinusNumber(View v) {
        String numString = editText.getText().toString();
        if (TextUtils.isEmpty(numString)) {
            num = minimumNumValue;
            editText.setText(String.valueOf(minimumNumValue));
            if (onNumChangeListener != null) {
                onNumChangeListener.onNumChange(PlusMinusNum.this, num);
            }
        } else {
            //default: float+0.1/float-0.1 | int+1/int-1
            //Later expansion: custom difference
            //Be careful: float+0.1/float-0.1 decimal precision problem
            if (v.getTag().equals(plusButtonText)) {
                // The maximum value judgment
                if ((num * 10 + 1) / 10 > maximumNumValue) {//int: (++num > maximumNumValue)
                    showToast("! > maximum(" + maximumNumValue + ")");
                } else {
                    num = (num * 10 + 1) / 10;
                    editText.setText(String.valueOf(num));

                    if (onNumChangeListener != null) {
                        onNumChangeListener.onNumChange(PlusMinusNum.this, num);
                    }
                }
            } else if (v.getTag().equals(minusButtonText)) {
                // The minimum value judgment
                if ((num * 10 - 1) / 10 < minimumNumValue) {//int: (--num < minimumNumValue)
                    showToast("! < minimum(" + minimumNumValue + ")");
                } else {
                    num = (num * 10 - 1) / 10;
                    editText.setText(String.valueOf(num));

                    if (onNumChangeListener != null) {
                        onNumChangeListener.onNumChange(PlusMinusNum.this, num);
                    }
                }
            }
        }
    }

    // ============================@Listener@============================
    class OnTextChangeListener implements TextWatcher {

        @Override
        public void afterTextChanged(Editable s) {
            String numString = s.toString();
            if (!TextUtils.isEmpty(numString)) {
                try {
                    float numI = Float.parseFloat(numString);
                    if (numI < minimumNumValue) {
                        showToast("! < minimum(" + minimumNumValue + ")");
                    } else if (numI > maximumNumValue) {
                        showToast("! > maximum(" + maximumNumValue + ")");
                    } else {
                        editText.setSelection(editText.getText().toString()
                                .length());
                        if (num != numI) {
                            num = numI;
                            if (onNumChangeListener != null) {
                                onNumChangeListener.onNumChange(PlusMinusNum.this, num);
                            }
                        }
                    }
                } catch (Exception e) {//NumberFormat
                    //illegal input
                    showToast("! illegal input");
                    setText("");
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {

        }

    }


    class OnButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            plusMinusNumber(v);
        }

    }

    class OnButtonClickLongListener implements OnLongClickListener {

        @Override
        public boolean onLongClick(View v) {
            curLongClickView = v;
            startTimer();
            return false;
        }
    }


    // ============================@Timer@============================Auto Plus/Minus
    private Timer mTimer;
    private TimerTask mTimerTask;
    private boolean mTimerStart = false;
    // View
    private View curLongClickView;

    public void startTimer() {
        if (mTimerStart) {
            return;
        }
        if (mTimer == null)
            mTimer = new Timer();
        mTimerTask = new MyTimerTask();
        mTimer.schedule(mTimerTask, 0, 300);
        mTimerStart = true;
    }

    public void stopTimer() {
        if (!mTimerStart) {
            return;
        }
        mTimerStart = false;
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
            mTimer = null;
        }
    }

    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            // Child Thread
            if (curLongClickView != null)
                curLongClickView.post(new Runnable() {
                    @Override
                    public void run() {
                        //Main Thread
                        plusMinusNumber(curLongClickView);
                    }
                });
        }
    }

    // ------------------------------K------------------------------@Assist
    public int dpToPx(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    private Toast toast;

    private void showToast(String str) {
        if (toast != null) {
            toast.cancel();
            toast = null;
        }
        toast = Toast.makeText(getContext(), str, Toast.LENGTH_SHORT);
        toast.show();
    }
}
