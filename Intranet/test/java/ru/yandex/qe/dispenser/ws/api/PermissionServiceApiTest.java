package ru.yandex.qe.dispenser.ws.api;

import java.util.Set;

import javax.ws.rs.HttpMethod;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiPermission;

public class PermissionServiceApiTest extends ApiTestBase {

    @Test
    public void getPermissionRegressionTest() {
        DiPermission personInfo = dispenser().permission().getFor("aqru").perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/permission?login=aqru");
        Assertions.assertEquals(DiPermission.builder()
                .login("aqru")
                .permissionSet(Set.of())
                .build(), personInfo);

        personInfo = dispenser().permission().getFor("keyd").perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/permission?login=keyd");
        Assertions.assertEquals(DiPermission.builder()
                .login("keyd")
                .permissionSet(Set.of(DiPermission.Permission.CAN_VIEW_PROVIDER_GROUP_REPORT,
                        DiPermission.Permission.CAN_VIEW_OWNING_COST))
                .build(), personInfo);

        personInfo = dispenser().permission().getFor("sancho").perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/permission?login=sancho");
        Assertions.assertEquals(DiPermission.builder()
                .login("sancho")
                .permissionSet(Set.of(DiPermission.Permission.CAN_VIEW_PROVIDER_GROUP_REPORT))
                .build(), personInfo);
    }
}
