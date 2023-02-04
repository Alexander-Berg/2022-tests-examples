package ru.yandex.general.consts;

import lombok.Getter;

public class Accounts {

    private Accounts() {
    }

    @Getter
    public enum AccountsForOfferCreation {

        ACCOUNT_1("yandex-team-67221.04601", "rN0y.cyOv"),
        ACCOUNT_2("yandex-team-92036.12404", "DFeP.eXEK"),
        ACCOUNT_3("yandex-team-55296.57611", "ctin.3Pgk"),
        ACCOUNT_4("yandex-team-14845.70991", "2d1c.aGYv"),
        ACCOUNT_5("yandex-team-87399.71379", "fY3p.qY1b"),
        ACCOUNT_6("yandex-team-34758.66848", "ZzWB.YqU0"),
        ACCOUNT_7("yandex-team-24590.05490", "GaR5.K4QK"),
        ACCOUNT_8("yandex-team-49704.56124", "GCN1.YBEM"),
        ACCOUNT_9("yandex-team-68113.92877", "ZcfR.a0Ov"),
        ACCOUNT_10("yandex-team-29490.12717", "WJoZ.jrln"),
        ACCOUNT_11("yandex-team-62433.05005", "BCzQ.xnVo"),
        ACCOUNT_12("yandex-team-59025.03324", "S1mv.wcim"),
        ACCOUNT_13("yandex-team-85526.98358", "tRPD.lBpb"),
        ACCOUNT_14("yandex-team-11086.64359", "NWwp.JyY8"),
        ACCOUNT_15("yandex-team-22242.04715", "uOT0.HEkY"),
        ACCOUNT_16("yandex-team-72732.17727", "D3au.12AY"),
        ACCOUNT_17("yandex-team-67829.14295", "9LSN.b7NE"),
        ACCOUNT_18("yandex-team-05477.91950", "yvmt.3cI1"),
        ACCOUNT_19("yandex-team-14203.04901", "Rovl.KuJs"),
        ACCOUNT_20("yandex-team-59459.31966", "gQE7.MAbn"),
        ACCOUNT_21("yandex-team-45092.93052", "NKj5.m2nz"),
        ACCOUNT_22("yandex-team-87929.03075", "11Lk.Nysc"),
        ACCOUNT_23("yandex-team-96920.46938", "9eZF.HaOM"),
        ACCOUNT_24("yandex-team-90607.50302", "v0rf.d811"),
        ACCOUNT_25("yandex-team-88760.71303", "orhq.y7jD"),
        ACCOUNT_26("yandex-team-47205.48787", "LgcY.f7aq"),
        ACCOUNT_27("yandex-team-20355.49904", "jwGN.uMqo"),
        ACCOUNT_28("yandex-team-89155.84833", "fanY.LJhx"),
        ACCOUNT_29("yandex-team-04651.93040", "ABfI.T76P"),
        ACCOUNT_30("yandex-team-75735.81179", "d4n4.Ifkk");

        private String login;
        private String password;

        AccountsForOfferCreation(String login, String password) {
            this.login = login;
            this.password = password;
        }
    }

}
