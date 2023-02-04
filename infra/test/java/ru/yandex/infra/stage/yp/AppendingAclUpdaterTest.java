package ru.yandex.infra.stage.yp;

import java.util.Collections;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValueFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.yp.client.api.AccessControl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

class AppendingAclUpdaterTest {
    private static final String SUBJECTS_KEY = "subjects";
    private static final String PERMISSIONSS_KEY = "permissions";
    private static final ConfigList SUBJECTS_LIST = ConfigValueFactory.fromIterable(ImmutableList.of("subject1", "subject2"));

    @Test
    void appendEntry() {
        AppendingAclUpdater updater = new AppendingAclUpdater(ImmutableSet.of("subject"),
                ImmutableList.of(AccessControl.EAccessControlPermission.ACA_WRITE));
        assertThat(updater.getEntry(), equalTo(AccessControl.TAccessControlEntry.newBuilder()
                .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
                .addPermissions(AccessControl.EAccessControlPermission.ACA_WRITE)
                .addSubjects("subject")
                .build()
        ));
        assertThat(updater.update(Acl.EMPTY).getEntries(), contains(updater.getEntry()));
    }

    @Test
    void configure() {
        Config config = ConfigFactory.empty()
                .withValue(SUBJECTS_KEY, SUBJECTS_LIST)
                .withValue(PERMISSIONSS_KEY, ConfigValueFactory.fromIterable(ImmutableList.of("none", "read",
                        "read_secrets", "write", "create", "ssh_access", "root_ssh_access", "use",
                        "get_qyp_vm_status")));
        AccessControl.TAccessControlEntry entry = AppendingAclUpdater.configure(config).update(Acl.EMPTY).getEntries().get(0);
        assertThat(entry.getPermissionsList(), containsInAnyOrder(AccessControl.EAccessControlPermission.values()));
        assertThat(entry.getSubjectsList(), containsInAnyOrder("subject1", "subject2"));
        assertThat(entry.getAction(), equalTo(AccessControl.EAccessControlAction.ACA_ALLOW));
    }

    @Test
    void configureDefaultPermissions() {
        Config config = ConfigFactory.empty()
                .withValue(SUBJECTS_KEY, SUBJECTS_LIST);
        assertThat(AppendingAclUpdater.configure(config).update(Acl.EMPTY).getEntries().get(0).getPermissionsList(),
                containsInAnyOrder(AccessControl.EAccessControlPermission.ACP_READ, AccessControl.EAccessControlPermission.ACA_WRITE)
        );
    }

    @Test
    void throwForInvalidPermission() {
        Config config = ConfigFactory.empty()
                .withValue(SUBJECTS_KEY, SUBJECTS_LIST)
                .withValue(PERMISSIONSS_KEY, ConfigValueFactory.fromIterable(ImmutableList.of("smth")));
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> AppendingAclUpdater.configure(config));
        assertThat(ex.getMessage(), containsString("Unknown yp permission 'smth'"));
    }

    @Test
    void createNullAppenderIfNoSubjects() {
        Config config = ConfigFactory.empty()
                .withValue(SUBJECTS_KEY, ConfigValueFactory.fromIterable(Collections.emptyList()))
                .withValue(PERMISSIONSS_KEY, ConfigValueFactory.fromIterable(ImmutableList.of("read")));
        assertThat(AppendingAclUpdater.configure(config).update(Acl.EMPTY).getEntries(), empty());
    }
}
