import pytest

from ads.bsyeti.libs.events.proto.bs_chevent_pb2 import TBsChEvent
from ads.bsyeti.libs.events.proto.joined_efh_profile_pb2 import TJoinedEFHProfileHitLog
from ads.bsyeti.libs.py_proto_counters.proto_counters import GeneralCountersIterator
from ads.quality.adv_machine.lib.catmachine.protos.protos_pb2 import (
    TCatEngineProfile,
    TCatEngineItem,
    ECategoryType,
    ECatProfileType,
)
from adv.partner.proto.page_pb2 import TPartnerPage
from yabs.server.cs.importers.caesar_page.lib.protos.caesar_page_pb2 import TCSPage, TBlockSettings
from ads.bsyeti.libs.events.proto.types_pb2 import EMessageType
from ads.bsyeti.libs.events.proto.money_map_page_pb2 import TMoneyMapPage
from yabs.proto import user_profile_pb2
from yabs.server.proto.keywords import keywords_data_pb2

from ads.bsyeti.caesar.tests.ft.common.event import make_event
from ads.bsyeti.caesar.tests.ft.common.service_event import make_service_event, make_full_state, TFullState


def poly2(a, b):
    poly2_magic = 0x4906BA494954CB65
    return (poly2_magic * (a + poly2_magic * b)) % 2**64


class TestCasePageCounters:
    table = "Pages"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        timestamp = time - 24 * 3600

        body = TBsChEvent()
        body.PageID = profile_id
        body.CounterType = (profile_id % 2) + 1
        body.ImpID = 14
        body.SelectType = 14
        body.PlaceID = 542
        body.EventTime = timestamp
        body.BannerID = 6164856744  # has InterfaceTagID = 221
        body.DistributionTagID = 221

        yield make_event(profile_id, timestamp, body)

        if profile_id not in self.expected:
            self.expected[profile_id] = {"clicks": 0, "shows": 0}

        self.expected[profile_id]["shows"] += float(body.CounterType == 1)
        self.expected[profile_id]["clicks"] += float(body.CounterType == 2)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            has_imp_id_values = False
            has_place_id_interface_tag_id_values = False
            for counter_id, counter in GeneralCountersIterator(packed_counters=profile.Counters.PackedCounters):
                if counter_id == 1575:  # 1575 — shows by ImpID
                    for ckey, cvalue in counter:
                        has_imp_id_values = True
                        assert ckey == 14
                elif counter_id == 1578:  # 1578 — clicks by ImpID
                    for ckey, cvalue in counter:
                        has_imp_id_values = True
                        assert ckey == 14
                elif counter_id == 1907:  # shows by PlaceID,InterfaceTagID
                    for ckey, cvalue in counter:
                        has_place_id_interface_tag_id_values = True
                        assert ckey == poly2(542, 221)

            assert has_imp_id_values
            assert has_place_id_interface_tag_id_values


class TestCasePartnerPage:
    table = "Pages"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        body = TPartnerPage()
        body.PageID = profile_id
        body.State = 1
        body.UpdateTime = 1609448400

        yield make_event(profile_id, time, body)

        if profile_id not in self.expected:
            self.expected[profile_id] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            assert self.expected[profile.PageID] == profile.PartnerPage


class TestCasePartnerPageProto:
    table = "Pages"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        body = TPartnerPage()
        body.PageID = profile_id
        body.State = 1
        body.UpdateTime = 1609448400

        yield make_event(profile_id, time, body, message_type=EMessageType.PARTNER_PAGE_PROTO)

        if profile_id not in self.expected:
            self.expected[profile_id] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            assert self.expected[profile.PageID] == profile.Resources.PartnerPageFromProtoTopic


