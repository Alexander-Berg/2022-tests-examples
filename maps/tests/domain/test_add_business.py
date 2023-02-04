import pytest

from maps_adv.geosmb.clients.bvm import BvmNotFound
from maps_adv.geosmb.marksman.server.lib.exceptions import (
    BizNotFound,
    NoOrgInfo,
    OrgWithoutCounter,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_clients(domain, bvm, geosearch):
    await domain.add_business(biz_id=123)

    bvm.fetch_permalinks_by_biz_id.assert_called_with(biz_id=123)
    geosearch.resolve_org.assert_called_with(permalink=54321)


async def test_uses_dm_to_add_business(domain, dm):
    await domain.add_business(biz_id=123)

    dm.add_business.assert_awaited_with(biz_id=123, permalink=54321, counter_id=444)


async def test_raises_if_no_data_from_bvm(domain, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.side_effect = BvmNotFound

    with pytest.raises(BizNotFound):
        await domain.add_business(biz_id=123)


async def test_raises_if_not_data_from_geosearch(domain, geosearch):
    geosearch.resolve_org.coro.return_value = None

    with pytest.raises(NoOrgInfo):
        await domain.add_business(biz_id=123)


async def test_raises_if_org_has_no_counter(domain, geosearch):
    geosearch.resolve_org.coro.return_value.metrika_counter = None

    with pytest.raises(OrgWithoutCounter):
        await domain.add_business(biz_id=123)
