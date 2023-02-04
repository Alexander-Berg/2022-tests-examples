package ru.yandex.general.consts;

import lombok.Getter;

public class UserStatus {

    private UserStatus() {
    }

    public static final String BANNED = "Banned";
    public static final String OK = "Ok";

    @Getter
    public enum UserBanDescriptions {

        FRAUD("Недобросовестные действия", "Мы узнали, что вы недобросовестно поступаете с продавцами, покупателями или соискателями. Правила сайта строго запрещают любые нечестные действия, поэтому ваш кабинет и все объявления заблокированы. Создавать новые публикации вы тоже не можете."),
        SPAM("Похоже на спам", "Мы заметили, что вы создаёте сомнительные объявления. Мы против недобросовестной и неуместной рекламы, поэтому ваш кабинет и все объявления заблокированы, также вы не можете создавать новые публикации.");

        private String title;
        private String textHtml;

        UserBanDescriptions(String title, String textHtml) {
            this.title = title;
            this.textHtml = textHtml;
        }
    }

}
