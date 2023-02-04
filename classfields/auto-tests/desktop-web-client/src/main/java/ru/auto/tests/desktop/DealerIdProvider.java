package ru.auto.tests.desktop;


import ru.auto.tests.passport.account.Account;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.07.18
 */
public class DealerIdProvider {
    private static final Map<String, String> dealerId = new HashMap<String, String>() {{
        put("14090654", "16453");
        put("23117336", "27326");
        put("22863796", "26774");
        put("18693585", "23427");
        put("16165433", "21011");
        put("21236408", "25490");
        put("24713246", "31262");
        put("11296277", "20101");
        put("33427392", "26626");
        put("308985", "668");
        put("725515", "384");
        put("18563892", "23332");
        put("28371472", "31508");
        put("11328285", "16061");
        put("12639970", "2602");
        put("27984576", "31828");
        put("21563670", "29004");
        put("20625123", "24755");
        put("28588748", "31674");
        put("10703195", "1872");
        put("10380477", "9015");
        put("97986", "78");
        put("24151684", "21151");
        put("2032087", "1034");
        put("35428389", "37033");
        put("3849112", "2222");
        put("28296254", "31464");
        put("10924048", "11460");
        put("26034204", "8957");
        put("29543726", "32452");
        put("30679", "208");
        put("10843244", "23310");
        put("11618471", "16541");
        put("14285796", "18864");
        put("17221837", "22087");
        put("4145036", "2496");
        put("20831817", "24897");
        put("8371466", "6434");
    }};

    public static String dealerId(Account account) {
        return dealerId.get(account.getId());
    }

}
