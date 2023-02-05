package com.yandex.mail.yables;

import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;

import com.yandex.mail.R;
import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;

import static com.yandex.mail.util.Utils.arrayToList;
import static com.yandex.mail.yables.YableReflowViewAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class YableReflowViewTest {

    public YableReflowView createReflow() {
        YableReflowView reflow = (YableReflowView) LayoutInflater.from(
                new ContextThemeWrapper(IntegrationTestRunner.app(), R.style.YaTheme_Compose_Light)
        ).inflate(R.layout.compose_recipients, null);
        reflow.viewBinding.copyEditText.setContainer(reflow);
        return reflow;
    }

    @Test
    public void testCreates() {
        YableReflowView reflow = createReflow();
        Collection<String> addresses = arrayToList("test@ya.ru", "test2@ya.ru", "test@@@@");
        for (String address : addresses) {
            reflow.createYable(address, true);
        }

        String text = reflow.getText();
        for (String address : addresses) {
            assertThat(text).contains(address);
        }
    }

    @Test
    public void testRemovesAll() {
        YableReflowView reflow = createReflow();
        Collection<String> addresses = arrayToList("test@ya.ru", "test2@ya.ru", "test@@@@");
        for (String address : addresses) {
            reflow.createYable(address, true);
        }
        reflow.removeAllYables();

        String text = reflow.getText();
        for (String address : addresses) {
            assertThat(text).doesNotContain(address);
        }
    }

    @Test
    public void testShrinks() {
        YableReflowView reflow = createReflow();
        Collection<String> addresses = arrayToList("test@ya.ru", "test2@ya.ru", "test@@@@");
        for (String address : addresses) {
            reflow.createYable(address, true);
        }

        reflow.collapse();

        assertThat(reflow).isCollapsed();
    }

    @Test
    public void testExpands() {
        YableReflowView reflow = createReflow();
        Collection<String> addresses = arrayToList("test@ya.ru", "test2@ya.ru", "test@@@@");
        for (String address : addresses) {
            reflow.createYable(address, true);
        }

        reflow.collapse(); // TODO is it necessary?
        reflow.expand();

        assertThat(reflow).isExpanded();
    }

    @Test
    public void testRemovesOnTap() {
        final YableReflowView reflow = createReflow();
        ArrayList<String> addresses = arrayToList("test@ya.ru", "test2@ya.ru", "test3@ya.ru");
        for (String address : addresses) {
            reflow.createYable(address, false);
        }

        // TODO: in what state do we start?
        reflow.collapse(); // TODO is it necessary?
        reflow.expand();

        YableView yable = reflow.getChildYables().get(1);
        tapYable(yable);
        yable.findViewById(R.id.yable_delete_icon).performClick();
        reflow.collapse();

        assertThat(reflow).isCollapsed();
    }

    // TODO not sure if it is reliable...
    public static void tapYable(YableView yable) {
        MotionEvent down = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_DOWN, yable.getX(), yable.getY(), 0);
        yable.onTouchEvent(down);
        MotionEvent up = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_UP, yable.getX(), yable.getY(), 0);
        yable.onTouchEvent(up);
    }

    @Test
    public void testSelectedYable() {
        final YableReflowView reflow = createReflow();
        // '\n' to test MOBILEMAIL-9990
        ArrayList<String> addresses = arrayToList("\ntest@ya.ru", "test2@ya.ru");
        for (String address : addresses) {
            reflow.createYable(address, false);
        }

        reflow.collapse(); // TODO is it necessary?
        reflow.expand();

        YableView yable = reflow.getChildYables().get(0);
        tapYable(yable);

        assertThat(reflow.getSelectedView()).isSameAs(yable);
    }

    // TODO test for showing real name on tap
}
