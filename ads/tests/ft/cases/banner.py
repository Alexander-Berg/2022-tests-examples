import pytest
import json
import random

from ads.bsyeti.caesar.libs.profiles.proto.banner_pb2 import TBannerProfileProto
from ads.bsyeti.caesar.libs.profiles.proto.host_pb2 import THostProfileProto
from ads.bsyeti.caesar.libs.profiles.proto.mobile_app_pb2 import TMobileAppData
from ads.bsyeti.caesar.libs.profiles.proto.mobile_deeplinks_pb2 import TMobileAppDeeplinks
from ads.bsyeti.caesar.libs.profiles.proto.url_pb2 import TUrlProfileProto
from ads.bsyeti.caesar.libs.profiles.python import normalize_host, normalize_url
from ads.bsyeti.caesar.tests.ft.common.matcher import check_fields_presence
from ads.bsyeti.libs.events.proto.banner_resources_pb2 import TBannerResources
from ads.bsyeti.libs.events.proto.bs_chevent_pb2 import TBsChEvent
from ads.bsyeti.big_rt.lib.supplier.yt_sorted_table.fullstate.proto.caesar_service_pb2 import TCaesarService
from ads.bsyeti.big_rt.lib.events.proto.event_pb2 import TEventMessage
from ads.bsyeti.libs.events.proto.joined_efh_profile_pb2 import TJoinedEFHProfileHitLog
from ads.bsyeti.libs.events.proto.modadvert_banner_texts_pb2 import TModAdvertBannerTexts
from ads.bsyeti.libs.events.proto.types_pb2 import EMessageType
from ads.bsyeti.libs.py_proto_counters.proto_counters import GeneralCountersIterator
from adv.direct.proto.banner_resources.banner_resources_pb2 import BannerResources
from ads.quality.adv_machine.lib.catmachine.protos.protos_pb2 import (
    TCatEngineProfile,
    TCatEngineItem,
    ECategoryType,
    ECatProfileType,
)
from irt.bannerland.proto.v1.bannerland.perf_banner_pb2 import TPerfBanner
from market.idx.datacamp.proto.specific import CarSpecific_pb2
from yabs.proto import user_profile_pb2
from yabs.server.proto.keywords import keywords_data_pb2

from ads.bsyeti.caesar.tests.ft.common.event import make_event

TURBO_ORIGINAL_URL_NORMALIZED = "http://12vt.ru/catalog/avtoelektrika/styazhki/"
TURBO_ORIGINAL_URL = "https://12vt.ru/catalog/avtoelektrika/styazhki/?r1=yandext&r2=&utm_source=abacaba#anchor"
TURBO_URL = "https://yandex.ru/turbo/12vt.ru/n/yandexturbolisting/catalog/avtoelektrika/styazhki/"
TURBO_URL_RESULT = (
    "https://yandex.ru/turbo/12vt.ru"
    "/n/yandexturbolisting/catalog/avtoelektrika/styazhki/?r1=yandext&r2=&utm_source=abacaba"
)
APPSFLYER_URL = (
    "http://app.appsflyer.com/com.greatapp" "?pid=chartboost_int&&af_adset_id=54822&clickid=wrong1&clickid=wrong2"
)
APPSFLYER_URL_RESULT = "http://app.appsflyer.com/com.greatapp?af_adset_id=54822&clickid=clickid&pid=chartboost_int"
ALREADY_CANONIZED_APPSLYER_URL = (
    "http://app.appsflyer.com/com.greatapp"
    "?pid=chartboost_int&af_adset_id=54822&af_c_id=0&c=YandexDirect_0&clickid=clickid"
)
KOCHAVA_IOS_URL = (
    "https://control.kochava.com/v1/cpi/click?device_id_type=idfa&redirect=https%3A%2F%2Fwww.yoox.com%2FRU"
)
KOCHAVA_IOS_URL_RESULT = "https://control.kochava.com/v1/cpi/click?device_id_type=idfa&idfa={IDFA_UC}&redirect=https%3A%2F%2Fwww.yoox.com%2FRU"
KOCHAVA_ANDROID_URL = "https://control.kochava.com/v1/cpi/click?device_id_type=adid"
KOCHAVA_ANDROID_URL_RESULT = "https://control.kochava.com/v1/cpi/click?adid={GOOGLE_AID_LC}&device_id_type=adid"
MOBILE_APP_BUNDLE_ID = "my_app"
MOBILE_APP_SOURCE_ID = 123
MOBILE_APP_LOCALE = "ru"
MOBILE_DEEPLINK = "mimarket://details/detailmini?id=lol.kek.cheburek&startDownload=true"
NORMALIZED_URL = "http://yandex.ru/"
NORMALIZED_HOST = "yandex.ru"
URL_WITH_DATA = "https://market.yandex.ru/"
NORMALIZED_URL_WITH_DATA = "http://market.yandex.ru/"
PROMO_BANNER_URL = "https://alfabank-sme.turbopages.org/promo/media/alfabank_sme/5-sovetov-dlia-biznesa-kak-uderjatsia-na-plavu-v-krizis-62a34dfdac7a075aa2a09840?&headless=true"

