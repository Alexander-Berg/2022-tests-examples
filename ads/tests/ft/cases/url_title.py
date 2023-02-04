import random

import pytest

from ads.bsyeti.caesar.libs.profiles.proto.url_pb2 import TUrlProfileProto
from ads.bsyeti.caesar.tests.ft.common.matcher import check_fields_presence
from ads.bsyeti.libs.events.proto.normalized_url_title_log_pb2 import TNormalizedUrlTitleLog
from google.protobuf.timestamp_pb2 import Timestamp as PbTimestamp
from market.idx.datacamp.proto.external.Offer_pb2 import Offer, MarketOfferContent
from robot.jupiter.protos.compatibility.offer_pb2 import TContentAttrsOffer
from robot.jupiter.protos.compatibility.urldat_pb2 import TUrldat
from robot.jupiter.protos.compatibility.web_factors_pb2 import SDocErf2InfoProto
from robot.mercury.protos.adv_factors_pb2 import TAdvFactors

from ads.bsyeti.caesar.tests.ft.common.event import make_event


class _TestCaseBase:
    table = "NormalizedUrls"
    profile_class = TUrlProfileProto

    def __init__(self):
        self.expected = {}

    @staticmethod
    def _check_fields(expected, actual, fields):
        for field in fields:
            expected_value = getattr(expected, field)
            actual_value = getattr(actual, field)
            assert expected_value == actual_value, "%s != %s for %s" % (expected_value, actual_value, field)


class TestCaseNormalizedUrlTitle(_TestCaseBase):
    def make_events(self, time, shard, profile_id):
        body = TNormalizedUrlTitleLog()
        body.Title = "title {}".format(random.randint(1, 100500))
        body.TimeStamp = time

        event = make_event("url {}".format(profile_id), time, body)
        yield event

        if event.ProfileID not in self.expected:
            self.expected[event.ProfileID] = {}
        expected = self.expected[event.ProfileID]
        if body.Title not in expected:
            expected[body.Title] = 0.0
        expected[body.Title] += 1.0

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            expected = self.expected[profile.NormalizedUrl.encode("utf-8")]
            for x in profile.Titles.TitleCounters:
                assert expected[x.Title] == pytest.approx(x.DecayedCount, 0.01)


class TestCaseNormalizedUrlRobotFactors(_TestCaseBase):
    def make_events(self, time, shard, profile_id):
        event_profile_id = "url {}".format(profile_id)

        body = TAdvFactors()
        body.TitleRawUTF8 = "title {}".format(random.randint(1, 100500))
        body.Timestamp = time
        body.HttpCode = 404
        body.Url = event_profile_id

        urldat = TUrldat()
        erf = SDocErf2InfoProto()

        if self._get_random_bool():
            urldat.HttpCode = 200
            urldat.IP = 12345678
            urldat.IsRedirect = True
        if self._get_random_bool():
            erf.IsShop = True
            erf.IsBlog = True
            erf.Hops = 123

        body.Urldat = urldat.SerializeToString()
        body.Erf = erf.SerializeToString()

        offer = TContentAttrsOffer()
        offer.NoProductsProbability = random.random()
        offer.OneProductProbability = random.random()

        body.Offer = offer.SerializeToString()

        event = make_event(event_profile_id, time, body)
        yield event

        if event.ProfileID not in self.expected or self.expected[event.ProfileID].Timestamp < body.Timestamp:
            self.expected[event.ProfileID] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            expected = self.expected[profile.NormalizedUrl.encode("utf-8")]
            assert profile.RobotFactors.UpdateTime == expected.Timestamp, "RobotFactors.UpdateTime != Timestamp"
            assert profile.Landing.UpdateTime == expected.Timestamp, "Landing.UpdateTime != Timestamp"

            self._check_fields(expected, profile.Landing, ("HttpCode", "Url"))
            assert profile.Landing.Title == expected.TitleRawUTF8, "Landing.Title != TitleRawUTF8"

            erf = SDocErf2InfoProto()
            erf.ParseFromString(expected.Erf)
            self._check_fields(erf, profile.RobotFactors.Erf, ("IsShop", "IsBlog", "Hops"))

            urldat = TUrldat()
            urldat.ParseFromString(expected.Urldat)
            self._check_fields(urldat, profile.RobotFactors.Urldat, ("HttpCode", "IP", "IsRedirect"))

            content_attrs_offer = TContentAttrsOffer()
            assert content_attrs_offer.ParseFromString(expected.Offer), "Cannot parse expected serialized offer"
            self._check_fields(
                content_attrs_offer,
                profile.RobotFactors.ContentAttrsOffer,
                ("NoProductsProbability", "OneProductProbability"),
            )

            check_fields_presence(
                expected,
                [
                    "Erf",
                    "Bert",
                    "Urldat",
                ],
                profile.RobotFactors,
                [
                    "Erf",
                    "Bert",
                    "Urldat",
                ],
                empty_string_as_nonpresent=True,
            )
            check_fields_presence(
                expected,
                [
                    "Url",
                    "TitleRawUTF8",
                ],
                profile.Landing,
                ["Url", "Title"],
                empty_string_as_nonpresent=True,
            )

    @staticmethod
    def _get_random_bool():
        return random.choice([True, False])


class TestCaseNormalizedUrlMarketOffers(_TestCaseBase):
    def make_events(self, time, shard, profile_id):
        offer = Offer()
        offer.timestamp.CopyFrom(PbTimestamp(seconds=time))

        offer_content = MarketOfferContent()
        offer_content.category_id = 1
        offer_content.category_name = "some category"
        offer_content.vendor = 2
        offer_content.vendor_name = "some vendor"
        offer_content.model_id = 3
        offer_content.model_name = "some model"

        offer.market_content.CopyFrom(offer_content)

        event = make_event("url {}".format(profile_id), time, offer)
        yield event

        if (
            event.ProfileID not in self.expected
            or self.expected[event.ProfileID].timestamp.ToSeconds() < offer.timestamp.ToSeconds()
        ):
            self.expected[event.ProfileID] = offer

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            expected = self.expected[profile.NormalizedUrl.encode("utf-8")]
            assert (
                profile.MarketOffer.UpdateTime == expected.timestamp.ToSeconds()
            ), "MarketOffer.timestamp != timestamp"

            self._check_fields(
                expected.market_content,
                profile.MarketOffer.MarketOfferContent,
                ["category_id", "category_name", "vendor", "vendor_name", "model_id", "model_name"],
            )
