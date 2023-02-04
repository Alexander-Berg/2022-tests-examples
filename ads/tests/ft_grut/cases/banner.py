from dataclasses import dataclass
import os
import random

from typing import Any, Dict, Iterable, List, Optional, Tuple

from ads.bsyeti.caesar.libs.profiles.python import get_banner_table_row_from_proto
from ads.bsyeti.caesar.libs.profiles.python import get_order_table_row_from_proto
from ads.bsyeti.caesar.libs.profiles.proto.banner_pb2 import TBannerProfileProto
from ads.bsyeti.caesar.libs.profiles.proto.order_pb2 import TOrderProfileProto

from irt.bannerland.proto.v1.bannerland.perf_banner_pb2 import TPerfBanner

from grut.python.object_api.client import ObjectApiClient
from grut.libs.object_api.proto import object_api_service_pb2
from grut.libs.proto.objects.autogen import schema_pb2
from grut.libs.proto.watch import watch_pb2
from grut.libs.proto.event_type import event_pb2
from grut.libs.bigrt.events.watchlog.proto.types_pb2 import EMessageType

from ads.bsyeti.caesar.tests.ft.common.event import make_event


@dataclass
class _BannerType:
    value: int
    upper_bits: int


_BANNER_TYPE_DYNAMIC = _BannerType(value=2, upper_bits=3)
_BANNER_TYPE_SMART = _BannerType(value=3, upper_bits=2)
_BANNER_TYPES = [_BANNER_TYPE_DYNAMIC, _BANNER_TYPE_SMART]
_ORDER_ID = 123
_COUNTER_ID = 123


def _get_list_chunks(lst: List[Any], chunk_size: int) -> List[Any]:
    for i in range(0, len(lst), chunk_size):
        yield lst[i : i + chunk_size]


def _get_grut_address() -> str:
    return os.environ["OBJECT_API_ADDRESS_0"]


class GrutBannersGenerator:
    def __init__(
        self,
        grut_client: ObjectApiClient,
        patch_for_bannerland: bool,
        fixed_campaign_id: Optional[int],
        preferred_banner_type: Optional[_BannerType],
    ) -> None:
        self.grut_client: ObjectApiClient = grut_client
        self.patch_for_bannerland: bool = patch_for_bannerland
        self.id2type: Dict[int, Optional[_BannerType]] = {}
        self.fixed_campaign_id: Optional[int] = fixed_campaign_id
        self.preferred_banner_type = preferred_banner_type

    @staticmethod
    def _generate_object_id() -> int:
        return random.randint(1000, 1000000000)

    def _create_campaign(self) -> int:
        if self.fixed_campaign_id:
            # some tests needs predefined campaign_id
            return self.fixed_campaign_id
        else:
            return self._generate_object_id()

    def _create_ad_group(self) -> int:
        return self._generate_object_id()

    @staticmethod
    def _to_bannerland_banner_id(banner_id: int, banner_type: _BannerType) -> int:
        offset = 56
        lower_mask = (1 << offset) - 1
        lower = banner_id & lower_mask
        upper = banner_type.upper_bits << offset
        return upper + lower

    def _create_banners(self, campaign_id: int, ad_group_id: int, banner_count: int) -> None:
        # make unique banner ids
        while len(self.id2type) < banner_count:
            banner_type: Optional[_BannerType] = None
            banner_id = self._generate_object_id()
            if self.patch_for_bannerland:
                if self.preferred_banner_type:
                    banner_type = self.preferred_banner_type
                else:
                    banner_type = _BANNER_TYPES[random.randint(0, len(_BANNER_TYPES) - 1)]
                banner_id = self._to_bannerland_banner_id(banner_id, banner_type)
            self.id2type[banner_id] = banner_type

        req = object_api_service_pb2.TReqCreateObjects()
        req.object_type = schema_pb2.OT_BANNERLAND_BANNER
        for banner_num, banner_id in enumerate(self.id2type.keys()):
            subrequest = req.payloads.add()

            meta = schema_pb2.TBannerlandBannerMeta()
            meta.id = banner_id
            meta.campaign_id = campaign_id
            subrequest.meta = meta.SerializeToString()

            spec = schema_pb2.TBannerlandBannerSpec()
            spec.banner.BannerId = banner_id
            spec.banner.AdGroupId = ad_group_id
            spec.banner.OrderId = campaign_id
            spec.banner.OfferId = str(campaign_id)
            spec.banner.DirectParentBannerId = self._generate_object_id()
            spec.banner.Title = "Banner #{}".format(banner_num)
            spec.banner.Href = "http://ya.ru/banner/{}".format(banner_id)
            subrequest.spec = spec.SerializeToString()

        response = self.grut_client.create_objects(req)
        assert response.commit_timestamp > 0

    def generate(self, banner_count: int = 500):
        campaign_id = self._create_campaign()
        ad_group_id = self._create_ad_group()
        self._create_banners(campaign_id, ad_group_id, banner_count)

    def iterate(self):
        request = object_api_service_pb2.TReqGetObjects()
        request.object_type = schema_pb2.OT_BANNERLAND_BANNER
        request.attribute_selector.extend(["/meta", "/spec"])
        request.skip_nonexistent = True
        for banner_id in self.id2type.keys():
            meta = schema_pb2.TBannerlandBannerMeta()
            meta.id = banner_id
            request.identities.append(meta.SerializeToString())

        response = self.grut_client.get_objects(request)
        for payload in response.payloads:
            banner = schema_pb2.TBannerlandBanner()
            banner.ParseFromString(payload.protobuf)
            banner_id = int(banner.meta.id)
            yield (banner_id, self.id2type[banner_id], banner)

    @staticmethod
    def make_banners(
        patch_for_bannerland: bool, fixed_campaign_id: Optional[int], preferred_banner_type: Optional[_BannerType]
    ) -> Dict[int, Tuple[Optional[_BannerType], schema_pb2.TBanner]]:
        result = {}
        with ObjectApiClient(_get_grut_address(), "grpc", {"enable_ssl": False}) as grut_client:
            db_generator = GrutBannersGenerator(
                grut_client=grut_client,
                patch_for_bannerland=patch_for_bannerland,
                fixed_campaign_id=fixed_campaign_id,
                preferred_banner_type=preferred_banner_type,
            )
            db_generator.generate()
            for banner_id, banner_type, banner in db_generator.iterate():
                result[banner_id] = (banner_type, banner)
        return result