MDS_META = {
    "meta": {
        "ColorWizBack": "#FFFFF0",
        "ColorWizButton": "#FFFFF1",
        "ColorWizButtonText": "#FFFFF2",
        "average-color": "#FFFFF3",
        "cbirdaemon": {"cbirdaemon": {"nnpreds": {"ok_quality_v3": 0.33}}},
        "background-colors": {
            "bottom": "#FFFFF4",
            "left": "#FFFFF5",
            "right": "#FFFFF6",
            "top": "#FFFFF7",
        },
        "SomeField": "SomeJunk",
    },
    "sizes": {
        "x90": {
            "path": "/get-direct/00/x90",
            "height": 100,
            "width": 90,
            "smart-center": {"h": 68, "w": 71, "x": 3, "y": 0},
            "smart-centers": {
                "16:5": {"h": 28, "w": 90, "x": 1, "y": 28},
                "16:9": {"h": 51, "w": 90, "x": 0, "y": 11},
                "1:1": {"h": 68, "w": 68, "x": 4, "y": 0},
                "3:4": {"h": 68, "w": 51, "x": 12, "y": 0},
                "4:3": {"h": 68, "w": 90, "x": 0, "y": 0},
            },
        },
        "x160": {
            "path": "/get-direct/00/x160",
            "height": 90,
            "width": 160,
            "smart-center": {"h": 90, "w": 94, "x": 4, "y": 0},
            "smart-centers": {
                "16:5": {"h": 38, "w": 160, "x": 1, "y": 37},
                "16:9": {"h": 68, "w": 160, "x": 0, "y": 15},
                "1:1": {"h": 90, "w": 90, "x": 6, "y": 0},
                "3:4": {"h": 90, "w": 68, "x": 17, "y": 0},
                "4:3": {"h": 90, "w": 160, "x": 0, "y": 0},
            },
        },
        "y90": {
            "path": "/get-direct/00/y90",
            "height": 90,
            "width": 120,
            "smart-center": {"h": 90, "w": 94, "x": 4, "y": 0},
            "smart-centers": {
                "16:5": {"h": 38, "w": 120, "x": 1, "y": 37},
                "16:9": {"h": 68, "w": 120, "x": 0, "y": 15},
                "1:1": {"h": 90, "w": 90, "x": 6, "y": 0},
                "3:4": {"h": 90, "w": 68, "x": 17, "y": 0},
                "4:3": {"h": 90, "w": 120, "x": 0, "y": 0},
            },
        },
    },
}
DIRECT_IMAGE_FORMATS_TO_FILTER = {"X160"}


def _make_app_data():
    app_data = TMobileAppData()
    app_data.Title = MOBILE_APP_BUNDLE_ID
    return app_data.SerializeToString()


def _make_app_deeplinks():
    app_deeplinks = TMobileAppDeeplinks()
    app_deeplinks.AppGalleryDeeplink = MOBILE_DEEPLINK
    app_deeplinks.AppGetDeeplink = MOBILE_DEEPLINK
    return app_deeplinks.SerializeToString()


def _make_landing(title):
    landing = TUrlProfileProto.TLanding()
    landing.UpdateTime = random.randint(10**6, 10**10)
    if title:
        landing.Title = title
        landing.Url = "http://some-landing.ru/"
    return landing.SerializeToString()


def _make_sitelinks(href):
    sitelinks = [TBannerResources.TDirectBannersLogFields.TSitelink()]
    sitelinks[0].Href = href
    return sitelinks


