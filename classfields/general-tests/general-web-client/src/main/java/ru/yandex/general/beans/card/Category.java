package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Category {

    String id;
    String shortName;
    SearchLinks searchLinks;
    List<Category> parents;

}
