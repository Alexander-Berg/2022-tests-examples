package ru.yandex.solomon.model.timeseries;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class TimelineTest {

    @Test
    public void dropStepsShorterThan() {
        {
            Assert.assertEquals(new Timeline(), new Timeline().dropStepsShorterThan(10));
        }
        {
            Timeline orig = new Timeline(new long[]{ 100, 200, 300, 350, 400, 500 }, SortedOrCheck.SORTED_UNIQUE);
            Timeline shorter = orig.dropStepsShorterThan(100);
            Timeline expected = new Timeline(new long[]{ 100, 200, 300, 400, 500 }, SortedOrCheck.SORTED_UNIQUE);
            Assert.assertEquals(expected, shorter);
        }
        {
            Assert.assertEquals(
                    new Timeline(new long[0], SortedOrCheck.SORTED_UNIQUE),
                    new Timeline(new long[0], SortedOrCheck.SORTED_UNIQUE).dropStepsShorterThan(100));
            Assert.assertEquals(
                    new Timeline(new long[]{ 1000 }, SortedOrCheck.SORTED_UNIQUE),
                    new Timeline(new long[]{ 1000 }, SortedOrCheck.SORTED_UNIQUE).dropStepsShorterThan(100));
            Assert.assertEquals(
                    new Timeline(new long[]{ 1000 }, SortedOrCheck.SORTED_UNIQUE),
                    new Timeline(new long[]{ 1000 }, SortedOrCheck.SORTED_UNIQUE).dropStepsShorterThan(10000));
            Assert.assertEquals(
                    new Timeline(new long[]{ 1000, 2000 }, SortedOrCheck.SORTED_UNIQUE),
                    new Timeline(new long[]{ 1000, 2000 }, SortedOrCheck.SORTED_UNIQUE).dropStepsShorterThan(100));
            Assert.assertEquals(
                    new Timeline(new long[]{ 1000, 2000 }, SortedOrCheck.SORTED_UNIQUE),
                    new Timeline(new long[]{ 1000, 2000 }, SortedOrCheck.SORTED_UNIQUE).dropStepsShorterThan(10000));
        }
    }

    private static class NavigatorTester {
        private final Timeline timeline;
        private final Timeline.Navigator navigator;

        public NavigatorTester(long[] timeline) {
            this.timeline = new Timeline(timeline, SortedOrCheck.SORTED_UNIQUE);
            this.navigator = this.timeline.new Navigator();
        }

        public void scrollAndCheckPos(long scrollTo, int expectedPos) {
            navigator.scrollToMillis(scrollTo);
            Assert.assertEquals(expectedPos, navigator.pos());
        }
    }

    @Test
    public void navigator() {
        NavigatorTester tester = new NavigatorTester(new long[]{ 100, 200, 300, 400, 500, 500, 600 });
        tester.scrollAndCheckPos(50, 0);
        tester.scrollAndCheckPos(100, 0);
        tester.scrollAndCheckPos(150, 0);
        tester.scrollAndCheckPos(200, 1);
        tester.scrollAndCheckPos(300, 2);
        tester.scrollAndCheckPos(350, 2);
        tester.scrollAndCheckPos(450, 3);
        tester.scrollAndCheckPos(600, 6);
        tester.scrollAndCheckPos(800, 6);
    }

}
