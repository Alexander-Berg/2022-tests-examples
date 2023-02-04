package ru.yandex.wmconsole.verification;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.wmconsole.data.VerificationStateEnum;
import ru.yandex.wmconsole.data.info.UsersHostsInfo;
import ru.yandex.wmtools.common.sita.SitaService;
import ru.yandex.wmtools.common.sita.SitaUrlFetchJsonResponse;
import ru.yandex.wmtools.common.sita.SitaUrlFetchResponse;

/**
 * @author aherman
 */
public class MetaTagVerifierTest {
    @Test
    public void testCheckMetaTag() throws Exception {
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream("burtsevpr.livejournal.com.resp.json");
        SitaUrlFetchResponse response =
                SitaUrlFetchJsonResponse.parse(new InputStreamReader(resourceAsStream, SitaService.ISO8859_1), false);

        UsersHostsInfo ver = new UsersHostsInfo(VerificationStateEnum.IN_PROGRESS,
                0x6adc64c961fe5616L,
                VerificationTypeEnum.HTML_FILE,
                new Date(),
                null,
                0,
                0,
                "burtsevpr.livejournal.com",
                null,
                200);

        UsersHostsInfo usersHostsInfo =
                new JerichoMetaTagChecker().checkMetatag(ver, new URL("http://burtsevpr.livejournal.com.resp.json"), response);

        Assert.assertTrue("Actual verification state: " + usersHostsInfo.getVerificationState(),
                usersHostsInfo.getVerificationState().isVerified());
    }

    @Test
    public void testCheckMetaTag1() throws Exception {
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream("wmtest.people.yandex.net.resp.json");
        SitaUrlFetchResponse response =
                SitaUrlFetchJsonResponse.parse(new InputStreamReader(resourceAsStream, SitaService.ISO8859_1), false);

        UsersHostsInfo ver = new UsersHostsInfo(VerificationStateEnum.IN_PROGRESS,
                0x6666b9331245d2d8L,
                VerificationTypeEnum.HTML_FILE,
                new Date(),
                null,
                0,
                0,
                "wmtest.people.yandex.net",
                null,
                200);

        UsersHostsInfo usersHostsInfo =
                new JerichoMetaTagChecker().checkMetatag(ver, new URL("http://wmtest.people.yandex.net.resp.json"),
                        response);

        Assert.assertTrue("Actual verification state: " + usersHostsInfo.getVerificationState(),
                usersHostsInfo.getVerificationState().isVerified());
    }

    @Test
    public void testWMCON_5831() throws Exception {
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream("WMCON_5831.metatag.verification.json");
        SitaUrlFetchResponse response =
                SitaUrlFetchJsonResponse.parse(new InputStreamReader(resourceAsStream, SitaService.ISO8859_1), false);

        UsersHostsInfo ver = new UsersHostsInfo(VerificationStateEnum.IN_PROGRESS,
                0x4ee70ed1be943e44L,
                VerificationTypeEnum.META_TAG,
                new Date(),
                null,
                0,
                0,
                "wmtest.people.yandex.net",
                null,
                200);

        UsersHostsInfo usersHostsInfo =
                new JerichoMetaTagChecker().checkMetatag(ver, new URL("http://wmtest.people.yandex.net.resp.json"),
                        response);

        Assert.assertFalse("Actual verification state: " + usersHostsInfo.getVerificationState(),
                usersHostsInfo.getVerificationState().isVerified());
    }

    @Test
    public void test_WMCON5865_eklatmebel() throws Exception {
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream("WMCON5865_eklatmebel.ru.json");
        SitaUrlFetchResponse response =
                SitaUrlFetchJsonResponse.parse(new InputStreamReader(resourceAsStream, SitaService.ISO8859_1), false);

        UsersHostsInfo ver = new UsersHostsInfo(VerificationStateEnum.IN_PROGRESS,
                0x6e36db2e6181e345L,
                VerificationTypeEnum.META_TAG,
                new Date(),
                null,
                0,
                0,
                "eklatmebel.ru",
                null,
                200);

        UsersHostsInfo usersHostsInfo =
                new JerichoMetaTagChecker().checkMetatag(ver, new URL("http://eklatmebel.ru"), response);

        Assert.assertTrue("Actual verification state: " + usersHostsInfo.getVerificationState(),
                usersHostsInfo.getVerificationState().isVerified());
    }

