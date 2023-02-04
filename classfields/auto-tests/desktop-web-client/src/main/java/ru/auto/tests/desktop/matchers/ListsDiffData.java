package ru.auto.tests.desktop.matchers;

import io.qameta.allure.attachment.AttachmentData;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;

/**
 * @author kurau (Yuri Kalinin)
 */
@Accessors(chain = true)
public class ListsDiffData implements AttachmentData {

    @Getter
    @Setter
    private Collection common;

    @Getter
    @Setter
    private Collection expected;

    @Getter
    @Setter
    private Collection actual;

    @Override
    public String getName() {
        return " Two Lists Diff ";
    }

    public static ListsDiffData listsDiffData() {
        return new ListsDiffData();
    }
}
