package ru.yandex.realty.beans.developer.office;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Logotype {

    private String minicard;
    private String main;
    private String alike;
    private String large;
    private String cosmic;
    private String appMiddle;
    private String appLarge;
    private String appSnippetMini;
    private String appSnippetSmall;
    private String appSnippetMiddle;
    private String appSnippetLarge;
    private String optimize;

}