class TestCaseMoneyMapPage:
    table = "Pages"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        page = TCSPage()
        page.PageID = profile_id
        page.State = 1
        page.TargetType = 3

        yield make_service_event(
            profile_id, time - 100, {"CSPage": make_full_state(TFullState.EFullStateExist.FS_YES, page)}
        )

        body = TMoneyMapPage()
        body.PageID = profile_id
        body.AbcID = 1
        body.Platform = "something"

        yield make_event(profile_id, time, body, message_type=EMessageType.MONEY_MAP_PAGE)

        if profile_id not in self.expected:
            self.expected[profile_id] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            assert self.expected[profile.PageID] == profile.Resources.MoneyMap.MoneyMapPage


class TestCaseMoneyMapPageImp:
    table = "Pages"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        page = TCSPage()
        page.PageID = profile_id
        page.State = 1
        page.TargetType = 3
        page_imp_items = page.PageImpItems.add()
        page_imp_items.PageID = profile_id
        page_imp_items.ImpID = 2
        page_imp_items.DSPType = 1

        yield make_service_event(
            profile_id, time - 100, {"CSPage": make_full_state(TFullState.EFullStateExist.FS_YES, page)}
        )

        body1 = TMoneyMapPage()
        body1.PageID = profile_id
        body1.ImpID = 1
        body1.AbcID = 1
        body1.Platform = "something"

        yield make_event(profile_id, time, body1, message_type=EMessageType.MONEY_MAP_PAGE_IMP)

        body2 = TMoneyMapPage()
        body2.PageID = profile_id
        body2.ImpID = 1
        body2.AbcID = 1231
        body2.Platform = "masOS"

        yield make_event(profile_id, time, body2, message_type=EMessageType.MONEY_MAP_PAGE_IMP)

        if profile_id not in self.expected:
            self.expected[(profile_id, body2.ImpID)] = body2

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            for pageImp in profile.Resources.MoneyMap.MoneyMapPageImp:
                assert self.expected[(pageImp.PageID, pageImp.ImpID)] == pageImp


class TestCasePageCatProfiles:
    table = "Pages"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        if profile_id not in self.expected:
            item = user_profile_pb2.Profile.ProfileItem()
            item.keyword_id = keywords_data_pb2.KW_KRYPTA_TOP_DOMAINS
            item.uint_values.append(5)

            profile = user_profile_pb2.Profile()
            profile.items.append(item)

            body = TJoinedEFHProfileHitLog()
            body.EventTime = 1629753192
            body.PageID = profile_id
            body.QTailID = 4
            body.PlaceID = 542
            body.SelectType = 82
            body.CounterType = 2
            body.ProfileDump = profile.SerializeToString()

            yield make_event(profile_id, time, body)

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

            catProfile = TCatEngineProfile()
            catProfile.ProfileType = ECatProfileType.PageSmoothing
            catProfile.Items.append(catItem1)
            catProfile.Items.append(catItem2)
            catProfile.Items.append(catItem3)

            self.expected[profile_id] = []
            self.expected[profile_id].append(catProfile)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            assert len(self.expected[profile.PageID]) == len(profile.CatProfiles.Values)

            for i in range(len(profile.CatProfiles.Values)):
                expected = self.expected[profile.PageID][i]
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


class TestCaseUpdateCSPageByFullState:
    table = "Pages"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        page = TCSPage()
        page.PageID = profile_id
        page.State = 1
        page.TargetType = 3

        yield make_service_event(
            profile_id, time - 100, {"CSPage": make_full_state(TFullState.EFullStateExist.FS_YES, page)}
        )

        if profile_id not in self.expected:
            self.expected[profile_id] = page

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            assert self.expected[profile.PageID] == profile.Resources.CSPage


