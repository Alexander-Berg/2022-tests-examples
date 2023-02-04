package ru.yandex.realty.model.raw;

import org.junit.Assert;
import org.junit.Test;

public class RawViewReviewValidationTest {

    @Test
    public void testConversion() throws Exception {

        String[] matchedValues = new String[] {
        "http://www.youtube.com/watch?v=iwGFalTRHDA&feature=related",
        "http://youtu.be/iwGFalTRHDA",
        "http://www.youtube.com/embed/watch?feature=player_embedded&v=iwGFalTRHDA",
        "http://www.youtube.com/watch?v=iwGFalTRHDA",
        "http://youtu.be/t-ZRX8984sc",
        "https://www.youtube.com/watch?v=iwGFalTRHDA&list=PLDfKAXSi6kUZnATwAUfN6tg1dULU-7XcD"
        };
        String[] notMatchedValues = new String[] {
        "https://www.youtube.com/iwGFalTRHDA",
        "https://www.youtube.com/?f=iwGFalTRHDA",
        "https://www.youtuber.com/iwGFalTRHDA",
        "https://youtuber.com/iwGFalTRHDA",
        "https://youtube.ru/iwGFalTRHDA",
        "http://www.youtuber.com/watch?v=iwGFalTRHDA",
        "http://www.youtuber.com/watch?v=http://youtu.be/t-ZRX8984sc"
        };

        RawVideoReviewImpl object = new RawVideoReviewImpl();
        for (String value :
                matchedValues) {
            object.setYoutubeVideoReviewUrl(value);
            Assert.assertEquals(value, object.getYoutubeVideoReviewUrl());
        }

        object = new RawVideoReviewImpl();
        for (String value :
                notMatchedValues) {
            object.setYoutubeVideoReviewUrl(value);
            Assert.assertNull(object.getYoutubeVideoReviewUrl());
        }
    }
}
