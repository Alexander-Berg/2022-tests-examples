from __future__ import unicode_literals
import inject
import mock
import pytest
import six

from awacs.lib.rpc import exceptions
from infra.awacs.proto import api_pb2, model_pb2
from awacs.web import component_service
from awtest.mocks.sandbox_client import MockSandboxClient
from infra.swatlib import sandbox
from infra.swatlib.auth import abc
from awtest.api import call, set_login_to_root_users
from awtest.core import wait_until_passes


LOGIN = 'admin'


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client):
    def configure(b):
        b.bind(abc.IAbcClient, mock.Mock())
        b.bind(sandbox.ISandboxClient, MockSandboxClient())
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def test_get_remove_draft_publish_retire_list_component(cache):
    VERSION = '178-1'
    draft_req_pb = api_pb2.DraftComponentRequest()
    with pytest.raises(exceptions.BadRequestError, match='"type" must be set'):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    draft_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
    with pytest.raises(exceptions.BadRequestError, match='"version" must be set'):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    draft_req_pb.version = 'Some version'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(component_service.draft_component, draft_req_pb, LOGIN)
    assert six.text_type(e.value) == '"startrek_issue_key" must be set'

    draft_req_pb.startrek_issue_key = 'SWATOPS-112'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(component_service.draft_component, draft_req_pb, LOGIN)
    assert six.text_type(e.value) == '"version": is not ^[0-9]+-[0-9]+$'

    draft_req_pb.version = VERSION
    with pytest.raises(exceptions.BadRequestError) as e:
        call(component_service.draft_component, draft_req_pb, LOGIN)
    assert six.text_type(e.value) == 'one of ("spec.source.sandbox_resource") must be set for selected component type'

    draft_req_pb.spec.source.sandbox_resource.task_id = '637214020'
    with pytest.raises(exceptions.BadRequestError, match='"spec.source.sandbox_resource.task_type" must be set'):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    draft_req_pb.spec.source.sandbox_resource.task_type = 'BUILD_BALANCER_BUNDLE'
    draft_req_pb.spec.source.sandbox_resource.resource_id = '1416163086'
    draft_req_pb.spec.source.sandbox_resource.resource_type = 'BALANCER'
    with pytest.raises(exceptions.BadRequestError, match='"spec.source.sandbox_resource.resource_type" must be one of '
                                                         '"BALANCER_EXECUTABLE" for selected component type'):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    draft_req_pb.spec.source.sandbox_resource.resource_type = 'BALANCER_EXECUTABLE'
    draft_req_pb.message = 'Balancer 178-1'
    with pytest.raises(exceptions.ForbiddenError, match='Only root users can change components'):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    set_login_to_root_users(LOGIN)
    component_pb = call(component_service.draft_component, draft_req_pb, LOGIN).component
    assert component_pb.meta.type == model_pb2.ComponentMeta.PGINX_BINARY
    assert component_pb.meta.version == VERSION
    spec_pb = draft_req_pb.spec
    spec_pb.source.sandbox_resource.rbtorrent = 'rbtorrent:8616184c36d85d294d2bb2e7aa6906a45089680d'
    assert component_pb.spec == spec_pb
    assert component_pb.status.status == component_pb.status.DRAFTED
    assert component_pb.status.drafted.author == LOGIN
    assert component_pb.status.drafted.message == draft_req_pb.message
    assert not component_pb.status.HasField('published')
    assert not component_pb.status.HasField('retired')
    with pytest.raises(exceptions.ConflictError, match='Component "PGINX_BINARY" of version "{}" already exists'
                                                       ''.format(VERSION)):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    wait_until_passes(lambda: cache.must_get_component(model_pb2.ComponentMeta.PGINX_BINARY, component_pb.meta.version))
    remove_req_pb = api_pb2.RemoveComponentRequest()
    remove_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
    remove_req_pb.version = VERSION
    call(component_service.remove_component, remove_req_pb, LOGIN)
    component_pb = call(component_service.draft_component, draft_req_pb, LOGIN).component

    wait_until_passes(lambda: cache.must_get_component(model_pb2.ComponentMeta.PGINX_BINARY, component_pb.meta.version))
    get_req_pb = api_pb2.GetComponentRequest()
    with pytest.raises(exceptions.BadRequestError, match='"type" must be set'):
        call(component_service.get_component, get_req_pb, LOGIN)

    get_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
    with pytest.raises(exceptions.BadRequestError, match='"version" must be set'):
        call(component_service.get_component, get_req_pb, LOGIN)
    get_req_pb.version = VERSION

    resp_pb = call(component_service.get_component, get_req_pb, LOGIN)
    assert resp_pb.component == component_pb

    publ_req_pb = api_pb2.PublishComponentRequest()
    publ_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
    publ_req_pb.version = VERSION
    publ_req_pb.message = 'Publish balancer 178-1'
    with pytest.raises(exceptions.BadRequestError, match='Different people should draft and publish one component'):
        call(component_service.publish_component, publ_req_pb, LOGIN)

    LOGIN2 = 'pirogov'
    with pytest.raises(exceptions.ForbiddenError, match='Only root users can change components'):
        call(component_service.publish_component, publ_req_pb, LOGIN2)

    set_login_to_root_users(LOGIN2)
    component_pb = call(component_service.publish_component, publ_req_pb, LOGIN2).component
    assert component_pb.status.status == component_pb.status.PUBLISHED
    assert component_pb.status.drafted.author == LOGIN
    assert component_pb.status.drafted.message == draft_req_pb.message
    assert component_pb.status.published.author == LOGIN2
    assert component_pb.status.published.message == publ_req_pb.message
    assert component_pb.status.published.startrek_issue_key == publ_req_pb.startrek_issue_key
    assert component_pb.meta.generation
    assert not component_pb.status.HasField('retired')

    with pytest.raises(exceptions.BadRequestError, match='Only drafted components can be removed'):
        call(component_service.remove_component, remove_req_pb, LOGIN)

    retire_req_pb = api_pb2.RetireComponentRequest()
    retire_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
    retire_req_pb.version = VERSION
    retire_req_pb.message = 'Out of date'
    with pytest.raises(exceptions.BadRequestError, match='"superseded_by" must be set'):
        call(component_service.retire_component, retire_req_pb, LOGIN)

    retire_req_pb.superseded_by = VERSION
    with pytest.raises(exceptions.ForbiddenError, match='Only root users can change components'):
        call(component_service.retire_component, retire_req_pb, 'noname')

    with pytest.raises(exceptions.BadRequestError, match='"superseded_by": can not be superseded by itself'):
        call(component_service.retire_component, retire_req_pb, LOGIN)

    retire_req_pb.superseded_by = '185-3'
    with pytest.raises(exceptions.BadRequestError, match='"superseded_by": component PGINX_BINARY of version "185.3" '
                                                         'does not exist'):
        call(component_service.retire_component, retire_req_pb, LOGIN)

    draft_req_pb.version = '185-3'
    call(component_service.draft_component, draft_req_pb, LOGIN)

    with pytest.raises(exceptions.BadRequestError, match='"superseded_by": must be PUBLISHED version'):
        call(component_service.retire_component, retire_req_pb, LOGIN)

    publ_req_pb2 = api_pb2.PublishComponentRequest()
    publ_req_pb2.type = model_pb2.ComponentMeta.PGINX_BINARY
    publ_req_pb2.version = '185-3'
    publ_req_pb2.message = 'Publish balancer 185-3'
    publ_req_pb2.startrek_issue_key = 'SWATOPS-112'
    call(component_service.publish_component, publ_req_pb2, LOGIN2)

    component_pb = call(component_service.retire_component, retire_req_pb, LOGIN).component
    assert component_pb.status.status == component_pb.status.RETIRED
    assert component_pb.status.drafted.author == LOGIN
    assert component_pb.status.drafted.message == draft_req_pb.message
    assert component_pb.status.published.author == LOGIN2
    assert component_pb.status.published.message == publ_req_pb.message
    assert component_pb.status.published.startrek_issue_key == publ_req_pb.startrek_issue_key
    assert component_pb.status.retired.author == LOGIN
    assert component_pb.status.retired.message == retire_req_pb.message
    assert component_pb.status.retired.superseded_by == retire_req_pb.superseded_by

    list_req_pb = api_pb2.ListComponentsRequest()
    list_req_pb.sort_order = api_pb2.ASCEND
    list_req_pb.sort_target = list_req_pb.VERSION
    component_pbs = call(component_service.list_components, list_req_pb, LOGIN).components
    assert [pb.meta.version for pb in component_pbs] == ['178-1', '185-3']

    list_req_pb = api_pb2.ListComponentsRequest()
    list_req_pb.sort_order = api_pb2.DESCEND
    list_req_pb.sort_target = list_req_pb.VERSION
    component_pbs = call(component_service.list_components, list_req_pb, LOGIN).components
    assert [pb.meta.version for pb in component_pbs] == ['185-3', '178-1']


