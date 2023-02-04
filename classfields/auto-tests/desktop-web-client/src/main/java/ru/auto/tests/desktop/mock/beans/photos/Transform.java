package ru.auto.tests.desktop.mock.beans.photos;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Transform {

    Integer angle;
    Boolean blur;

    public static Transform transform() {
        return new Transform();
    }

}