class _TestCaseBase:
    table = "Banners"

    extra_profiles = {"Orders": []}

    def __init__(self) -> None:
        self.expected = GrutBannersGenerator.make_banners(
            patch_for_bannerland=self.patch_for_bannerland(),
            fixed_campaign_id=self.fixed_campaign_id(),
            preferred_banner_type=self.preferred_banner_type(),
        )

    @property
    def extra_config_args(self) -> Dict[str, Any]:
        return {"grut_address": _get_grut_address(), "disable_banner_moderation": True}

    def patch_for_bannerland(self) -> bool:
        return True

    def fixed_campaign_id(self) -> Optional[int]:
        return None

    def preferred_banner_type(self) -> Optional[_BannerType]:
        return None

    def generate_profile_ids(self, shard_count: int) -> Dict[int, List[int]]:
        banner_ids: List[int] = list(self.expected.keys())
        chunk_size: int = len(banner_ids) // shard_count + 1
        return {
            shard_num: banner_ids_chunks
            for shard_num, banner_ids_chunks in enumerate(_get_list_chunks(banner_ids, chunk_size))
        }

    def make_events(self, time: int, shard: int, profile_id: int):
        now = time - 24 * 3600
        body = watch_pb2.TBigRtEvent()
        body.timestamp = now
        body.event_type = event_pb2.EEventType.ET_OBJECT_CREATED
        body.object_type = schema_pb2.OT_BANNERLAND_BANNER
        body.object_id = str(profile_id).encode("utf-8")
        yield make_event(profile_id, now, body, message_type=EMessageType.GRUT_EVENT)


class TestCaseGrutBannerlandSuccess(_TestCaseBase):
    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            assert profile.BannerID in self.expected

            banner_type, banner = self.expected[profile.BannerID]
            assert banner_type.value == profile.Flags.Source

            bannerland = banner.spec.banner
            assert bannerland.BannerId == profile.BannerID
            assert bannerland.OrderId == profile.OrderID
            assert bannerland.AdGroupId == profile.Resources.GroupBannerID
            assert bannerland.Title == profile.Resources.Title.decode("utf-8")
            assert bannerland.Href == profile.Resources.Href.decode("utf-8")
            assert bannerland.DirectParentBannerId == profile.Resources.ParentExportID
            assert bannerland.OfferId == profile.Resources.BannerLandSpecificFields.OfferID