class _TestCaseBase:
    table = "Banners"
    profile_class = TBannerProfileProto

    extra_profiles = {
        "NormalizedUrls": [
            {
                "NormalizedUrl": NORMALIZED_URL,
                "RobotFactors": "",
                "Landing": "",
                "Titles": "",
                "Erf": "",
                "MarketOffer": "",
            },
            {
                "NormalizedUrl": NORMALIZED_URL_WITH_DATA,
                "RobotFactors": "",
                "Landing": _make_landing("landing page title"),
                "Titles": "",
                "Erf": "",
                "MarketOffer": "",
            },
        ],
        "NormalizedHosts": [
            {
                "NormalizedHost": NORMALIZED_HOST,
                "RobotFactors": "",
            }
        ],
        "Orders": [],
        "AdGroups": [],
        "TurboUrlDict": [{"OriginalUrl": TURBO_ORIGINAL_URL_NORMALIZED, "TurboUrl": TURBO_URL}],
        "MobileApps": [
            {
                "BundleId": MOBILE_APP_BUNDLE_ID,
                "SourceID": MOBILE_APP_SOURCE_ID,
                "RegionName": MOBILE_APP_LOCALE,
                "LocaleName": "",
                "Data": _make_app_data(),
            }
        ],
        "MobileDeeplinks": [
            {
                "BundleId": MOBILE_APP_BUNDLE_ID,
                "SourceID": MOBILE_APP_SOURCE_ID,
                "RegionName": MOBILE_APP_LOCALE,
                "LocaleName": "",
                "Deeplinks": _make_app_deeplinks(),
            }
        ],
        "AliveBanners": [],
    }

    def __init__(self):
        self.expected = {}


class TestCaseAliveBanners(_TestCaseBase):
    def make_events(self, time, shard, profile_id):
        now = time - 24 * 3600

        body = TBannerResources()
        body.Source = 1
        body.Version = now
        event = make_event(profile_id, now, body)
        yield event

        for i in range(hash(profile_id) % 10 + 1):
            event.Type = EMessageType.BS_CHEVENT
            event.TimeStamp = now + hash(profile_id + i) % 100
            body = TBsChEvent()
            body.CounterType = (profile_id % 2) + 1
            body.SelectType = 14
            body.PlaceID = 542
            body.EventTime = event.TimeStamp
            event.Body = body.SerializeToString()
            yield event

            self.expected[profile_id] = max(self.expected.get(profile_id, 0), event.TimeStamp)

    def check_profiles(self, profiles):
        pass

    def check_alive_banners_table(self, rows):
        assert len(rows) == len(self.expected)
        for row in rows:
            assert self.expected.get(row[b"BannerID"], -1) == row[b"AliveTimeStamp"], row


class TestCaseBannerCounters(_TestCaseBase):
    def make_events(self, time, shard, profile_id):
        timestamp = time - 24 * 3600

        body = TBannerResources()
        body.Source = 1
        body.Version = timestamp
        yield make_event(profile_id, timestamp, body)

        body = TBsChEvent()
        body.CounterType = (profile_id % 2) + 1
        body.SelectType = 14
        body.PlaceID = 542
        body.EventTime = timestamp
        yield make_event(profile_id, timestamp, body)

        if profile_id not in self.expected:
            self.expected[profile_id] = {"clicks": 0, "shows": 0}

        self.expected[profile_id]["shows"] += float(body.CounterType == 1)
        self.expected[profile_id]["clicks"] += float(body.CounterType == 2)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            shows = 0
            clicks = 0
            has_select_type_values = False
            for counter_id, counter in GeneralCountersIterator(packed_counters=profile.Counters.PackedCounters):
                if counter_id == 1169:  # 1169 — shows by SelectType
                    for ckey, cvalue in counter:
                        has_select_type_values = True
                        assert ckey == 14
                elif counter_id == 1172:  # 1169 — shows by SelectType
                    for ckey, cvalue in counter:
                        has_select_type_values = True
                        assert ckey == 14
                if counter_id == 1123:
                    for ckey, cvalue in counter:
                        shows = cvalue
                if counter_id == 1124:
                    for ckey, cvalue in counter:
                        clicks = cvalue

            assert has_select_type_values
            assert self.expected[profile.BannerID]["clicks"] == pytest.approx(clicks, 0.01)
            assert self.expected[profile.BannerID]["shows"] == pytest.approx(shows, 0.01)


