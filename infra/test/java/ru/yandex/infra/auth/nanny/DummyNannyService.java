package ru.yandex.infra.auth.nanny;

import java.util.HashSet;
import java.util.Set;

import ru.yandex.infra.auth.RoleSubject;
import ru.yandex.infra.auth.idm.service.IdmService;

public class DummyNannyService implements NannyService {

    public Set<NannyRole> roles = new HashSet<>();

    @Override
    public void updateRoleSubject(RoleSubject roleSubject, IdmService.RequestType requestType) {

    }

    @Override
    public Set<NannyRole> getRolesWithSubjects(String projectId, String serviceName, String serviceUuid) {
        return roles;
    }

    @Override
    public void syncNannyServiceRoleIntoYP(NannyRole nannyRole) {

    }

    @Override
    public void syncNannyServiceAuthAttrsAsync(String serviceId, String commitMessage) {

    }

    @Override
    public boolean isNodesSyncEnabled() {
        return true;
    }
}