def add_page_imp_item(self, page, imp_id):
    profile_id = page.PageID
    page_imp = page.PageImpItems.add()
    page_imp.PageID = profile_id
    page_imp.ImpID = imp_id
    page_imp.DSPType = 2
    page_imp.Options = (1 << 15) | (1 << 32)
    page_imp.VmapID = 13
    page_imp.CustomData = "kolodinevgeny"
    page_imp.BlockSettings.PageID = profile_id
    page_imp.BlockSettings.ImpID = page_imp.ImpID
    if self.block_settings_expected.get((profile_id, page_imp.ImpID), None) is None:
        page_imp.BlockSettings.BlockSettingsID = self.block_settings_id
        self.block_settings_id += 1
        self.block_settings_expected[(page_imp.PageID, page_imp.ImpID)] = page_imp.BlockSettings
    else:
        page_imp.BlockSettings.BlockSettingsID = self.block_settings_expected[(profile_id, page_imp.ImpID)].BlockSettingsID


def build_page(self, profile_id):
    page = TCSPage()
    page.PageID = profile_id
    page.State = 1
    page.TargetType = 2
    page.Name = "{}.test".format(profile_id)
    page.Options = 1 << 31 | 1 << 33  # Video,Protected
    page.Lang = "ru"
    page.BannerLangAsString = "ru,en"
    page.ContentType = "text/html"

    page.PartnerPage.PageID = profile_id
    page.PartnerPage.PartnerID = 3
    page.PartnerPage.DomainList = "abacaba"

    add_page_imp_item(self, page, 1)
    add_page_imp_item(self, page, 2)

    return page


def check_caesar_page_rows(self, rows):
    assert len(rows) == len(self.expected)
    for row in rows:
        expected_row = self.expected[row[b"PageID"]]
        assert row[b"State"] == expected_row.State
        assert row[b"TargetType"] == expected_row.TargetType
        assert row[b"Name"].decode() == expected_row.Name
        assert row[b"Options"] == expected_row.Options
        assert row[b"OptionsVideo"]
        assert row[b"OptionsProtected"]
        assert not row[b"OptionsMobile"]
        assert row[b"Lang"] == b"ru"
        assert row[b"BannerLang"] == 5
        assert row[b"BannerLangRu"]
        assert row[b"BannerLangEn"]
        assert not row[b"BannerLangUk"]
        assert row[b"ContentType"].decode() == expected_row.ContentType


def check_caesar_page_imp_rows(self, rows):
    assert len(rows) == len(self.expected) * 2
    expected_rows = dict()
    for cs_proto in self.expected.values():
        for page_imp_proto in cs_proto.PageImpItems:
            expected_rows[(page_imp_proto.PageID, page_imp_proto.ImpID)] = page_imp_proto

    for row in rows:
        expected_row = expected_rows[(row[b"PageID"], row[b"ImpID"])]
        assert row[b"DSPType"] == expected_row.DSPType
        assert row[b"OptionsUnmoderatedRtbAuction"]
        assert row[b"OptionsOnlyVerticalReachVideo"]
        assert not row[b"OptionsAllowMultipleDspAds"]
        assert row[b"VmapID"] == expected_row.VmapID
        assert row[b"CustomData"].decode() == expected_row.CustomData


def check_caesar_partner_page_rows(self, rows):
    assert len(rows) == len(self.expected)
    for row in rows:
        expected_row = self.expected[row[b"PageID"]]
        assert row[b"PartnerID"] == expected_row.PartnerPage.PartnerID
        assert row[b"DomainList"].decode() == expected_row.PartnerPage.DomainList


def check_caesar_block_settings_rows(self, rows):
    expected_rows = dict()
    for cs_page in self.expected.values():
        for page_imp_proto in cs_page.PageImpItems:
            expected_rows[page_imp_proto.BlockSettings.BlockSettingsID] = page_imp_proto.BlockSettings

    assert len(rows) == len(expected_rows)
    for row in rows:
        expected_row = expected_rows[row[b"BlockSettingsID"]]
        assert row[b"PageID"] == expected_row.PageID
        assert row[b"ImpID"] == expected_row.ImpID