def test_draft_instancectl():
    VERSION = '1.202'
    set_login_to_root_users(LOGIN)
    draft_req_pb = api_pb2.DraftComponentRequest()
    draft_req_pb.type = model_pb2.ComponentMeta.INSTANCECTL
    draft_req_pb.version = '0.0.1a'
    draft_req_pb.startrek_issue_key = 'SWATOPS-112'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(component_service.draft_component, draft_req_pb, LOGIN)
    assert six.text_type(e.value) == r'"version": is not ^[0-9]+\.[0-9]+$'

    draft_req_pb.version = VERSION
    with pytest.raises(exceptions.BadRequestError) as e:
        call(component_service.draft_component, draft_req_pb, LOGIN)
    assert six.text_type(e.value) == 'one of ("spec.source.sandbox_resource") must be set for selected component type'

    draft_req_pb.spec.source.sandbox_resource.task_id = '639956062'
    with pytest.raises(exceptions.BadRequestError, match='"spec.source.sandbox_resource.task_type" '
                                                         'must be set'):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    draft_req_pb.spec.source.sandbox_resource.task_type = 'BUILD_INSTANCE_CTL'
    draft_req_pb.spec.source.sandbox_resource.resource_id = '1423168226'
    draft_req_pb.spec.source.sandbox_resource.resource_type = 'INSTANCECTL'

    draft_req_pb.spec.source.sandbox_resource.task_id = '12341234'
    with pytest.raises(exceptions.BadRequestError, match='"spec.source.sandbox_resource.task_type" is different '
                                                         'with type of task 12341234, real: RANDOM_TASK'):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    draft_req_pb.spec.source.sandbox_resource.task_id = '639956062'
    draft_req_pb.spec.source.sandbox_resource.resource_id = '12341234'
    with pytest.raises(exceptions.BadRequestError, match='"spec.source.sandbox_resource.resource_type" is different '
                                                         'with type of resource 12341234, real: RANDOM_RESOURCE'):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    draft_req_pb.spec.source.sandbox_resource.resource_id = '1423168226'
    draft_req_pb.spec.source.sandbox_resource.rbtorrent = 'rbtorrent:1234123412341234132412341234123412341234'
    with pytest.raises(exceptions.BadRequestError, match='"spec.source.sandbox_resource.rbtorrent" is different '
                                                         'with skynet_id of resource 1423168226, real: '):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    draft_req_pb.spec.source.sandbox_resource.rbtorrent = 'rbtorrent:123'
    with pytest.raises(exceptions.BadRequestError, match='"spec.source.sandbox_resource.rbtorrent" must match pattern'):
        call(component_service.draft_component, draft_req_pb, LOGIN)

    draft_req_pb.spec.source.sandbox_resource.rbtorrent = 'rbtorrent:54af0d45b0349d85c260ebdef64306768b6eed5c'
    call(component_service.draft_component, draft_req_pb, LOGIN)


