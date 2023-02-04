package ru.auto.tests.desktop.mock.beans.carfaxReport;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CommentsInfo {

    Commentable commentable;

    public static CommentsInfo commentsInfo() {
        return new CommentsInfo();
    }

}
