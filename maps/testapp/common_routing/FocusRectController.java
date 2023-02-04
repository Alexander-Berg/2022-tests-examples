package com.yandex.maps.testapp.common_routing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;

import com.yandex.mapkit.ScreenPoint;
import com.yandex.mapkit.ScreenRect;
import com.yandex.maps.testapp.R;

import androidx.annotation.Nullable;

public class FocusRectController implements SeekBar.OnSeekBarChangeListener {
    public interface FocusRectControllerListener {
        void updateFocusRect();
    }

    public void start() {
        moveToTop();

        if (focusPoint != null) {
            focusPointView.setPosition(focusPoint);
            focusXSlider.setProgress((int)focusPoint.getX());
            focusYSlider.setProgress((int)focusPoint.getY());
        }

        leftBorderSlider.setProgress((int) rect.getTopLeft().getX());
        rightBorderSlider.setProgress(width - (int) rect.getBottomRight().getX() - 1);
        topBorderSlider.setProgress((int) rect.getTopLeft().getY());
        bottomBorderSlider.setProgress(height - (int) rect.getBottomRight().getY() - 1);

        backgroundShadow.setVisibility(View.VISIBLE);
        borderView.setVisibility(View.VISIBLE);
        buttonsView.setVisibility(View.VISIBLE);
    }

    public static class FocusPointView extends View {
        Paint paint = new Paint();

        private ScreenPoint focusPoint;
        private static final float focusPointCrossSize = 32.f;

        public FocusPointView(Context context, AttributeSet attrs) {
            super(context, attrs);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(4.f);
        }

        public void setPosition(ScreenPoint focusPoint) {
            this.focusPoint = focusPoint;
            invalidate();
        }

        public ScreenPoint position() {
            return focusPoint;
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (focusPoint != null) {
                canvas.drawLine(focusPoint.getX() - focusPointCrossSize / 2.f, focusPoint.getY() - focusPointCrossSize / 2.f,
                        focusPoint.getX() + focusPointCrossSize / 2.f, focusPoint.getY() + focusPointCrossSize / 2.f, paint);
                canvas.drawLine(focusPoint.getX() - focusPointCrossSize / 2.f, focusPoint.getY() + focusPointCrossSize / 2.f,
                        focusPoint.getX() + focusPointCrossSize / 2.f, focusPoint.getY() - focusPointCrossSize / 2.f, paint);
            }
        }
    }

    public FocusRectController(Context context, ViewGroup rootLayout,
                               FocusRectControllerListener listener,
                               ScreenRect rect, @Nullable ScreenPoint focusPoint,
                               int width, int height) {
        this.listener = listener;
        this.rect = rect;
        this.focusPoint = focusPoint;
        this.width = width;
        this.height = height;

        // https://stackoverflow.com/questions/20431089/removeview-not-working-after-layoutinflater-inflateresource-root-true
        mainView = LayoutInflater.from(context).inflate(R.layout.focus_rect_controller, rootLayout, false);
        rootLayout.addView(mainView);
        backgroundShadow = mainView.findViewById(R.id.background_shadow_image_view);
        borderView = mainView.findViewById(R.id.focus_rect_borders_layout);
        buttonsView = mainView.findViewById(R.id.focus_rect_buttons_layout);
        showBordersCheckbox = mainView.findViewById(R.id.show_borders_checkbox);
        hideController();

        leftBorder = mainView.findViewById(R.id.left_border_image);
        rightBorder = mainView.findViewById(R.id.right_border_image);
        topBorder = mainView.findViewById(R.id.top_border_image);
        bottomBorder = mainView.findViewById(R.id.bottom_border_image);

        leftBorderSlider = mainView.findViewById(R.id.left_border_slider);
        rightBorderSlider = mainView.findViewById(R.id.right_border_slider);
        topBorderSlider = mainView.findViewById(R.id.top_border_slider);
        bottomBorderSlider = mainView.findViewById(R.id.bottom_border_slider);

        leftBorderSlider.setOnSeekBarChangeListener(this);
        rightBorderSlider.setOnSeekBarChangeListener(this);
        topBorderSlider.setOnSeekBarChangeListener(this);
        bottomBorderSlider.setOnSeekBarChangeListener(this);

        if (focusPoint != null) {
            focusXSlider = mainView.findViewById(R.id.xpos_focus_point_slider);
            focusYSlider = mainView.findViewById(R.id.ypos_focus_point_slider);
            focusPointView = mainView.findViewById(R.id.focus_point_view);

            focusXSlider.setOnSeekBarChangeListener(this);
            focusYSlider.setOnSeekBarChangeListener(this);

            focusXSlider.setMax(width);
            focusYSlider.setMax(height);
        } else {
            mainView.findViewById(R.id.focus_point_configuration_layout).setVisibility(View.GONE);
        }

        leftBorderSlider.setMax(width / 2);
        rightBorderSlider.setMax(width / 2);
        topBorderSlider.setMax(height / 2);
        bottomBorderSlider.setMax(height / 2);

        Button cancelChangingButton = mainView.findViewById(R.id.cancel_borders_changing_button);
        cancelChangingButton.setOnClickListener(this::cancelBordersConfiguration);
        Button saveChangingButton = mainView.findViewById(R.id.save_borders_changing_button);
        saveChangingButton.setOnClickListener(this::saveBordersConfiguration);
    }

