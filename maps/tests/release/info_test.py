import logging
from datetime import datetime, timedelta

import isodate
import pytest
import typing as tp
from click import unstyle
from dataclasses import dataclass

from maps.infra.sedem.cli.commands.release import release_info, rollback
from maps.infra.sedem.cli.tests.release.fixtures.rendering_fixture import RenderingFixture
from maps.infra.sedem.cli.tests.release.utils.test_data import (
    assert_click_result,
    AcceptanceStatus,
    MachineCandidate,
    MachineRelease,
    ReleaseSpec,
    ReleaseStatus,
    set_sedem_machine,
    table_rows_as_list,
)
from maps.infra.sedem.common.release.sandbox.release_spec import SandboxReleaseSpec, SandboxDeployUnitSpec
from maps.infra.sedem.common.release.utils import ReleaseError
from maps.infra.sedem.machine.lib.collections import ReleaseCandidateStatus
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture, Commit
from maps.pylibs.fixtures.garden_fixture import GardenDeployUnit, GardenModule
from maps.pylibs.fixtures.matchers import Match
from maps.pylibs.fixtures.sandbox.resources import (
    GardenUploadModuleBinary,
    MapsBuildBinaryTask,
    Resource,
    SandboxTasksBinaryResource,
    Task,
    YaPackage,
    YaPackageResource,
    YtGardenModuleBinaryResource,
)
from maps.pylibs.terminal_utils.utils import red, yellow, green, cyan, color_login_in_text

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


def format_verbose_time(time: datetime) -> str:
    return time.astimezone(isodate.LOCAL).strftime('%Y-%m-%d %H:%M:%S')


def format_commit_hash(commit: Commit) -> str:
    return commit.arc_hash[:7]


def format_comment(commit: Commit) -> str:
    return color_login_in_text(f'(@{commit.author}) {commit.message}')


@dataclass
class ReleaseComponentTestCase:
    path: str
    display_name: str
    release_spec: tp.Optional[ReleaseSpec]
    deploy_type: str
    resource_type: tp.Type[Resource]
    resource_task_type: tp.Type[Task]
    resource_attrs: dict[str, str]

    def __str__(self) -> str:
        return self.deploy_type


COMPONENTS = [
    ReleaseComponentTestCase(
        path='maps/infra/teapot',
        display_name='maps-core-teapot',
        release_spec=None,
        deploy_type='nanny',
        resource_type=YaPackageResource,
        resource_task_type=YaPackage,
        resource_attrs={'resource_name': 'core-teapot'},
    ),
    ReleaseComponentTestCase(
        path='maps/garden/modules/backa_export',
        display_name='backa_export',
        release_spec=None,
        deploy_type='garden',
        resource_type=YtGardenModuleBinaryResource,
        resource_task_type=GardenUploadModuleBinary,
        resource_attrs={'resource_name': 'backa_export'},
    ),
    ReleaseComponentTestCase(
        path='maps/infra/ecstatic/sandbox',
        display_name='ecstatic-reconfigurator',
        release_spec=SandboxReleaseSpec(
            task_type='ECSTATIC_RECONFIGURATOR',
            resource_id=12345678,
            deploy_units=[
                SandboxDeployUnitSpec(name='postcommit_stable'),
                SandboxDeployUnitSpec(name='scheduler_stable'),
                SandboxDeployUnitSpec(name='postcommit_testing'),
                SandboxDeployUnitSpec(name='scheduler_testing'),
            ],
        ),
        deploy_type='sandbox',
        resource_type=SandboxTasksBinaryResource,
        resource_task_type=MapsBuildBinaryTask,
        resource_attrs={'service_name': 'maps-core-ecstatic-reconfigurator'},
    ),
]