class TestCaseBannerResources(_TestCaseBase):
    SHARDS_COUNT = 10

    def __init__(self):
        super().__init__()
        self.iter_num = 0

    def make_events(self, time, shard, profile_id):
        self.iter_num += 1

        event = TEventMessage()
        event.TimeStamp = time - 24 * 3600
        event.ProfileID = str(profile_id).encode("utf-8")
        event.Type = EMessageType.BANNER_RESOURCES

        body = TBannerResources()
        body.Version = self.iter_num
        body.Source = 1
        body.Stop = not bool(profile_id % 5)
        body.Archive = not bool(profile_id % 7)
        body.UpdateInfo = bool(profile_id % 3)
        if body.UpdateInfo:
            n = (shard + 1) * profile_id
            if 0 == n % 9:
                body.Href = TURBO_ORIGINAL_URL
            elif 1 == n % 9:
                body.Href = NORMALIZED_URL
            elif 2 == n % 9:
                body.Href = URL_WITH_DATA
            elif 3 == n % 9:
                body.Href = APPSFLYER_URL
            elif 4 == n % 9:
                body.Href = KOCHAVA_IOS_URL
            elif 5 == n % 9:
                body.Href = KOCHAVA_ANDROID_URL
            elif 6 == n % 9:
                body.Href = ALREADY_CANONIZED_APPSLYER_URL
            elif 7 == n % 9:
                body.Href = PROMO_BANNER_URL
            else:
                body.Href = "https://ya.ru/{}".format(n)
            body.Title = "title {}".format(n)
            body.Body = "banner {}".format(n)
            body.Site = "ya{}.ru".format(n)
            body.TemplateID = n
            body.Categories.extend([x % 100 for x in range(profile_id % 8)])
            body.DirectBannersLogFields.Sitelinks.extend(_make_sitelinks(body.Href))
            body.AppData.BundleId = MOBILE_APP_BUNDLE_ID
            body.AppData.SourceID = MOBILE_APP_SOURCE_ID
            body.AppData.RegionName = MOBILE_APP_LOCALE
            body.DeeplinksFlags.EnableAppGet = True

            imagesInfo = body.DirectBannersLogFields.Resources.ImagesInfo.add()
            imagesInfo.MdsMeta = json.dumps(MDS_META)

            imageFormats = ("x90", "x160", "y90")
            for imageFormat in imageFormats:
                meta_decription = MDS_META["sizes"][imageFormat]

                image = imagesInfo.Images.add()
                image.Format = imageFormat.upper()
                image.Width = meta_decription["width"]
                image.Height = meta_decription["height"]
                image.Url = meta_decription["path"]

                if imageFormat == "y90":
                    sc = image.SmartCenters.add()
                    sc.X = meta_decription["smart-center"]["x"]
                    sc.Y = meta_decription["smart-center"]["y"]
                    sc.W = meta_decription["smart-center"]["w"]
                    sc.H = meta_decription["smart-center"]["h"]

        event.Body = body.SerializeToString()

        yield event

        if profile_id not in self.expected:
            self.expected[profile_id] = {"Flags": TBannerResources(), "Resources": TBannerResources()}
        expected = self.expected[profile_id]

        if expected["Flags"].Version < event.TimeStamp:
            expected["Flags"] = body
        if body.UpdateInfo and expected["Resources"].Version < event.TimeStamp:
            expected["Resources"] = body
            for image_info in expected["Resources"].DirectBannersLogFields.Resources.ImagesInfo:
                for i, image in enumerate(image_info.Images):
                    if image.Format in DIRECT_IMAGE_FORMATS_TO_FILTER:
                        del image_info.Images[i]

    def check_smart_center(self, expected, actual):
        coord_fields = ("X", "Y", "W", "H")
        for field in coord_fields:
            expected_value = getattr(expected, field)
            actual_value = getattr(actual, field)
            assert expected_value == actual_value, "%s != %s for coordinate %s" % (expected_value, actual_value, field)

    def check_promo_banner_flag(self, profile):
        if isinstance(profile.Resources.Href, bytes):
            href = profile.Resources.Href.decode("utf-8")
        else:
            href = profile.Resources.Href

        if href == PROMO_BANNER_URL:
            assert profile.Resources.IsPromoBanner is True
        else:
            assert profile.Resources.IsPromoBanner is False

    def check_smart_centers(self, mds_meta, actual, check_smart_center):
        sc_mds_meta_fields = {"X": "x", "Y": "y", "W": "w", "H": "h"}

        smart_centers = [(f, sc) for f, sc in mds_meta["sizes"][actual.Format.lower()]["smart-centers"].items()]
        smart_centers.sort(key=lambda x: x[0])

        assert len(smart_centers) == len(actual.SmartCenters)
        for (expected_smart_center_pair, actual_smart_center) in zip(smart_centers, actual.SmartCenters):
            for actual_field, expected_field in sc_mds_meta_fields.items():
                expected_value = expected_smart_center_pair[1][expected_field]
                actual_value = getattr(actual_smart_center, actual_field)
                assert expected_value == actual_value, "%s != %s for %s" % (
                    expected_value,
                    actual_value,
                    expected_field,
                )

        if check_smart_center:
            smart_center = mds_meta["sizes"][actual.Format.lower()]["smart-center"]
            for actual_field, expected_field in sc_mds_meta_fields.items():
                expected_value = smart_center[expected_field]
                actual_value = getattr(actual.SmartCenter, actual_field)
                assert expected_value == actual_value, "%s != %s for %s" % (
                    expected_value,
                    actual_value,
                    expected_field,
                )

    def check_parsed_mds_meta(self, mds_meta, actual):
        parsed_mds_meta_fields = {
            "ColorWizBack": "ColorWizBack",
            "ColorWizButton": "ColorWizButton",
            "ColorWizButtonText": "ColorWizButtonText",
            "AverageColor": "average-color",
        }

        for actual_feild, mds_meta_path in parsed_mds_meta_fields.items():
            expected_value = mds_meta["meta"][mds_meta_path]
            actual_value = getattr(actual, actual_feild)
            assert expected_value == actual_value, "%s != %s for mds_meta path %s" % (
                expected_value,
                actual_value,
                mds_meta_path,
            )

        expected_image_quality = mds_meta["meta"]["cbirdaemon"]["cbirdaemon"]["nnpreds"]["ok_quality_v3"]
        actual_image_quality = actual.ImageQuality

        assert abs(actual_image_quality - expected_image_quality) < 1e-6, "%f != %f for ImageQuality" % (
            expected_image_quality,
            actual_image_quality,
        )

    def check_images_info(self, expected, actual):
        mds_meta = json.loads(expected.MdsMeta)
        self.check_parsed_mds_meta(mds_meta, actual.ParsedMdsMeta)

        basic_image_fields = ("Format", "Width", "Height", "Url")
        assert len(expected.Images) == len(actual.Images)
        for (expected_image, actual_image) in zip(expected.Images, actual.Images):
            for field in basic_image_fields:
                expected_value = getattr(expected_image, field)
                actual_value = getattr(actual_image, field)
                assert expected_value == actual_value, "%s != %s for Image field %s" % (
                    expected_value,
                    actual_value,
                    field,
                )

            check_mds_meta_smart_center = True
            if len(expected_image.SmartCenters) > 0:
                check_mds_meta_smart_center = False
                self.check_smart_center(expected_image.SmartCenters[0], actual_image.SmartCenter)

            self.check_smart_centers(mds_meta, actual_image, check_mds_meta_smart_center)

    def get_url_profile(self, url):
        for row in self.extra_profiles["NormalizedUrls"]:
            if row["NormalizedUrl"] == url:
                return row

    def check_url_info(self, actual):
        url_profile = self.get_url_profile(NORMALIZED_URL_WITH_DATA)
        assert url_profile

        url_info = actual.Resources.Url
        landing = TUrlProfileProto.TLanding()
        landing.ParseFromString(url_profile["Landing"])
        assert url_info.LandingTitle == landing.Title
        assert url_info.LandingUpdateTime == landing.UpdateTime
        assert url_info.LandingUrl == landing.Url

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        turbo_url_checked_count = 0
        normalized_url_checked_count = 0
        normalized_url_with_data_checked_count = 0
        normalized_url_by_gemini = 0
        mobile_fetched_count = 0
        resource_fields = ("Version", "TemplateID", "Categories", "Title", "Body", "Site")
        flag_fields = ("Version", "Stop", "Archive")

        for profile in profiles:
            self.check_promo_banner_flag(profile)
            expected = self.expected[profile.BannerID]
            for field in resource_fields:
                actual_value = getattr(expected["Resources"], field)
                expected_value = getattr(profile.Resources, field)
                if isinstance(expected_value, str) and isinstance(actual_value, bytes):
                    actual_value = actual_value.decode("utf-8")
                elif isinstance(expected_value, bytes) and isinstance(actual_value, str):
                    expected_value = expected_value.decode("utf-8")

                assert expected_value == actual_value, "%s != %s for key %s" % (expected_value, actual_value, field)

            if APPSFLYER_URL.encode() == profile.Resources.OriginalHref:
                assert APPSFLYER_URL_RESULT.encode() == profile.Resources.Href
                assert APPSFLYER_URL_RESULT.encode() == profile.Resources.CanonizedHref
            elif KOCHAVA_IOS_URL.encode() == profile.Resources.OriginalHref:
                assert KOCHAVA_IOS_URL_RESULT.encode() == profile.Resources.Href
                assert KOCHAVA_IOS_URL_RESULT.encode() == profile.Resources.CanonizedHref
            elif KOCHAVA_ANDROID_URL.encode() == profile.Resources.OriginalHref:
                assert KOCHAVA_ANDROID_URL_RESULT.encode() == profile.Resources.Href
                assert KOCHAVA_ANDROID_URL_RESULT.encode() == profile.Resources.CanonizedHref
            else:
                assert not profile.Resources.HasField("OriginalHref")
                assert expected["Resources"].Href.encode() == profile.Resources.Href
                if not profile.Flags.Stop and not profile.Flags.Archive:
                    assert expected["Resources"].Href.encode() == profile.Resources.CanonizedHref
            if profile.Resources.HasField("OriginalHref"):
                assert expected["Resources"].Href.encode() == profile.Resources.OriginalHref

            if (
                profile.Resources.HasField("DirectBannersLogFields")
                and not profile.Flags.Stop
                and not profile.Flags.Archive
            ):
                assert [profile.Resources.Href.decode()] == profile.Resources.Sitelinks
                assert [profile.Resources.Href.decode()] == profile.Resources.CanonizedSitelinks
                assert [profile.Resources.OriginalHref.decode()] == profile.Resources.OriginalSitelinks

            if profile.Resources.Href == TURBO_ORIGINAL_URL.encode():
                assert TURBO_URL_RESULT == profile.Resources.TurboUrl.Url
                turbo_url_checked_count += 1

            for field in flag_fields:
                actual_value = getattr(expected["Flags"], field)
                expected_value = getattr(profile.Flags, field)
                assert expected_value == actual_value, "%s != %s for key %s" % (expected_value, actual_value, field)

            if hasattr(expected["Resources"], "DirectBannersLogFields"):
                expected_images_info = expected["Resources"].DirectBannersLogFields.Resources.ImagesInfo
                actual_images_info = profile.Resources.DirectBannersLogFields.Resources.ImagesInfo

                assert len(actual_images_info) == len(expected_images_info)
                for (expected_info, actual_info) in zip(expected_images_info, actual_images_info):
                    self.check_images_info(expected_info, actual_info)

            if normalize_url(profile.Resources.Href) == NORMALIZED_URL:
                empty_url_profile = TUrlProfileProto()
                robot_factors = ["Bert", "Erf", "Urldat"]
                check_fields_presence(
                    empty_url_profile.RobotFactors, robot_factors, profile.Resources.Url, robot_factors
                )
                check_fields_presence(
                    empty_url_profile.Landing,
                    ["Url", "Title"],
                    profile.Resources.Url,
                    ["LandingUrl", "LandingTitle"],
                    empty_string_as_nonpresent=True,
                )
                normalized_url_checked_count += 1
            if normalize_host(profile.Resources.Href) == NORMALIZED_HOST:
                empty_host_profile = THostProfileProto()
                robot_factors = ["Herf"]
                check_fields_presence(
                    empty_host_profile.RobotFactors, robot_factors, profile.Resources.Host, robot_factors
                )
            if (
                normalize_url(profile.Resources.Href) == NORMALIZED_URL_WITH_DATA
                and not profile.Flags.Stop
                and not profile.Flags.Archive
            ):
                self.check_url_info(profile)
                normalized_url_with_data_checked_count += 1
            if normalize_url(profile.Resources.Href):
                assert (
                    profile.Resources.Url.GeminiUrl or profile.Resources.Url.IsGeminiUrlBad
                ), "No GeminiUrl with not empty href"
                normalized_url_by_gemini += 1

            if expected["Resources"].AppData.BundleId:
                app_data = profile.Resources.AppData
                assert app_data.BundleId == MOBILE_APP_BUNDLE_ID
                assert app_data.SourceID == MOBILE_APP_SOURCE_ID
                assert app_data.RegionName == MOBILE_APP_LOCALE

                if not profile.Flags.Stop and not profile.Flags.Archive:
                    mobile_fetched_count += 1

                    mobile_app_data = profile.Resources.MobileAppData
                    assert mobile_app_data.AppData.Title == MOBILE_APP_BUNDLE_ID
                    assert mobile_app_data.UpdateTimestamp > 0

                    mobile_app_deeplinks = profile.Resources.MobileAppDeeplinks
                    assert not mobile_app_deeplinks.AppDeeplinks.AppGalleryDeeplink  # AppGallery flag is disabled
                    assert mobile_app_deeplinks.AppDeeplinks.AppGetDeeplink == MOBILE_DEEPLINK
                    assert mobile_app_deeplinks.UpdateTimestamp > 0
                else:
                    assert not profile.Resources.HasField("MobileAppData")
                    assert not profile.Resources.HasField("MobileAppDeeplinks")
            else:
                assert not profile.Resources.HasField("AppData")

        assert 0 != turbo_url_checked_count
        assert 0 != normalized_url_checked_count
        assert 0 != normalized_url_with_data_checked_count
        assert 0 != normalized_url_by_gemini
        assert 0 != mobile_fetched_count