class TestCaseGrutBannerlandBadId(_TestCaseBase):
    assert_empty_profiles = False

    def patch_for_bannerland(self) -> bool:
        return False

    def check_profiles(self, profiles):
        assert len(profiles) == 0


class _TestCaseOfferUrlDictBase(_TestCaseBase):
    @staticmethod
    def _make_order_row(order_id: Optional[int], counter_id: int) -> Dict[str, Any]:
        order = TOrderProfileProto()
        assert order_id
        order.OrderID = order_id
        order.Resources.DirectBannersLogFields.CounterID = counter_id
        serialized_profile = order.SerializeToString()
        return get_order_table_row_from_proto(str(order_id), serialized_profile)

    @property
    def extra_profiles(self) -> Dict[str, Any]:
        return {
            "Orders": [self._make_order_row(self.fixed_campaign_id(), _COUNTER_ID)],
            "OfferUrlDict": [],
        }

    @property
    def extra_config_args(self) -> Dict[str, Any]:
        config = super().extra_config_args
        config["offer_url_dict_enabled"] = True
        return config


class TestCaseGrutOfferUrlDict(_TestCaseOfferUrlDictBase):
    def fixed_campaign_id(self) -> Optional[int]:
        return _ORDER_ID

    def preferred_banner_type(self) -> Optional[_BannerType]:
        return _BANNER_TYPE_SMART

    def check_profiles(self, profiles):
        assert len(profiles) == len(self.expected)

    def check_offer_url_dict_table(self, rows):
        # Each banner has unique url so OfferUrlDict should have same number of rows
        assert len(rows) == len(self.expected)


class TestCaseGrutOfferUrlDictSkipBannerType(_TestCaseOfferUrlDictBase):
    def fixed_campaign_id(self) -> Optional[int]:
        return _ORDER_ID + 1

    def preferred_banner_type(self) -> Optional[_BannerType]:
        return _BANNER_TYPE_DYNAMIC

    def check_profiles(self, profiles):
        assert len(profiles) == len(self.expected)

    def check_offer_url_dict_table(self, rows):
        # No offers for dynamic banners
        assert len(rows) == 0


class TestCaseSkipGrutBannersForBannerLand(_TestCaseBase):
    def __init__(self):
        super().__init__()
        self.banner_profiles: Dict[int, TBannerProfileProto] = {}

    def _make_banner_profiles(self) -> Iterable[Dict[str, Any]]:
        rows = []
        for banner_id, (banner_type, grut_banner) in self.expected.items():
            bannerland_banner = grut_banner.spec.banner

            banner = TBannerProfileProto()
            banner.BannerID = banner_id
            banner.OrderID = bannerland_banner.OrderId

            banner.Flags.Source = banner_type.value
            banner.Flags.IsFromGrut = True
            banner.Flags.Version = 1

            banner.Resources.Title = bannerland_banner.Title.encode("utf-8")
            banner.Resources.Href = bannerland_banner.Href.encode("utf-8")

            self.banner_profiles[banner_id] = banner

            rows.append(get_banner_table_row_from_proto(str(banner_id), banner.SerializeToString()))
        return rows

    @property
    def extra_profiles(self) -> Dict[str, Any]:
        return {
            "Orders": [],
            "Banners": self._make_banner_profiles(),
        }

    def preferred_banner_type(self) -> Optional[_BannerType]:
        return _BANNER_TYPE_SMART

    def make_perf_banner(self, now: int, profile_id: int) -> TPerfBanner:
        _, grut_banner = self.expected[profile_id]
        bannerland_banner = grut_banner.spec.banner

        result = TPerfBanner()
        result.BannerID = profile_id
        result.OrderID = bannerland_banner.OrderId
        result.Title = bannerland_banner.Title + "_mutated"
        result.Href = bannerland_banner.Href + "#mutated"
        result.DeltaTimestamp = now
        return result

    def make_events(self, time: int, shard: int, profile_id: int):
        now = time - 24 * 3600
        perf_banner = self.make_perf_banner(now, profile_id)
        yield make_event(profile_id, now, perf_banner)

    def check_profiles(self, profiles):
        assert len(profiles) == len(self.banner_profiles)
        for profile in profiles:
            assert profile.BannerID in self.banner_profiles
            expected_profile = self.banner_profiles[profile.BannerID]

            # assert profile not mutated
            assert expected_profile.Flags.Version == profile.Flags.Version
            assert expected_profile.Resources.Title == profile.Resources.Title
            assert expected_profile.Resources.Href == profile.Resources.Href
