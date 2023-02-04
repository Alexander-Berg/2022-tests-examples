package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Salary {

    String salaryRur;

    public static Salary salary(String value) {
        return new Salary().setSalaryRur(value);
    }

}
