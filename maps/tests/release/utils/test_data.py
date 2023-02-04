import logging
import typing as tp
from dataclasses import dataclass, InitVar, field
from datetime import datetime, timedelta

import click
import isodate
from bson import ObjectId
from click.testing import CliRunner

from maps.infra.sedem.cli.lib.release.components.adapters import config_proto_from_service_config
from maps.infra.sedem.cli.lib.release.release import ReleaseVersion, ReleaseStatus, AcceptanceStatus
from maps.infra.sedem.cli.lib import utils
from maps.infra.sedem.common.release.approval import ApprovalStatus
from maps.infra.sedem.lib.config import LocalServiceConfigBuilder
from maps.infra.sedem.machine.lib.collections import (
    ApprovalDocument,
    Commit,
    DeployDocument,
    DeployStatusDocument,
    Release,
    ReleaseCandidate,
    ReleaseCandidateProgress,
    ReleaseCandidateStatus,
    ReleaseCompletion,
    ReleaseSpec,
    ServiceConfig,
    AcceptanceTestSetLaunchDocument,
    AcceptanceTestSetStatus,
    AcceptanceTestLaunchDocument,
    AcceptanceTestStatus
)
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.terminal_utils.table import Table

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


ARC_TAG_NAME = 'tags/releases/maps/{service_name}-{major}.{minor}'


@dataclass
class ReleaseComponent:
    name: str
    path: str
    canonical_name: tp.Optional[str] = None
    deploy_type: tp.Optional[str] = None


@dataclass
class MachineCandidate:
    trunk_revision: tp.Optional[int] = None
    commit_hash: tp.Optional[str] = None
    release_spec: tp.Optional[ReleaseSpec] = None
    status: tp.Optional[str] = ReleaseCandidateStatus.READY
    task_id: tp.Optional[str] = '12345'
    start_time: tp.Optional[datetime] = datetime.now(isodate.LOCAL)
    end_time: tp.Optional[datetime] = datetime.now(isodate.LOCAL)


@dataclass
class MachineRelease:
    @dataclass
    class Deploy:
        step: str
        time: datetime = field(default_factory=lambda: datetime.now(isodate.LOCAL) - timedelta(hours=24))
        failed: bool = False

    @dataclass
    class Rejection:
        author: str = 'robot-maps-sandbox'
        reason: str = 'some reason'
        time: datetime = field(default_factory=lambda: datetime.now(isodate.LOCAL))

    @dataclass
    class Acceptance:
        stage: str
        status: AcceptanceStatus

    version: str
    trunk_revision: tp.Optional[int] = None
    hotfix_hash: tp.Optional[str] = None
    tag_hash: tp.Optional[str] = None
    tag_generation: tp.Optional[int] = None
    st_ticket: tp.Optional[str] = None
    release_time: tp.Optional[datetime] = None
    deployed_to: InitVar[tp.Union[None, str, tp.List[str]]] = None
    deploys: tp.List[Deploy] = field(default_factory=list)
    approval_status: tp.Optional[ApprovalStatus] = None
    rejection: tp.Optional[Rejection] = None
    rejected: bool = False
    custom_message: tp.Optional[str] = None
    release_spec: tp.Optional[ReleaseSpec] = None
    status: ReleaseStatus = ReleaseStatus.READY
    acceptances: list[Acceptance] = field(default_factory=list)

    def __post_init__(self, deployed_to: tp.Union[None, str, tp.List[str]]) -> None:
        if deployed_to is None:
            deployed_to = []
        elif not isinstance(deployed_to, list):
            deployed_to = [deployed_to]
        self.deploys += [self.Deploy(step=step) for step in deployed_to]
        if self.rejected:
            self.rejection = self.Rejection()