@pytest.mark.parametrize('verbose', (False, True))
def test_nanny_state(fixture_factory,
                     rendering_fixture: RenderingFixture,
                     verbose: bool) -> None:
    component: ReleaseComponentTestCase = COMPONENTS[0]

    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

    commit1 = arcadia_fixture.set_commit(revision=1, author='john-doe', message='commit1')
    commit2 = arcadia_fixture.set_commit(revision=2, author='jane-doe', message='commit2')
    commit3 = arcadia_fixture.set_commit(revision=3, author='jane-doe', message='commit3')

    commit12_hash = arcadia_fixture.random_arc_hash()

    set_sedem_machine(
        fixture_factory,
        component.path,
        MachineRelease(version='v1.1', release_spec=None,
                       trunk_revision=1,
                       st_ticket='MAPSRELEASES-1', deployed_to=['stable', 'testing']),
        MachineRelease(version='v1.2', release_spec=None,
                       hotfix_hash=commit2.arc_hash, tag_generation=12, tag_hash=commit12_hash,
                       st_ticket='MAPSRELEASES-1', deployed_to='testing', rejected=True),
    )

    commit12 = arcadia_fixture.commit_info(arc_commit_hash=commit12_hash)

    def deploy_time(commit):
        return commit.time + timedelta(minutes=2)

    resource_name = component.resource_attrs['resource_name']
    api_fixture.nanny.set_docker_tag(
        service_name=f'maps_{resource_name.replace("-", "_")}_stable',
        docker_tag=f'maps/{resource_name}:{commit1.revision}',
        entered=deploy_time(commit1),
    )
    api_fixture.nanny.set_docker_tag(
        service_name=f'maps_{resource_name.replace("-", "_")}_testing',
        docker_tag=f'maps/{resource_name}:{commit12_hash}',
        entered=deploy_time(commit2),
    )
    api_fixture.nanny.set_docker_tag(
        service_name=f'maps_{resource_name.replace("-", "_")}_unstable',
        docker_tag=f'maps/{resource_name}:{commit3.revision}',
        entered=deploy_time(commit3),
    )

    assert_click_result(release_info, [component.path] + (['--verbose'] if verbose else []))
    state_table, *_ = rendering_fixture.tables

    def format_deploy_time(commit):
        return format_verbose_time(deploy_time(commit))

    def combine_rows(*deploy_units):
        return ['\n'.join(column) for column in zip(*deploy_units)]

    def shortened_link(deploy_unit):
        return api_fixture.url_shortener.shorten_url(
            f'https://nanny.yandex-team.ru/ui/#'
            f'/services/catalog/maps_{resource_name.replace("-", "_")}_{deploy_unit}/'
        )

    assert state_table.title == f'Service {component.display_name}'
    assert state_table.header == ['', 'Status', 'Version', 'St ticket', 'Comment']

    if verbose:
        assert table_rows_as_list(state_table) == [
            combine_rows(
                ['stable', f'Deployed ({format_deploy_time(commit1)})',
                 'v1.1 (r1)', 'MAPSRELEASES-1', unstyle(format_comment(commit1))],
                ['', shortened_link('stable'), '', '', ''],
            ),
            combine_rows(
                ['load', 'Not created', '-', '-', '-'],
                ['testing', f'Deployed ({format_deploy_time(commit2)})',
                 f'[REJECTED] v1.2 ({format_commit_hash(commit12)})',
                 'MAPSRELEASES-1', unstyle(format_comment(commit2))],
                ['', shortened_link('testing'), '', '', ''],
            ),
            combine_rows(
                ['unstable', f'Deployed ({format_deploy_time(commit3)})',
                 'r3', '-', unstyle(format_comment(commit3))],
                ['', shortened_link('unstable'), '', '', ''],
            ),
        ]
    else:
        assert table_rows_as_list(state_table) == [
            ['stable', Match.Str(), 'v1.1', 'MAPSRELEASES-1', unstyle(format_comment(commit1))],
            ['load\ntesting', Match.Contains('Not created'), '-\n[R] v1.2',
             '-\nMAPSRELEASES-1', '-\n' + unstyle(format_comment(commit2))],
            ['unstable', Match.Str(), 'r3', '-', unstyle(format_comment(commit3))]
        ]


@pytest.mark.parametrize('verbose', (False, True))
def test_garden_state(fixture_factory,
                      rendering_fixture: RenderingFixture,
                      verbose: bool) -> None:
    component: ReleaseComponentTestCase = COMPONENTS[1]

    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

    commit1 = arcadia_fixture.set_commit(revision=1, author='john-doe', message='commit1')
    commit2 = arcadia_fixture.set_commit(revision=2, author='jane-doe', message='commit2')

    commit12_hash = arcadia_fixture.random_arc_hash()

    set_sedem_machine(
        fixture_factory,
        component.path,
        MachineRelease(version='v1.1', release_spec=None,
                       trunk_revision=1,
                       st_ticket='MAPSRELEASES-1', deployed_to=['stable', 'testing']),
        MachineRelease(version='v1.2', release_spec=None,
                       hotfix_hash=commit2.arc_hash, tag_generation=12, tag_hash=commit12_hash,
                       st_ticket='MAPSRELEASES-1', deployed_to='testing', rejected=True),
    )

    commit12 = arcadia_fixture.commit_info(arc_commit_hash=commit12_hash)

    def deploy_time(commit):
        return commit.time + timedelta(minutes=2)

    resource_name = component.resource_attrs['resource_name']
    api_fixture.garden.set_module(deploy_unit=GardenDeployUnit.STABLE,
                                  module=GardenModule(module_name=resource_name,
                                                      module_version=commit1.revision,
                                                      released_at=deploy_time(commit1).isoformat()))
    api_fixture.garden.set_module(deploy_unit=GardenDeployUnit.TESTING,
                                  module=GardenModule(module_name=resource_name,
                                                      module_version=commit12_hash,
                                                      released_at=deploy_time(commit2).isoformat()))

    assert_click_result(release_info, [component.path] + (['--verbose'] if verbose else []))
    state_table, *_ = rendering_fixture.tables

    def format_deploy_time(commit):
        return format_verbose_time(deploy_time(commit))

    def combine_rows(*deploy_units):
        return ['\n'.join(column) for column in zip(*deploy_units)]

    def shortened_link(deploy_unit):
        environment = 'datatesting' if deploy_unit == 'testing' else 'stable'
        return api_fixture.url_shortener.shorten_url(
            f'https://garden.maps.yandex-team.ru/{environment}/{resource_name}'
        )

    assert state_table.title == f'Garden module {component.display_name}'
    assert state_table.header == ['', 'Status', 'Version', 'St ticket', 'Comment']

    if verbose:
        assert table_rows_as_list(state_table) == [
            combine_rows(
                ['stable', f'Deployed ({format_deploy_time(commit1)})',
                 'v1.1 (r1)', 'MAPSRELEASES-1', unstyle(format_comment(commit1))],
                ['', shortened_link('stable'), '', '', ''],
            ),
            combine_rows(
                ['testing', f'Deployed ({format_deploy_time(commit2)})',
                 f'[REJECTED] v1.2 ({format_commit_hash(commit12)})',
                 'MAPSRELEASES-1', unstyle(format_comment(commit2))],
                ['', shortened_link('testing'), '', '', ''],
            ),
        ]
    else:
        assert table_rows_as_list(state_table) == [
            ['stable', Match.Str(), 'v1.1', 'MAPSRELEASES-1', unstyle(format_comment(commit1))],
            ['testing', Match.Str(), '[R] v1.2', 'MAPSRELEASES-1', unstyle(format_comment(commit2))],
        ]


