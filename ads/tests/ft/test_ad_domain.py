import pytest

from ads.bsyeti.libs.events.proto.verified_domain_pb2 import TVerifiedDomain
from ads.bsyeti.caesar.tests.ft.common import select_profiles
from ads.bsyeti.caesar.tests.ft.common.event import make_event


@pytest.mark.table("AdDomains")
def test_profiles(yt_cluster, caesar, tables, queue, get_timestamp):
    expected = {}
    with queue.writer() as queue_writer:
        for profile_id in range(50):
            body = TVerifiedDomain()
            body.Domain = "domain{}.ru".format(profile_id)
            body.Verified = bool(profile_id % 3)
            event = make_event(body.Domain, get_timestamp(60), body)

            queue_writer.write(event)
            expected[event.ProfileID] = body.Verified

    profiles = select_profiles(yt_cluster, tables, "AdDomains")
    assert len(expected) == len(profiles)
    for profile in profiles:
        assert expected[profile.Domain.encode("utf-8")] == profile.DomainFlags.Verified