def set_sedem_machine(fixture_factory,
                      service_path: str,
                      *args: list[tp.Union[MachineCandidate, MachineRelease]]) -> None:
    api_fixture = fixture_factory(ApiFixture)
    arcadia_fixture = fixture_factory(ArcadiaFixture)
    machine_fixture = fixture_factory(MachineFixture)

    config_proxy = LocalServiceConfigBuilder().load_config(
        utils.path2arcadia(service_path),
    )
    service_config = ServiceConfig.from_proto(
        config=config_proto_from_service_config(config_proxy),
        revision=1
    )
    machine_fixture.add_service_config(service_config)

    candidates = []
    releases = []
    for arg in args:
        if isinstance(arg, MachineCandidate):
            candidates.append(arg)
        else:
            releases.append(arg)

    for machine_candidate in candidates:
        if trunk_revision := machine_candidate.trunk_revision:
            commit_info = arcadia_fixture.commit_info(revision=trunk_revision)
        elif commit_hash := machine_candidate.commit_hash:
            commit_info = arcadia_fixture.commit_info(arc_commit_hash=commit_hash)
        else:
            assert False, 'One of {trunk_revision, commit_hash} should be specified'
        candidate = ReleaseCandidate(
            service_name=service_config.name,
            commit=Commit(
                arc_commit_hash=commit_info.arc_hash,
                svn_revision=commit_info.revision,
                author=commit_info.author,
                message=commit_info.message,
                time=commit_info.time,
            ),
            task_id=machine_candidate.task_id,
            release_spec=machine_candidate.release_spec,
            progress=ReleaseCandidateProgress(
                start_time=machine_candidate.start_time,
                end_time=machine_candidate.end_time,
                status=machine_candidate.status,
            )
        )
        machine_fixture.add_candidate(candidate)

    for machine_release in releases:
        release_version = ReleaseVersion.from_string(machine_release.version)
        major_version, minor_version = release_version.branch, release_version.tag
        release_author = 'robot-maps-sandbox'
        release_time = machine_release.release_time or (
            datetime.now(isodate.LOCAL) - timedelta(hours=24)
        )

        deploys = []
        for deploy in machine_release.deploys:
            for stage in service_config.stages:
                if stage.name != deploy.step:
                    continue
                for deploy_unit in stage.deploy_units:
                    if deploy.failed:
                        deploy_status = DeployStatusDocument(
                            failure=DeployStatusDocument.Failure(reason='Failed')
                        )
                    else:
                        deploy_status = DeployStatusDocument(
                            success=DeployStatusDocument.Success()
                        )
                    deploys.append(DeployDocument(
                        deploy_id=ObjectId(),
                        deploy_unit=deploy_unit,
                        author=release_author,
                        start_time=deploy.time,
                        end_time=deploy.time,
                        deploy_status=deploy_status,
                    ))

        if minor_version == 1:
            trunk_revision = machine_release.trunk_revision
            origin_commit_info = arcadia_fixture.commit_info(revision=trunk_revision)
            release_commit_info = origin_commit_info
        else:
            if trunk_revision := machine_release.trunk_revision:
                origin_commit_info = arcadia_fixture.commit_info(revision=trunk_revision)
            elif hotfix_hash := machine_release.hotfix_hash:
                origin_commit_info = arcadia_fixture.commit_info(arc_commit_hash=hotfix_hash)
            else:
                assert False, 'One of {trunk_revision, hotfix_hash} should be specified'
            if machine_release.tag_generation:
                release_commit_info = arcadia_fixture.set_commit(
                    arc_hash=machine_release.tag_hash,
                    generation=machine_release.tag_generation,
                    author=origin_commit_info.author,
                    message=origin_commit_info.message,
                    branch=ARC_TAG_NAME.format(
                        service_name=service_config.name,
                        major=major_version,
                        minor=minor_version,
                    )
                )
            else:
                release_commit_info = None

        origin_commit = Release.Commit(
            arc_commit_hash=origin_commit_info.arc_hash,
            author=origin_commit_info.author,
            message=origin_commit_info.message,
            time=origin_commit_info.time,
        )
        if origin_commit_info.revision:
            origin_commit.revision = origin_commit_info.revision

        if release_commit_info:
            release_commit = Release.Commit(
                arc_commit_hash=release_commit_info.arc_hash,
                author=release_commit_info.author,
                message=release_commit_info.message,
                time=release_commit_info.time,
            )
            if release_commit_info.revision:
                release_commit.revision = release_commit_info.revision
        else:
            release_commit = None

        if machine_release.status == ReleaseStatus.READY:
            completion = ReleaseCompletion(
                ready=ReleaseCompletion.Ready()
            )
        elif machine_release.status == ReleaseStatus.PREPARING:
            completion = ReleaseCompletion(
                preparing=ReleaseCompletion.Preparing(
                    build=ReleaseCompletion.Preparing.PreparingBuild()
                )
            )
        elif machine_release.status == ReleaseStatus.BROKEN:
            if not release_commit:
                details = ReleaseCompletion.Broken(
                    hotfix=ReleaseCompletion.Broken.BrokenHotfix(reason='')
                )
            else:
                details = ReleaseCompletion.Broken(
                    build=ReleaseCompletion.Broken.BrokenBuild(reason='')
                )
            completion = ReleaseCompletion(broken=details)
        else:
            assert False, f'Unknown release status {machine_release.status}'

        acceptance_documents = []

        for acceptance in machine_release.acceptances:
            if acceptance.status == AcceptanceStatus.PENDING:
                acceptance_documents.append(AcceptanceTestSetLaunchDocument(
                    acceptance_id=ObjectId(),
                    stage=acceptance.stage,
                    status=AcceptanceTestSetStatus.PENDING,
                ))
            elif acceptance.status == AcceptanceStatus.FAILURE:
                acceptance_documents.append(AcceptanceTestSetLaunchDocument(
                    acceptance_id=ObjectId(),
                    stage=acceptance.stage,
                    status=AcceptanceTestSetStatus.FINISHED,
                    launches=[AcceptanceTestLaunchDocument(
                        status=AcceptanceTestStatus.FAILURE
                    )],
                    start_time=release_time,
                    end_time=release_time,
                ))
            elif acceptance.status == AcceptanceStatus.CANCELLED:
                acceptance_documents.append(AcceptanceTestSetLaunchDocument(
                    acceptance_id=ObjectId(),
                    stage=acceptance.stage,
                    status=AcceptanceTestSetStatus.CANCELLED,
                    end_time=datetime.now(isodate.LOCAL),
                ))
            elif acceptance.status == AcceptanceStatus.SUCCESS:
                acceptance_documents.append(AcceptanceTestSetLaunchDocument(
                    acceptance_id=ObjectId(),
                    stage=acceptance.stage,
                    status=AcceptanceTestSetStatus.FINISHED,
                    launches=[AcceptanceTestLaunchDocument(
                        status=AcceptanceTestStatus.SUCCESS
                    )],
                    start_time=datetime.now(isodate.LOCAL),
                    end_time=datetime.now(isodate.LOCAL),
                ))
            elif acceptance.status == AcceptanceStatus.EXECUTING:
                acceptance_documents.append(AcceptanceTestSetLaunchDocument(
                    acceptance_id=ObjectId(),
                    stage=acceptance.stage,
                    status=AcceptanceTestSetStatus.EXECUTING,
                    launches=[AcceptanceTestLaunchDocument(
                        status=AcceptanceTestStatus.EXECUTING
                    )],
                    start_time=datetime.now(isodate.LOCAL),
                ))
            else:
                assert False, 'Unknown AcceptaneStatus value'

        release = Release(
            service_name=service_config.name,
            major=major_version,
            minor=minor_version,
            origin_commit=origin_commit,
            author=release_author,
            created_at=release_time,
            completed_at=release_time,
            deploys=deploys,
            release_spec=machine_release.release_spec,
            completion=completion,
            acceptance=acceptance_documents,
        )
        if release_commit:
            release.release_commit = release_commit
        if machine_release.custom_message:
            release.message = machine_release.custom_message
        if machine_release.st_ticket:
            release.st_ticket = machine_release.st_ticket
        if machine_release.rejection:
            release.rejected = Release.Rejected(
                author=machine_release.rejection.author,
                reason=machine_release.rejection.reason,
                time=machine_release.rejection.time
            )
        if machine_release.approval_status:
            now = datetime.now(isodate.LOCAL)
            release.approval = ApprovalDocument(
                author='robot-maps-sandbox',
                start_time=now,
                status=machine_release.approval_status,
            )
            if machine_release.approval_status != ApprovalStatus.PENDING:
                release.approval.end_time = now
        machine_fixture.add_release(release)


def assert_click_result(function: tp.Callable, args: tp.List[str]) -> None:
    runner = CliRunner()
    result = runner.invoke(function, args)
    if result.exception:
        raise result.exception
    logger.info(result.output)
    assert result.exit_code == 0


def table_rows_as_list(table: Table, unstyle: bool = True) -> tp.List[tp.List[str]]:
    result = []
    table.header = None  # skip header
    for row in table.rows():
        if unstyle:
            row = [click.unstyle(cell) for cell in row]
        result.append(row)
    return result
