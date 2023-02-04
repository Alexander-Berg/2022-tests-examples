package ru.auto.tests.desktop.mock.beans.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Predicate {

    Parameters deepEquals;
    Parameters equals;
    Parameters matches;
    Parameters contains;
    Parameters startsWith;
    Parameters endsWith;
    Parameters exists;
    List<Predicate> or;

    public static Predicate predicate() {
        return new Predicate();
    }

}
