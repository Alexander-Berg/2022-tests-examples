package ru.yandex.spirit.it_tests.configuration;

public class SpiritKKT {
    public String serviceName;
    public String kktSN;
    public String fnSN;
    public String inn;
    public int id = -1;
    public String address = "";

    public SpiritKKT(String ServiceName, String KktSn, String FnSn, String Inn) {
        serviceName = ServiceName;
        kktSN = KktSn;
        fnSN = FnSn;
        inn = Inn;
    }
}