class TestCaseBannerModAdvert(_TestCaseBase):
    def __init__(self):
        super().__init__()

    def make_events(self, time, shard, profile_id):
        body = TModAdvertBannerTexts()
        body.texts.brands_description.extend(["brand {}".format(i) for i in range(3)])
        body.texts.content_description.extend(["content {}".format(i) for i in range(3)])
        body.object_version = "E96F8BF0-05BF-11EB-AA5E-9C9EB885E89C"

        yield make_event(profile_id, time - 24 * 3600, body)

        if profile_id not in self.expected:
            self.expected[profile_id] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        fields = {
            "brands_description": "BrandsDescription",
            "content_description": "ContentDescription",
        }
        for profile in profiles:
            expected = self.expected[profile.BannerID]
            for src_field, dst_field in fields.items():
                text_actual = getattr(profile.Resources.ModAdvertBannerTexts, dst_field)
                text_expected = getattr(expected.texts, src_field)
                assert text_expected == text_actual, "%s != %s for key %s" % (text_expected, text_actual, src_field)

            expected_version = expected.object_version
            actual_version = profile.Resources.ModAdvertBannerTexts.ObjectVersion
            assert expected_version == actual_version


class TestCaseEssBannerResources(_TestCaseBase):
    def make_events(self, time, shard, profile_id):
        timestamp = time - 24 * 3600
        body = BannerResources()
        body.iter_id = timestamp
        body.order_id = 1
        body.adgroup_id = 1
        body.banner_id = profile_id
        body.export_id = 1
        body.feed_info.value.market_business_id = 11
        body.feed_info.value.market_shop_id = 12
        body.feed_info.value.market_feed_id = 13
        body.feed_info.value.direct_feed_id = 14
        if 0 != profile_id % 3:
            body.mobile_content_bundle_id = MOBILE_APP_BUNDLE_ID
            body.mobile_content_source = MOBILE_APP_SOURCE_ID

        yield make_event(profile_id, timestamp, body)

        if profile_id not in self.expected:
            self.expected[profile_id] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        mobile_fetched_count = 0
        for profile in profiles:
            expected = self.expected[profile.BannerID]

            if expected.feed_info:
                assert (
                    expected.feed_info.value.market_business_id
                    == profile.Resources.EssFeedInfo.Value.market_business_id
                )
                assert expected.iter_id == profile.Resources.EssFeedInfo.Version

            if expected.mobile_content_bundle_id:
                rc_app_key = profile.Resources.AppDataEss
                assert rc_app_key.BundleId == MOBILE_APP_BUNDLE_ID
                assert rc_app_key.SourceID == MOBILE_APP_SOURCE_ID

                assert not profile.Resources.HasField("MobileAppData")
                assert not profile.Resources.HasField("MobileAppDeeplinks")

                mobile_fetched_count += 1
            else:
                assert not profile.Resources.HasField("AppDataEss")

        assert mobile_fetched_count > 0


