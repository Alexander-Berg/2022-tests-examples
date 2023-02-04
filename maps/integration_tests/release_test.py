import dataclasses
import logging
import typing as tp
from unittest.mock import patch

import pytest

from maps.infra.sedem.client.machine_api import (
    MachineApi, MachineApiError, MachineBadRequestError, MachineConflictError
)
from maps.infra.sedem.client.sedem_api import SedemApi
from maps.infra.sedem.proto import sedem_pb2
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture, MongoFixture
from maps.pylibs.fixtures.api_fixture import ABCFixture
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.arc_fixture import ArcFixture
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture
from maps.pylibs.fixtures.matchers import Match
from maps.pylibs.fixtures.pushok_fixture import PushokFixture
from maps.pylibs.fixtures.st_fixture import STFixture
from maps.pylibs.fixtures.sandbox.resources import YaPackageResource, YtGardenModuleBinaryResource
from maps.pylibs.fixtures.sandbox.tasks import (  # noqa
    SedemMachineReleaseHotfix, SedemMachineReleaseHotfixDefinition, SedemMachineReleaseBuildDefinition
)
from maps.pylibs.fixtures.nanny_fixture import NannyFixture


logger = logging.getLogger(__name__)
FAKE_OAUTH = 'OAuth AQAD-FAKE'
DeployStatusProto = sedem_pb2.DeployCommitRequest.DeployStatus


class Component:
    @dataclasses.dataclass
    class Abc:
        id: int
        slug: str
        members: dict[str, list[str]]

    subsystem: str
    name: str
    abc: Abc
    followers: list[str]

    def __init__(self, name: str, abc: Abc, followers: set[str]):
        _, subsystem, service_name = name.split('-', maxsplit=2)
        self.subsystem = subsystem
        self.name = service_name
        self.abc = abc
        self.followers = followers

    def canonical(self) -> str:
        return f'{self.subsystem}-{self.name}'

    def module_name(self) -> str:
        return self.name.replace('-', '_')

    def summary_name(self) -> str:
        return f'{self.subsystem} {self.name.replace("-", " ")}'.title()

    def resource_name(self) -> str:
        if self.subsystem == 'core':
            return self.canonical()
        if self.subsystem == 'garden':
            return self.module_name()
        raise NotImplementedError()

    def __str__(self) -> str:
        return f'maps-{self.subsystem}-{self.name}'


TEST_COMPONENTS = [
    Component(
        'maps-core-teacup',
        Component.Abc(
            123,
            'maps-core-teacup',
            {'developer': ['comradeandrew', 'khrolenko', 'vmazaev', 'alexey-savin']},
        ),
        {'robot-maps-sandbox'}
    ),
    Component(
        'maps-garden-ymapsdf',
        Component.Abc(
            122,
            'maps-core-garden',
            {'developer': ['comradeandrew', 'khrolenko', 'vmazaev', 'alexey-savin']},
        ),
        {'robot-maps-sandbox'}
    ),
    Component(
        'maps-garden-ymapsdf',
        Component.Abc(
            122,
            'maps-core-garden',
            {
                'developer': ['comradeandrew', 'khrolenko', 'vmazaev', 'alexey-savin'],
                'maps_garden_module_developer': ['alexbobkov']
            },
        ),
        {'robot-maps-sandbox', 'alexbobkov'}
    ),
]


def setup_abc_fixture(abc: ABCFixture, component: Component):
    abc.add_service(component.abc.id, component.abc.slug)
    for role, members in component.abc.members.items():
        abc.add_members(component.abc.slug, members, role)


