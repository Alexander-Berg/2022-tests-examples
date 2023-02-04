package ru.yandex.realty.beans.developer.slide;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

@Getter
@Setter
@Accessors(chain = true)
public class SlideResponse {

    public static final String SLIDE_TEMPLATE = "mock/developer/slideTemplate.json";

    int order;
    String type;
    String regionLink;
    Site site;
    Photo photo;
    String title;
    List<String> titlePriority;
    boolean button;
    String buttonText;
    SliderPhoto sliderPhoto;

    public static SlideResponse slide() {
        return new SlideResponse();
    }

    public static SlideResponse slideTemplate() {
        return new GsonBuilder().create().fromJson(getResourceAsString(SLIDE_TEMPLATE), SlideResponse.class);
    }

    public SlideResponse withSiteId(String id) {
        getSite().setId(id);
        return this;
    }

    public SlideResponse withSiteName(String name) {
        getSite().setName(name);
        return this;
    }

    public SlideResponse withSiteRgid(String rgid) {
        getSite().setRgid(rgid);
        return this;
    }


}
