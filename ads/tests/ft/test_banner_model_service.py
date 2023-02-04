import pytest

from ads.bsyeti.libs.events.proto.banner_resources_pb2 import TBannerResources
from ads.bsyeti.caesar.libs.profiles.proto.order_pb2 import TOrderProfileProto
from ads.bsyeti.caesar.libs.profiles.python import get_order_table_row_from_proto
from ads.bsyeti.caesar.tests.ft.common import select_profiles
from ads.bsyeti.caesar.tests.ft.common.event import make_event
from ads.bsyeti.caesar.tests.ft.common.model_service import (
    create_response,
    create_request_matcher,
    create_response_matcher,
)


TABLE = "Banners"


def _make_order_row(order_id, goal_id):
    order = TOrderProfileProto()
    order.OrderID = order_id
    order.Resources.AutoBudget.AutoBudgetGoalID = goal_id
    serialized_profile = order.SerializeToString()
    return get_order_table_row_from_proto(str(order_id), serialized_profile)


EXTRA_PROFILES = {
    "AliveBanners": [],
    "Orders": [_make_order_row(order_id=1, goal_id=100)],
    "AdGroups": [],
    "Goals": [{"GoalID": 100}],
    "TurboUrlDict": [],
    "MobileApps": [],
    "MobileDeeplinks": [],
    "NormalizedUrls": [],
    "NormalizedHosts": [],
}


def base_test_request_tsar(yt_cluster, caesar, tables, queue, get_timestamp, model_service):
    rsp = create_response(
        [
            {
                "TsarVectors": {
                    "Vectors": [
                        {
                            "ComputedTime": 100500,
                            "VectorID": 1,
                            "CompressedVector": b"aaaa",
                        }
                    ],
                },
                "BannerID": 1,
                "ItemIndex": 0,
            }
        ]
    )

    request = create_request_matcher()
    response = create_response_matcher(rsp)
    model_service.expect(request, response)

    with queue.writer() as queue_writer:
        for profile_id in range(1):
            body = TBannerResources()
            body.OrderID = 1
            body.Version = 1
            body.Source = 1
            body.Stop = False
            body.Archive = False
            body.Title = "title"
            body.Body = "banner"
            body.Site = "ya.ru"
            body.UpdateInfo = True

            queue_writer.write(make_event(profile_id, get_timestamp(60), body))

    profiles = select_profiles(yt_cluster, tables, "Banners")
    assert len(profiles) == 1
    assert profiles[0].TsarVectors


@pytest.mark.table(TABLE)
@pytest.mark.extra_profiles(EXTRA_PROFILES)
@pytest.mark.extra_config_args({"enable_scatter": True})
def test_scatter_request_tsar(yt_cluster, caesar, tables, queue, get_timestamp, model_service):
    base_test_request_tsar(yt_cluster, caesar, tables, queue, get_timestamp, model_service)


@pytest.mark.table(TABLE)
@pytest.mark.extra_profiles(EXTRA_PROFILES)
@pytest.mark.extra_config_args({"enable_scatter": False})
def test_http_request_tsar(yt_cluster, caesar, tables, queue, get_timestamp, model_service):
    base_test_request_tsar(yt_cluster, caesar, tables, queue, get_timestamp, model_service)
