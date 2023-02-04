package ru.auto.tests.realtyapi.v2.testdata;

public class CabinetTestData {
    private CabinetTestData() {}

    public static final String ME = "me";
    public static final String VALID_UID = "48965719";
    public static final int INVALID_PAGE = -1;

    public static Object[] getBillingDomain() {
        return new Object[]{"calls", "offers"};
    }

    public static Object[][] getCabinetData() {
        return new Object[][]{
                {"817648965", "56524733", "offers", "client"},
                {"817648965", "56524733", "calls", "client"},
                {"683261718", "46295873", "offers", "agency"},
                {"683261718", "46295873", "calls", "agency"}
        };
    }

    public static Object[][] getAgencies() {
        return new Object[][]{
                {"360673113", "40820974", "calls"},
                {"789283233", "54866596", "offers"}
        };
    }
}