def test_garden_unreachable(fixture_factory) -> None:
    component: ReleaseComponentTestCase = COMPONENTS[1]

    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    fixture_factory(ArcadiaFixture)

    set_sedem_machine(
        fixture_factory,
        component.path,
    )

    api_fixture.garden.set_offline()

    with pytest.raises(ReleaseError, match=r'Failed to establish connection with Garden server'):
        assert_click_result(release_info, [component.path])


@pytest.mark.parametrize('verbose', (False, True))
def test_sandbox_state(fixture_factory,
                       rendering_fixture: RenderingFixture,
                       verbose: bool) -> None:
    component: ReleaseComponentTestCase = COMPONENTS[2]

    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

    commit1 = arcadia_fixture.set_commit(revision=1, author='john-doe', message='commit1')
    commit2 = arcadia_fixture.set_commit(revision=2, author='jane-doe', message='commit2')

    commit12_hash = arcadia_fixture.random_arc_hash()

    set_sedem_machine(
        fixture_factory,
        component.path,
        MachineRelease(version='v1.1', release_spec=component.release_spec.copy(update={'resource_id': 101}),
                       trunk_revision=1,
                       st_ticket='MAPSRELEASES-1', deployed_to=['stable', 'testing']),
        MachineRelease(version='v1.2', release_spec=component.release_spec.copy(update={'resource_id': 102}),
                       hotfix_hash=commit2.arc_hash, tag_generation=12, tag_hash=commit12_hash,
                       st_ticket='MAPSRELEASES-1', deployed_to='testing', rejected=True),
    )

    commit12 = arcadia_fixture.commit_info(arc_commit_hash=commit12_hash)

    resource_stable = component.resource_type(
        id=101,
        task=component.resource_task_type(),
        attributes={
            **component.resource_attrs,
            **commit1.sandbox_attributes()
        },
    )
    resource_testing = component.resource_type(
        id=102,
        task=component.resource_task_type(),
        attributes={
            **component.resource_attrs,
            **commit12.sandbox_attributes()
        },
    )

    def deploy_time(commit):
        return commit.time + timedelta(minutes=2)

    api_fixture.sandbox.add_template(
        alias=f'MAPS_CORE_{component.release_spec.task_type}_POSTCOMMIT_STABLE',
        task_type=component.release_spec.task_type,
        resource_id=resource_stable.id,
        updated=deploy_time(commit1),
    )
    api_fixture.sandbox.add_scheduler(
        task_type=component.release_spec.task_type,
        tags=['SEDEM_MANAGED', f'SERVICE:MAPS_CORE_{component.release_spec.task_type}_SCHEDULER_TESTING'],
        resource_id=resource_testing.id,
        updated=deploy_time(commit2),
    )

    assert_click_result(release_info, [component.path] + (['--verbose'] if verbose else []))
    state_table, *_ = rendering_fixture.tables

    def format_deploy_time(commit):
        return format_verbose_time(deploy_time(commit))

    def combine_rows(*deploy_units):
        return ['\n'.join(column) for column in zip(*deploy_units)]

    def shortened_link(unit_type, deploy_unit):
        if unit_type == 'scheduler':
            scheduler = api_fixture.sandbox.scheduler(
                tag=f'SERVICE:MAPS_CORE_{component.release_spec.task_type}_{deploy_unit.upper()}'
            )
            url_suffix = f'scheduler/{scheduler.scheduler_id}'
        elif unit_type == 'template':
            template_alias = f'MAPS_CORE_{component.release_spec.task_type}_{deploy_unit.upper()}'
            url_suffix = f'template/{template_alias}'
        else:
            assert False, 'Unknown unit type {unit_type}'
        return api_fixture.url_shortener.shorten_url(f'https://sandbox.yandex-team.ru/{url_suffix}')

    assert state_table.title == f'Sandbox task {component.display_name}'
    assert state_table.header == ['', 'Status', 'Version', 'St ticket', 'Comment']

    if verbose:
        assert table_rows_as_list(state_table) == [
            combine_rows(
                ['postcommit_stable', f'Deployed ({format_deploy_time(commit1)})', 'v1.1 (r1)',
                 'MAPSRELEASES-1', unstyle(format_comment(commit1))],
                ['', shortened_link('template', 'postcommit_stable'), '', '', ''],
                ['scheduler_stable', 'Not created', '-', '-', '-'],
            ),
            combine_rows(
                ['postcommit_testing', 'Not created', '-', '-', '-'],
                ['scheduler_testing', f'Deployed ({format_deploy_time(commit2)})',
                 f'[REJECTED] v1.2 ({format_commit_hash(commit12)})',
                 'MAPSRELEASES-1', unstyle(format_comment(commit2))],
                ['', shortened_link('scheduler', 'scheduler_testing'), '', '', ''],
            ),
        ]
    else:
        assert table_rows_as_list(state_table) == [
            ['postcommit_stable\nscheduler_stable', Match.Contains('Not created'), 'v1.1\n-',
             'MAPSRELEASES-1\n-', unstyle(format_comment(commit1)) + '\n-'],
            ['postcommit_testing\nscheduler_testing', Match.Contains('Not created'), '-\n[R] v1.2',
             '-\nMAPSRELEASES-1', '-\n' + unstyle(format_comment(commit2))],
        ]


