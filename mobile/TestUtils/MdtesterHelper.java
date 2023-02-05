package ru.yandex.direct.ui.testutils;

public class MdtesterHelper {
    private final static String[] CAMPAIGNS = {
            null,
            "01 поиск+сети (ручное)",
            "02 поиск+сети (ручное раздельное)",
            "03 поиск+сети (авто)",
            "04 поиск (ручное)",
            "05 поиск (авто)",
            "06 сети (ручное)",
            "07 сети (авто)",
    };

    public static String getCampaignName(int index) {
        if (index < 1 || index >= CAMPAIGNS.length) {
            throw new IllegalArgumentException("mdtester has no campaign with index: " + index +
                    ". Only indices from 1 to " + CAMPAIGNS.length + " are being accepted.");
        }

        return CAMPAIGNS[index];
    }
}
