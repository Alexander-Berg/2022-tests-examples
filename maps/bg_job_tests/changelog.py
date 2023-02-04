import typing as tp
from dataclasses import dataclass
from unittest.mock import Mock, AsyncMock

import pytest
from google.protobuf.wrappers_pb2 import Int64Value

from arc.api.public.shared_pb2 import Commit, Attribute
from maps.infra.sedem.machine.lib import ci
from maps.infra.sedem.machine.lib.changelog import ChangelogCollector
from maps.infra.sedem.machine.lib.release_api import ReleaseApi


@dataclass
class ChangelogTestCase:
    name: str
    current_release: Mock
    previous_release: tp.Optional[Mock]
    stable_release: tp.Optional[Mock]
    commits: list[Commit]
    expected_changelog: str

    @property
    def major_version(self) -> int:
        return self.current_release.major

    def __str__(self) -> str:
        return self.name


CHANGELOG_TEST_CASES = [
    ChangelogTestCase(
        name='empty',
        current_release=Mock(
            major=1,
            origin_commit=Mock(revision=1),
        ),
        previous_release=None,
        stable_release=None,
        commits=[
            Commit(
                Oid='abc',
                SvnRevision=1,
                Author='john-doe',
                Message='feature',
            ),
        ],
        expected_changelog='No previous release has been found. Empty changelog',
    ),
    ChangelogTestCase(
        name='to_previous',
        current_release=Mock(
            major=2,
            origin_commit=Mock(revision=42),
        ),
        previous_release=Mock(
            major=1,
            origin_commit=Mock(revision=1),
        ),
        stable_release=None,
        commits=[
            Commit(
                Oid='def',
                SvnRevision=42,
                Author='jane-doe',
                Message='Feature',
                Attributes=[Attribute(Name='pr.tickets', Value='GEOINFRA-10')],
            ),
            Commit(
                Oid='abc',
                SvnRevision=1,
                Author='john-doe',
                Message='Initial',
            ),
        ],
        expected_changelog=(
            "<{Previous version v1.1: ((https://a.yandex-team.ru/arc/commit/2 r2)) to "
            "((https://a.yandex-team.ru/arc/commit/42 r42)) changelog\n"
            "#|\n"
            "|| Revision | ST ticket | Author | Message ||\n"
            "|| ((https://a.yandex-team.ru/arc/commit/42 42)) | %%GEOINFRA-10%% | staff:jane-doe | %%Feature%% ||\n"
            "|#\n}>"
        ),
    ),
    ChangelogTestCase(
        name='to_previous_and_stable',
        current_release=Mock(
            major=4,
            origin_commit=Mock(revision=42),
        ),
        previous_release=Mock(
            major=3,
            origin_commit=Mock(revision=24),
        ),
        stable_release=Mock(
            major=1,
            origin_commit=Mock(revision=1),
        ),
        commits=[
            Commit(
                Oid='ghi',
                SvnRevision=42,
                Author='john-doe',
                Message='Another feature',
            ),
            Commit(
                Oid='def',
                SvnRevision=24,
                Author='jane-doe',
                Message='Feature',
                Attributes=[Attribute(Name='pr.tickets', Value='GEOINFRA-10')],
            ),
            Commit(
                Oid='abc',
                SvnRevision=1,
                Author='john-doe',
                Message='Initial',
            ),
        ],
        expected_changelog=(
            "<{Previous version v3.1: ((https://a.yandex-team.ru/arc/commit/25 r25)) to "
            "((https://a.yandex-team.ru/arc/commit/42 r42)) changelog\n"
            "#|\n"
            "|| Revision | ST ticket | Author | Message ||\n"
            "|| ((https://a.yandex-team.ru/arc/commit/42 42)) | %% %% | staff:john-doe | %%Another feature%% ||\n"
            "|#\n}>"
            "<{Stable version v1.1: ((https://a.yandex-team.ru/arc/commit/2 r2)) to "
            "((https://a.yandex-team.ru/arc/commit/24 r24)) changelog\n"
            "#|\n"
            "|| Revision | ST ticket | Author | Message ||\n"
            "|| ((https://a.yandex-team.ru/arc/commit/24 24)) | %%GEOINFRA-10%% | staff:jane-doe | %%Feature%% ||\n"
            "|#\n}>"
        ),
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CHANGELOG_TEST_CASES, ids=str)
async def test_changelog(monkeypatch, release_api: ReleaseApi, case: ChangelogTestCase) -> None:
    release_api.lookup_major_release.side_effect = [
        case.current_release,
        case.previous_release,
    ]
    release_api.lookup_latest_major_release_deployed_to_stable.side_effect = [
        case.stable_release,
    ]

    monkeypatch.setattr(
        ci.BackgroundCiClient, '_get_tvm_credentials', AsyncMock(return_value=None)
    )
    monkeypatch.setattr(
        ci.BackgroundCiClient, '_request_service_stub', AsyncMock(
            return_value=ci.GetFlowLaunchesResponse(
                launches=[
                    ci.FlowLaunch(
                        number=len(case.commits) - i,
                        triggered_by='robot-ci',
                        revision_hash=commit.Oid,
                    )
                    for i, commit in enumerate(sorted(
                        case.commits,
                        key=lambda commit: commit.SvnRevision,
                        reverse=True,
                    ))
                ],
                offset=ci.Offset(
                    total=Int64Value(value=len(case.commits)),
                    has_more=False,
                ),
            ),
        )
    )

    changelog_collector = ChangelogCollector(
        auth_client=Mock(),
        tvm_client=Mock(),
        arc_client=Mock(try_get_commit=AsyncMock(
            wraps=lambda commit_hash: next((
                commit
                for commit in case.commits
                if commit.Oid == commit_hash
            ), None)
        )),
    )

    changelog = await changelog_collector.generate_major_changelog(
        release_api=release_api,
        service_config=Mock(
            path='maps/infra/mock',
        ),
        major_version=case.major_version,
    )

    assert changelog == case.expected_changelog
