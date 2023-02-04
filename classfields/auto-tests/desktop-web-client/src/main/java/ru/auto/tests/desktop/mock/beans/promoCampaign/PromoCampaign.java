package ru.auto.tests.desktop.mock.beans.promoCampaign;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class PromoCampaign {

    Campaign campaign;

    public static PromoCampaign promoCampaign() {
        return new PromoCampaign();
    }

}
