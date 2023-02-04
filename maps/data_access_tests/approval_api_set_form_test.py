import uuid

import pytest

from maps.infra.sedem.machine.tests.typing import (
    ApprovalFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.approval_api import ApprovalApi, ApprovalStatus


@pytest.mark.asyncio
async def test_set_approval_form_uuid(mongo: MongoFixture,
                                      release_factory: ReleaseFactory,
                                      approval_factory: ApprovalFactory,
                                      service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service', sox=True)

    release_ids = []
    for major_version in range(1, 4):
        await release_factory(
            service_config=service_config,
            major_version=major_version,
            minor_version=1,
            origin_arc_hash=f'hash{major_version}.1',
            st_ticket=f'TESTQUEUE-{major_version}',
        )
        release_ids.append(await approval_factory(
            service_config=service_config,
            release_major_version=major_version,
            release_minor_version=1,
            ok_uuid=None,
            status=ApprovalStatus.PENDING,
        ))

    async with await mongo.async_client.start_session() as session:
        await ApprovalApi(session).set_approval_form_uuid(
            release_id=release_ids[1],
            form_uuid=str(uuid.uuid4()),
        )
        approval = await ApprovalApi(session).load_approval(release_ids[1])

    assert approval.ok_uuid is not None
