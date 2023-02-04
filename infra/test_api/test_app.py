# coding: utf-8
import time

import mock
import pytest

from infra.awacs.proto import model_pb2, api_pb2
from awacs.lib.rpc.authentication import AuthSubject
from awacs.lib.rpc.exceptions import ForbiddenError
from awacs.web.app import create_app
from awacs.web.auth.core import StaffAuth, authorize_update, authorize_create
from sepelib.core import config


def test_app():
    # smoke test
    app = create_app(name='awacsd',
                     hostname='localhost',
                     version='0.0.1',
                     version_timestamp=int(time.time()))
    assert len(app.blueprints) > 5


@mock.patch.object(StaffAuth, '_get_group_ids', return_value=[])
@mock.patch.object(StaffAuth, '_get_cached_group_ids', return_value=[])
def test_staff_auth(_1, _2):
    staff = StaffAuth()
    acl_pb = model_pb2.StaffAuth()
    acl_pb.owners.logins.extend(['romanovich'])

    req_pb = api_pb2.UpdateUpstreamRequest()
    req_pb.spec.labels['x'] = 'y'

    pb = model_pb2.Upstream()

    with pytest.raises(ForbiddenError) as e:
        staff.authorize_update(acl_pb, pb, req_pb, 'keepclean')
    assert e.match('User "keepclean" is not authorized to perform such actions: "EDIT_SPEC"')

    staff.authorize_update(acl_pb, pb, req_pb, 'romanovich')

    acl_pb.owners.group_ids.extend(['123'])

    with mock.patch.object(StaffAuth, '_get_group_ids', return_value=['456']) as m, \
            mock.patch.object(StaffAuth, '_get_cached_group_ids', return_value=[]) as m2:
        with pytest.raises(ForbiddenError) as e:
            staff.authorize_update(acl_pb, pb, req_pb, 'keepclean')
    assert e.match('User "keepclean" is not authorized to perform such actions: "EDIT_SPEC"')
    assert m.called
    assert m2.called

    with mock.patch.object(StaffAuth, '_get_group_ids', return_value=['123', '456']) as m, \
            mock.patch.object(StaffAuth, '_get_cached_group_ids', return_value=[]) as m2:
        staff.authorize_update(acl_pb, pb, req_pb, 'keepclean')
    assert m.called
    assert not m2.called

    with mock.patch.object(StaffAuth, '_get_group_ids', return_value=[]) as m, \
            mock.patch.object(StaffAuth, '_get_cached_group_ids', return_value=['123', '456']) as m2:
        staff.authorize_update(acl_pb, pb, req_pb, 'keepclean')
    assert m.called
    assert m2.called

    req_pb = api_pb2.UpdateUpstreamRequest()
    req_pb.meta.auth.type = model_pb2.Auth.STAFF
    req_pb.meta.auth.staff.owners.logins.append('keepclean')

    with pytest.raises(ForbiddenError) as e:
        staff.authorize_update(acl_pb, pb, req_pb, 'keepclean')
    assert e.match('User "keepclean" is not authorized to perform such actions: "EDIT_AUTH"')

    req_pb.spec.labels['x'] = 'y'

    with pytest.raises(ForbiddenError) as e:
        staff.authorize_update(acl_pb, pb, req_pb, 'keepclean')
    assert e.match('User "keepclean" is not authorized to perform such actions: "EDIT_AUTH", "EDIT_SPEC"')

    acl_pb.owners.logins.append('keepclean')
    staff.authorize_update(acl_pb, pb, req_pb, 'keepclean')


@mock.patch.object(StaffAuth, '_get_group_ids', return_value=[])
@mock.patch.object(StaffAuth, '_get_cached_group_ids', return_value=[])
def test_auth(_1, _2):
    req_pb = api_pb2.UpdateUpstreamRequest()
    req_pb.spec.labels['x'] = 'y'

    pb = model_pb2.Upstream()
    pb.meta.auth.type = pb.meta.auth.STAFF

    auth_subject = AuthSubject(login='romanovich')

    acl_pb = model_pb2.StaffAuth()
    acl_pb.owners.logins.append('romanovich')

    prev_run_auth_value = config.get_value('run.auth')
    config.set_value('run.auth', True)
    try:
        with pytest.raises(ForbiddenError) as e:
            authorize_update(pb, req_pb, auth_subject=auth_subject)
        assert e.match('User "romanovich" is not authorized to perform such actions: "EDIT_SPEC"')

        authorize_update(pb, req_pb, auth_subject=auth_subject, acl=acl_pb)
    finally:
        config.set_value('run.auth', prev_run_auth_value)

    namespace_pb = model_pb2.Namespace()
    namespace_pb.meta.id = 'test-namespace'
    namespace_pb.meta.auth.type = pb.meta.auth.STAFF
    namespace_pb.meta.auth.staff.owners.logins.append('nekto0n')

    prev_run_auth_value = config.get_value('run.auth')
    config.set_value('run.auth', True)
    try:
        with pytest.raises(ForbiddenError) as e:
            authorize_create(namespace_pb, auth_subject=auth_subject)
        assert e.match('User "romanovich" is not authorized to create objects in namespace "test-namespace"')
    finally:
        config.set_value('run.auth', prev_run_auth_value)