def test_default_components(checker):
    LOGIN2 = 'ferenets'
    set_login_to_root_users(LOGIN)
    set_login_to_root_users(LOGIN2)

    VERSIONS = ('178-1', '185-4')
    set_default_req_pb = api_pb2.SetComponentAsDefaultRequest()
    set_default_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
    set_default_req_pb.version = VERSIONS[0]
    with pytest.raises(exceptions.BadRequestError, match='"cluster" must be set'):
        call(component_service.set_component_as_default, set_default_req_pb, LOGIN)
    for version in VERSIONS:
        draft_req_pb = api_pb2.DraftComponentRequest()
        draft_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
        draft_req_pb.version = version
        draft_req_pb.startrek_issue_key = 'SWATOPS-112'
        draft_req_pb.spec.source.sandbox_resource.task_id = '637214020'
        draft_req_pb.spec.source.sandbox_resource.task_type = 'BUILD_BALANCER_BUNDLE'
        draft_req_pb.spec.source.sandbox_resource.resource_id = '1416163086'
        draft_req_pb.spec.source.sandbox_resource.resource_type = 'BALANCER_EXECUTABLE'
        call(component_service.draft_component, draft_req_pb, LOGIN)

        set_default_req_pb.version = version
        set_default_req_pb.cluster = 'SAS'
        with pytest.raises(exceptions.BadRequestError, match='Only published components can be marked as default'):
            call(component_service.set_component_as_default, set_default_req_pb, LOGIN)

        publ_req_pb = api_pb2.PublishComponentRequest()
        publ_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
        publ_req_pb.version = version
        publ_req_pb.message = 'Publish balancer {}'.format(version)
        publ_req_pb.startrek_issue_key = 'SWATOPS-112'
        call(component_service.publish_component, publ_req_pb, LOGIN2)

    set_default_req_pb.version = VERSIONS[0]
    set_default_req_pb.cluster = 'LOL'
    with pytest.raises(exceptions.BadRequestError, match='"cluster" must be one of "MAN", "SAS", "VLA", "MYT", "IVA"'):
        call(component_service.set_component_as_default, set_default_req_pb, LOGIN)

    set_default_req_pb.cluster = 'SAS'
    call(component_service.set_component_as_default, set_default_req_pb, LOGIN)

    with pytest.raises(exceptions.BadRequestError, match='Component is already marked as default in SAS'):
        call(component_service.set_component_as_default, set_default_req_pb, LOGIN)

    for check in checker:
        with check:
            get_req_pb = api_pb2.GetComponentRequest(type=model_pb2.ComponentMeta.PGINX_BINARY, version=VERSIONS[0])
            component_pb = call(component_service.get_component, get_req_pb, LOGIN).component
            assert component_pb.status.published.marked_as_default['SAS'].author == LOGIN
            assert list(component_pb.status.published.marked_as_default) == ['SAS']

    set_default_req_pb.cluster = 'MAN'
    call(component_service.set_component_as_default, set_default_req_pb, LOGIN2)
    for check in checker:
        with check:
            get_req_pb = api_pb2.GetComponentRequest(type=model_pb2.ComponentMeta.PGINX_BINARY, version=VERSIONS[0])
            component_pb = call(component_service.get_component, get_req_pb, LOGIN).component
            assert component_pb.status.published.marked_as_default['MAN'].author == LOGIN2
            assert sorted(list(component_pb.status.published.marked_as_default)) == ['MAN', 'SAS']

    set_default_req_pb.version = VERSIONS[1]
    call(component_service.set_component_as_default, set_default_req_pb, LOGIN)
    for check in checker:
        with check:
            get_req_pb = api_pb2.GetComponentRequest(type=model_pb2.ComponentMeta.PGINX_BINARY, version=VERSIONS[1])
            component_pb = call(component_service.get_component, get_req_pb, LOGIN).component
            assert component_pb.status.published.marked_as_default['MAN'].author == LOGIN
            assert list(component_pb.status.published.marked_as_default) == ['MAN']

            get_req_pb = api_pb2.GetComponentRequest(type=model_pb2.ComponentMeta.PGINX_BINARY, version=VERSIONS[0])
            component_pb = call(component_service.get_component, get_req_pb, LOGIN).component
            assert list(component_pb.status.published.marked_as_default) == ['SAS']

    retire_req_pb = api_pb2.RetireComponentRequest()
    retire_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
    retire_req_pb.version = VERSIONS[0]
    retire_req_pb.superseded_by = VERSIONS[1]
    with pytest.raises(exceptions.BadRequestError, match='Default components can not be retired'):
        call(component_service.retire_component, retire_req_pb, LOGIN)


def test_draft_endpoint_certs_root():
    set_login_to_root_users(LOGIN)
    draft_req_pb = api_pb2.DraftComponentRequest()
    draft_req_pb.type = model_pb2.ComponentMeta.ENDPOINT_ROOT_CERTS
    draft_req_pb.startrek_issue_key = 'AWACS-454'
    draft_req_pb.version = '0.0.1'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(component_service.draft_component, draft_req_pb, LOGIN)
    assert six.text_type(e.value) == 'one of ("spec.source.sandbox_resource", "spec.source.url_file") must be set for selected component type'

    draft_req_pb.spec.source.url_file.url = 'crls.yandex.net'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(component_service.draft_component, draft_req_pb, LOGIN)
    assert six.text_type(e.value) == '"spec.source.url_file.url": is not valid URL'

    draft_req_pb.spec.source.url_file.url = 'https://crls.yandex.net/allCAs.pem'
    call(component_service.draft_component, draft_req_pb, LOGIN)