    @Test
    public void test_WMCON5884_wludss_ru() throws Exception {
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream("WMCON5884_wludss.ru.resp.json");
        SitaUrlFetchResponse response =
                SitaUrlFetchJsonResponse.parse(new InputStreamReader(resourceAsStream, SitaService.ISO8859_1), false);

        UsersHostsInfo ver = new UsersHostsInfo(VerificationStateEnum.IN_PROGRESS,
                0x676b8fbc58b99381L,
                VerificationTypeEnum.META_TAG,
                new Date(),
                null,
                0,
                0,
                "wludss.ru",
                null,
                200);

        UsersHostsInfo usersHostsInfo =
                new JerichoMetaTagChecker().checkMetatag(ver, new URL("http://wludss.ru"), response);

        Assert.assertTrue("Actual verification state: " + usersHostsInfo.getVerificationState(),
                usersHostsInfo.getVerificationState().isVerified());
    }

    @Test
    public void test_WMCON5884_forum_baikliga_rf() throws Exception {
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream("WMCON5884_forum.baikliga.rf.resp.json");
        SitaUrlFetchResponse response =
                SitaUrlFetchJsonResponse.parse(new InputStreamReader(resourceAsStream, SitaService.ISO8859_1), false);

        UsersHostsInfo ver = new UsersHostsInfo(VerificationStateEnum.IN_PROGRESS,
                0x68036cf9dcff0741L,
                VerificationTypeEnum.META_TAG,
                new Date(),
                null,
                0,
                0,
                "форум.байклига.рф",
                null,
                200);

        UsersHostsInfo usersHostsInfo =
                new JerichoMetaTagChecker().checkMetatag(ver, new URL("http://форум.байклига.рф"), response);

        Assert.assertTrue("Actual verification state: " + usersHostsInfo.getVerificationState(),
                usersHostsInfo.getVerificationState().isVerified());
    }

    @Test
    public void test_WMCON5884_youareperfect_ru() throws Exception {
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream("WMCON5884_youareperfect.ru.resp.json");
        SitaUrlFetchResponse response =
                SitaUrlFetchJsonResponse.parse(new InputStreamReader(resourceAsStream, SitaService.ISO8859_1), false);

        UsersHostsInfo ver = new UsersHostsInfo(VerificationStateEnum.IN_PROGRESS,
                0x68171666bb03e24aL,
                VerificationTypeEnum.META_TAG,
                new Date(),
                null,
                0,
                0,
                "youareperfect.ru",
                null,
                200);

        UsersHostsInfo usersHostsInfo =
                new JerichoMetaTagChecker().checkMetatag(ver, new URL("http://youareperfect.ru/"), response);

        Assert.assertTrue("Actual verification state: " + usersHostsInfo.getVerificationState(),
                usersHostsInfo.getVerificationState().isVerified());
    }

    @Test
    public void test_WMCON5884_coinsspb_com() throws Exception {
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream("WMCON5884_coinsspb.com.resp.json");
        SitaUrlFetchResponse response =
                SitaUrlFetchJsonResponse.parse(new InputStreamReader(resourceAsStream, SitaService.ISO8859_1), false);

        UsersHostsInfo ver = new UsersHostsInfo(VerificationStateEnum.IN_PROGRESS,
                0x560fe19c1d567f24L,
                VerificationTypeEnum.META_TAG,
                new Date(),
                null,
                0,
                0,
                "coinsspb.com",
                null,
                200);

        UsersHostsInfo usersHostsInfo =
                new JerichoMetaTagChecker().checkMetatag(ver, new URL("http://coinsspb.com/"), response);

        Assert.assertTrue("Actual verification state: " + usersHostsInfo.getVerificationState(),
                usersHostsInfo.getVerificationState().isVerified());
    }
}
