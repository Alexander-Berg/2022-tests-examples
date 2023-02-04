import pytest

from ads.bsyeti.libs.events.proto.target_domain_id_update_pb2 import TTargetDomainIDUpdate
from ads.bsyeti.caesar.tests.ft.common import select_profiles
from ads.bsyeti.caesar.tests.ft.common.event import make_event


@pytest.mark.table("Orders")
def test_profiles(yt_cluster, caesar, tables, queue, get_timestamp):
    expected = {}

    target_domain_id_updates = [
        {
            "OrderID": 0,
            "Events": [
                (0, -1),
                (1, 1),
                (2, 1),
                (2, -1),
                (3, 1),
                (1, 1),
                (0, -1),
                (4, 1),
                (5, 1),
                (4, -1),
                (3, 1),
                (5, -1),
                (2, 1),
                (1, -1),
                (0, 1),
            ],
            "Result": 3,
            "ArraySize": 3,
        },
        {
            "OrderID": 1,
            "Events": [(0, -1), (1, 1), (1, -1), (2, 1)],
            "Result": 2,
            "ArraySize": 1,
        },
        {
            "OrderID": 2,
            "Events": [(0, -1), (1, 1), (0, -1), (2, 1), (0, -1), (2, 1), (2, -1), (1, 1)],
            "Result": 1,
            "ArraySize": 2,
        },
        {
            "OrderID": 3,
            "Events": [(i + 1, 1) for i in range(200)],
            "Result": 1,
            "ArraySize": 100,
        },
        {
            "OrderID": 4,
            "Events": [(0, 1)] * 10 + [(1, 1)],
            "Result": 1,
            "ArraySize": 1,
        },
    ]

    with queue.writer() as queue_writer:
        for profile_updates in target_domain_id_updates:
            for target_domain_id, sign in profile_updates["Events"]:

                body = TTargetDomainIDUpdate()
                body.TimeStamp = get_timestamp(2022)
                body.OrderID = profile_updates["OrderID"]
                body.TargetDomainID = target_domain_id
                body.Sign = sign

                event = make_event(body.OrderID, body.TimeStamp, body)
                queue_writer.write(event)

            expected[profile_updates["OrderID"]] = profile_updates["Result"], profile_updates["ArraySize"]

    profiles = select_profiles(yt_cluster, tables, "Orders")

    assert len(expected) == len(profiles)
    for profile in profiles:
        expected_result, expected_size = expected[profile.OrderID]
        prod_controls = profile.AutobudgetResources.ComputedAutobudgetControls
        preprod_controls = profile.AutobudgetResources.ComputedAutobudgetControls

        assert expected_result == prod_controls.TopTargetDomainID
        assert expected_result == preprod_controls.TopTargetDomainID
        assert expected_size == len(prod_controls.TargetDomainIDCounters)
        assert expected_size == len(preprod_controls.TargetDomainIDCounters)
