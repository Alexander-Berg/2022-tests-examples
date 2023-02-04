import logging
from datetime import timedelta

from maps.infra.sedem.cli.commands.release import release_history
from maps.infra.sedem.cli.tests.release.fixtures.rendering_fixture import RenderingFixture
from maps.infra.sedem.cli.tests.release.utils.test_data import (
    assert_click_result, set_sedem_machine, MachineRelease, table_rows_as_list, ReleaseStatus, ApprovalStatus, MachineCandidate
)
from maps.infra.sedem.machine.lib.collections import ReleaseCandidateStatus

from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


class TestReleaseHistory:
    def test_history_for_nanny_service(self,
                                       fixture_factory,
                                       rendering_fixture: RenderingFixture):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

        commit1 = arcadia_fixture.set_commit(revision=1, author='john-doe', message='initial ecstatic commit')
        commit4 = arcadia_fixture.set_commit(revision=4, author='jane-doe', message='add feature')
        commit7 = arcadia_fixture.set_commit(revision=7, author='mr-reese', message='bugfix')
        commit10 = arcadia_fixture.set_commit(revision=10, author='robot-srch-releaser', message='another feature')
        commit15 = arcadia_fixture.set_commit(revision=15, author='john-doe', message='hotfix')  # noqa
        commit16 = arcadia_fixture.set_commit(revision=16, author='john-doe', message='another hotfix')  # noqa

        tag17_arc_hash = arcadia_fixture.random_arc_hash()
        commit17 = arcadia_fixture.set_commit(revision=17, arc_hash=tag17_arc_hash, author='john-doe', message='another hotfix')  # noqa

        def release_time(commit):
            return commit.time + timedelta(minutes=2)

        def format_release_time(commit):
            return release_time(commit).astimezone().strftime('%Y-%m-%d %H:%M:%S')

        set_sedem_machine(
            fixture_factory,
            'maps/infra/ecstatic/coordinator',
            MachineRelease(version='v1.1', trunk_revision=1, st_ticket='MAPSRELEASES-1',
                           release_time=release_time(commit1),
                           deployed_to=['testing', 'prestable', 'stable']),
            MachineRelease(version='v2.1', trunk_revision=4, st_ticket='MAPSRELEASES-2',
                           release_time=release_time(commit4),
                           deployed_to=['testing', 'prestable']),
            MachineRelease(version='v3.1', trunk_revision=7, st_ticket='MAPSRELEASES-3',
                           release_time=release_time(commit7),
                           deployed_to='testing',
                           rejection=MachineRelease.Rejection(author='jane-doe', reason='some reason')),
            MachineRelease(version='v4.1', trunk_revision=10, st_ticket='MAPSRELEASES-4',
                           release_time=release_time(commit10),
                           deployed_to='testing'),
            MachineRelease(version='v4.2', trunk_revision=15, tag_hash=tag17_arc_hash, tag_generation=17,
                           release_time=release_time(commit15),
                           st_ticket='MAPSRELEASES-4'),
            MachineRelease(version='v4.3', trunk_revision=16, st_ticket='MAPSRELEASES-4',
                           release_time=release_time(commit16),
                           status=ReleaseStatus.BROKEN),
            MachineCandidate(
                trunk_revision=commit1.revision,
                task_id='1'
            ),
            MachineCandidate(
                trunk_revision=commit4.revision,
                task_id='2'
            ),
            MachineCandidate(
                trunk_revision=commit10.revision,
                task_id='4'
            ),
            MachineCandidate(
                trunk_revision=commit17.revision,
                task_id='5',
                status=ReleaseCandidateStatus.BUILDING
            ),
        )

        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_ecstatic_coordinator_stable', docker_tag='maps/core-ecstatic-coordinator:1'
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_ecstatic_coordinator_prestable', docker_tag='maps/core-ecstatic-coordinator:4'
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_ecstatic_coordinator_testing', docker_tag='maps/core-ecstatic-coordinator:10'
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_ecstatic_coordinator_load', docker_tag='maps/core-ecstatic-coordinator:10'
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_ecstatic_coordinator_unstable', docker_tag='maps/core-ecstatic-coordinator:15'
        )

        assert_click_result(release_history, ['maps/infra/ecstatic/coordinator'])
        history_table, *_ = rendering_fixture.tables

        assert table_rows_as_list(history_table) == [
            ['Release v4.3 [RELEASE BROKEN]\n'
             'Source commit: https://a.yandex-team.ru/commit/16\n'
             'Target commit: -\n'
             'St ticket: https://st.yandex-team.ru/MAPSRELEASES-4\n'
             'Deployed to: -\n'
             f'Created at: {format_release_time(commit16)}\n'
             'Comment: (@john-doe) another hotfix'],
            ['Release v4.2\n'
             'Source commit: https://a.yandex-team.ru/commit/15\n'
             f'Target commit: https://a.yandex-team.ru/arc_vcs/commit/{tag17_arc_hash}\n'
             'St ticket: https://st.yandex-team.ru/MAPSRELEASES-4\n'
             'Deployed to: -\n'
             f'Created at: {format_release_time(commit15)}\n'
             'Comment: (@john-doe) hotfix'],
            ['Release v4.1 (load, testing)\n'
             'Commit: https://a.yandex-team.ru/commit/10\n'
             'St ticket: https://st.yandex-team.ru/MAPSRELEASES-4\n'
             'Deployed to: testing\n'
             f'Created at: {format_release_time(commit10)}\n'
             'Comment: (@robot-srch-releaser) another feature'],
            ['Release v3.1 [REJECTED]\n'
             'Commit: https://a.yandex-team.ru/commit/7\n'
             'St ticket: https://st.yandex-team.ru/MAPSRELEASES-3\n'
             'Deployed to: testing\n'
             f'Created at: {format_release_time(commit7)}\n'
             'Rejected: (@jane-doe) some reason\n'
             'Comment: (@mr-reese) bugfix'],
            ['Release v2.1 (prestable)\n'
             'Commit: https://a.yandex-team.ru/commit/4\n'
             'St ticket: https://st.yandex-team.ru/MAPSRELEASES-2\n'
             'Deployed to: testing, prestable\n'
             f'Created at: {format_release_time(commit4)}\n'
             'Comment: (@jane-doe) add feature'],
            ['Release v1.1 (stable)\n'
             'Commit: https://a.yandex-team.ru/commit/1\n'
             'St ticket: https://st.yandex-team.ru/MAPSRELEASES-1\n'
             'Deployed to: testing, prestable, stable\n'
             f'Created at: {format_release_time(commit1)}\n'
             'Comment: (@john-doe) initial ecstatic commit']
        ]

    def test_empty_history_for_nanny_service(self,
                                             fixture_factory,
                                             rendering_fixture: RenderingFixture):
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot'
        )

        assert_click_result(release_history, ['maps/infra/teapot'])
        history_table, *_ = rendering_fixture.tables

        assert history_table.title == 'Release history is empty'
        assert not history_table

    def test_history_for_sox_service(self,
                                     fixture_factory,
                                     rendering_fixture: RenderingFixture):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

        commit1 = arcadia_fixture.set_commit(revision=1, author='john-doe', message='initial commit')
        commit2 = arcadia_fixture.set_commit(revision=2, author='jane-doe', message='add feature')
        commit3 = arcadia_fixture.set_commit(revision=3, author='mr-reese', message='bugfix')

        def release_time(commit):
            return commit.time + timedelta(minutes=2)

        def format_release_time(commit):
            return release_time(commit).astimezone().strftime('%Y-%m-%d %H:%M:%S')

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teaspoon',
            MachineRelease(version='v1.1', trunk_revision=1, st_ticket='MAPSRELEASES-1',
                           release_time=release_time(commit1),
                           approval_status=ApprovalStatus.APPROVED,
                           deployed_to=['testing', 'prestable', 'stable']),
            MachineRelease(version='v2.1', trunk_revision=2, st_ticket='MAPSRELEASES-2',
                           release_time=release_time(commit2),
                           approval_status=ApprovalStatus.DECLINED,
                           deployed_to=['testing', 'prestable']),
            MachineRelease(version='v3.1', trunk_revision=3, st_ticket='MAPSRELEASES-3',
                           release_time=release_time(commit3),
                           approval_status=ApprovalStatus.PENDING,
                           deployed_to='testing'),
            MachineCandidate(
                trunk_revision=commit1.revision,
                task_id=1
            ),
            MachineCandidate(
                trunk_revision=commit2.revision,
                task_id=2
            ),
            MachineCandidate(
                trunk_revision=commit3.revision,
                task_id=3
            ),
        )

        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_teaspoon_stable', docker_tag='maps/core-teaspoon:1'
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_teaspoon_prestable', docker_tag='maps/core-teaspoon:2'
        )
        api_fixture.nanny.set_docker_tag(
            service_name='maps_core_teaspoon_testing', docker_tag='maps/core-teaspoon:3'
        )

        assert_click_result(release_history, ['maps/infra/teaspoon'])
        history_table, *_ = rendering_fixture.tables

        assert table_rows_as_list(history_table) == [
            ['Release v3.1 (testing)\n'
             'SOX: [PRESTABLE PENDING] [STABLE PENDING]\n'
             'Commit: https://a.yandex-team.ru/commit/3\n'
             'St ticket: https://st.yandex-team.ru/MAPSRELEASES-3\n'
             'Deployed to: testing\n'
             f'Created at: {format_release_time(commit3)}\n'
             'Comment: (@mr-reese) bugfix'],
            ['Release v2.1 (prestable)\n'
             'SOX: [PRESTABLE DECLINED] [STABLE DECLINED]\n'
             'Commit: https://a.yandex-team.ru/commit/2\n'
             'St ticket: https://st.yandex-team.ru/MAPSRELEASES-2\n'
             'Deployed to: testing, prestable\n'
             f'Created at: {format_release_time(commit2)}\n'
             'Comment: (@jane-doe) add feature'],
            ['Release v1.1 (stable)\n'
             'SOX: [PRESTABLE APPROVED] [STABLE APPROVED]\n'
             'Commit: https://a.yandex-team.ru/commit/1\n'
             'St ticket: https://st.yandex-team.ru/MAPSRELEASES-1\n'
             'Deployed to: testing, prestable, stable\n'
             f'Created at: {format_release_time(commit1)}\n'
             'Comment: (@john-doe) initial commit']
        ]