def test_sandbox_inconsistent_task_types(fixture_factory) -> None:
    component: ReleaseComponentTestCase = COMPONENTS[2]

    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    fixture_factory(ArcadiaFixture)

    set_sedem_machine(
        fixture_factory,
        component.path,
    )

    api_fixture.sandbox.add_template(
        alias=f'MAPS_CORE_{component.release_spec.task_type}_POSTCOMMIT_TESTING',
        task_type='SOME_WEIRD_TASK_TYPE',
    )

    with pytest.raises(ReleaseError, match='Inconsistent task types'):
        assert_click_result(release_info, [component.path])


def test_sox_service_state_title(fixture_factory,
                                 rendering_fixture: RenderingFixture) -> None:
    fixture_factory(ApiFixture)
    fixture_factory(ArcadiaFixture)

    set_sedem_machine(
        fixture_factory,
        'maps/infra/teaspoon',
    )

    assert_click_result(release_info, ['maps/infra/teaspoon'])
    state_table, *_ = rendering_fixture.tables

    assert state_table.title == 'Service maps-core-teaspoon [SOX]'


@pytest.mark.parametrize('verbose', (True, False))
@pytest.mark.parametrize('tests_status', list(AcceptanceStatus))
def test_acceptance_info(fixture_factory, rendering_fixture: RenderingFixture,
                         tests_status: AcceptanceStatus, verbose: bool) -> None:
    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

    commit1 = arcadia_fixture.set_commit(revision=1, author='john-doe', message='commit1')
    api_fixture.nanny.set_docker_tag(
        service_name='maps_core_teapot_testing',
        docker_tag=f'maps/core-teapot:{commit1.revision}',
        entered=commit1.time,
    )
    set_sedem_machine(
        fixture_factory, 'maps/infra/teapot',
        MachineRelease(
            version='1.1',
            trunk_revision=1,
            acceptances=[
                MachineRelease.Acceptance(
                    stage='testing',
                    status=tests_status
                )
            ],
            deployed_to=['testing']
        )
    )
    assert_click_result(release_info, ['maps/infra/teapot'] + (['--verbose'] if verbose else []))
    state_table, *_ = rendering_fixture.tables
    for row in table_rows_as_list(state_table):
        if 'testing' not in row[0]:
            continue
        status = row[1].split('\n')[1]
        if tests_status == AcceptanceStatus.PENDING:
            assert status == 'Tests pending'
        elif tests_status == AcceptanceStatus.FAILURE:
            assert status.startswith('Tests failed')
        elif tests_status == AcceptanceStatus.EXECUTING:
            assert status.startswith('Tests executing')
        elif tests_status == AcceptanceStatus.CANCELLED:
            assert status.startswith('Tests cancelled')
        elif tests_status == AcceptanceStatus.SUCCESS:
            assert 'Tests' not in status
        break
    else:
        assert False, 'Not found testing stage'


