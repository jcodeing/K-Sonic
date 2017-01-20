//MIT License
//
//Copyright (c) 2016 Jcodeing <jcodeing@gmail.com>
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

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

    private LinearLayout mainLinearLayout;
    private LinearLayout leftLinearLayout;
    private LinearLayout centerLinearLayout;
    private LinearLayout rightLinearLayout;

    private Button plusButton;
    private Button minusButton;
    private EditText editText;

    private int editTextLayoutWidth;
    private int editTextLayoutHeight;
    private int editTextMinimumWidth;
    private int editTextMinimumHeight;
    private int editTextMinHeight;
    private int editTextHeight;

    // =========@Value@=========float/int
    public float minimumNumValue;
    public float maximumNumValue;
    private float num;

    private OnNumChangeListener onNumChangeListener;

    // ------------------------------K------------------------------@Construction
    public PlusMinusNum(Context context, AttributeSet attrs) {
        super(context, attrs);
        //layout xmlns:app="http://schemas.android.com/apk/res-auto"
        //values attrs declare-styleable
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PlusMinusNum);
        minimumNumValue = typedArray.getFloat(R.styleable.PlusMinusNum_minimumNumValue, 0.1f);
        maximumNumValue = typedArray.getFloat(R.styleable.PlusMinusNum_maximumNumValue, 2.0f);
        num = typedArray.getFloat(R.styleable.PlusMinusNum_numValue, 1.0f);
        typedArray.recycle();
        initialize();
    }

    public PlusMinusNum(Context context) {
        super(context);
        initialize();
    }

    // ------------------------------K------------------------------@Initialize
    private void initialize() {
        initETWithHeight();
        initView();
        setViewsLayoutParams();
        addAllView();
        setViewListener();
    }

    private void initETWithHeight() {
        editTextLayoutWidth = -1;
        editTextLayoutHeight = -1;
        editTextMinimumWidth = -1;
        editTextMinimumHeight = -1;
        editTextMinHeight = -1;
        editTextHeight = -1;
    }

    private void initView() {
        mainLinearLayout = new LinearLayout(getContext());
        leftLinearLayout = new LinearLayout(getContext());
        centerLinearLayout = new LinearLayout(getContext());
        rightLinearLayout = new LinearLayout(getContext());
        plusButton = new Button(getContext());
        minusButton = new Button(getContext());
        editText = new EditText(getContext());

        minusButton.setText("-");
        minusButton.setTextColor(0xff666666);
        minusButton.setTextScaleX(3f);
        minusButton.setTag("-");
        minusButton.setBackgroundResource(R.drawable.num_minus);
        minusButton.setPadding(0, 0, 0, 0);

        plusButton.setText("+");
        plusButton.setTextColor(0xff666666);
        // plusButton.setTextScaleX(1f);
        plusButton.setTag("+");
        plusButton.setBackgroundResource(R.drawable.num_plus);
        plusButton.setPadding(0, 0, 0, 0);

        editText.setInputType(InputType.TYPE_CLASS_NUMBER);//TYPE_NULL
        editText.setText(String.valueOf(num));
        editText.setTextColor(0xff229EBF);
        editText.setBackgroundResource(R.drawable.num_edit);
        editText.setPadding(0, 0, 0, 0);
        editText.setGravity(Gravity.CENTER);
    }

    private void setViewsLayoutParams() {
        LayoutParams viewLayoutParams = new LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        plusButton.setLayoutParams(viewLayoutParams);
        minusButton.setLayoutParams(viewLayoutParams);
        editText.setLayoutParams(viewLayoutParams);
        setETWidthHeight();

        // plusButton.setPadding(0, 0, 0, 0);
        // minusButton.setPadding(0, 0, 0, 0);
        // editText.setPadding(0, 0, 0, 0);

        viewLayoutParams.gravity = Gravity.CENTER;
        centerLinearLayout.setLayoutParams(viewLayoutParams);

        centerLinearLayout.setFocusable(true);
        centerLinearLayout.setFocusableInTouchMode(true);

        viewLayoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        viewLayoutParams.weight = 1.0f;
        leftLinearLayout.setLayoutParams(viewLayoutParams);
        rightLinearLayout.setLayoutParams(viewLayoutParams);

        viewLayoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        mainLinearLayout.setLayoutParams(viewLayoutParams);
        mainLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
    }

    private void setETWidthHeight() {
        // =========@setMinimumWidth Height@=========
        if (editTextMinimumWidth < 0) {
            editTextMinimumWidth = dpToPx(65f);
        }
        editText.setMinimumWidth(editTextMinimumWidth);

        if (editTextHeight > 0) {
            if (editTextMinHeight >= 0 && editTextMinHeight > editTextHeight) {
                editTextHeight = editTextMinHeight;
            }
            editText.setHeight(editTextHeight);
        }

        // =========@setLayoutParams@=========
        if (editTextLayoutHeight > 0) {
            if (editTextMinimumHeight > 0
                    && editTextMinimumHeight > editTextLayoutHeight) {
                editTextLayoutHeight = editTextMinimumHeight;
            }

            LayoutParams layoutParams = (LayoutParams) editText
                    .getLayoutParams();
            layoutParams.height = editTextLayoutHeight;
            editText.setLayoutParams(layoutParams);
        }

        if (editTextLayoutWidth > 0) {
            if (editTextMinimumWidth > 0
                    && editTextMinimumWidth > editTextLayoutWidth) {
                editTextLayoutWidth = editTextMinimumWidth;
            }

            LayoutParams layoutParams = (LayoutParams) editText
                    .getLayoutParams();
            layoutParams.width = editTextLayoutWidth;
            editText.setLayoutParams(layoutParams);
        }
    }

    private void addAllView() {
        mainLinearLayout.addView(leftLinearLayout, 0);
        mainLinearLayout.addView(centerLinearLayout, 1);
        mainLinearLayout.addView(rightLinearLayout, 2);

        leftLinearLayout.addView(minusButton);
        centerLinearLayout.addView(editText);
        rightLinearLayout.addView(plusButton);

        addView(mainLinearLayout);
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

    public void setEditTextMinimumWidth(int editTextMinimumWidth) {
        if (editTextMinimumWidth > 0) {
            this.editTextMinimumWidth = editTextMinimumWidth;
            editText.setMinimumWidth(editTextMinimumWidth);
        }

    }

    public void setEditTextMinimumHeight(int editTextMinimumHeight) {
        if (editTextMinimumHeight > 0) {
            this.editTextMinimumHeight = editTextMinimumHeight;
            editText.setMinimumHeight(editTextMinimumHeight);
        }
    }

    public void setEditTextMinHeight(int editTextMinHeight) {
        if (editTextMinHeight > 0) {
            this.editTextMinHeight = editTextMinHeight;
            editText.setMinHeight(editTextMinHeight);
        }
    }

    public void setEditTextHeight(int editTextHeight) {
        this.editTextHeight = editTextHeight;
        setETWidthHeight();
    }

    public void setEditTextLayoutWidth(int editTextLayoutWidth) {
        this.editTextLayoutWidth = editTextLayoutWidth;
        setETWidthHeight();
    }

    public void setEditTextLayoutHeight(int editTextLayoutHeight) {
        this.editTextLayoutHeight = editTextLayoutHeight;
        setETWidthHeight();
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
            if (v.getTag().equals("+")) {
                // The maximum value judgment
                if ((num * 10 + 1) / 10 > maximumNumValue) {//int: (++num > maximumNumValue)
                    Toast.makeText(getContext(),
                            "! > maximum(" + maximumNumValue + ")",
                            Toast.LENGTH_SHORT).show();
                } else {
                    num = (num * 10 + 1) / 10;
                    editText.setText(String.valueOf(num));

                    if (onNumChangeListener != null) {
                        onNumChangeListener.onNumChange(PlusMinusNum.this, num);
                    }
                }
            } else if (v.getTag().equals("-")) {
                // The minimum value judgment
                if ((num * 10 - 1) / 10 < minimumNumValue) {//int: (--num < minimumNumValue)
                    Toast.makeText(getContext(),
                            "! < minimum(" + minimumNumValue + ")",
                            Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(),
                                "! < minimum(" + minimumNumValue + ")",
                                Toast.LENGTH_SHORT).show();
                    } else if (numI > maximumNumValue) {
                        Toast.makeText(getContext(),
                                "! > maximum(" + maximumNumValue + ")",
                                Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "! illegal input", Toast.LENGTH_SHORT).show();
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
}
