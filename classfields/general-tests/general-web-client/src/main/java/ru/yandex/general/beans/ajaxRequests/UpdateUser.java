package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.yandex.general.beans.ajaxRequests.User.user;

@Setter
@Getter
@Accessors(chain = true)
public class UpdateUser {

    User user;

    public static UpdateUser updateUser() {
        return new UpdateUser();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