@pytest.mark.parametrize('component,verbose,limit', (
    (component, verbose, releases_limit)
    for component in COMPONENTS[:-1]  # FIXME: include sandbox task after fixing stable release deducing
    for verbose in (False, True)
    for releases_limit in (2, 7)
), ids=str)
def test_deploy_candidates(fixture_factory,
                           rendering_fixture: RenderingFixture,
                           component: ReleaseComponentTestCase,
                           verbose: bool,
                           limit: int) -> None:
    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

    commit1 = arcadia_fixture.set_commit(revision=1, author='john-doe', message='commit1')
    commit2 = arcadia_fixture.set_commit(revision=2, author='jane-doe', message='commit2')
    commit3 = arcadia_fixture.set_commit(revision=3, author='jane-doe', message='commit3')
    commit4 = arcadia_fixture.set_commit(revision=4, author='john-doe', message='commit4')
    commit5 = arcadia_fixture.set_commit(revision=5, author='john-doe', message='commit5')
    commit6 = arcadia_fixture.set_commit(revision=6, author='jane-doe', message='commit6')
    commit7 = arcadia_fixture.set_commit(revision=7, author='jane-doe', message='commit7')

    commit12_hash = arcadia_fixture.random_arc_hash()
    commit13_hash = arcadia_fixture.random_arc_hash()
    commit15_hash = arcadia_fixture.random_arc_hash()
    commit16_hash = arcadia_fixture.random_arc_hash()

    def release_time(commit):
        return commit.time + timedelta(minutes=2)

    def format_release_time(commit):
        return format_verbose_time(release_time(commit))

    set_sedem_machine(
        fixture_factory,
        component.path,
        MachineRelease(version='v1.1', release_spec=component.release_spec,
                       trunk_revision=1,
                       st_ticket='MAPSRELEASES-1', release_time=release_time(commit1), deployed_to='stable'),
        MachineRelease(version='v1.2', release_spec=component.release_spec,
                       hotfix_hash=commit2.arc_hash, tag_generation=12, tag_hash=commit12_hash,
                       st_ticket='MAPSRELEASES-1', release_time=release_time(commit2), deployed_to='stable'),
        MachineRelease(version='v1.3', release_spec=component.release_spec,
                       hotfix_hash=commit3.arc_hash, tag_generation=13, tag_hash=commit13_hash,
                       st_ticket='MAPSRELEASES-1', release_time=release_time(commit3)),
        # failed to merge (no release commit) release
        MachineRelease(version='v1.4', release_spec=component.release_spec,
                       hotfix_hash=commit4.arc_hash,
                       st_ticket='MAPSRELEASES-1', release_time=release_time(commit4), status=ReleaseStatus.BROKEN),
        # failed to build release
        MachineRelease(version='v1.5', release_spec=component.release_spec,
                       hotfix_hash=commit5.arc_hash, tag_generation=15, tag_hash=commit15_hash,
                       st_ticket='MAPSRELEASES-1', release_time=release_time(commit5), status=ReleaseStatus.BROKEN),
        # building release
        MachineRelease(version='v1.6', release_spec=component.release_spec,
                       hotfix_hash=commit6.arc_hash, tag_generation=16, tag_hash=commit16_hash,
                       st_ticket='MAPSRELEASES-1', release_time=release_time(commit6), status=ReleaseStatus.PREPARING),
        MachineRelease(version='v2.1', release_spec=component.release_spec,
                       trunk_revision=7,
                       st_ticket='MAPSRELEASES-2', release_time=release_time(commit7), rejected=True),
    )

    commit12 = arcadia_fixture.commit_info(arc_commit_hash=commit12_hash)
    commit13 = arcadia_fixture.commit_info(arc_commit_hash=commit13_hash)
    commit15 = arcadia_fixture.commit_info(arc_commit_hash=commit15_hash)
    commit16 = arcadia_fixture.commit_info(arc_commit_hash=commit16_hash)

    if component.deploy_type == 'nanny':
        resource_name = component.resource_attrs['resource_name']
        api_fixture.nanny.set_docker_tag(
            service_name=f'maps_{resource_name.replace("-", "_")}_stable',
            docker_tag=f'maps/{resource_name}:{commit12_hash}',
        )
    elif component.deploy_type == 'garden':
        resource_name = component.resource_attrs['resource_name']
        api_fixture.garden.set_module(
            deploy_unit=GardenDeployUnit.STABLE,
            module=GardenModule(
                module_name=resource_name,
                module_version=commit12_hash,
            ),
        )
    elif component.deploy_type == 'sandbox':
        resource = component.resource_type(
            id=component.release_spec.resource_id,
            task=component.resource_task_type(),
            attributes={
                **component.resource_attrs,
                **commit12.sandbox_attributes()
            },
        )
        api_fixture.sandbox.add_template(
            alias=f'MAPS_CORE_{component.release_spec.task_type}_POSTCOMMIT_STABLE',
            task_type=component.release_spec.task_type,
            resource_id=resource.id,
        )
        api_fixture.sandbox.add_scheduler(
            task_type=component.release_spec.task_type,
            tags=['SEDEM_MANAGED', 'SERVICE:MAPS_CORE_{component.release_spec.task_type}_SCHEDULER_STABLE'],
            resource_id=resource.id,
        )

    assert_click_result(release_info, [component.path, '-r', str(limit)] + (['--verbose'] if verbose else []))
    _, deploy_candidates_table, _ = rendering_fixture.tables

    expected_title = 'Deploy (step) candidates'
    if limit <= 5:
        expected_title += f' (last {limit})'
    assert deploy_candidates_table.title == expected_title
    assert deploy_candidates_table.header == ['Version', 'Created', 'St ticket', 'Comment']

    if verbose:
        expected_candidates = [
            [f'[{red("REJECTED")}] v2.1 (r7)',
             format_release_time(commit7), 'MAPSRELEASES-2', format_comment(commit7)],
            [f'[{yellow("PREPARING")}] v1.6 ({format_commit_hash(commit16)})',
             format_release_time(commit6), 'MAPSRELEASES-1', format_comment(commit6)],
            [f'[{red("BROKEN")}] v1.5 ({format_commit_hash(commit15)})',
             format_release_time(commit5), 'MAPSRELEASES-1', format_comment(commit5)],
            [f'[{red("BROKEN")}] v1.4 (n/a)',
             format_release_time(commit4), 'MAPSRELEASES-1', format_comment(commit4)],
            [f'[{green("READY")}] v1.3 ({format_commit_hash(commit13)})',
             format_release_time(commit3), 'MAPSRELEASES-1', format_comment(commit3)],
        ]
    else:
        expected_candidates = [
            [f'{red("[R]")} v2.1', Match.Str(), 'MAPSRELEASES-2', format_comment(commit7)],
            [yellow('v1.6'), Match.Str(), 'MAPSRELEASES-1', format_comment(commit6)],
            [red('v1.5'), Match.Str(), 'MAPSRELEASES-1', format_comment(commit5)],
            [red('v1.4'), Match.Str(), 'MAPSRELEASES-1', format_comment(commit4)],
            ['v1.3', Match.Str(), 'MAPSRELEASES-1', format_comment(commit3)],
        ]
    expected_candidates = expected_candidates[:limit]

    assert table_rows_as_list(deploy_candidates_table, unstyle=False) == expected_candidates


