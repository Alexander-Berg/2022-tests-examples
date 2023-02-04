import pytest
from smb.common.testing_utils import dt
from yql.client.parameter_value_builder import YqlParameterValueBuilder

from maps_adv.geosmb.marksman.server.lib.tasks import CdpUsersSyncTask

pytestmark = [pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm, domain):
    return CdpUsersSyncTask(config=config, dm=dm, domain=domain)


@pytest.mark.freeze_time(dt("2020-05-16 12:00:00"))
async def test_uses_yql_params(task, dm):
    dm.list_biz_ids.return_value = [111, 222]

    params = await task.fetch_yql_query_params()

    expected_params = {
        "$date_to_match_from": YqlParameterValueBuilder.make_string("2020-05-10"),
        "$date_to_match_to": YqlParameterValueBuilder.make_string("2020-05-15"),
        "$biz_ids": YqlParameterValueBuilder.make_list(
            [
                YqlParameterValueBuilder.make_uint64(111),
                YqlParameterValueBuilder.make_uint64(222),
            ]
        ),
    }
    jsoned_params = {key: value.to_json() for key, value in params.items()}
    expected_jsoned_params = {
        key: value.to_json() for key, value in expected_params.items()
    }

    assert jsoned_params == expected_jsoned_params
