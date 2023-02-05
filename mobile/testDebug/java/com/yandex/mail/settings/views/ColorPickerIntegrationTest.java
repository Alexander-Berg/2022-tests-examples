package com.yandex.mail.settings.views;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.view.MotionEvent;

import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import androidx.annotation.NonNull;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.yandex.mail.asserts.ViewConditions.measuredSize;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SuppressLint("WrongCall")
@RunWith(IntegrationTestRunner.class)
public class ColorPickerIntegrationTest {

    @SuppressWarnings("NullableProblems") // @Before
    @Mock
    @NonNull
    private Canvas mockCanvas;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private ColorPicker view;

    @Before
    public void beforeEachTest() {
        MockitoAnnotations.initMocks(this);
        view = new ColorPicker(IntegrationTestRunner.app());
    }

    @Test
    public void onMeasure_shouldNotTakeSpaceOnEmptyColorList() {
        view.setVerticalOffset(0);
        view.setHorizontalOffset(0);
        view.measure(makeMeasureSpec(480, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
        assertThat(view).is(measuredSize(480, 0));
    }

    @Test
    public void onMeasure_shouldTakeRightSpaceOnNonEmptyList() {
        view.setVerticalOffset(0);
        view.setHorizontalOffset(0);
        view.setCircleRadius(10);
        view.setColors(listOf(0xFFFFFF, 0x000000));

        view.measure(makeMeasureSpec(480, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
        assertThat(view).is(measuredSize(480, 40));
    }

    @Test
    public void onDraw_shouldDrawCircles() {
        view.setVerticalOffset(0);
        view.setHorizontalOffset(0);
        view.setColors(listOf(0xFFFFFF));
        view.setCircleRadius(10);

        view.measure(makeMeasureSpec(480, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
        view.onDraw(mockCanvas);

        verify(mockCanvas).drawCircle(240, 10, 10, view.paint);
    }

    @Test
    public void onTouch_shouldChangeChosenColor() {
        view.setVerticalOffset(0);
        view.setHorizontalOffset(0);
        view.setColors(listOf(0xFFFFFF, 0x000000));
        view.setCircleRadius(10);
        view.measure(makeMeasureSpec(480, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));

        view.onTouchEvent(getMotionDownEventByCoordinates(235, 10));
        view.onTouchEvent(getMotionUpEventByCoordinates(235, 10));

        assertThat(view.getColor()).isEqualTo(0xFFFFFF);

        view.onTouchEvent(getMotionDownEventByCoordinates(245, 30));
        view.onTouchEvent(getMotionUpEventByCoordinates(245, 30));

        assertThat(view.getColor()).isEqualTo(0x000000);
    }

    @Test
    public void onTouch_shouldNotChangeColorIfNotInTheArea() {
        view.setVerticalOffset(0);
        view.setHorizontalOffset(0);
        view.setColors(listOf(0xFFFFFF, 0x000000));
        view.setCircleRadius(10);
        view.measure(makeMeasureSpec(480, EXACTLY), makeMeasureSpec(100, EXACTLY));

        assertThat(view.getColor()).isEqualTo(0xFFFFFF);

        view.onTouchEvent(getMotionDownEventByCoordinates(480, 100));
        view.onTouchEvent(getMotionUpEventByCoordinates(480, 100));

        assertThat(view.getColor()).isEqualTo(0xFFFFFF);
    }

    @Test
    public void onTouch_shouldNotChangeColorOnLongMove() {
        view.setVerticalOffset(0);
        view.setHorizontalOffset(0);
        view.setColors(listOf(0xFFFFFF, 0x000000));
        view.setCircleRadius(10);
        view.measure(makeMeasureSpec(480, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));

        view.onTouchEvent(getMotionDownEventByCoordinates(240, 10));
        view.onTouchEvent(getMotionUpEventByCoordinates(240, 10));

        assertThat(view.getColor()).isEqualTo(0xFFFFFF);

        view.onTouchEvent(getMotionDownEventByCoordinates(240, 30));
        view.onTouchEvent(getMotionMoveEventByCoordinates(480, 30));
        view.onTouchEvent(getMotionUpEventByCoordinates(480, 30));

        assertThat(view.getColor()).isEqualTo(0xFFFFFF);
    }

    @Test
    public void addExtraColor_shouldChangeNumberOfColors() {
        view.setColors(listOf(0x111111, 0x222222));
        view.addExtraColor(0x333333);

        assertThat(view.circles.size()).isEqualTo(3);
    }

    @Test
    public void addExtraColor_shouldNotChangeNumberOfColorsIfColorPresent() {
        view.setColors(listOf(0x111111, 0x222222));
        view.addExtraColor(0x222222);

        assertThat(view.circles.size()).isEqualTo(2);
    }

    @Test
    public void addExtraColor_shouldDrawCorrectlyOnEvenNumberOfColors() {
        view.setVerticalOffset(0);
        view.setHorizontalOffset(0);
        view.setColors(listOf(0x111111, 0x222222));
        view.addExtraColor(0x333333);
        view.setCircleRadius(10);

        view.measure(makeMeasureSpec(480, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
        view.onDraw(mockCanvas);

        verify(mockCanvas).drawCircle(240, 10, 10, view.paint);
        verify(mockCanvas).drawCircle(230, 30, 10, view.paint);
        verify(mockCanvas).drawCircle(250, 30, 10, view.paint);
    }

    @Test
    public void addExtraColor_shouldDrawCorrectlyOnOddNumberOfColors() {
        view.setVerticalOffset(0);
        view.setHorizontalOffset(0);
        view.setColors(listOf(0x111111, 0x222222, 0x333333));
        view.addExtraColor(0x444444);
        view.setCircleRadius(10);

        view.measure(makeMeasureSpec(480, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
        view.onDraw(mockCanvas);

        verify(mockCanvas).drawCircle(225, 10, 10, view.paint);
        verify(mockCanvas).drawCircle(245, 10, 10, view.paint);
        verify(mockCanvas).drawCircle(235, 30, 10, view.paint);
        verify(mockCanvas).drawCircle(255, 30, 10, view.paint);
    }

    @NonNull
    private MotionEvent getMotionDownEventByCoordinates(int x, int y) {
        return MotionEvent.obtain(1, 1, MotionEvent.ACTION_DOWN, x, y, 0);
    }

    @NonNull
    private MotionEvent getMotionMoveEventByCoordinates(int x, int y) {
        return MotionEvent.obtain(1, 1, MotionEvent.ACTION_MOVE, x, y, 0);
    }

    @NonNull
    private MotionEvent getMotionUpEventByCoordinates(int x, int y) {
        return MotionEvent.obtain(1, 1, MotionEvent.ACTION_UP, x, y, 0);
    }
}