@pytest.mark.parametrize('component,verbose,limit', (
    (component, verbose, candidates_limit)
    for component in COMPONENTS
    for verbose in (False, True)
    for candidates_limit in (2, 5)
), ids=str)
def test_release_candidates(fixture_factory,
                            rendering_fixture: RenderingFixture,
                            component: ReleaseComponentTestCase,
                            verbose: bool,
                            limit: int) -> None:
    fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

    commit3 = arcadia_fixture.set_commit(revision=3, author='jane-doe', message='commit3')
    commit4 = arcadia_fixture.set_commit(revision=4, author='john-doe', message='commit4')
    commit5 = arcadia_fixture.set_commit(revision=5, author='john-doe', message='commit5')

    def format_build_time(commit):
        return format_verbose_time(build_time(commit))

    def build_time(commit):
        return commit.time + timedelta(minutes=2)

    set_sedem_machine(
        fixture_factory,
        component.path,
        MachineCandidate(
            trunk_revision=commit3.revision,
            task_id='3',
            start_time=build_time(commit3).isoformat(),
            end_time=build_time(commit3).isoformat(),
        ),
        MachineCandidate(
            trunk_revision=commit4.revision,
            status=ReleaseCandidateStatus.BROKEN,
            task_id='4',
            start_time=build_time(commit4).isoformat(),
            end_time=build_time(commit4).isoformat(),
        ),
        MachineCandidate(
            trunk_revision=commit5.revision,
            status=ReleaseCandidateStatus.BUILDING,
            task_id='5',
            start_time=build_time(commit5).isoformat(),
            end_time=build_time(commit5).isoformat(),
        ),
    )

    assert_click_result(release_info, [component.path, '-c', str(limit)] + (['--verbose'] if verbose else []))

    *_, release_candidates_table = rendering_fixture.tables

    expected_title = 'Release (start) candidates'
    if limit <= 3:
        expected_title += f' (last {limit})'
    assert release_candidates_table.title == expected_title
    assert release_candidates_table.header == ['Version', 'Created', 'Sb task', 'Comment']

    if verbose:
        expected_candidates = [
            [f'[{yellow("NOT READY")}] r5', format_build_time(commit5), '5', format_comment(commit5)],
            [f'[{red("BROKEN")}] r4', format_build_time(commit4), '4', format_comment(commit4)],
            [f'[{green("READY")}] r3', format_build_time(commit3), '3', format_comment(commit3)],
        ]
    else:
        expected_candidates = [
            [yellow('r5'), Match.Str(), '5', format_comment(commit5)],
            [red('r4'), Match.Str(), '4', format_comment(commit4)],
            ['r3', Match.Str(), '3', format_comment(commit3)],
        ]
    expected_candidates = expected_candidates[:limit]

    assert table_rows_as_list(release_candidates_table, unstyle=False) == expected_candidates


@pytest.mark.parametrize('component,verbose', (
    (component, verbose)
    for component in COMPONENTS
    for verbose in (False, True)
), ids=str)
def test_rollback_candidates(fixture_factory,
                             rendering_fixture: RenderingFixture,
                             component: ReleaseComponentTestCase,
                             verbose: bool) -> None:
    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

    commit1 = arcadia_fixture.set_commit(revision=1, author='john-doe', message='commit1')
    commit2 = arcadia_fixture.set_commit(revision=2, author='jane-doe', message='commit2')
    commit3 = arcadia_fixture.set_commit(revision=3, author='jane-doe', message='commit3')
    commit4 = arcadia_fixture.set_commit(revision=4, author='john-doe', message='commit4')

    commit12_hash = arcadia_fixture.random_arc_hash()
    commit13_hash = arcadia_fixture.random_arc_hash()

    def deploy_time(commit):
        return commit.time + timedelta(minutes=2)

    def format_deploy_time(commit):
        return format_verbose_time(deploy_time(commit))

    set_sedem_machine(
        fixture_factory,
        component.path,
        MachineRelease(version='v1.1', release_spec=component.release_spec,
                       trunk_revision=1, st_ticket='MAPSRELEASES-1',
                       deploys=[MachineRelease.Deploy(step='stable', time=deploy_time(commit1))]),
        MachineRelease(version='v1.2', release_spec=component.release_spec,
                       hotfix_hash=commit2.arc_hash, tag_generation=12, tag_hash=commit12_hash,
                       st_ticket='MAPSRELEASES-1'),
        MachineRelease(version='v1.3', release_spec=component.release_spec,
                       hotfix_hash=commit3.arc_hash, tag_generation=13, tag_hash=commit13_hash,
                       st_ticket='MAPSRELEASES-1', rejected=True,
                       deploys=[MachineRelease.Deploy(step='stable', time=deploy_time(commit3))]),
        MachineRelease(version='v2.1', release_spec=component.release_spec,
                       trunk_revision=4, st_ticket='MAPSRELEASES-1',
                       deploys=[MachineRelease.Deploy(step='stable', time=deploy_time(commit4))]),
    )

    commit13 = arcadia_fixture.commit_info(arc_commit_hash=commit13_hash)

    if component.deploy_type == 'nanny':
        resource_name = component.resource_attrs['resource_name']
        api_fixture.nanny.set_docker_tag(
            service_name=f'maps_{resource_name.replace("-", "_")}_stable',
            docker_tag=f'maps/{resource_name}:{commit4.revision}',
        )
    elif component.deploy_type == 'garden':
        resource_name = component.resource_attrs['resource_name']
        api_fixture.garden.set_module(
            deploy_unit=GardenDeployUnit.STABLE,
            module=GardenModule(
                module_name=resource_name,
                module_version=commit4.revision,
            ),
        )
    elif component.deploy_type == 'sandbox':
        resource = component.resource_type(
            id=component.release_spec.resource_id,
            task=component.resource_task_type(),
            attributes={
                **component.resource_attrs,
                **commit4.sandbox_attributes()
            },
        )
        api_fixture.sandbox.add_template(
            alias=f'MAPS_CORE_{component.release_spec.task_type}_POSTCOMMIT_STABLE',
            task_type=component.release_spec.task_type,
            resource_id=resource.id,
        )
        api_fixture.sandbox.add_scheduler(
            task_type=component.release_spec.task_type,
            tags=['SEDEM_MANAGED', 'SERVICE:MAPS_CORE_{component.release_spec.task_type}_SCHEDULER_STABLE'],
            resource_id=resource.id,
        )

    assert_click_result(rollback, [component.path, 'stable', '-l', '10'] + (['--verbose'] if verbose else []))
    _, rollback_candidates_table = rendering_fixture.tables

    expected_title = f'Stable releases for {component.display_name} (among last 10)'
    assert rollback_candidates_table.title == expected_title
    assert rollback_candidates_table.header == ['Version', 'Deployed', 'St ticket', 'Comment']

    if verbose:
        expected_candidates = [
            [f'[{green("READY")}] v2.1 (r4)',
             format_deploy_time(commit4), 'MAPSRELEASES-1', format_comment(commit4)],
            [f'[{red("REJECTED")}] v1.3 ({format_commit_hash(commit13)})',
             format_deploy_time(commit3), 'MAPSRELEASES-1', format_comment(commit3)],
            [f'[{green("READY")}] v1.1 (r1)',
             format_deploy_time(commit1), 'MAPSRELEASES-1', format_comment(commit1)],
        ]
    else:
        expected_candidates = [
            ['v2.1', Match.Str(), 'MAPSRELEASES-1', format_comment(commit4)],
            [f'{red("[R]")} v1.3', Match.Str(), 'MAPSRELEASES-1', format_comment(commit3)],
            ['v1.1', Match.Str(), 'MAPSRELEASES-1', format_comment(commit1)],
        ]

    assert table_rows_as_list(rollback_candidates_table, unstyle=False) == expected_candidates


