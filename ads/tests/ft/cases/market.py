import random

from ads.bsyeti.caesar.libs.profiles.proto.market_category_pb2 import TMarketCategoryProfileProto
from ads.bsyeti.caesar.libs.profiles.proto.market_fesh_pb2 import TMarketFeshProfileProto
from ads.bsyeti.caesar.libs.profiles.proto.market_model_pb2 import TMarketModelProfileProto
from ads.bsyeti.caesar.libs.profiles.proto.market_review_pb2 import TMarketReviewProfileProto
from ads.bsyeti.caesar.libs.profiles.proto.market_vendor_pb2 import TMarketVendorProfileProto
from ads.bsyeti.caesar.libs.profiles.proto.market_video_review_pb2 import TMarketVideoReviewProfileProto

from dj.lib.proto.profile_enums_pb2 import EProfileNamespace
from dj.lib.proto.profile_pb2 import TProfileProto as DJProfileProto
from dj.services.market.proto.profile_enums_pb2 import EObjectType

from ads.bsyeti.caesar.tests.ft.common.event import make_event


class _TestCaseMarketBase:
    object_namespace = EProfileNamespace.PN_Market

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        body = DJProfileProto()
        body.ObjectNamespace = self.object_namespace
        body.ObjectType = int(self.object_type)
        body.ObjectId = str(profile_id)
        body.ArchiveData.Values.add()
        body.ArchiveData.Values[-1].key = str(profile_id) * 2
        body.ArchiveData.Values[-1].value = "123"

        event = make_event(profile_id, time - int(random.randint(0, 60)), body)
        yield event

        if profile_id not in self.expected or self.expected[profile_id].ProfileData[0].Timestamp <= event.TimeStamp:
            expected = self.profile_class().DjProfiles
            expected.ProfileData.add()
            expected.ProfileData[0].Timestamp = event.TimeStamp
            expected.ProfileData[0].Profile.CopyFrom(body)
            self.expected[profile_id] = expected

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            expected = self.expected[getattr(profile, self.profile_key)]
            assert expected == profile.DjProfiles


class TestCaseMarketCategories(_TestCaseMarketBase):
    table = "MarketCategories"
    profile_class = TMarketCategoryProfileProto
    profile_key = "CategoryID"
    object_type = EObjectType.OT_Category


class TestCaseMarketFeshes(_TestCaseMarketBase):
    table = "MarketFeshes"
    profile_class = TMarketFeshProfileProto
    profile_key = "FeshID"
    object_type = EObjectType.OT_Fesh


class TestCaseMarketModels(_TestCaseMarketBase):
    table = "MarketModels"
    profile_class = TMarketModelProfileProto
    profile_key = "ModelID"
    object_type = EObjectType.OT_Product

    extra_profiles = {
        "NormalizedUrls": [],
    }


class TestCaseMarketReviews(_TestCaseMarketBase):
    table = "MarketReviews"
    profile_class = TMarketReviewProfileProto
    profile_key = "ReviewID"
    object_type = EObjectType.OT_Review


class TestCaseMarketVendors(_TestCaseMarketBase):
    table = "MarketVendors"
    profile_class = TMarketVendorProfileProto
    profile_key = "VendorID"
    object_type = EObjectType.OT_Vendor


class TestCaseMarketVideoReviews(_TestCaseMarketBase):
    table = "MarketVideoReviews"
    profile_class = TMarketVideoReviewProfileProto
    profile_key = "VideoReviewID"
    object_type = EObjectType.OT_VideoReview
