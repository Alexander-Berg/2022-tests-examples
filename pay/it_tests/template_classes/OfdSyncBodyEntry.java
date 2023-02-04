package ru.yandex.darkspirit.it_tests.template_classes;

public class OfdSyncBodyEntry {
    public String kkt_reg_id;
    public String fs_id;
    public int count;
    public boolean success = true;

    public OfdSyncBodyEntry(String kktFn, String rnm, int Count) {
        fs_id = kktFn;
        kkt_reg_id = rnm;
        count = Count;
    }
}