def test_custom_initial_step(fixture_factory,
                             rendering_fixture: RenderingFixture):
    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    fixture_factory(ArcadiaFixture)

    set_sedem_machine(
        fixture_factory,
        'maps/renderer/tilesgen',
        MachineRelease(
            version='v1.1',
            trunk_revision=1,
        ),
    )

    for deploy_unit in ('dataprestable',
                        'datavalidation',
                        'load',
                        'prestable',
                        'stable',
                        'testing'):
        api_fixture.nanny.set_docker_tag(
            service_name=f'maps_core_renderer_tilesgen_{deploy_unit}',
            docker_tag='maps/core-renderer-tilesgen:1'
        )

    assert_click_result(release_info, ['maps/renderer/tilesgen'])
    state_table, *_ = rendering_fixture.tables

    assert table_rows_as_list(state_table) == [
        ['dataprestable\ndatavalidation\nstable', Match.Str(), Match.Str(), Match.Str(), Match.Str()],
        ['prestable', Match.Str(), Match.Str(), Match.Str(), Match.Str()],
        ['load\ntesting', Match.Str(), Match.Str(), Match.Str(), Match.Str()],
    ]


class TestReleaseInfo:
    def test_custom_release_message(self,
                                    fixture_factory,
                                    rendering_fixture: RenderingFixture):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

        commit = arcadia_fixture.set_commit(revision=1, author='john-doe', message='initial teapot commit')

        custom_message = 'Custom release message'
        YaPackageResource(attributes={
            'resource_name': 'core-teapot',
            'major_release_num': '1', 'minor_release_num': '1',
            **commit.sandbox_attributes()
        })
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v1.1',
                trunk_revision=commit.revision,
                custom_message=custom_message,
            ),
        )
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_unstable', docker_tag='maps/core-teapot:1')

        assert_click_result(release_info, ['maps/infra/teapot'])
        state_table, *_ = rendering_fixture.tables

        assert table_rows_as_list(state_table) == Match.Contains(
            ['unstable', Match.Str(), 'v1.1', Match.Str(), Match.Contains(custom_message)]
        )

    @pytest.mark.parametrize('verbose', [False, True])
    def test_info_for_meta_service(self,
                                   verbose: bool,
                                   fixture_factory,
                                   rendering_fixture: RenderingFixture):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

        # For verbose output
        deploy_time = datetime(
            year=2020, month=4, day=1, hour=10, minute=30, second=0, tzinfo=isodate.LOCAL
        )

        def verbosify(row: tp.List[str]) -> tp.List[str]:
            if not verbose:
                return row
            new_row = list(row)
            SERVICE_NAME = 0
            DEPLOYED = 1
            VERSION = 2
            new_row[DEPLOYED] = f'Deployed ({format_verbose_time(deploy_time)})'
            if new_row[SERVICE_NAME].endswith('core-teapot'):
                if new_row[VERSION] == 'v1.1':
                    new_row[VERSION] = 'v1.1 (r1)'
                if new_row[VERSION] == 'v2.1':
                    new_row[VERSION] = 'v2.1 (r4)'
            if new_row[SERVICE_NAME].endswith('core-teacup'):
                if new_row[VERSION] == 'v1.1':
                    new_row[VERSION] = 'v1.1 (r7)'
                if new_row[VERSION] == 'v2.1':
                    new_row[VERSION] = 'v2.1 (r10)'
            return new_row

        commit1 = arcadia_fixture.set_commit(revision=1, author='john-doe', message='initial teapot commit')
        commit4 = arcadia_fixture.set_commit(revision=4, author='jane-doe', message='add feature for teapot')
        commit7 = arcadia_fixture.set_commit(revision=7, author='mr-reese', message='initial teacup commit')
        commit10 = arcadia_fixture.set_commit(revision=10, author='mr-finch', message='bugfix for teacup')

        YaPackageResource(attributes={
            'resource_name': 'core-teapot',
            'major_release_num': '1', 'minor_release_num': '1',
            **commit1.sandbox_attributes()
        })
        YaPackageResource(attributes={
            'resource_name': 'core-teapot',
            'major_release_num': '2', 'minor_release_num': '1',
            **commit4.sandbox_attributes()
        })
        YaPackageResource(attributes={
            'resource_name': 'core-teacup',
            'major_release_num': '1', 'minor_release_num': '1',
            **commit7.sandbox_attributes()
        })
        YaPackageResource(attributes={
            'resource_name': 'core-teacup',
            'major_release_num': '2', 'minor_release_num': '1',
            **commit10.sandbox_attributes()
        })

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(version='v1.1', release_spec=None,
                           trunk_revision=1, st_ticket='MAPSRELEASES-1',
                           deploys=[MachineRelease.Deploy(step='stable', time=deploy_time)]),
            MachineRelease(version='v2.1', release_spec=None,
                           trunk_revision=4, st_ticket='MAPSRELEASES-3',
                           deploys=[MachineRelease.Deploy(step='testing', time=deploy_time)]),
        )
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teacup',
            MachineRelease(version='v1.1', release_spec=None,
                           trunk_revision=7, st_ticket='MAPSRELEASES-2',
                           deploys=[MachineRelease.Deploy(step='stable', time=deploy_time)]),
            MachineRelease(version='v2.1', release_spec=None,
                           trunk_revision=10, st_ticket='MAPSRELEASES-4',
                           deploys=[MachineRelease.Deploy(step='testing', time=deploy_time)]),
        )

        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_teapot_stable', docker_tag='maps/core-teapot:1', entered=deploy_time
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:4', entered=deploy_time
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:4', entered=deploy_time
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_teacup_stable', docker_tag='maps/core-teacup:7', entered=deploy_time
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_teacup_testing', docker_tag='maps/core-teacup:10', entered=deploy_time
        )

        assert_click_result(release_info, ['maps/infra/teaset'] + (['--verbose'] if verbose else []))
        service_state_table, candidate_state_table = rendering_fixture.tables

        assert service_state_table.title == 'Meta-service maps-core-teaset'
        assert service_state_table.header == ['Stable release', 'Status', 'Version', 'ST Ticket', 'Comment']
        assert table_rows_as_list(service_state_table) == [
            verbosify(['maps-core-teapot', Match.Str(), 'v1.1', 'MAPSRELEASES-1', '(@john-doe) initial teapot commit']),
            verbosify(['maps-core-teacup', Match.Str(), 'v1.1', 'MAPSRELEASES-2', '(@mr-reese) initial teacup commit'])
        ]

        assert candidate_state_table.title == 'Meta-release candidate'
        assert candidate_state_table.header == ['Testing release', 'Status', 'Version', 'ST Ticket', 'Comment']
        assert table_rows_as_list(candidate_state_table) == [
            verbosify(['maps-core-teapot', Match.Str(), 'v2.1', 'MAPSRELEASES-3', '(@jane-doe) add feature for teapot']),
            verbosify(['maps-core-teacup', Match.Str(), 'v2.1', 'MAPSRELEASES-4', '(@mr-finch) bugfix for teacup'])
        ]

    def test_stable_hotfixes(self,
                             fixture_factory,
                             rendering_fixture: RenderingFixture):
        """
        Stable has hotfixes with different trunk revisions. Will yield different 'color-relation' between deploy units.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

        tag12_arc_hash = arcadia_fixture.random_arc_hash()
        tag13_arc_hash = arcadia_fixture.random_arc_hash()

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teacup',
            MachineRelease(version='v1.1', release_spec=None, trunk_revision=1),
            MachineRelease(version='v2.1', release_spec=None, trunk_revision=4),
            # v1.2 is hotfix from trunk of v2.1
            MachineRelease(version='v1.2', release_spec=None,
                           trunk_revision=4, tag_hash=tag12_arc_hash, tag_generation=8),
            # v1.3 is hotfix from trunk newer that v2.1
            MachineRelease(version='v1.3', release_spec=None,
                           trunk_revision=9, tag_hash=tag13_arc_hash, tag_generation=11)
        )
        # Testing trunk newer than stable
        api_fixture.nanny.set_docker_tag('maps_core_teacup_stable', 'maps/core-teacup:1')
        api_fixture.nanny.set_docker_tag('maps_core_teacup_testing', 'maps/core-teacup:4')

        assert_click_result(release_info, ['maps/infra/teacup'])
        state_table = rendering_fixture.tables[0]
        assert table_rows_as_list(state_table, unstyle=False) == [
            ['stable', Match.Str(), 'v1.1', Match.Str(), Match.Str()],
            ['testing', Match.Str(), cyan('v2.1'), Match.Str(), Match.Str()]
        ]

        # Stable trunk same as testing, but stable hotfix created later
        api_fixture.nanny.set_docker_tag('maps_core_teacup_stable', f'maps/core-teacup:{tag12_arc_hash}')
        api_fixture.nanny.set_docker_tag('maps_core_teacup_testing', 'maps/core-teacup:4')

        assert_click_result(release_info, ['maps/infra/teacup'])
        state_table = rendering_fixture.tables[0]
        assert table_rows_as_list(state_table, unstyle=False) == [
            ['stable', Match.Str(), 'v1.2', Match.Str(), Match.Str()],
            ['testing', Match.Str(), cyan('v2.1'), Match.Str(), Match.Str()]
        ]

        # Stable trunk newer than testing
        api_fixture.nanny.set_docker_tag('maps_core_teacup_stable', f'maps/core-teacup:{tag13_arc_hash}')
        api_fixture.nanny.set_docker_tag('maps_core_teacup_testing', 'maps/core-teacup:4')

        assert_click_result(release_info, ['maps/infra/teacup'])
        state_table = rendering_fixture.tables[0]
        assert table_rows_as_list(state_table, unstyle=False) == [
            ['stable', Match.Str(), 'v1.3', Match.Str(), Match.Str()],
            ['testing', Match.Str(), cyan('v2.1'), Match.Str(), Match.Str()]
        ]

    def test_unknown_component(self, fixture_factory):
        set_sedem_machine(fixture_factory, service_path='maps/infra/teapot')  # ensure machine mocked

        with pytest.raises(ReleaseError, match='No config for maps-core-teacup found'):
            assert_click_result(release_info, ['maps/infra/teacup'])  # try to release unknown service
