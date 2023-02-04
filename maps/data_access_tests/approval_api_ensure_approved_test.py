import asyncio
import typing as tp
from dataclasses import dataclass

import pytest

from maps.infra.sedem.machine.tests.typing import (
    ApprovalFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.approval_api import ApprovalApi, ApprovalStatus
from maps.infra.sedem.machine.lib.release_api import ReleaseCompletion


@dataclass
class EnsureApprovalExistsTest:
    name: str

    initial_status: tp.Optional[ApprovalStatus]

    expected_status: ApprovalStatus

    def __str__(self) -> str:
        return self.name


ENSURE_APPROVAL_TEST_CASES = [
    EnsureApprovalExistsTest(
        name='not_created_yet',
        initial_status=None,
        expected_status=ApprovalStatus.PENDING,
    ),
    EnsureApprovalExistsTest(
        name='pending',
        initial_status=ApprovalStatus.PENDING,
        expected_status=ApprovalStatus.PENDING,
    ),
    EnsureApprovalExistsTest(
        name='approved',
        initial_status=ApprovalStatus.APPROVED,
        expected_status=ApprovalStatus.APPROVED,
    ),
    EnsureApprovalExistsTest(
        name='declined',
        initial_status=ApprovalStatus.DECLINED,
        expected_status=ApprovalStatus.DECLINED,
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', ENSURE_APPROVAL_TEST_CASES, ids=str)
async def test_ensure_approval(mongo: MongoFixture,
                               release_factory: ReleaseFactory,
                               approval_factory: ApprovalFactory,
                               service_config_factory: ServiceConfigFactory,
                               case: EnsureApprovalExistsTest) -> None:
    service_config = await service_config_factory(name='maps-fake-service', sox=True)
    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        st_ticket='TESTQUEUE-1',
    )
    if case.initial_status is not None:
        await approval_factory(
            service_config=service_config,
            release_major_version=1,
            release_minor_version=1,
            status=case.initial_status,
        )

    async with await mongo.async_client.start_session() as session:
        approval = await ApprovalApi(session).ensure_approval_exists(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            author='jane-doe',
        )

    if case.expected_status:
        if not case.initial_status:
            assert approval.author == 'jane-doe'
        assert approval.status == case.expected_status
    else:
        assert approval is None


@dataclass
class EnsureApprovalExistsFailsTest:
    name: str

    release_completion: tp.Optional[ReleaseCompletion]

    expected_error: str

    def __str__(self) -> str:
        return self.name


ENSURE_APPROVAL_FAILS_TEST_CASES = [
    EnsureApprovalExistsFailsTest(
        name='non_existent_release',
        release_completion=None,
        expected_error=r'No release [^\s]+ for [^\s]+ found',
    ),
    EnsureApprovalExistsFailsTest(
        name='broken_release',
        release_completion=ReleaseCompletion(broken=ReleaseCompletion.Broken(
            build=ReleaseCompletion.Broken.BrokenBuild(reason='some reason')
        )),
        expected_error=r'Release [^\s]+ for [^\s]+ is broken',
    ),
    # FIXME: Should raise an error as preparing release can have no ticket
    # EnsureApprovalExistsFailsTest(
    #     name='preparing_release',
    #     release_completion=ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
    #         build=ReleaseCompletion.Preparing.PreparingBuild(operation_id='12345')
    #     )),
    #     expected_error=r'Release v1\.1 for [^\s]+ is not ready',
    # ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', ENSURE_APPROVAL_FAILS_TEST_CASES, ids=str)
async def test_ensure_approval_fails(mongo: MongoFixture,
                                     release_factory: ReleaseFactory,
                                     service_config_factory: ServiceConfigFactory,
                                     case: EnsureApprovalExistsFailsTest) -> None:
    service_config = await service_config_factory(name='maps-fake-service', sox=True)
    if case.release_completion is not None:
        await release_factory(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            st_ticket='TESTQUEUE-1',
            completion=case.release_completion,
        )

    with pytest.raises(Exception, match=case.expected_error):
        async with await mongo.async_client.start_session() as session:
            await ApprovalApi(session).ensure_approval_exists(
                service_config=service_config,
                major_version=1,
                minor_version=1,
                author='jane-doe',
            )


@pytest.mark.asyncio
async def test_concurrent_approval_creation(mongo: MongoFixture,
                                            release_factory: ReleaseFactory,
                                            service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service', sox=True)
    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='hash1',
        release_arc_hash='hash1',
        st_ticket='TESTQUEUE-1',
    )
    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=2,
        origin_arc_hash='hash2',
        release_arc_hash='hash3',
        st_ticket='TESTQUEUE-1',
    )

    async with await mongo.async_client.start_session() as session1:
        async with await mongo.async_client.start_session() as session2:
            results = await asyncio.gather(
                ApprovalApi(session1).ensure_approval_exists(
                    service_config=service_config,
                    major_version=1,
                    minor_version=1,
                    author='jane-doe',
                ),
                ApprovalApi(session2).ensure_approval_exists(
                    service_config=service_config,
                    major_version=1,
                    minor_version=2,
                    author='jane-doe',
                ),
                return_exceptions=True
            )

    errors = [
        result for result in results
        if isinstance(result, Exception)
    ]

    assert len(errors)  # expected error not found
    with pytest.raises(Exception, match=r'Another deployment for release v1 is waiting for approval'):
        raise errors.pop()
    if errors:
        raise errors.pop()  # unexpected 2nd error
