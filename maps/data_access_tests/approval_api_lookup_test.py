import typing as tp
from contextlib import nullcontext
from dataclasses import dataclass

import pytest

from maps.infra.sedem.machine.tests.typing import (
    ApprovalFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.approval_api import ApprovalApi, ApprovalStatus
from maps.infra.sedem.machine.lib.config_api import ServiceConfig
from maps.infra.sedem.machine.lib.release_api import ReleaseCompletion, ReleaseStatus


async def init_lookup_data(service_config: ServiceConfig,
                           release_factory: ReleaseFactory,
                           approval_factory: ApprovalFactory) -> None:
    for major_version in range(1, 4):
        for minor_version in range(1, 6):
            if major_version < 3:
                completion = {
                    1: ReleaseCompletion(ready=ReleaseCompletion.Ready()),
                    2: ReleaseCompletion(broken=ReleaseCompletion.Broken(
                        build=ReleaseCompletion.Broken.BrokenBuild(reason='some reason')
                    )),
                    3: ReleaseCompletion(ready=ReleaseCompletion.Ready()),
                    4: ReleaseCompletion(ready=ReleaseCompletion.Ready()),
                    5: ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
                        build=ReleaseCompletion.Preparing.PreparingBuild(operation_id='12345')
                    )),
                }[minor_version]
            else:
                completion = ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
                    major=ReleaseCompletion.Preparing.PreparingMajor()
                ))
            await release_factory(
                service_config=service_config,
                major_version=major_version,
                minor_version=minor_version,
                st_ticket=f'TESTQUEUE-{major_version}',
                origin_arc_hash=f'hash{major_version}.{minor_version}.1',
                origin_revision=major_version * 100 + minor_version * 10 + 1,
                release_arc_hash=f'hash{major_version}.{minor_version}.{1 if minor_version == 1 else 2}',
                release_revision=major_version * 100 + minor_version * 10 + (1 if minor_version == 1 else 2),
                completion=completion,
            )
            if completion.status() == ReleaseStatus.READY:
                status = {
                    (1, 1): ApprovalStatus.APPROVED,
                    (1, 3): ApprovalStatus.PENDING,
                    (1, 4): None,
                    (2, 1): ApprovalStatus.DECLINED,
                    (2, 3): ApprovalStatus.PENDING,
                    (2, 4): None,
                }[(major_version, minor_version)]
                if status:
                    await approval_factory(
                        service_config=service_config,
                        release_major_version=major_version,
                        release_minor_version=minor_version,
                        status=status,
                        **({'ok_uuid': None} if (major_version, minor_version) == (2, 3) else {})
                    )
            if minor_version == 1 and completion.status() != ReleaseStatus.READY:
                break


@dataclass
class LookupApprovalTest:
    name: str

    release_major: int
    release_minor: int

    expected_status: tp.Optional[ApprovalStatus]
    expected_error: tp.Optional[str]

    def __str__(self) -> str:
        return self.name


LOOKUP_APPROVAL_TEST_CASES = [
    LookupApprovalTest(
        name='approved',
        release_major=1,
        release_minor=1,
        expected_status=ApprovalStatus.APPROVED,
        expected_error=None,
    ),
    LookupApprovalTest(
        name='pending',
        release_major=1,
        release_minor=3,
        expected_status=ApprovalStatus.PENDING,
        expected_error=None,
    ),
    LookupApprovalTest(
        name='declined',
        release_major=2,
        release_minor=1,
        expected_status=ApprovalStatus.DECLINED,
        expected_error=None,
    ),
    LookupApprovalTest(
        name='not_created_yet',
        release_major=2,
        release_minor=4,
        expected_status=None,
        expected_error=None,
    ),
    LookupApprovalTest(
        name='broken_release',
        release_major=1,
        release_minor=2,
        expected_status=None,
        expected_error=r'Release v1.2 for [^\s]+ is broken',
    ),
    LookupApprovalTest(
        name='preparing_release',
        release_major=3,
        release_minor=1,
        expected_status=None,
        expected_error=None,  # FIXME: Should raise an error as preparing release can have no ticket
    ),
    LookupApprovalTest(
        name='non_existent_release',
        release_major=4,
        release_minor=1,
        expected_status=None,
        expected_error=r'No release v4.1 for [^\s]+ found',
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', LOOKUP_APPROVAL_TEST_CASES, ids=str)
async def test_lookup(mongo: MongoFixture,
                      approval_factory: ApprovalFactory,
                      release_factory: ReleaseFactory,
                      service_config_factory: ServiceConfigFactory,
                      case: LookupApprovalTest) -> None:
    service_config = await service_config_factory(name='maps-fake-service', sox=True)
    await init_lookup_data(service_config, release_factory, approval_factory)

    if case.expected_error is not None:
        check_expected_error = pytest.raises(Exception, match=case.expected_error)
    else:
        check_expected_error = nullcontext()

    with check_expected_error:
        async with await mongo.async_client.start_session() as session:
            approval = await ApprovalApi(session).lookup_approval(
                service_config=service_config,
                major_version=case.release_major,
                minor_version=case.release_minor,
            )

    if case.expected_error is None:
        if case.expected_status is not None:
            assert approval.status == case.expected_status
        else:
            assert approval is None


@pytest.mark.asyncio
async def test_load_approval(mongo: MongoFixture,
                             approval_factory: ApprovalFactory,
                             release_factory: ReleaseFactory,
                             service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service', sox=True)
    for major_version in range(1, 4):
        await release_factory(
            service_config=service_config,
            major_version=major_version,
            minor_version=1,
            origin_arc_hash=f'hash{major_version}.1',
            st_ticket=f'TESTQUEUE-{major_version}',
        )

    release_id = await approval_factory(
        service_config=service_config,
        release_major_version=2,
        release_minor_version=1,
        status=ApprovalStatus.APPROVED,
    )

    async with await mongo.async_client.start_session() as session:
        approval = await ApprovalApi(session).load_approval(release_id)

    assert approval.service_name == service_config.name
    assert approval.release_version == 'v2.1'
    assert approval.st_ticket == 'TESTQUEUE-2'

    assert approval.status == ApprovalStatus.APPROVED


@pytest.mark.asyncio
async def test_lookup_without_form(mongo: MongoFixture,
                                   approval_factory: ApprovalFactory,
                                   release_factory: ReleaseFactory,
                                   service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service', sox=True)
    await init_lookup_data(service_config, release_factory, approval_factory)

    async with await mongo.async_client.start_session() as session:
        release_ids = ApprovalApi(session).lookup_release_ids_without_approval_form()
        approvals = [
            await ApprovalApi(session).load_approval(release_id)
            async for release_id in release_ids
        ]

    assert len(approvals) and all(
        approval.ok_uuid is None
        for approval in approvals
    )


@pytest.mark.asyncio
async def test_lookup_pending(mongo: MongoFixture,
                              approval_factory: ApprovalFactory,
                              release_factory: ReleaseFactory,
                              service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service', sox=True)
    await init_lookup_data(service_config, release_factory, approval_factory)

    async with await mongo.async_client.start_session() as session:
        release_ids = ApprovalApi(session).lookup_release_ids_with_pending_approval()
        approvals = [
            await ApprovalApi(session).load_approval(release_id)
            async for release_id in release_ids
        ]

    assert len(approvals) and all(
        approval.status == ApprovalStatus.PENDING
        for approval in approvals
    )