class TestCaseBLPerfBanner(_TestCaseBase):
    @property
    def extra_config_args(self):
        return {"disable_banner_moderation": True}

    def generate_profile_ids(self, _):
        return {0: [2 << 56]}

    def make_events(self, time, shard, profile_id):
        timestamp = time - 24 * 3600

        event = TEventMessage()
        event.TimeStamp = timestamp
        event.ProfileID = str(profile_id).encode("utf-8")
        event.Type = EMessageType.BANNER_BL_PERF

        body = TPerfBanner()
        body.BannerID = profile_id
        body.OrderID = 1
        body.Title = "Some title"
        body.Href = "https://some.com"
        body.DeltaTimestamp = timestamp
        body.DisplayInfo.state_enum = CarSpecific_pb2.ECarState.CAR_STATE_EXCELLENT

        event.Body = body.SerializeToString()

        yield event

        if profile_id not in self.expected:
            self.expected[profile_id] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            expected = self.expected[profile.BannerID]

            assert profile.Resources.BannerLandSpecificFields.DisplayInfo.state_enum == expected.DisplayInfo.state_enum


class TestCaseBannerWatcher(_TestCaseBase):
    def __init__(self):
        super().__init__()
        self.by_shard = {}
        self.iter_num = 0

    def make_events(self, time, shard, profile_id):
        self.iter_num += 1
        timestamp = time - 24 * 3600
        if 0 != (profile_id % 3):
            body = TBannerResources()
            body.Source = 1
            body.Version = self.iter_num
            body.OrderID = profile_id

            if 0 == (shard % 2):
                yield make_event(profile_id, timestamp, body)

                body = TBsChEvent()
                body.CounterType = (profile_id % 2) + 1
                body.SelectType = 14
                body.PlaceID = 542
                body.EventTime = time

            # account only profiles which real updated, touched empty profiles will not be saved
            self.expected[profile_id] = 1
        else:
            body = TCaesarService()

        yield make_event(profile_id, timestamp, body)

        self.by_shard[shard] = 1

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

    def check_watcher_events(self, events_by_shard):
        assert 0 != len(events_by_shard)
        for shard, events in enumerate(events_by_shard):
            # if there was updates for shard, there should be events for shard
            # we cannot count of events, because updates can be merged in processing
            assert bool(events) == bool(self.by_shard.get(shard, 0))
            for event in events:
                if 0 != event.BannerID % 3:
                    if 0 != (shard % 2):
                        assert 0 == 2 & event.ServiceFields.ChangedColumns
                    else:
                        assert 0 != 2 & event.ServiceFields.ChangedColumns
                else:
                    # just touched by service log, nothing modified
                    assert 0 == event.ServiceFields.ChangedColumns


