# coding: utf-8

import inject
import mock
import pytest
import six
from six.moves import range
from sepelib.core import config as appconfig

from awacs.model import util
from awacs.lib.rpc import exceptions
from awacs.lib.rpc.exceptions import BadRequestError
from infra.awacs.proto import api_pb2
from awacs.web import knob_service, validation, namespace_service
from infra.swatlib.auth import abc
from awtest.api import call, create_namespace_with_order_in_progress, fill_object_upper_limits
from awtest.core import wait_until, wait_until_passes


@pytest.fixture(autouse=True)
def deps(binder):
    def configure(b):
        b.bind(abc.IAbcClient, mock.Mock())
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


NAMESPACE_ID = 'test_namespace_1'
LOGIN = 'login'
GROUP = '1'


def create_namespace(namespace_id):
    """
    :type namespace_id: str
    """
    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_id
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    call(namespace_service.create_namespace, req_pb, util.NANNY_ROBOT_LOGIN)


def test_forbidden_operations_during_namespace_order(zk_storage, cache, enable_auth):
    # forbid removal
    create_namespace_with_order_in_progress(zk_storage, cache, NAMESPACE_ID)

    req_pb = api_pb2.CreateKnobRequest()
    req_pb.meta.id = NAMESPACE_ID
    req_pb.meta.namespace_id = NAMESPACE_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.comment = 'c'
    req_pb.spec.type = req_pb.spec.BOOLEAN
    k_pb = call(knob_service.create_knob, req_pb, LOGIN).knob
    wait_until_passes(lambda: cache.must_get_knob(NAMESPACE_ID, NAMESPACE_ID))

    req_pb = api_pb2.RemoveKnobValueRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID, version=k_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(knob_service.remove_knob_value, req_pb, LOGIN)

    req_pb = api_pb2.RemoveKnobRequest(namespace_id=NAMESPACE_ID, id=NAMESPACE_ID, version=k_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(knob_service.remove_knob, req_pb, LOGIN)


@pytest.mark.parametrize('max_count,custom_count', [
    (0, None),
    (1, None),
    (10, None),
    (5, 10),
    (10, 5),
])
def test_namespace_objects_total_limit(max_count, custom_count, cache, create_default_namespace):
    create_default_namespace(NAMESPACE_ID)

    appconfig.set_value('common_objects_limits.knob', max_count)
    if custom_count is not None:
        fill_object_upper_limits(NAMESPACE_ID, 'knob', custom_count, LOGIN)
    count = custom_count or max_count

    req_pb = api_pb2.CreateKnobRequest()
    req_pb.meta.id = NAMESPACE_ID
    req_pb.meta.namespace_id = NAMESPACE_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = req_pb.spec.BOOLEAN
    for _ in range(count):
        call(knob_service.create_knob, req_pb, LOGIN)
        req_pb.meta.id += 'a'

    def check():
        list_req_pb = api_pb2.ListKnobsRequest(namespace_id=NAMESPACE_ID)
        assert call(knob_service.list_knobs, list_req_pb, LOGIN).total == count
    wait_until_passes(check)

    with pytest.raises(exceptions.BadRequestError,
                       match='Exceeded limit of knobs in the namespace: {}'.format(count)):
        call(knob_service.create_knob, req_pb, LOGIN)


def test_create_get_delete_knob(mongo_storage, cache):
    login = 'morrison'
    knob_id = 'knob-1'
    namespace_id = NAMESPACE_ID

    create_namespace(namespace_id)

    comment = 'Creating very important knob'

    req_pb = api_pb2.CreateKnobRequest()
    req_pb.meta.id = knob_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.SetInParent()  # TODO

    def check_error():
        with pytest.raises(exceptions.BadRequestError) as e:
            with mock.patch.object(validation.knob, 'validate_request',
                                   side_effect=BadRequestError('BAD')):
                call(knob_service.create_knob, req_pb, login)
        assert six.text_type(e.value) == 'BAD'

    wait_until_passes(check_error)

    req_pb.meta.comment = comment
    with pytest.raises(BadRequestError) as e:
        call(knob_service.create_knob, req_pb, login)
    e.match('"spec.type" must be set')

    req_pb.spec.type = req_pb.spec.BOOLEAN
    resp_pb = call(knob_service.create_knob, req_pb, login)

    knob_pb = resp_pb.knob
    assert knob_pb.meta.id == knob_id
    assert knob_pb.meta.namespace_id == namespace_id
    assert knob_pb.meta.author == login
    assert knob_pb.meta.comment == comment
    assert knob_pb.spec.type == req_pb.spec.BOOLEAN
    assert knob_pb.spec.mode == req_pb.spec.MANAGED
    assert not knob_pb.spec.shared

    def check_knob():
        req_pb = api_pb2.GetKnobRequest(namespace_id=namespace_id, id=knob_id)
        resp_pb = call(knob_service.get_knob, req_pb, login)
        knob_pb = resp_pb.knob
        assert knob_pb.meta.comment == comment

        assert len(knob_pb.statuses) == 1
        r_status = knob_pb.statuses[-1]
        assert r_status.id == knob_pb.meta.version
        assert not r_status.validated and not r_status.in_progress and not r_status.active
        return r_status

    r_status = wait_until(check_knob, timeout=1)

    rev_pb = mongo_storage.must_get_knob_rev(r_status.id)
    assert rev_pb.meta.id == r_status.id
    assert rev_pb.spec == knob_pb.spec

    req_pb = api_pb2.RemoveKnobRequest(namespace_id=namespace_id, id=knob_id, version=r_status.id)
    call(knob_service.remove_knob, req_pb, login)

    def check_remove():
        req_pb = api_pb2.GetKnobRequest(namespace_id=namespace_id, id=knob_id)
        knob_pb = call(knob_service.get_knob, req_pb, login).knob
        assert knob_pb.spec.deleted
        assert knob_pb.meta.comment == 'Marked as deleted by {}'.format(login)

    wait_until_passes(check_remove)


@pytest.mark.skip
def test_update_knob(mongo_storage):
    login = 'morrison'
    namespace_id = NAMESPACE_ID
    knob_id = 'knob-1'

    create_namespace(namespace_id)

    comment = 'Updating very important knob'

    req_pb = api_pb2.CreateKnobRequest()
    req_pb.meta.id = knob_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = req_pb.spec.BOOLEAN

    def check_create():
        resp_pb = call(knob_service.create_knob, req_pb, login)
        knob_pb = resp_pb.knob
        assert knob_pb.meta.id == knob_id
        assert knob_pb.meta.namespace_id == namespace_id
        return knob_pb

    knob_pb = wait_until_passes(check_create)
    initial_version = knob_pb.meta.version
    assert mongo_storage.list_knob_revs(namespace_id=namespace_id, knob_id=knob_id).total == 1

    req_pb = api_pb2.UpdateKnobRequest()
    req_pb.meta.id = knob_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.version = 'xxx'
    req_pb.spec.shared = True

    with pytest.raises(exceptions.ConflictError) as e:
        with mock.patch.object(validation.knob, 'validate_request'):
            call(knob_service.update_knob, req_pb, login)
    e.match('Knob modification conflict')
    assert mongo_storage.list_knob_revs(namespace_id=namespace_id, knob_id=knob_id).total == 1

    req_pb.meta.version = knob_pb.meta.version
    req_pb.meta.comment = comment

    def check_update():
        with mock.patch.object(validation.knob, 'validate_request'):
            call(knob_service.update_knob, req_pb, login)

    wait_until_passes(check_update)

    def check_knob():
        req_pb = api_pb2.GetKnobRequest(namespace_id=namespace_id, id=knob_id)
        resp_pb = call(knob_service.get_knob, req_pb, login)
        knob_pb = resp_pb.knob
        assert knob_pb.meta.version != initial_version
        assert knob_pb.meta.comment == comment
        return knob_pb

    knob_pb = wait_until_passes(check_knob)
    assert mongo_storage.list_knob_revs(namespace_id=namespace_id, knob_id=knob_id).total == 2

    assert len(knob_pb.statuses) == 1
    assert [r.id for r in knob_pb.statuses] == [knob_pb.meta.version]
    r_status_pb = knob_pb.statuses[-1]
    assert r_status_pb.id == knob_pb.meta.version
    assert not r_status_pb.validated and not r_status_pb.in_progress and not r_status_pb.active

    rev_pb = mongo_storage.must_get_knob_rev(r_status_pb.id)
    assert rev_pb.meta.id == r_status_pb.id
    assert rev_pb.meta.comment == comment
    assert rev_pb.spec == knob_pb.spec


@pytest.mark.skip
@mock.patch.object(validation.knob, 'validate_request')
def test_list_knobs_and_revisions(_1, cache):
    login = 'morrison'
    namespace_id = NAMESPACE_ID

    ids = ['a', 'b', 'c', 'd']
    knobs = {}
    for id in ids:
        req_pb = api_pb2.CreateKnobRequest()
        req_pb.meta.id = id
        req_pb.meta.namespace_id = namespace_id
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF
        req_pb.spec.SetInParent()
        knobs[id] = call(knob_service.create_knob, req_pb, login).knob

    req_pb = api_pb2.ListKnobsRequest(namespace_id=namespace_id)

    def check_list():
        response = call(knob_service.list_knobs, req_pb, login)
        assert len(response.knobs) == 4
        assert [b.meta.id for b in response.knobs] == ids

    wait_until_passes(check_list)

    req_pb.skip = 1
    response = call(knob_service.list_knobs, req_pb, login)
    assert len(response.knobs) == 3
    assert [b.meta.id for b in response.knobs] == ids[1:]

    req_pb.skip = 1
    req_pb.limit = 2
    response = call(knob_service.list_knobs, req_pb, login)
    assert len(response.knobs) == 2
    assert [b.meta.id for b in response.knobs] == ids[1:3]

    req_pb.skip = 100
    req_pb.limit = 0
    response = call(knob_service.list_knobs, req_pb, login)
    assert len(response.knobs) == 0

    for _ in range(3):
        req_pb = api_pb2.UpdateKnobRequest()
        req_pb.meta.id = 'a'
        req_pb.meta.namespace_id = namespace_id
        req_pb.meta.version = knobs['a'].meta.version
        req_pb.spec.CopyFrom(knobs['a'].spec)
        req_pb.spec.shared = not req_pb.spec.shared
        knobs['a'] = call(knob_service.update_knob, req_pb, login).knob

    req_pb = api_pb2.ListKnobRevisionsRequest(id='a', namespace_id=namespace_id)
    resp_pb = call(knob_service.list_knob_revisions, req_pb, login)
    assert resp_pb.total == 4
    assert set(rev.meta.namespace_id for rev in resp_pb.revisions) == {namespace_id}
    assert len(resp_pb.revisions) == 4
    assert resp_pb.revisions[0].meta.id == knobs['a'].meta.version

    req_pb = api_pb2.ListKnobRevisionsRequest(id='a', namespace_id=namespace_id, skip=2)
    resp_pb = call(knob_service.list_knob_revisions, req_pb, login)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 2

    req_pb = api_pb2.ListKnobRevisionsRequest(id='a', namespace_id=namespace_id, skip=2, limit=1)
    resp_pb = call(knob_service.list_knob_revisions, req_pb, login)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 1


@pytest.mark.skip
@mock.patch.object(validation.knob, 'validate_request')
def test_get_knob_revision(_, cache):
    login = 'morrison'
    namespace_id = NAMESPACE_ID

    knob_id_to_pb = {}
    ids = ['a', 'b', 'c', 'd']
    for id in ids:
        req_pb = api_pb2.CreateKnobRequest()
        req_pb.meta.id = id
        req_pb.meta.namespace_id = namespace_id
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF

        b = call(knob_service.create_knob, req_pb, login).knob
        knob_id_to_pb[b.meta.id] = b

    req_pb = api_pb2.GetKnobRevisionRequest(id=knob_id_to_pb['a'].meta.version)
    resp_pb = call(knob_service.get_knob_revision, req_pb, login)
    assert resp_pb.revision.spec == knob_id_to_pb['a'].spec
