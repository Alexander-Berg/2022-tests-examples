package ru.yandex.general.consts;

import lombok.Getter;

public class CardStatus {

    private CardStatus() {
    }

    public static final String BANNED = "Banned";
    public static final String INACTIVE = "Inactive";
    public static final String ACTIVE = "Active";
    public static final String REMOVED = "Removed";

    public static final String CANT_CALL_REASON_TEXT = "Мы не смогли с вами связаться, поэтому сняли объявление с " +
            "публикации. Если предложение актуально, нажмите Активировать и постарайтесь быть на связи. " +
            "Например, укажите в описании время, в которое вам удобно принимать звонки или сообщения.";
    public static final String CANT_CALL_REASON_TITLE = "Вы долго не выходили на связь";


    @Getter
    public enum OfferBanReasons {

        SPAM("SPAMMING", "Похоже на спам", "Ваше объявление выглядит как спам и нарушает <a target=\\\"_blank\\\" " +
                "href=\\\"https://yandex.ru/legal/classified_termsofuse/\\\">правила сервиса</a>. Мы не рекомендуем " +
                "размещать сомнительный контент, иначе ваш личный кабинет придётся заблокировать. Если это ошибка," +
                " напишите в <a target=\\\"_blank\\\" href=\\\"https://yandex.ru/chat#/user/2d001e07-0165-9004-5" +
                "972-3c2857a2ac80\\\">службу поддержки</a>, мы поможем разобраться.",
                "Ваше объявление выглядит как спам и нарушает правила сервиса. Мы не рекомендуем размещать " +
                        "сомнительный контент, иначе ваш личный кабинет придётся заблокировать. Если это ошибка, " +
                        "напишите в службу поддержки, мы поможем разобраться."),
        WRONG_OFFER_CATEGORY("WRONG_OFFER_CATEGORY", "Неверная категория", "Ваше объявление размещено не в том " +
                "разделе. Чтобы оно вернулось на сайт, перенесите его в правильную категорию. Если не знаете, какую " +
                "выбрать, прочтите <a target=\"_blank\" href=\"https://yandex.ru/support/o-desktop/rules.html\">наши " +
                "советы</a> или напишите в <a target=\"_blank\" href=\"https://yandex.ru/chat#/user/2d001e07-0165-" +
                "9004-5972-3c2857a2ac80\">службу поддержки</a>.", "Ваше объявление размещено не в том разделе. Чтобы " +
                "оно вернулось на сайт, перенесите его в правильную категорию. Если не знаете, какую выбрать, " +
                "прочтите наши советы или напишите в службу поддержки."),
        WRONG_PHOTO("WRONG_PHOTO", "Фотографии не относятся к товару", "Мы не смогли сопоставить товар и снимки — " +
                "возможно, вы добавили не те фотографии. Внимательно проверьте их ещё раз и удалите лишние.", ""),
        STOPWORD("STOPWORD", "Нецензурная лексика или оскорбления", "Скорее всего, в вашем объявлении есть грубые и " +
                "неуместные выражения, ссылки или реклама. Удалите всё лишнее и постарайтесь перефразировать. Мы " +
                "уверены, вы сможете найти подходящие слова.", "Скорее всего, в вашем объявлении есть грубые и " +
                "неуместные выражения, ссылки или реклама. Удалите всё лишнее и постарайтесь перефразировать." +
                " Мы уверены, вы сможете найти подходящие слова.");

        private String code;
        private String title;
        private String reason;
        private String reasonNoLinks;

        OfferBanReasons(String code, String title, String reason, String reasonNoLinks) {
            this.code = code;
            this.title = title;
            this.reason = reason;
            this.reasonNoLinks = reasonNoLinks;
        }

    }

    @Getter
    public enum CardDeactivateStatuses {

        SOLD_ON_YANDEX("Продал на Яндексе", "SOLD_ON_YANDEX", "SoldOnYandex"),
        SOLD_SOMEWHERE("Продал в другом месте", "SOLD_SOMEWHERE", "SoldSomewhere"),
        RETHINK("Передумал продавать", "RETHINK", "Rethink"),
        OTHER("Другая причина", "OTHER", "Other"),
        EXPIRED("Закончился срок размещения", "", "Expired");


        private String name;
        private String goalValue;
        private String mockValue;

        CardDeactivateStatuses(String name, String goalValue, String mockValue) {
            this.name = name;
            this.goalValue = goalValue;
            this.mockValue = mockValue;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