class TestCaseBannerCatProfiles(_TestCaseBase):
    def make_events(self, time, shard, profile_id):
        if profile_id not in self.expected:
            item = user_profile_pb2.Profile.ProfileItem()
            item.keyword_id = keywords_data_pb2.KW_KRYPTA_TOP_DOMAINS
            item.uint_values.append(5)

            profile = user_profile_pb2.Profile()
            profile.items.append(item)

            body = TJoinedEFHProfileHitLog()
            body.EventTime = 1629753192
            body.BannerID = profile_id
            body.PageID = 3
            body.QTailID = 4
            body.PlaceID = 542
            body.SelectType = 82
            body.CounterType = 2
            body.ProfileDump = profile.SerializeToString()

            yield make_event(profile_id, time, body)

            body = BannerResources()
            body.iter_id = time
            body.order_id = 1
            body.adgroup_id = 1
            body.banner_id = profile_id
            body.export_id = 1
            body.banner_id = profile_id
            body.feed_info.value.market_business_id = profile_id % 5
            body.feed_info.value.market_shop_id = profile_id % 10
            body.feed_info.value.market_feed_id = 100 + profile_id % 20
            body.feed_info.value.direct_feed_id = 1000 + profile_id % 30

            yield make_event(profile_id, 1629753192, body)

            catItem1 = TCatEngineItem()
            catItem1.Type = ECategoryType.KryptaTopDomains
            catItem1.Ids.append(5)
            catItem1.Tfs.append(0.01)

            catItem2 = TCatEngineItem()
            catItem2.Type = ECategoryType.DayOfAWeek
            catItem2.Ids.append(1)
            catItem2.Tfs.append(0.01)

            catItem3 = TCatEngineItem()
            catItem3.Type = ECategoryType.HourOfDay
            catItem3.Ids.append(0)
            catItem3.Tfs.append(0.01)

            catItem4 = TCatEngineItem()
            catItem4.Type = ECategoryType.PageID
            catItem4.Ids.append(3)
            catItem4.Tfs.append(0.01)

            catItem5 = TCatEngineItem()
            catItem5.Type = ECategoryType.PageQtailID
            catItem5.Ids.append(4)
            catItem5.Tfs.append(0.01)

            catItem6 = TCatEngineItem()
            catItem6.Type = ECategoryType.HourOfWeek
            catItem6.Ids.append(24)
            catItem6.Tfs.append(0.01)

            catProfile = TCatEngineProfile()
            catProfile.ProfileType = ECatProfileType.BannerSmoothing
            catProfile.Items.append(catItem1)
            catProfile.Items.append(catItem2)
            catProfile.Items.append(catItem3)
            catProfile.Items.append(catItem4)
            catProfile.Items.append(catItem5)
            catProfile.Items.append(catItem6)

            self.expected[profile_id] = []
            self.expected[profile_id].append(catProfile)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            assert len(self.expected[profile.BannerID]) == len(profile.CatProfiles.Values)

            for i in range(len(profile.CatProfiles.Values)):
                expected = self.expected[profile.BannerID][i]
                actual = profile.CatProfiles.Values[i]

                actual.Items.sort(key=lambda item: item.Type)

                assert expected.ProfileType == actual.ProfileType
                assert len(expected.Items) == len(actual.Items)

                for j in range(len(expected.Items)):
                    assert expected.Items[j].Type == actual.Items[j].Type

                    assert len(expected.Items[j].Ids) == len(actual.Items[j].Ids)
                    assert len(expected.Items[j].Tfs) == len(actual.Items[j].Tfs)

                    for z in range(len(expected.Items[j].Ids)):
                        assert expected.Items[j].Ids[z] == actual.Items[j].Ids[z]
                        assert expected.Items[j].Tfs[z] == pytest.approx(actual.Items[j].Tfs[z])
