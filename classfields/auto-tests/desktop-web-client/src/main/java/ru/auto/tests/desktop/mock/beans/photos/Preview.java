package ru.auto.tests.desktop.mock.beans.photos;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Preview {

    Integer version;
    Integer width;
    Integer height;
    String data;

    public static Preview preview() {
        return new Preview();
    }

}
