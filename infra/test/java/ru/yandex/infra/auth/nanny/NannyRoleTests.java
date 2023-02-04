package ru.yandex.infra.auth.nanny;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.RoleSubject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class NannyRoleTests {
    @Test
    void getRoleSubjectsTest() {
        var role = new NannyRole("", "", "", Collections.emptySet(), Collections.emptySet());
        assertThat(role.getRoleSubjects(), equalTo(List.of()));

        role = new NannyRole("", "", "", Set.of("user1", "user2"), Set.of(345L, 676L));
        assertThat(Set.of(role.getRoleSubjects().toArray()), equalTo(Set.of(
                new RoleSubject("user1", 0L, role),
                new RoleSubject("user2", 0L, role),
                new RoleSubject("", 345L, role),
                new RoleSubject("", 676L, role)
        )));
    }
}