    public ScreenRect getRect() {
        return rect;
    }

    public ScreenPoint getFocusPoint() {
        return focusPoint;
    }

    private void hideController() {
        backgroundShadow.setVisibility(View.GONE);
        if (!showBordersCheckbox.isChecked())
            borderView.setVisibility(View.GONE);
        buttonsView.setVisibility(View.GONE);
    }

    private void cancelBordersConfiguration(View view) {
        hideController();
    }

    private void saveBordersConfiguration(View view) {
        hideController();

        rect = new ScreenRect(
                new ScreenPoint(leftBorderSlider.getProgress(), topBorderSlider.getProgress()),
                new ScreenPoint(width - rightBorderSlider.getProgress() - 1, height - bottomBorderSlider.getProgress() - 1)
        );
        if (focusPoint != null)
            focusPoint = focusPointView.position();
        listener.updateFocusRect();
    }

    private void updateFocusPointPosition() {
        if (focusPoint == null)
            return;
        int newX = focusXSlider.getProgress();
        int newY = focusYSlider.getProgress();
        newX = Math.max(newX, leftBorderSlider.getProgress());
        newX = Math.min(newX, width - rightBorderSlider.getProgress() - 1);
        newY = Math.max(newY, topBorderSlider.getProgress());
        newY = Math.min(newY, height - bottomBorderSlider.getProgress() - 1);

        focusXSlider.setProgress(newX);
        focusYSlider.setProgress(newY);
        focusPointView.setPosition(new ScreenPoint(newX, newY));
    }

    private void moveToTop() {
        ViewGroup parentView = (ViewGroup) mainView.getParent();
        parentView.removeView(mainView);
        parentView.addView(mainView);
    }

    private static void updateBorder(View border, int value, boolean width) {
        if (width)
            border.getLayoutParams().width = value;
        else
            border.getLayoutParams().height = value;
        border.requestLayout();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
        if (seekBar == leftBorderSlider)
            updateBorder(leftBorder, value, true);
        else if (seekBar == rightBorderSlider)
            updateBorder(rightBorder, value, true);
        else if (seekBar == topBorderSlider)
            updateBorder(topBorder, value, false);
        else if (seekBar == bottomBorderSlider)
            updateBorder(bottomBorder, value, false);

        if (fromUser)
            updateFocusPointPosition();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private FocusRectControllerListener listener;
    private View mainView, borderView, buttonsView, backgroundShadow;
    private CheckBox showBordersCheckbox;
    private View leftBorder, rightBorder, topBorder, bottomBorder;
    private SeekBar leftBorderSlider, rightBorderSlider, topBorderSlider, bottomBorderSlider;
    private SeekBar focusXSlider, focusYSlider;
    private FocusPointView focusPointView;
    private ScreenRect rect;
    private ScreenPoint focusPoint;
    private int width, height;
}