class TestRelease:

    @staticmethod
    def configuration_update(configs: tp.List[sedem_pb2.ServiceConfig], revision: int):
        sedem_api = SedemApi(FAKE_OAUTH)
        for config in configs:
            request = sedem_pb2.UpdateConfigurationRequest(
                service_config=config,
            )
            sedem_api.configuration_update(
                configuration=request,
                revision=revision
            )

    def initialize_configs(self):
        teacup_config = sedem_pb2.ServiceConfig(
            name="maps-core-teacup",
            path="maps/infra/teacup",
            abc_slug="maps-core-teacup",
            release_type=sedem_pb2.ServiceConfig.ReleaseType.NANNY,
            stages=[
                sedem_pb2.ServiceConfig.Stage(name="testing", deploy_units=["testing", "load"]),
                sedem_pb2.ServiceConfig.Stage(name="stable", deploy_units=["stable"]),
            ],
        )
        ymapsdf = sedem_pb2.ServiceConfig(
            name="maps-garden-ymapsdf",
            path="maps/garden/modules/ymapsdf",
            abc_slug="maps-core-garden",
            release_type=sedem_pb2.ServiceConfig.ReleaseType.GARDEN,
            stages=[
                sedem_pb2.ServiceConfig.Stage(name="testing", deploy_units=["testing"]),
                sedem_pb2.ServiceConfig.Stage(name="stable", deploy_units=["stable"]),
            ],
        )
        teaspoon_config = sedem_pb2.ServiceConfig(
            name='maps-core-teaspoon',
            path='maps/infra/teaspoon',
            abc_slug='maps-core-teaspoon',
            release_type=sedem_pb2.ServiceConfig.ReleaseType.NANNY,
            sox=True,
            stages=[
                sedem_pb2.ServiceConfig.Stage(name='testing', deploy_units=['testing']),
                sedem_pb2.ServiceConfig.Stage(name='prestable', deploy_units=['prestable']),
                sedem_pb2.ServiceConfig.Stage(name='stable', deploy_units=['stable']),
            ],
        )
        self.configuration_update(
            configs=[teacup_config, ymapsdf, teaspoon_config],
            revision=6536417
        )

    @pytest.mark.parametrize('component', TEST_COMPONENTS)
    def test_release_create(self, fixture_factory, component: Component):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arc_fixture: ArcFixture = fixture_factory(ArcFixture)

        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()
        setup_abc_fixture(api_fixture.abc, component)

        revision_to_release = 100
        commit_info = arcadia_fixture.commit_info(revision=revision_to_release)
        resource_attributes = {
            **commit_info.sandbox_attributes(),
            'resource_name': component.resource_name()
        }
        if component.subsystem == 'core':
            YaPackageResource(attributes=resource_attributes)
        else:
            YtGardenModuleBinaryResource(attributes=resource_attributes)

        machine_api = MachineApi(FAKE_OAUTH)
        created = machine_api.release_create(
            service_name=str(component),
            major_version=1,
            revision=revision_to_release,
            message='First release'
        )
        lookup_response = machine_api.release_lookup(
            service_name=str(component),
            limit=10,
        )
        assert lookup_response.releases[0].id == created.release_id

        get_response = machine_api.release_get(release_id=created.release_id)
        assert get_response.release.id == created.release_id
        assert get_response.release.WhichOneof('Completion') == 'ready'

        branches = arc_fixture.list_branches()
        assert branches == Match.Contains(
            Match.EndsWith(f'/{component.subsystem}-{component.name}-1'),
            Match.EndsWith(f'/{component.subsystem}-{component.name}-1.1')
        )

    @pytest.mark.parametrize('component', TEST_COMPONENTS)
    def test_release_create_startrek_tickets(self, fixture_factory, component: Component):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa

        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()

        setup_abc_fixture(api_fixture.abc, component)

        release1_revision = 99
        release2_revision = 100
        for revision in (release1_revision, release2_revision):
            commit_info = arcadia_fixture.commit_info(revision)
            resource_attributes = {
                **commit_info.sandbox_attributes(),
                'resource_name': component.resource_name()
            }
            if component.subsystem == 'core':
                YaPackageResource(attributes=resource_attributes)
            else:
                YtGardenModuleBinaryResource(attributes=resource_attributes)

        machine_api = MachineApi(FAKE_OAUTH)
        release1_id = machine_api.release_create(
            service_name=str(component),
            major_version=1,
            revision=release1_revision,
            message='First release'
        )
        response1 = machine_api.release_get(release_id=release1_id.release_id)
        release2_id = machine_api.release_create(
            service_name=str(component),
            major_version=2,
            revision=release2_revision,
            message='Second release'
        )
        response2 = machine_api.release_get(release_id=release2_id.release_id)
        ticket1 = api_fixture.startrek.get_issue('MAPSRELEASES-1')
        ticket2 = api_fixture.startrek.get_issue('MAPSRELEASES-2')
        assert set(ticket1.tags) == {f'service_name_{component}', 'major_1', f'sm_release_{component}_major_1'}
        assert response1.release.st_ticket == 'MAPSRELEASES-1'
        assert response2.release.st_ticket == 'MAPSRELEASES-2'
        assert ticket2.links == [ticket1.key]
        assert set(ticket1.followers) == component.followers
        assert set(ticket2.followers) == component.followers
        assert ticket2.summary == f'Release {component.summary_name()} #2'
        assert ticket2.description == 'Empty changelog for tests'

    def test_release_create_invalid_revision(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        self.initialize_configs()

        machine_api = MachineApi(FAKE_OAUTH)
        service_name = 'maps-core-teacup'
        revision = 100500
        with pytest.raises(MachineApiError, match=f'Revision r{revision} not found in arcadia'):
            machine_api.release_create(
                service_name=service_name,
                major_version=1,
                revision=revision,
                message='First release'
            )
        lookup_response = machine_api.release_lookup(
            service_name='maps-core-teacup',
            limit=10,
        )
        assert len(lookup_response.releases) == 0

    def test_release_lookup(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()

        machine_api = MachineApi(FAKE_OAUTH)
        for major_version, revision in enumerate(range(91, 101), start=1):  # 1 -> 10 major releases
            commit_info = arcadia_fixture.commit_info(revision)
            YaPackageResource(attributes={
                **commit_info.sandbox_attributes(),
                'resource_name': 'core-teacup'
            })
            machine_api.release_create(
                service_name='maps-core-teacup',
                major_version=major_version,
                revision=revision,
                message=f'Release on {revision}'
            )
        lookup_first_page = machine_api.release_lookup(
            service_name='maps-core-teacup',
            limit=5
        )
        lookup_next_page = machine_api.release_lookup(
            service_name='maps-core-teacup',
            limit=5,
            next_=lookup_first_page.next
        )
        all_releases = list(lookup_first_page.releases) + list(lookup_next_page.releases)
        first_release, *_, last_release = all_releases
        assert first_release.major == 10
        assert last_release.major == 1

    @pytest.mark.parametrize('component', TEST_COMPONENTS)
    def test_release_lookup_by_filter(self, fixture_factory, component: Component):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()
        setup_abc_fixture(api_fixture.abc, component)

        machine_api = MachineApi(FAKE_OAUTH)
        revisions, versions = (10, 20), (1, 2)
        arc_hashes = []
        for major_version, revision in zip(versions, revisions):
            commit_info = arcadia_fixture.commit_info(revision)
            resource_attributes = {
                **commit_info.sandbox_attributes(),
                'resource_name': component.resource_name(), 'branch': 'trunk',
            }
            if component.subsystem == 'core':
                YaPackageResource(attributes=resource_attributes)
            else:
                YtGardenModuleBinaryResource(attributes=resource_attributes)
            machine_api.release_create(
                service_name=str(component),
                major_version=major_version,
                revision=revision,
                message=f'Release on {revision}'
            )
            arc_hashes.append(
                arcadia_fixture.commit_info(revision=revision).arc_hash
            )

        for major_version, revision, arc_hash in zip(versions, revisions, arc_hashes):
            lookup_page = machine_api.release_lookup(
                service_name=str(component),
                version=f'v{major_version}.1',
                revision=revision,
                arc_commit_hash=arc_hash,
                status='ready',
                limit=10
            )
            all_releases = list(lookup_page.releases)
            assert len(all_releases) == 1
            release = all_releases[0]
            assert release.origin_commit.revision == revision
            assert release.release_commit.revision == revision
            assert release.major == major_version

    @pytest.mark.parametrize('component', TEST_COMPONENTS)
    def test_release_hotfix(self, fixture_factory, component: Component):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arc_fixture: ArcFixture = fixture_factory(ArcFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()
        setup_abc_fixture(api_fixture.abc, component)

        machine_api = MachineApi(FAKE_OAUTH)
        revision = 98
        hotfix1_revision = 99
        hotfix2_revision = 100
        commit_info = arcadia_fixture.commit_info(revision)
        resource_attributes = {
            **commit_info.sandbox_attributes(),
            'resource_name': component.resource_name()
        }
        if component.subsystem == 'core':
            YaPackageResource(attributes=resource_attributes)
        else:
            YtGardenModuleBinaryResource(attributes=resource_attributes)
        lookup_response = machine_api.release_lookup(
            service_name=str(component),
        )
        assert len(lookup_response.releases) == 0
        release_create_response = machine_api.release_create(
            service_name=str(component),
            major_version=1,
            revision=revision,
            message=f'Release on r{revision}'
        )
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release

        release_hotfix_response = machine_api.release_hotfix(
            service_name=str(component),
            major_version=release11.major,
            revision=hotfix1_revision,
        )
        release_get_response = machine_api.release_get(release_id=release_hotfix_response.release_id)
        release12 = release_get_response.release
        assert release12.major == 1 and release12.minor == 2
        assert release12.origin_commit.revision == hotfix1_revision
        assert release12.st_ticket == release11.st_ticket
        assert release12.release_commit.arc_commit_hash
        assert release12.WhichOneof('Completion') == 'ready'
        assert arc_fixture.list_branches() == Match.Contains(f'tags/releases/maps/{component.canonical()}-1.2')

        release_hotfix_response = machine_api.release_hotfix(
            service_name=str(component),
            major_version=release11.major,
            revision=hotfix2_revision,
        )
        release_get_response = machine_api.release_get(release_id=release_hotfix_response.release_id)
        release13 = release_get_response.release
        # Minor release is incremented after ready
        assert release13.major == 1 and release13.minor == 3
        assert release13.origin_commit.revision == hotfix2_revision
        assert release13.st_ticket == release11.st_ticket
        assert release13.release_commit.arc_commit_hash
        assert release13.WhichOneof('Completion') == 'ready'
        assert arc_fixture.list_branches() == Match.Contains(f'tags/releases/maps/{component.canonical()}-1.3')

    @pytest.mark.parametrize('component', TEST_COMPONENTS)
    def test_release_hotfix_fail_build(self, fixture_factory, component):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()
        setup_abc_fixture(api_fixture.abc, component)

        machine_api = MachineApi(FAKE_OAUTH)
        revision = 98
        hotfix1_revision = 99
        hotfix2_revision = 100

        commit_info = arcadia_fixture.commit_info(revision)
        resource_attributes = {
            **commit_info.sandbox_attributes(),
            'resource_name': component.resource_name()
        }
        if component.subsystem == 'core':
            YaPackageResource(attributes=resource_attributes)
        else:
            YtGardenModuleBinaryResource(attributes=resource_attributes)
        lookup_response = machine_api.release_lookup(
            service_name=str(component),
        )
        assert len(lookup_response.releases) == 0
        release_create_response = machine_api.release_create(
            service_name=str(component),
            major_version=1,
            revision=revision,
            message=f'Release on r{revision}'
        )
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release

        with patch.object(SedemMachineReleaseBuildDefinition,
                          'successful_execution', False):
            release_hotfix_response = machine_api.release_hotfix(
                service_name=str(component),
                major_version=release11.major,
                revision=hotfix1_revision,
            )
        release_get_response = machine_api.release_get(release_id=release_hotfix_response.release_id)
        release12 = release_get_response.release
        assert release12.major == 1 and release12.minor == 2
        assert release12.WhichOneof('Completion') == 'broken'
        assert release12.broken.reason.startswith('Build failed: Failed to build')

        with patch.object(SedemMachineReleaseBuildDefinition,
                          'successful_execution', True):
            release_hotfix_response = machine_api.release_hotfix(
                service_name=str(component),
                major_version=release11.major,
                revision=hotfix2_revision,
            )
        release_get_response = machine_api.release_get(release_id=release_hotfix_response.release_id)
        release13 = release_get_response.release
        # Minor release is incremented after build failure
        assert release13.major == 1 and release13.minor == 3
        assert release13.WhichOneof('Completion') == 'ready'

    @pytest.mark.parametrize('component', TEST_COMPONENTS)
    def test_release_hotfix_fail_hotfix(self, fixture_factory, component: Component):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()
        setup_abc_fixture(api_fixture.abc, component)

        machine_api = MachineApi(FAKE_OAUTH)
        revision = 98
        hotfix1_revision = 99
        hotfix2_revision = 100
        commit_info = arcadia_fixture.commit_info(revision)
        resource_attributes = {
            **commit_info.sandbox_attributes(),
            'resource_name': component.resource_name()
        }
        if component.subsystem == 'core':
            YaPackageResource(attributes=resource_attributes)
        else:
            YtGardenModuleBinaryResource(attributes=resource_attributes)
        lookup_response = machine_api.release_lookup(
            service_name=str(component),
        )
        assert len(lookup_response.releases) == 0
        release_create_response = machine_api.release_create(
            service_name=str(component),
            major_version=1,
            revision=revision,
            message=f'Release on r{revision}'
        )
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release

        with patch.object(SedemMachineReleaseHotfixDefinition,
                          'successful_execution', False):
            release_hotfix_response = machine_api.release_hotfix(
                service_name=str(component),
                major_version=release11.major,
                revision=hotfix1_revision,
            )
        release_get_response = machine_api.release_get(release_id=release_hotfix_response.release_id)
        release12 = release_get_response.release
        assert release12.major == 1 and release12.minor == 2
        assert release12.WhichOneof('Completion') == 'broken'
        assert release12.broken.reason.startswith('Hotfix failed: Failed to hotfix')

        with patch.object(SedemMachineReleaseHotfixDefinition,
                          'successful_execution', True):
            release_hotfix_response = machine_api.release_hotfix(
                service_name=str(component),
                major_version=release11.major,
                revision=hotfix2_revision,
            )
        release_get_response = machine_api.release_get(release_id=release_hotfix_response.release_id)
        release12 = release_get_response.release
        # Minor release always increments
        assert release12.major == 1 and release12.minor == 3
        assert release12.WhichOneof('Completion') == 'ready'

    def test_release_hotfix_invalid_revision(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        self.initialize_configs()

        machine_api = MachineApi(FAKE_OAUTH)
        hotfix_revision = 100500

        with pytest.raises(MachineApiError, match=f'Revision r{hotfix_revision} not found in arcadia'):
            machine_api.release_hotfix(
                service_name='maps-core-teacup',
                major_version=1,
                revision=hotfix_revision,
            )

    def test_release_deploy(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()

        release_revision = 100
        commit_info = arcadia_fixture.commit_info(release_revision)
        YaPackageResource(attributes={
            **commit_info.sandbox_attributes(),
            'resource_name': 'core-teacup',
        })

        machine_api = MachineApi(FAKE_OAUTH)
        release_create_response = machine_api.release_create(
            service_name='maps-core-teacup',
            major_version=1,
            revision=release_revision,
            message='First release'
        )
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release = release_get_response.release
        assert len(release.deploys) == 0

        release_deploy_validate_result = machine_api.release_deploy_validate(
            service_name='maps-core-teacup',
            version=f'v{release.major}.{release.minor}',
            step='testing'
        )
        assert len(release_deploy_validate_result.checks) == 0

        release_deploy_prepare_response = machine_api.release_deploy_prepare(
            service_name='maps-core-teacup',
            version=f'v{release.major}.{release.minor}',
            step='testing'
        )

        assert set(release_deploy_prepare_response.deploys.keys()) == {'testing', 'load'}

        machine_api.release_deploy_commit(
            service_name='maps-core-teacup',
            version=f'v{release.major}.{release.minor}',
            step='testing',
            deploy_commit_request=sedem_pb2.DeployCommitRequest(
                deploys={
                    deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                    for deploy_id in release_deploy_prepare_response.deploys.values()
                }
            ),
        )

        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release = release_get_response.release
        assert len(release.deploys) == 2
        for deploy in release.deploys:
            assert deploy.deploy_unit in ['testing', 'load']

    def test_release_deploy_broken(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        self.initialize_configs()

        release_revision = 100
        commit_info = arcadia_fixture.commit_info(release_revision)
        YaPackageResource(attributes={
            **commit_info.sandbox_attributes(),
            'resource_name': 'core-teacup',
        })

        machine_api = MachineApi(FAKE_OAUTH)
        release_create_response = machine_api.release_create(
            service_name='maps-core-teacup',
            major_version=1,
            revision=release_revision,
            message='First release'
        )
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release = release_get_response.release
        assert len(release.deploys) == 0
        mongo = machine_fixture.mongo()
        broken_reason = 'Release has been broken by black magic!'
        mongo.update_one(
            'release',
            {'service_name': release.service_name, 'major': release.major, 'minor': release.minor},
            update={'$set': {'completion': {'broken': {'build': {'reason': broken_reason}}}}}
        )

        with pytest.raises(MachineBadRequestError, match=fr'{broken_reason}'):
            machine_api.release_deploy_validate(
                service_name='maps-core-teacup',
                version=f'v{release.major}.{release.minor}',
                step='testing'
            )
        with pytest.raises(MachineBadRequestError,
                           match=r'No ready release v1.1 for maps-core-teacup found'):
            machine_api.release_deploy_prepare(
                service_name='maps-core-teacup',
                version=f'v{release.major}.{release.minor}',
                step='testing'
            )

    def test_nonexistent_release_deploy(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)  # noqa
        self.initialize_configs()

        machine_api = MachineApi(FAKE_OAUTH)
        with pytest.raises(MachineBadRequestError,
                           match=r'No release v1.1 for maps-core-teacup found'):
            machine_api.release_deploy_validate(
                service_name='maps-core-teacup',
                version='v1.1',
                step='testing'
            )
        with pytest.raises(MachineBadRequestError,
                           match=r'No ready release v1.1 for maps-core-teacup found'):
            machine_api.release_deploy_prepare(
                service_name='maps-core-teacup',
                version='v1.1',
                step='testing'
            )

    def test_release_set_message(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arc_fixture: ArcFixture = fixture_factory(ArcFixture)  # noqa
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()

        revision_to_release = 100
        commit_info = arcadia_fixture.commit_info(revision_to_release)
        YaPackageResource(attributes={
            **commit_info.sandbox_attributes(),
            'resource_name': 'core-teacup',
        })

        machine_api = MachineApi(FAKE_OAUTH)
        created = machine_api.release_create(
            service_name='maps-core-teacup',
            major_version=1,
            revision=revision_to_release,
            message='First release'
        )

        get_response = machine_api.release_get(release_id=created.release_id)
        assert get_response.release.message == 'First release'
        release = get_response.release

        set_message_response = machine_api.release_set_message(
            service_name='maps-core-teacup',
            version=f'v{release.major}.{release.minor}',
            message='New release message',
        )
        assert set_message_response.release_id == created.release_id

        get_response = machine_api.release_get(release_id=created.release_id)
        assert get_response.release.message == 'New release message'

        machine_api.release_set_message(
            service_name='maps-core-teacup',
            version=f'v{release.major}.{release.minor}',
            message=None
        )

        get_response = machine_api.release_get(release_id=created.release_id)
        assert not get_response.release.message

    def test_release_reject(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa
        arc_fixture: ArcFixture = fixture_factory(ArcFixture)  # noqa
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        self.initialize_configs()

        revision_to_release = 100
        commit_info = arcadia_fixture.commit_info(revision_to_release)
        YaPackageResource(attributes={
            **commit_info.sandbox_attributes(),
            'resource_name': 'core-teacup',
        })

        machine_api = MachineApi(FAKE_OAUTH)
        created = machine_api.release_create(
            service_name='maps-core-teacup',
            major_version=1,
            revision=revision_to_release,
            message='First release'
        )

        get_response = machine_api.release_get(release_id=created.release_id)
        assert not get_response.release.rejected.reason
        release = get_response.release

        release_reject_response = machine_api.release_reject(
            service_name='maps-core-teacup',
            version=f'v{release.major}.{release.minor}',
            reason='Some reason'
        )
        assert release_reject_response.release_id == created.release_id

        get_response = machine_api.release_get(release_id=created.release_id)
        assert get_response.release.rejected.reason == 'Some reason'

    @pytest.mark.parametrize('component', TEST_COMPONENTS)
    def test_status_description(self, fixture_factory, component):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        self.initialize_configs()
        setup_abc_fixture(api_fixture.abc, component)

        machine_api = MachineApi(FAKE_OAUTH)
        revision = 98
        hotfix_revision = 99
        commit_info = arcadia_fixture.commit_info(revision)
        resource_attributes = {
            **commit_info.sandbox_attributes(),
            'resource_name': component.resource_name()
        }
        if component.subsystem == 'core':
            YaPackageResource(attributes=resource_attributes)
        else:
            YtGardenModuleBinaryResource(attributes=resource_attributes)

        release_create_response = machine_api.release_create(
            service_name=str(component),
            major_version=1,
            revision=revision,
            message=f'Release on r{revision}'
        )
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release

        assert release_get_response.description == (
            f'Release v1.1 (r{revision}) for {component} is ready'
        )

        release_hotfix_response = machine_api.release_hotfix(
            service_name=str(component),
            major_version=release11.major,
            revision=hotfix_revision,
        )
        release_get_response = machine_api.release_get(release_id=release_hotfix_response.release_id)

        tasks = api_fixture.sandbox.tasks()
        hotfix_task_id = next(task.id for task in tasks if task.type == 'SEDEM_MACHINE_RELEASE_HOTFIX')
        build_task_id = next(task.id for task in tasks if task.type == 'SEDEM_MACHINE_RELEASE_BUILD')

        assert release_get_response.description == (
            f'Release v1.2 (r{hotfix_revision}) for {component} is ready\n'
            f'Hotfix task [READY]: https://sandbox.yandex-team.ru/task/{hotfix_task_id}\n'
            f'Build task [READY]: https://sandbox.yandex-team.ru/task/{build_task_id}'
        )

        mongo: MongoFixture = machine_fixture.mongo()
        mongo.update_one(
            'release',
            {'service_name': str(component), 'major': 1, 'minor': 2},
            {'$set': {'completion': {'preparing': {'build': {}}}}}
        )
        mongo.update_one(
            'operation',
            {'operation_id': str(build_task_id)},
            {'$unset': {'sandbox_status': ''}}
        )
        release_get_response = machine_api.release_get(release_id=release_get_response.release.id)

        assert release_get_response.description == (
            f'Release v1.2 (r{hotfix_revision}) for {component} is building\n'
            f'Hotfix task [READY]: https://sandbox.yandex-team.ru/task/{hotfix_task_id}\n'
            f'Build task [RUNNING]: https://sandbox.yandex-team.ru/task/{build_task_id}'
        )

        mongo.update_one(
            'release',
            {'service_name': str(component), 'major': 1, 'minor': 2},
            {'$set': {'completion': {'broken': {'build': {'reason': 'whatever'}}}}}
        )
        mongo.update_one(
            'operation',
            {'operation_id': str(build_task_id)},
            {'$set': {'sandbox_status': 'broken', 'sandbox_error': 'whatever'}}
        )
        release_get_response = machine_api.release_get(release_id=release_get_response.release.id)

        assert release_get_response.description == (
            f'Release v1.2 (r{hotfix_revision}) for {component} is broken\n'
            f'Hotfix task [READY]: https://sandbox.yandex-team.ru/task/{hotfix_task_id}\n'
            f'Build task [BROKEN]: https://sandbox.yandex-team.ru/task/{build_task_id}'
        )

        mongo.update_one(
            'release',
            {'service_name': str(component), 'major': 1, 'minor': 2},
            {'$set': {'completion': {'preparing': {'hotfix': {}}}}}
        )
        mongo.update_one(
            'operation',
            {'operation_id': str(hotfix_task_id)},
            {'$unset': {'sandbox_status': ''}}
        )
        release_get_response = machine_api.release_get(release_id=release_get_response.release.id)

        assert release_get_response.description == (
            f'Release v1.2 (r{hotfix_revision}) for {component} is merging\n'
            f'Hotfix task [RUNNING]: https://sandbox.yandex-team.ru/task/{hotfix_task_id}'
        )

    def test_approve_deploy(self, fixture_factory):
        # Test initialization
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        abc_fixture: ABCFixture = api_fixture.abc
        pushok_fixture: PushokFixture = api_fixture.pushok
        st_fixture: STFixture = api_fixture.startrek
        nanny_fixture: NannyFixture = api_fixture.nanny
        self.initialize_configs()
        abc_fixture.add_service(123, 'maps-core-teaspoon')
        abc_fixture.add_members('maps-core-teaspoon', ['comradeandrew', 'khrolenko', 'vmazaev', 'alexey-savin'])

        # Release initialization
        machine_api = MachineApi(FAKE_OAUTH)
        revision = 98
        commit_info = arcadia_fixture.commit_info(revision)
        resource_attributes = {
            **commit_info.sandbox_attributes(),
            'resource_name': 'core-teaspoon'
        }
        YaPackageResource(attributes=resource_attributes)

        release_create_response = machine_api.release_create(
            service_name='maps-core-teaspoon',
            major_version=1,
            revision=revision,
            message=f'Release on r{revision}'
        )
        # Testing is deployed without any approval
        release_deploy_validate_result = machine_api.release_deploy_validate(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='testing'
        )
        assert len(release_deploy_validate_result.checks) == 0

        release_deploy_prepare_result = machine_api.release_deploy_prepare(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='testing'
        )
        assert set(release_deploy_prepare_result.deploys.keys()) == {'testing'}
        machine_api.release_deploy_commit(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='testing',
            deploy_commit_request=sedem_pb2.DeployCommitRequest(
                deploys={
                    deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                    for deploy_id in release_deploy_prepare_result.deploys.values()
                }
            ),
        )
        nanny_fixture.set_docker_tag('maps_core_teaspoon_testing',
                                     'maps/core-teaspoon:90',
                                     comment=f'Release...\n\n'
                                             f'deploy_id:{release_deploy_prepare_result.deploys["testing"]}')

        # First deploy to prestable requires approval
        release_deploy_validate_result = machine_api.release_deploy_validate(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='prestable'
        )
        assert len(release_deploy_validate_result.checks) == 0

        with pytest.raises(MachineConflictError, match='not approved yet'):
            machine_api.release_deploy_prepare(
                service_name='maps-core-teaspoon',
                version='v1.1',
                step='prestable'
            )
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release

        ticket = st_fixture.get_issue(release11.st_ticket)

        # Check approvement form is created
        prestable_uuid = pushok_fixture.uuid_from_st_ticket(st_ticket=release11.st_ticket, release_version='v1.1')
        assert any(f'https://ok.yandex-team.ru/approvements/{prestable_uuid}' in comment
                   for comment in ticket.comments)

        # Approve prestable
        pushok_fixture.approve(prestable_uuid)
        machine_fixture.wait_for_approval_update()

        # Next request actually finishes deployment
        release_deploy_validate_result = machine_api.release_deploy_validate(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='prestable'
        )
        assert len(release_deploy_validate_result.checks) == 0

        release_deploy_prepare_result = machine_api.release_deploy_prepare(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='prestable'
        )
        assert set(release_deploy_prepare_result.deploys.keys()) == {'prestable'}
        nanny_fixture.set_docker_tag('maps_core_teaspoon_prestable',
                                     'maps/core-teaspoon:98',
                                     comment=f'Release...\n\n'
                                             f'deploy_id:{release_deploy_prepare_result.deploys["prestable"]}')

        machine_api.release_deploy_commit(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='prestable',
            deploy_commit_request=sedem_pb2.DeployCommitRequest(
                deploys={
                    deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                    for deploy_id in release_deploy_prepare_result.deploys.values()
                }
            ),
        )
        machine_fixture.wait_for_deploy_status_update()

        # Check deployment finished
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release
        prestable_deploy = next(deploy for deploy in release11.deploys
                                if deploy.deploy_unit == 'prestable')
        assert prestable_deploy.WhichOneof('DeployStatus') == 'success'

        # Subsequent deployments don't require approve
        release_deploy_validate_result = machine_api.release_deploy_validate(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='prestable'
        )
        assert len(release_deploy_validate_result.checks) == 0

        release_deploy_prepare_result = machine_api.release_deploy_prepare(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='prestable'
        )
        assert set(release_deploy_prepare_result.deploys.keys()) == {'prestable'}
        machine_api.release_deploy_commit(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='prestable',
            deploy_commit_request=sedem_pb2.DeployCommitRequest(
                deploys={
                    deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                    for deploy_id in release_deploy_prepare_result.deploys.values()
                }
            ),
        )

    def test_decline_deploy(self, fixture_factory):
        # Test initialization
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        abc_fixture: ABCFixture = api_fixture.abc
        pushok_fixture: PushokFixture = api_fixture.pushok
        self.initialize_configs()
        abc_fixture.add_service(123, 'maps-core-teaspoon')
        abc_fixture.add_members('maps-core-teaspoon', ['comradeandrew', 'khrolenko', 'vmazaev', 'alexey-savin'])

        # Release initialization
        machine_api = MachineApi(FAKE_OAUTH)
        revision = 98
        commit_info = arcadia_fixture.commit_info(revision)
        resource_attributes = {
            **commit_info.sandbox_attributes(),
            'resource_name': 'core-teaspoon'
        }
        YaPackageResource(attributes=resource_attributes)

        release_create_response = machine_api.release_create(
            service_name='maps-core-teaspoon',
            major_version=1,
            revision=revision,
            message=f'Release on r{revision}'
        )
        release_id = release_create_response.release_id
        release_get_response = machine_api.release_get(release_id=release_id)
        release = release_get_response.release

        # Testing is deployed without any approval
        release_deploy_validate_result = machine_api.release_deploy_validate(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='testing'
        )
        assert len(release_deploy_validate_result.checks) == 0

        release_deploy_prepare_result = machine_api.release_deploy_prepare(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='testing'
        )
        assert set(release_deploy_prepare_result.deploys.keys()) == {'testing'}
        machine_api.release_deploy_commit(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='testing',
            deploy_commit_request=sedem_pb2.DeployCommitRequest(
                deploys={
                    deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                    for deploy_id in release_deploy_prepare_result.deploys.values()
                }
            ),
        )
        # First deploy to prestable requires approval
        release_deploy_validate_result = machine_api.release_deploy_validate(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='prestable'
        )
        assert len(release_deploy_validate_result.checks) == 0

        with pytest.raises(MachineConflictError, match='not approved yet'):
            machine_api.release_deploy_prepare(
                service_name='maps-core-teaspoon',
                version='v1.1',
                step='prestable'
            )

        # Decline prestable
        prestable_uuid = pushok_fixture.uuid_from_st_ticket(st_ticket=release.st_ticket, release_version='v1.1')
        pushok_fixture.decline(prestable_uuid, close=False)
        machine_fixture.wait_for_approval_update()

        # Check deployment is declined
        release_deploy_validate_result = machine_api.release_deploy_validate(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='prestable'
        )
        assert len(release_deploy_validate_result.checks) == 1
        assert 'declined' in release_deploy_validate_result.checks[0].message

        with pytest.raises(MachineConflictError, match='declined'):
            machine_api.release_deploy_prepare(
                service_name='maps-core-teaspoon',
                version='v1.1',
                step='prestable'
            )
        release_get_response = machine_api.release_get(release_id=release_id)
        release = release_get_response.release
        assert all(deploy for deploy in release.deploys
                   if deploy.deploy_unit != 'prestable')

    def test_update_deploy_status(self, fixture_factory):
        # Test initialization
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        nanny_fixture: NannyFixture = api_fixture.nanny
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        self.initialize_configs()

        # Release initialization
        machine_api = MachineApi(FAKE_OAUTH)
        revision = 98
        commit_info = arcadia_fixture.commit_info(revision)
        resource_attributes = {
            **commit_info.sandbox_attributes(),
            'resource_name': 'core-teaspoon'
        }
        YaPackageResource(attributes=resource_attributes)

        release_create_response = machine_api.release_create(
            service_name='maps-core-teaspoon',
            major_version=1,
            revision=revision,
            message=f'Release on r{revision}'
        )
        nanny_fixture.set_docker_tag('maps_core_teaspoon_testing',
                                     'maps/core-teaspoon:0')

        release_deploy_validate_result = machine_api.release_deploy_validate(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='testing'
        )
        assert len(release_deploy_validate_result.checks) == 0

        release_deploy_prepare_result = machine_api.release_deploy_prepare(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='testing'
        )
        assert set(release_deploy_prepare_result.deploys.keys()) == {'testing'}
        machine_api.release_deploy_commit(
            service_name='maps-core-teaspoon',
            version='v1.1',
            step='testing',
            deploy_commit_request=sedem_pb2.DeployCommitRequest(
                deploys={
                    deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                    for deploy_id in release_deploy_prepare_result.deploys.values()
                }
            ),
        )

        nanny_fixture.set_docker_tag_in_progress('maps_core_teaspoon_testing',
                                                 'maps/core-teaspoon:98')

        machine_fixture.wait_for_deploy_status_update()
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release
        assert len(release11.deploys) == 1

        assert release11.deploys[0].WhichOneof('DeployStatus') == 'executing'

        nanny_fixture.set_docker_tag('maps_core_teaspoon_testing',
                                     'maps/core-teaspoon:98',
                                     comment=f'Release...\n\n'
                                             f'deploy_id:{release_deploy_prepare_result.deploys["testing"]}')
        machine_fixture.wait_for_deploy_status_update()
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release
        assert len(release11.deploys) == 1

        assert release11.deploys[0].WhichOneof('DeployStatus') == 'success'

    def test_cancellation(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
        nanny_fixture: NannyFixture = api_fixture.nanny
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        self.initialize_configs()

        # Release initialization
        machine_api = MachineApi(FAKE_OAUTH)
        revision = 98
        commit_info = arcadia_fixture.commit_info(revision)
        resource_attributes = {
            **commit_info.sandbox_attributes(),
            'resource_name': 'core-teaspoon'
        }
        YaPackageResource(attributes=resource_attributes)

        release_create_response = machine_api.release_create(
            service_name='maps-core-teaspoon',
            major_version=1,
            revision=revision,
            message=f'Release on r{revision}'
        )
        deploys = []
        # make 2 concurrent deploys
        for _ in range(2):
            machine_api.release_deploy_validate(
                service_name='maps-core-teaspoon',
                version='v1.1',
                step='testing'
            )
            deploy_prepare_result = machine_api.release_deploy_prepare(
                service_name='maps-core-teaspoon',
                version='v1.1',
                step='testing'
            )
            machine_api.release_deploy_commit(
                service_name='maps-core-teaspoon',
                version='v1.1',
                step='testing',
                deploy_commit_request=sedem_pb2.DeployCommitRequest(
                    deploys={
                        deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                        for deploy_id in deploy_prepare_result.deploys.values()
                    }
                ),
            )
            deploys.append(deploy_prepare_result.deploys["testing"])
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release
        assert len(release11.deploys) == 2
        # check deploy cancelled
        assert {deploy.WhichOneof('DeployStatus') for deploy in release11.deploys} == {'cancelled', 'executing'}
        for deploy_id in deploys:
            # deploy to nanny
            nanny_fixture.set_docker_tag('maps_core_teaspoon_testing',
                                         'maps/core-teaspoon:98',
                                         comment=f'Release...\n\n'
                                                 f'deploy_id:{deploy_id}')
            machine_fixture.wait_for_deploy_status_update()
        release_get_response = machine_api.release_get(release_id=release_create_response.release_id)
        release11 = release_get_response.release
        assert len(release11.deploys) == 2
        assert {deploy.WhichOneof('DeployStatus') for deploy in release11.deploys} == {'cancelled', 'success'}
