import json

import pytest
from yql.util.format import YqlStruct

from maps_adv.geosmb.landlord.server.lib.tasks import ImportGoodsDataTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportGoodsDataTask(config=config, dm=dm)


async def test_actual(task):
    await task()
    assert False