class TestCaseCaesarPage:
    table = "Pages"
    extra_profiles = {
        "CaesarPage": [],
        "CaesarPageImp": [],
        "CaesarPartnerPage": [],
        "CaesarBlockSettings": [],
        "CaesarSSPPageMapping": [],
    }
    extra_config_args = {"enable_cspage_clearing": False}

    def __init__(self):
        self.expected = {}
        self.block_settings_expected = {}
        self.block_settings_id = 1

    def add_ssp_page_mapping_item(self, page, page_token):
        ssp_page_mapping = page.SSPPageMappings.add()
        ssp_page_mapping.PageID = page.PageID
        ssp_page_mapping.SSPID = page.PageID + 3
        ssp_page_mapping.PageToken = page_token

    def make_events(self, time, shard, profile_id):
        page = build_page(self, profile_id)

        self.add_ssp_page_mapping_item(page, "token0")
        self.add_ssp_page_mapping_item(page, "token1")

        yield make_service_event(
            profile_id, time - 100, {"CSPage": make_full_state(TFullState.EFullStateExist.FS_YES, page)}
        )

        if profile_id not in self.expected:
            self.expected[profile_id] = page

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            assert self.expected[profile.PageID].PartnerPage == profile.Resources.CSPage.PartnerPage
            for pageImp in profile.Resources.CSPage.PageImpItems:
                assert "kolodinevgeny" == pageImp.CustomData
                assert pageImp.PageID == pageImp.BlockSettings.PageID
                assert pageImp.ImpID == pageImp.BlockSettings.ImpID

    def check_caesar_page_table(self, rows):
        check_caesar_page_rows(self, rows)

    def check_caesar_page_imp_table(self, rows):
        check_caesar_page_imp_rows(self, rows)

    def check_caesar_partner_page_table(self, rows):
        check_caesar_partner_page_rows(self, rows)

    def check_caesar_block_settings_table(self, rows):
        check_caesar_block_settings_rows(self, rows)

    def check_caesar_s_s_p_page_mapping_table(self, rows):
        assert len(rows) == len(self.expected) * 2
        expected_rows = dict()
        for cs_proto in self.expected.values():
            for ssp_page_mapping_proto in cs_proto.SSPPageMappings:
                expected_rows[(ssp_page_mapping_proto.SSPID, ssp_page_mapping_proto.PageToken)] = ssp_page_mapping_proto

        assert len(rows) == len(expected_rows)
        for row in rows:
            expected_row = expected_rows[(row[b"SSPID"], row[b"PageToken"].decode())]
            assert row[b"PageID"] == expected_row.PageID


class TestCaseClearCaesarPage:
    table = "Pages"
    extra_profiles = {
        "CaesarPage": [],
        "CaesarPageImp": [],
        "CaesarPartnerPage": [],
        "CaesarBlockSettings": [],
    }
    extra_config_args = {"enable_cspage_clearing": True}

    def __init__(self):
        self.expected = {}
        self.block_settings_expected = {}
        self.block_settings_id = 1

    def make_events(self, time, shard, profile_id):
        page = build_page(self, profile_id)

        yield make_service_event(
            profile_id, time - 100, {"CSPage": make_full_state(TFullState.EFullStateExist.FS_YES, page)}
        )

        if profile_id not in self.expected:
            self.expected[profile_id] = page

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            assert TCSPage().PartnerPage == profile.Resources.CSPage.PartnerPage
            for pageImp in profile.Resources.CSPage.PageImpItems:
                assert "kolodinevgeny" == pageImp.CustomData
                assert TBlockSettings() == pageImp.BlockSettings

    def check_caesar_page_table(self, rows):
        check_caesar_page_rows(self, rows)

    def check_caesar_page_imp_table(self, rows):
        check_caesar_page_imp_rows(self, rows)

    def check_caesar_partner_page_table(self, rows):
        check_caesar_partner_page_rows(self, rows)

    def check_caesar_block_settings_table(self, rows):
        check_caesar_block_settings_rows(self, rows)
