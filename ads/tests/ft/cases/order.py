import datetime
import json
import pytest
import pytz
import random
import time
from typing import List

from google.protobuf.json_format import MessageToJson, ParseDict, MessageToDict
from google.protobuf.internal.encoder import _VarintBytes

from ads.autobudget.protos.experiment_pb2 import TAdsBidderSettings, TAutobudgetSettings
from ads.bsyeti.caesar.libs.profiles.proto.order_pb2 import TOrderProfileProto
from ads.bsyeti.libs.events.proto.active_order_counters_pb2 import TActiveOrderCounters  # noqa
from ads.bsyeti.caesar.libs.profiles.proto.goal_pb2 import TGoalProfileProto
from ads.bsyeti.libs.events.proto.order_resources_pb2 import TOrderResources
from ads.bsyeti.libs.events.proto.order_update_time_pb2 import TOrderUpdateTime
from ads.bsyeti.libs.events.proto.smart_dynamic_parent_event_pb2 import TSmartDynamicParentEvent

from adv.direct.proto.campaign.campaign_pb2 import Campaign
from ads.bsyeti.caesar.tests.ft.common.service_event import (
    make_service_event,
    make_full_state,
    TFullState,
)
from ads.bsyeti.caesar.tests.ft.common.event import make_event

from grut.libs.proto.experimental.banner import banner_source_pb2

from qabs_bsserver_pytest.bs_objects.experiments import UsersplitConfig, get_bigb_flags

from yatest.common import work_path


def make_simple_order_money():
    # taken from prestable
    order_money = TOrderProfileProto.TResources.TOrderMoney()
    order_money.GroupLastEventTime = 1627490818
    order_money.GroupMoneyLeft = -488453948394
    order_money.GroupMoneyLeftCur = 1506579207
    order_money.IsPeriodLimitReached = False
    order_money.IsPeriodLimitReachedCur = False
    order_money.OrderLastEventTime = 1600927776
    order_money.OrderMoneyLeft = -1592708
    order_money.OrderMoneyLeftCur = -485911
    return order_money


SIMPLE_ORDER_MONEY = make_simple_order_money()


def make_simple_autobudget_controls():
    autobudget_controls = TOrderProfileProto.TAutobudgetResources.TAutobudgetControls()
    autobudget_controls.DataTimestamp = 1600927776
    autobudget_controls.AutobudgetStartTime = 1600927796
    autobudget_controls.AutobudgetSettingFromBidder.CopyFrom(
        TOrderProfileProto.TAutobudgetResources.TAutobudgetControls.TAutobudgetSettingFromBidder()
    )
    return autobudget_controls


SIMPLE_AUTOBUDGET_CONTROLS = make_simple_autobudget_controls()


def make_order_body(timestamp):
    order_body = TOrderResources()
    order_body.Version = timestamp
    order_body.Stop = bool(random.randint(0, 1))
    order_body.Archive = bool(random.randint(0, 1))
    order_body.UpdateInfo = bool(random.randint(0, 1))

    meaningful_goal = order_body.DirectBannersLogFields.MeaningfulGoals.add()

    meaningful_goal.GoalID = random.randint(1, 100)
    meaningful_goal.Value = float(random.randint(1, 100))
    meaningful_goal.IsMetrikaSourceOfValue = bool(random.randint(0, 1))

    if order_body.UpdateInfo:
        order_body.ShowConditions.TargetTypes.extend(
            random.sample(
                [
                    TOrderResources.TT_SEARCH,
                    TOrderResources.TT_CATALOG,
                    TOrderResources.TT_PARTNER,
                    TOrderResources.TT_RSYA,
                ],
                random.randint(1, 4),
            )
        )
    return order_body


def make_order_event(profile_id, time):
    body = make_order_body(time)
    order_event = make_event(profile_id, time - random.randint(0, 60), body)
    return order_event, body


class _BaseTestCaseOrders:
    table = "Orders"

    def __init__(self):
        self.expected = {}


class _BaseTestCaseCofiguredAutobudgetOrder(_BaseTestCaseOrders):
    """Allows setup configuration for autobudget(both default, preprod and AB experiments) instead of using fixed Sandbox resources
    Args:
        default_settings: Default autobudget settings
        preprod_settings: Default autobudget settings
        experiments: List of AB experiments

    Inherite and initialize in the following manner:
        default_settings = TAdsBidderSettings()
        preprod_settings = TAdsBidderSettings()
        experiment_55 = TAutobudgetSettings()
        experiment_55.AutobudgetExperimentID = 55
        experiment_55.AdsBidderSettings.StartTime = 10

        super().__init__(default_settings, preprod_settings, [experiment_55])
    """

    AUTOBUDGET_DEFAULT_CONFIG_FILE = work_path("bidder-config-package/resource/config/default_test.conf")
    AUTOBUDGET_PREPROD_CONFIG_FILE = work_path("bidder-config-package/resource/config/predefault_test.conf")
    BIGB_AB_EXPERIMENTS_CONFIG_FILE = work_path("bigb_ab_production_config_test.json")
    extra_config_args = {
        "autobudget_default_config_file": AUTOBUDGET_DEFAULT_CONFIG_FILE,
        "autobudget_preprod_config_file": AUTOBUDGET_PREPROD_CONFIG_FILE,
        "bigb_ab_experiments_config_file": BIGB_AB_EXPERIMENTS_CONFIG_FILE,
    }

    def __init__(
        self,
        default_settings: TAdsBidderSettings,
        preprod_settings: TAdsBidderSettings,
        experiments: List[TAutobudgetSettings] = [],
    ):
        super().__init__()

        with open(self.AUTOBUDGET_DEFAULT_CONFIG_FILE, "wb") as conf:
            conf.write(_VarintBytes(default_settings.ByteSize()) + default_settings.SerializeToString())

        with open(self.AUTOBUDGET_PREPROD_CONFIG_FILE, "wb") as conf:
            conf.write(_VarintBytes(preprod_settings.ByteSize()) + preprod_settings.SerializeToString())

        usersplit_config = UsersplitConfig()
        default_param = {
            "AutobudgetSettings": {
                "AutobudgetExperimentID": 0,
            },
            "MetaParameters": {"ExperimentType": "DEFAULT"},
        }
        flags = get_bigb_flags(0, default_param)
        usersplit_config.add_experiment(flags)

        for exp in experiments:
            flags = get_bigb_flags(exp.AutobudgetExperimentID, {"AutobudgetSettings": MessageToDict(exp)})
            usersplit_config.add_experiment(flags)

        tsv = usersplit_config.get_tsv()

        cfg = {
            "configs": [
                {
                    "timestamp": 0,
                    "ab_config_version": 0,
                    "data": tsv,
                    "test_id_tree_info": {
                        "Infos": [
                            {
                                "ParentDimensionId": 0,
                                "Percent": 100,
                                "TestId": 0,
                                "Title": "default autobudget experiment",
                            }
                        ].extend(
                            [
                                {
                                    "ParentDimensionId": 0,
                                    "Percent": 100,
                                    "TestId": exp.AutobudgetExperimentID,
                                    "Title": "autobudget experiment",
                                }
                                for exp in experiments
                            ]
                        )
                    },
                }
            ],
            "long_format_version": 1,
        }
        with open(self.BIGB_AB_EXPERIMENTS_CONFIG_FILE, "w") as conf:
            conf.write(json.dumps(cfg))


class TestCaseOrders(_BaseTestCaseOrders):
    def mock_expression(self, expression, seed_id):
        and_len = seed_id % 4 + 1

        for and_idx in range(and_len):
            disjunction = getattr(expression, "and").add()
            or_len = (and_len + and_idx) % 7 + 1

            for or_idx in range(or_len):
                atom = getattr(disjunction, "or").add()
                atom.keyword = seed_id % 113
                atom.operation = seed_id % 73
                atom.value = "Value{}_{}_{}".format(seed_id, and_idx, or_idx)

    def mock_rf_options(self, rf_options):
        rf_options.max_shows_count = 2
        rf_options.max_shows_period = 10
        rf_options.stop_shows_period = 5
        rf_options.max_clicks_count = 4
        rf_options.max_clicks_period = 20

    def make_campaign(self, time, shard, profile_id):

        body = Campaign()

        body.order_id = profile_id
        body.iter_id = time
        body.update_time = time
        body.rotation_goal_id = profile_id % 5
        body.export_id = shard * 2 + profile_id % 2
        body.mobile_app_ids.values.extend([profile_id % 3, profile_id % 10])
        body.allowed_on_adult_content = bool(profile_id % 2)
        body.metatype = profile_id % 2
        body.is_ww_managed_order = bool(profile_id % 3)

        if profile_id % 2 == 0 and time % 2 == 0:
            self.mock_rf_options(body.rf_options)

        if profile_id % 2 == 0 and time % 2 == 0:
            multiplier = body.multipliers.multiplier.add()
            self.mock_expression(multiplier.condition, profile_id)
            multiplier.value = profile_id % 10
            body.metrika_counter_ids.values.append(profile_id % 9)

        event = make_event(profile_id, time, body)

        expected = self.expected[profile_id]

        if expected.Resources.ESSVersion <= body.iter_id:
            expected.Resources.ESSVersion = body.iter_id

            if time % 2 == 0 and profile_id % 2 == 0:
                expected.Resources.ClearField("ESSMultipliers")
                expected.Resources.ESSMultipliers.extend(body.multipliers.multiplier)
                expected.Resources.ClearField("ESSMetrikaCounterIDs")
                expected.Resources.ESSMetrikaCounterIDs.extend(body.metrika_counter_ids.values)

            expected.Resources.ESSRotationGoalID = body.rotation_goal_id
            if len(expected.Resources.ESSMobileAppIDs.values):
                expected.Resources.ESSMobileAppIDs.ClearField("values")
            expected.Resources.ESSMobileAppIDs.values.extend(body.mobile_app_ids.values)
            expected.Resources.ESSAllowedOnAdultContent = body.allowed_on_adult_content
            expected.Resources.ESSFrequencyShowsOptions.CopyFrom(body.rf_options)
            expected.Resources.ESSWidgetPartnerID = body.widget_partner_id
            expected.Resources.ESSMetatype = body.metatype
            expected.Resources.ESSIsWWManagedOrder = body.is_ww_managed_order

        return event

    def make_order(self, time, shard, profile_id):
        event, body = make_order_event(profile_id, time)
        if profile_id not in self.expected:
            self.expected[profile_id] = TOrderProfileProto()

        expected = self.expected[profile_id]
        if expected.Flags.Version <= body.Version:
            expected.Flags.Stop = body.Stop
            expected.Flags.Version = body.Version
            expected.Flags.Archive = body.Archive
        if body.UpdateInfo and expected.Resources.Version <= body.Version:
            expected.Resources.Version = body.Version
            if len(expected.Resources.ShowConditions.TargetTypes):
                expected.Resources.ShowConditions.ClearField("TargetTypes")
            expected.Resources.ShowConditions.TargetTypes.extend(body.ShowConditions.TargetTypes)

            if len(expected.Resources.DirectBannersLogFields.MeaningfulGoals):
                expected.Resources.DirectBannersLogFields.ClearField("MeaningfulGoals")
            expected.Resources.DirectBannersLogFields.MeaningfulGoals.extend(
                body.DirectBannersLogFields.MeaningfulGoals
            )

        return event

    def make_events(self, time, shard, profile_id):
        yield self.make_order(time, shard, profile_id)
        yield self.make_campaign(time, shard, profile_id)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            expected = self.expected[profile.OrderID]
            assert expected.Resources.Version == profile.Resources.Version
            assert expected.Resources.ShowConditions == profile.Resources.ShowConditions

            assert expected.Flags.Version == profile.Flags.Version
            assert expected.Flags.Stop == profile.Flags.Stop
            assert expected.Flags.Archive == profile.Flags.Archive

            assert expected.Resources.ESSRotationGoalID == profile.Resources.ESSRotationGoalID
            assert expected.Resources.ESSMobileAppIDs == profile.Resources.ESSMobileAppIDs
            assert expected.Resources.ESSMultipliers == profile.Resources.ESSMultipliers
            assert expected.Resources.ESSWidgetPartnerID == profile.Resources.ESSWidgetPartnerID
            assert expected.Resources.ESSMetrikaCounterIDs == profile.Resources.ESSMetrikaCounterIDs
            assert expected.Resources.ESSMetatype == profile.Resources.ESSMetatype
            assert expected.Resources.ESSIsWWManagedOrder == profile.Resources.ESSIsWWManagedOrder

            assert (
                expected.Resources.DirectBannersLogFields.MeaningfulGoals
                == profile.Resources.DirectBannersLogFields.MeaningfulGoals
            )


class TestCaseOrderUpdateTime(_BaseTestCaseOrders):

    extra_profiles = {
        "OrderUpdateTime": [],
    }

    def make_events(self, time, shard, profile_id):
        order_event, _ = make_order_event(profile_id, time)
        yield order_event

        body = TOrderUpdateTime()
        body.UpdateTime = order_event.TimeStamp

        event = make_event(profile_id, time - 24 * 3600, body)
        yield event

        self.expected[profile_id] = max(self.expected.get(profile_id, 0), event.TimeStamp)

    def check_profiles(self, profiles):
        pass

    def check_order_update_time_table(self, rows):
        assert len(rows) == len(self.expected)


class TestCaseCaesarAutoBudgetOrderCommon(_BaseTestCaseOrders):

    extra_profiles = {
        "CaesarAutoBudgetOrder": [],
        "CaesarAutoBudgetOrderWithHistory": [],
    }

    def __init__(self):
        super().__init__()
        self.test_cases = [
            {
                "Resources": {
                    "AutoBudget": {
                        "Enabled": True,
                        "AutoBudgetGoalID": 2,
                        "AutoBudgetROILevel": 1.00001,
                        "AutoBudgetRestartReason": "AUTOBUDGET_START",
                        "StrategyParams": {
                            "StrategyID": 888
                        }
                    },
                    "DirectBannersLogFields": {
                        "EngineID": 7,
                    },
                    "UpdateInfo": True,
                },
                "Expected": {
                    "GoalID": 2,
                    "ROILevel": 100.001,
                    "RestartReason": b"AUTOBUDGET_START",
                    "StrategyID": 888,
                },
            },
            {
                "Resources": {
                    "AutoBudget": {
                        "Enabled": True,
                        "AutoBudgetGoalID": 2,
                        "AutoBudgetROILevel": 3.22,
                        "StrategyParams": {
                            "StrategyID": 888
                        }
                    },
                    "DirectBannersLogFields": {
                        "EngineID": 7,
                    },
                    "UpdateInfo": True,
                },
                "Expected": {
                    "GoalID": 2,
                    "ROILevel": 322,
                    "StrategyID": 888,
                },
            },
        ]
        self.iter_case = iter(self.test_cases)

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = next(self.iter_case)
        self.expected[profile_id] = test_case

        order_body = TOrderResources()
        ParseDict(test_case["Resources"], order_body)
        order_body.Version = time

        yield make_event(profile_id, time - random.randint(0, 60), order_body)

    def check_profiles(self, profiles):
        pass

    def check_caesar_auto_budget_order_table(self, rows):
        assert len(rows) == len(self.test_cases)
        for row in rows:
            test_case = self.expected[row[b"OrderID"]]
            for key, value in test_case["Expected"].items():
                assert row[key.encode("ascii")] == value, test_case


class TestCaseCaesarAutoBudgetOrderMaxDayLimitMoney(_BaseTestCaseOrders):

    extra_profiles = {
        "CaesarAutoBudgetOrder": [],
        "CaesarAutoBudgetOrderWithHistory": [],
    }

    def __init__(self):
        super().__init__()
        one_day = 24 * 3600
        now = int(time.time())
        now -= now % 3600
        self.test_cases = [
            {
                "Messages": [
                    {
                        "Resources": {
                            "AutoBudget": {
                                "Enabled": True,
                                "AutoBudgetSoftRestartTime": now - one_day,
                                "AutoBudgetDayLimitMoneyCur": 1100,
                            },
                            "DirectBannersLogFields": {
                                "EngineID": 7,
                            },
                            "UpdateInfo": True,
                        },
                        "Timestamp": 123,
                    },
                    {
                        "Resources": {
                            "AutoBudget": {
                                "Enabled": True,
                                "AutoBudgetSoftRestartTime": now - one_day + 10,
                                "AutoBudgetDayLimitMoneyCur": 1000,
                            },
                            "DirectBannersLogFields": {
                                "EngineID": 7,
                            },
                            "UpdateInfo": True,
                        },
                        "Timestamp": 124,
                    },
                    {
                        "Resources": {
                            "AutoBudget": {
                                "Enabled": True,
                                "AutoBudgetSoftRestartTime": now - one_day + 20,
                                "AutoBudgetDayLimitMoneyCur": 1050,
                            },
                            "DirectBannersLogFields": {
                                "EngineID": 7,
                            },
                            "UpdateInfo": True,
                        },
                        "Timestamp": 125,
                    },
                ],
                "Expected": {"MaxLimitDayMoneyCur": 1100000000},
            },
            {
                "Messages": [
                    {
                        "Resources": {
                            "AutoBudget": {
                                "Enabled": True,
                                "AutoBudgetSoftRestartTime": now - 2 * one_day,
                                "AutoBudgetDayLimitMoneyCur": 1100,
                            },
                            "DirectBannersLogFields": {
                                "EngineID": 7,
                            },
                            "UpdateInfo": True,
                        },
                        "Timestamp": 223,
                    },
                    {
                        "Resources": {
                            "AutoBudget": {
                                "Enabled": True,
                                "AutoBudgetSoftRestartTime": now - one_day,
                                "AutoBudgetDayLimitMoneyCur": 1000,
                            },
                            "DirectBannersLogFields": {
                                "EngineID": 7,
                            },
                            "UpdateInfo": True,
                        },
                        "Timestamp": 225,
                    },
                ],
                "Expected": {"MaxLimitDayMoneyCur": 1100000000},
            },
            {
                "Messages": [
                    {
                        "Resources": {
                            "AutoBudget": {
                                "Enabled": True,
                                "AutoBudgetSoftRestartTime": now - 2 * one_day,
                                "AutoBudgetMaxDayLimitMoneyCur": 1100,
                                "AutoBudgetDayLimitMoneyCur": 1000,
                            },
                            "DirectBannersLogFields": {
                                "EngineID": 7,
                            },
                            "UpdateInfo": True,
                        },
                        "Timestamp": 323,
                    },
                    {
                        "Resources": {
                            "AutoBudget": {
                                "Enabled": True,
                                "AutoBudgetSoftRestartTime": now - one_day,
                                "AutoBudgetDayLimitMoneyCur": 1000,
                            },
                            "DirectBannersLogFields": {
                                "EngineID": 7,
                            },
                            "UpdateInfo": True,
                        },
                        "Timestamp": 325,
                    },
                ],
                "Expected": {"MaxLimitDayMoneyCur": 1000000000},
            },
            {
                "Messages": [
                    {
                        "Resources": {
                            "AutoBudget": {
                                "Enabled": True,
                                "AutoBudgetSoftRestartTime": now + 2 * one_day,
                                "AutoBudgetMaxDayLimitMoneyCur": 1100,
                                "AutoBudgetDayLimitMoneyCur": 1000,
                            },
                            "DirectBannersLogFields": {
                                "EngineID": 7,
                            },
                            "UpdateInfo": True,
                        },
                        "Timestamp": 423,
                    },
                ],
                "Expected": {"MaxLimitDayMoneyCur": 1000000000},
            },
        ]
        self.iter_case = iter(self.test_cases)

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = next(self.iter_case)
        self.expected[profile_id] = test_case

        for message in test_case["Messages"]:
            order_body = TOrderResources()
            ParseDict(message["Resources"], order_body)
            order_body.Version = message["Timestamp"]

            yield make_event(profile_id, message["Timestamp"], order_body)

    def check_profiles(self, profiles):
        pass

    def check_caesar_auto_budget_order_table(self, rows):
        assert len(rows) == len(self.test_cases)
        for row in rows:
            test_case = self.expected[row[b"OrderID"]]
            assert row[b"MaxLimitDayMoneyCur"] == test_case["Expected"]["MaxLimitDayMoneyCur"], test_case


class TestCaseCaesarOrderInfo(_BaseTestCaseOrders):

    extra_profiles = {
        "CaesarOrderInfo": [],
    }

    def __init__(self):
        super().__init__()
        self.test_cases = [
            {
                "Resources": {
                    "DirectBannersLogFields": {
                        "EngineID": 7,
                        "NDSHistory": [{"DateFrom": 1041368400, "NDS": 0}],
                    },
                    "UpdateInfo": True,
                },
                "Expected": {"TaxID": 1},
            },
            {
                "Resources": {
                    "DirectBannersLogFields": {
                        "EngineID": 7,
                        "NDSHistory": [
                            {"DateFrom": 1041368400, "NDS": 200000},
                            {"DateFrom": 1072904400, "NDS": 180000},
                        ],
                    },
                    "UpdateInfo": True,
                },
                "Expected": {"TaxID": 2},
            },
        ]
        self.iter_case = iter(self.test_cases)

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = next(self.iter_case)
        self.expected[profile_id] = test_case

        order_body = TOrderResources()
        ParseDict(test_case["Resources"], order_body)
        order_body.Version = time

        yield make_event(profile_id, time - random.randint(0, 60), order_body)

    def check_profiles(self, profiles):
        pass

    def check_caesar_order_info_table(self, rows):
        assert len(rows) == len(self.test_cases)
        for row in rows:
            test_case = self.expected[row[b"OrderID"]]
            expected_engineid = test_case["Resources"]["DirectBannersLogFields"]["EngineID"]
            expected_taxid = test_case["Expected"]["TaxID"]
            assert row[b"EngineID"] == expected_engineid, test_case
            assert row[b"TaxID"] == expected_taxid, test_case


class TestCaseDistributionTagID(_BaseTestCaseOrders):
    def __init__(self):
        super().__init__()
        self.test_cases = [
            {
                "Resources": {
                    "DirectBannersLogFields": {
                        "EngineID": 67,
                        "ClientID": 3228,
                        "DistributionTag": "semyon-made-a-test",
                    },
                    "UpdateInfo": True,
                },
                "Expected": {"DistributionTagID": 3228},
            },
            {
                "Resources": {
                    "DirectBannersLogFields": {
                        "EngineID": 67,
                        "ClientID": 228,
                        "DistributionTag": "transport-app",
                    },
                    "UpdateInfo": True,
                },
                "Expected": {"DistributionTagID": 284},
            },
        ]
        self.iter_case = iter(self.test_cases)

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = next(self.iter_case)
        self.expected[profile_id] = test_case

        order_body = TOrderResources()
        ParseDict(test_case["Resources"], order_body)
        order_body.Version = time

        yield make_event(profile_id, time - random.randint(0, 60), order_body)

    def check_profiles(self, profiles):
        for profile in profiles:
            test_case = self.expected[profile.OrderID]
            expected_distribution_tag_id = test_case["Expected"]["DistributionTagID"]
            assert profile.Resources.DistributionTagID == expected_distribution_tag_id, test_case


class TestCaseOrdersServiceLog(_BaseTestCaseOrders):
    def make_events(self, time, shard, profile_id):
        order_event, _ = make_order_event(profile_id, time)
        self.expected[profile_id] = order_event
        yield order_event
        yield make_service_event(profile_id, time - random.randint(0, 60))

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)


class TestCaseOrdersUpdateResourceByFullState(_BaseTestCaseOrders):
    def make_events(self, time, shard, profile_id):
        order_event, _ = make_order_event(profile_id, time)
        self.expected[profile_id] = order_event
        yield self.expected[profile_id]

        fs = make_full_state(TFullState.EFullStateExist.FS_YES, SIMPLE_ORDER_MONEY)
        yield make_service_event(profile_id, time - random.randint(0, 60), {"OrderMoney": fs})

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            assert MessageToJson(profile.Resources.OrderMoney) == MessageToJson(SIMPLE_ORDER_MONEY)


class TestCaseOrdersUpdateAutobudgetControlsByFullState(_BaseTestCaseOrders):
    def make_events(self, time, shard, profile_id):
        order_event, _ = make_order_event(profile_id, time)
        self.expected[profile_id] = order_event
        yield self.expected[profile_id]

        fs = make_full_state(TFullState.EFullStateExist.FS_YES, SIMPLE_AUTOBUDGET_CONTROLS)
        yield make_service_event(profile_id, time, {"AutobudgetControls": fs})

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            assert MessageToJson(profile.AutobudgetResources.YqlBidderAutobudgetControls) == MessageToJson(
                SIMPLE_AUTOBUDGET_CONTROLS
            )


class TestCaseOrdersCreateProfileByFullState(_BaseTestCaseOrders):
    def __init__(self):
        super().__init__()
        self.full_states = {}

    def make_events(self, time, shard, profile_id):
        if profile_id % 2 == 0:
            order_event, _ = make_order_event(profile_id, time)
            self.expected[profile_id] = order_event
            yield self.expected[profile_id]
        else:
            fs = make_full_state(TFullState.EFullStateExist.FS_YES, SIMPLE_ORDER_MONEY)
            self.full_states[profile_id] = make_service_event(
                profile_id, time - random.randint(0, 60), {"OrderMoney": fs}
            )
            yield self.full_states[profile_id]

    def check_profiles(self, profiles):
        assert len(self.expected) + len(self.full_states) == len(profiles)
        for profile in profiles:
            if profile.OrderID % 2 == 0:
                assert profile.OrderID in self.expected
            else:
                assert profile.OrderID in self.full_states


class TestCaseOrdersDoNotCreateProfileByFullState(_BaseTestCaseOrders):
    def make_events(self, time, shard, profile_id):
        if profile_id % 2 == 0:
            fs = make_full_state(TFullState.EFullStateExist.FS_NO)
        else:
            fs = make_full_state(TFullState.EFullStateExist.FS_UNKNOWN)

        yield make_service_event(profile_id, time - random.randint(0, 60), {"OrderMoney": fs})

    def check_profiles(self, profiles):
        # TODO(BIGB-1842): fix buggy behavior and uncomment
        # assert len(profiles) == 0
        pass


class TestCaseOrdersIsActiveFlag(_BaseTestCaseOrders):
    SHARDS_COUNT = 1

    def __init__(self):
        super().__init__()
        self.test_cases = [
            {
                "Resources": {
                    "DirectBannersLogFields": {"EngineID": 7},
                    "UpdateInfo": True,
                    "Stop": True,
                },
                "OrderMoney": {
                    "IsActiveExceptStopArchive": True
                },
                "Result": {
                    "IsActive": False,
                },
            },
            {
                "Resources": {
                    "DirectBannersLogFields": {"EngineID": 7},
                    "UpdateInfo": True,
                    "Stop": True,
                },
                "OrderMoney": {
                    "IsActiveExceptStopArchive": False
                },
                "Result": {
                    "IsActive": False,
                },
            },
            {
                "Resources": {
                    "DirectBannersLogFields": {"EngineID": 7},
                    "UpdateInfo": True,
                    "Stop": False,
                },
                "OrderMoney": {
                    "IsActiveExceptStopArchive": True
                },
                "Result": {
                    "IsActive": True,
                },
            },
            {
                "Resources": {
                    "DirectBannersLogFields": {"EngineID": 7},
                    "UpdateInfo": True,
                    "Stop": False,
                },
                "OrderMoney": {
                    "IsActiveExceptStopArchive": False
                },
                "Result": {
                    "IsActive": False,
                },
            },
        ]
        self.iter_case = iter(self.test_cases)

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = next(self.iter_case)
        self.expected[profile_id] = test_case

        order_body = TOrderResources()
        ParseDict(test_case["Resources"], order_body)
        order_body.Version = time

        yield make_event(profile_id, time - random.randint(0, 60), order_body)

        order_money = TOrderProfileProto.TResources.TOrderMoney()
        ParseDict(test_case["OrderMoney"], order_money)
        fs = make_full_state(TFullState.EFullStateExist.FS_YES, order_money)
        yield make_service_event(profile_id, time - random.randint(0, 60), {"OrderMoney": fs})

    def check_profiles(self, profiles):
        assert len(self.test_cases) == len(profiles)

        for profile in profiles:
            test_case = self.expected[profile.OrderID]
            expected_result = test_case["Result"]
            assert profile.Flags.IsActive == expected_result["IsActive"], test_case
            if expected_result["IsActive"]:
                assert profile.Flags.OrderFirstActiveTime > 0, test_case
                assert profile.Flags.LastActiveTime > 0, test_case
            else:
                assert profile.Flags.OrderFirstActiveTime == 0, test_case
                assert profile.Flags.LastActiveTime == 0, test_case


class TestCaseOrdersComputeAutobudgetControls(_BaseTestCaseCofiguredAutobudgetOrder):
    SHARDS_COUNT = 1

    def __init__(self):
        default_settings = TAdsBidderSettings()
        preprod_settings = TAdsBidderSettings()

        super().__init__(default_settings, preprod_settings)

        self.time = int(time.time())
        self.test_cases = [
            {  #
                "Resources": {
                    "AutoBudget": {
                        "AutoBudgetROILevel": 2,
                    },
                    "UpdateInfo": True,
                },
                "AutobudgetControls": {
                    "DataTimestamp": 1,
                    "ExperimentControls": [
                        {
                            "ExperimentID": 0,
                            "SearchControls": {
                                "Model": {
                                    "RoiBidParam": 3e-6,
                                    "CpaBidMultiplier": 1.0,
                                },
                            },
                            "FlatControls": {
                                "Model": {
                                    "RoiBidCurParam": 3e-6,
                                    "RoiBidParam": 3e-6,
                                    "CpaBidMultiplier": 1.0,
                                },
                            },
                        },
                    ],
                },
                "Result": {
                    "RoiBid": 6e-6,
                },
            },
        ]

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = self.test_cases[profile_id]
        self.expected[profile_id] = test_case

        order_body = TOrderResources()
        ParseDict(test_case["Resources"], order_body)
        order_body.Version = time
        yield make_event(profile_id, time - random.randint(0, 60), order_body)

        autobudget_controls = TOrderProfileProto.TAutobudgetResources.TAutobudgetControls()
        ParseDict(test_case["AutobudgetControls"], autobudget_controls)
        fs = make_full_state(TFullState.EFullStateExist.FS_YES, autobudget_controls)
        yield make_service_event(profile_id, time - random.randint(0, 60), {"AutobudgetControls": fs})

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            test_case = self.expected[profile.OrderID]
            expected_result = test_case["Result"]
            assert profile.AutobudgetResources.ComputedAutobudgetControls.DataTimestamp >= self.time
            assert (
                abs(
                    profile.AutobudgetResources.ComputedAutobudgetControls.ExperimentControls[
                        profile.OrderID
                    ].SearchControls.RawBids.RoiBid
                    - expected_result["RoiBid"]
                )
                < 1e-6
            )


class TestCaseAllowedSpendings(_BaseTestCaseCofiguredAutobudgetOrder):
    SHARDS_COUNT = 1
    NOW = datetime.datetime.now(pytz.timezone("Europe/Moscow"))

    def __init__(self):
        default_settings = TAdsBidderSettings()
        default_settings.ComputeControls = True
        default_settings.Controls.AllowedSpendingsSettings.AllowedToExpectedRatioSettings.SpendingsRatioDefault = 3
        default_settings.Controls.AllowedSpendingsSettings.AllowedToExpectedRatioSettings.SpendingsRatioMobileApp = 10
        default_settings.Controls.AllowedSpendingsSettings.ExpectedIncomeValueSettings.SumActionMult = 1.
        default_settings.Controls.AllowedSpendingsSettings.ExpectedIncomeValueSettings.SumActionPrior = 0.
        default_settings.Controls.AllowedSpendingsSettings.ExpectedIncomeValueSettings.SumABCoeffStep = 1.

        preprod_settings = TAdsBidderSettings()

        super().__init__(default_settings, preprod_settings)

        self.time = int(time.time())

        future_time = self.time + random.randint(100000, 900000)

        min_allowed_spending_time = future_time - future_time % 3600
        self.test_cases = [
            {  #
                "Resources": {
                    "AutoBudget": {
                        "AutoBudgetAvgCPA": 2,
                        "AutoBudgetAvgCPACur": 1,
                        "AutoBudgetPaidActions": True,
                    },
                    "DirectBannersLogFields": {
                        "GroupOrderID": 10,
                    },
                    "UpdateInfo": True,
                },
                "AutobudgetControls": {
                    "DataTimestamp": 10,
                    "ExperimentControls": [
                        {
                            "ExperimentID": 0,
                            "SearchControls": {
                                "Model": {
                                    "TrafficWeightsWithTimeTargeting": [10**5] * 168,
                                    "TrafficWeights": [2 * 10**5] * 168,
                                    "WeekLimitBidCurParam": 1e-5,
                                    "CpaBidMultiplier": 1,
                                    "CpaBidCurParam": 1,
                                    "CpaBidParam": 1,
                                    "SumWorkingHistoryABCoeff": 1,
                                    "SumAction": 1,
                                },
                            },
                            "FlatControls": {
                                "Model": {
                                    "TrafficWeightsWithTimeTargeting": [10**5] * 168,
                                    "TrafficWeights": [2 * 10**5] * 168,
                                    "WeekLimitBidCurParam": 1e-5,
                                    "CpaBidMultiplier": 1,
                                    "CpaBidCurParam": 1,
                                    "CpaBidParam": 1,
                                    "SumAction": 1,
                                    "SumWorkingHistoryABCoeff": 1,
                                },
                            },
                            "AllowedSpendings": [
                                {
                                    "Hour": min_allowed_spending_time + 3600,
                                },
                                {
                                    "Hour": min_allowed_spending_time,
                                },
                            ],
                        },
                    ],
                },
                "Result": {
                    "ExpectedSpendingsStartHour": min_allowed_spending_time,
                    "HourError": 0,
                    "ExpectedIncomeCur": 400,
                    "ExpectedSpendingCur": 7200,
                    "AllowedSpendingCur": 400,
                },
            },
            {  #
                "Resources": {
                    "AutoBudget": {
                        "AutoBudgetAvgCPA": 2,
                        "AutoBudgetAvgCPACur": 1,
                        "AutoBudgetGoalID": 13,
                        "AutoBudgetPaidActions": True,
                    },
                    "DirectBannersLogFields": {
                        "MeaningfulGoals": [
                            {
                                "GoalID": 11,
                            },
                            {
                                "GoalID": 1,
                            },
                        ],
                        "GroupOrderID": 10,
                    },
                    "UpdateInfo": True,
                },
                "AutobudgetControls": {
                    "DataTimestamp": 10,
                    "ExperimentControls": [
                        {
                            "ExperimentID": 0,
                            "SearchControls": {
                                "Model": {
                                    "TrafficWeights": [2 * 10**5] * 168,
                                    "TrafficWeightsWithTimeTargeting": [2 * 10**5] * 168,
                                    "SumAction": 100,
                                    "SumWorkingHistoryABCoeff": 1,
                                    "WeekLimitBidCurParam": 1e-5,
                                    "CpaBidMultiplier": 1,
                                    "CpaBidCurParam": 1,
                                },
                                "RawBids": {
                                    "CpaBidCur": 1000000,
                                },
                            },
                            "FlatControls": {
                                "Model": {
                                    "TrafficWeights": [2 * 10**5] * 168,
                                    "TrafficWeightsWithTimeTargeting": [3 * 10**5] * 168,
                                    "WeekLimitBidCurParam": 1e-5,
                                    "CpaBidMultiplier": 1,
                                    "CpaBidCurParam": 1,
                                    "SumAction": 200,
                                    "SumWorkingHistoryABCoeff": 1,
                                },
                                "RawBids": {
                                    "CpaBidCur": 1000000,
                                },
                            },
                        },
                    ],
                    "AutobudgetSettingFromBidder": {
                        "GoalID": 13,
                        "MeaningfulGoals": [
                            {
                                "GoalID": 12,
                            },
                            {
                                "GoalID": 1,
                            },
                        ],
                        "TargetCPACur": 2000000000,
                    },
                },
                "Result": {
                    "ExpectedSpendingsStartHour": self.time - self.time % 3600,
                    "HourError": 3600,
                    "ExpectedIncomeCur": 120000000,
                    "ExpectedSpendingCur": 18000,
                    "AllowedSpendingCur": 54000,
                },
            },
            {  #
                "Resources": {
                    "AutoBudget": {
                        "AutoBudgetAvgCPA": 2,
                        "AutoBudgetAvgCPACur": 1,
                    },
                    "DirectBannersLogFields": {
                        "GroupOrderID": 10,
                        "ContentType": "mobile_content"
                    },
                    "UpdateInfo": True,
                },
                "AutobudgetControls": {
                    "DataTimestamp": 10,
                    "ExperimentControls": [
                        {
                            "ExperimentID": 0,
                            "SearchControls": {
                                "Model": {
                                    "TrafficWeightsWithTimeTargeting": [0] * 168,
                                    "TrafficWeights": [10**5] * 168,
                                    "WeekLimitBidCurParam": 1e-5,
                                    "CpaBidMultiplier": 1,
                                    "CpaBidCurParam": 1,
                                },
                            },
                            "FlatControls": {
                                "Model": {
                                    "TrafficWeightsWithTimeTargeting": [0] * 168,
                                    "TrafficWeights": [10**5] * 168,
                                    "WeekLimitBidCurParam": 1e-5,
                                    "CpaBidMultiplier": 1,
                                    "CpaBidCurParam": 1,
                                },
                            },
                            "AllowedSpendings": [
                                {
                                    "Hour": min_allowed_spending_time + 3600,
                                },
                                {
                                    "Hour": min_allowed_spending_time,
                                },
                            ],
                        },
                    ],
                },
                "Result": {
                    "ExpectedSpendingsStartHour": min_allowed_spending_time,
                    "HourError": 0,
                    "ExpectedIncomeCur": 0,
                    "ExpectedSpendingCur": 7200,
                    "AllowedSpendingCur": 72000,
                },
            },
        ]

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = self.test_cases[profile_id]
        self.expected[profile_id] = test_case

        order_body = TOrderResources()
        ParseDict(test_case["Resources"], order_body)
        order_body.Version = time
        yield make_event(profile_id, time - random.randint(0, 60), order_body)

        autobudget_controls = TOrderProfileProto.TAutobudgetResources.TAutobudgetControls()
        ParseDict(test_case["AutobudgetControls"], autobudget_controls)
        fs = make_full_state(TFullState.EFullStateExist.FS_YES, autobudget_controls)
        yield make_service_event(profile_id, time - random.randint(0, 60), {"AutobudgetControls": fs})
        yield make_service_event(profile_id, time - random.randint(0, 60), {"ComputedAutobudgetControls": fs})

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            test_case = self.expected[profile.OrderID]
            expected_result = test_case["Result"]

            assert 2 == len(profile.AutobudgetResources.ComputedAutobudgetControls.ExperimentControls)
            allowed_spendings = profile.AutobudgetResources.ComputedAutobudgetControls.ExperimentControls[0].AllowedSpendings

            assert 25 == len(allowed_spendings)

            for idx, allowed_spending in enumerate(allowed_spendings):
                assert (
                    abs(expected_result["ExpectedSpendingsStartHour"] + 3600 * idx - allowed_spending.Hour)
                    <= expected_result["HourError"]
                )

                assert allowed_spending.ExpectedIncomeCur == pytest.approx(expected_result["ExpectedIncomeCur"], 1e-6)
                assert allowed_spending.ExpectedSpendingCur == pytest.approx(expected_result["ExpectedSpendingCur"], 1e-6)
                assert allowed_spending.AllowedSpendingCur == pytest.approx(expected_result["AllowedSpendingCur"], 1e-6)


class TestCaseOrdersAutoBudget(_BaseTestCaseOrders):
    SHARDS_COUNT = 1

    def __init__(self):
        super().__init__()
        self.test_cases = [
            {  # common
                "Resources": {
                    "AutoBudget": {
                        "StrategyParams": {
                            "Name": "period_fix_bid",
                        },
                        "AutoBudgetPeriodBudgetFinish": 1630184399,  # Aug 28 2021 23:59:59 GMT+0300
                        "AutoBudgetPeriodBudgetLimit": 1200,
                        "AutoBudgetPeriodBudgetLimitCur": 36000,
                    },
                    "DirectBannersLogFields": {
                        "StartTime": 1629838800,  # Aug 25 2021 00:00:00 GMT+0300
                    },
                    "UpdateInfo": True,
                },
                "Result": {
                    "AutoBudgetDayLimitMoney": 300,
                    "AutoBudgetDayLimitMoneyCur": 9000,
                },
            },
            {  # intraday
                "Resources": {
                    "AutoBudget": {
                        "StrategyParams": {
                            "Name": "period_fix_bid",
                        },
                        "AutoBudgetPeriodBudgetFinish": 1629925199,  # Aug 25 2021 23:59:59 GMT+0300
                        "AutoBudgetPeriodBudgetLimit": 1200,
                        "AutoBudgetPeriodBudgetLimitCur": 36000,
                    },
                    "DirectBannersLogFields": {
                        "StartTime": 1629838800,  # Aug 25 2021 00:00:00 GMT+0300
                    },
                    "UpdateInfo": True,
                },
                "Result": {
                    "AutoBudgetDayLimitMoney": 1200,
                    "AutoBudgetDayLimitMoneyCur": 36000,
                },
            },
            {  # not period_fix_bid strategy, but has DayLimitMoney
                "Resources": {
                    "AutoBudget": {
                        "StrategyParams": {
                            "Name": "default",
                        },
                        "AutoBudgetDayLimitMoney": 1200,
                        "AutoBudgetDayLimitMoneyCur": 36000,
                    },
                    "UpdateInfo": True,
                },
                "Result": {
                    "AutoBudgetDayLimitMoney": 1200,
                    "AutoBudgetDayLimitMoneyCur": 36000,
                },
            },
        ]
        self.iter_case = iter(self.test_cases)

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = next(self.iter_case)
        self.expected[profile_id] = test_case

        order_body = TOrderResources()
        ParseDict(test_case["Resources"], order_body)
        order_body.Version = time
        yield make_event(profile_id, time - random.randint(0, 60), order_body)

    def check_profiles(self, profiles):
        assert len(self.test_cases) == len(profiles)

        for profile in profiles:
            test_case = self.expected[profile.OrderID]
            expected_result = test_case["Result"]
            assert (
                expected_result["AutoBudgetDayLimitMoney"] == profile.Resources.AutoBudget.AutoBudgetDayLimitMoney
            ), test_case
            assert (
                expected_result["AutoBudgetDayLimitMoneyCur"] == profile.Resources.AutoBudget.AutoBudgetDayLimitMoneyCur
            ), test_case


class TestCaseBidNoiseRange(_BaseTestCaseCofiguredAutobudgetOrder):
    SHARDS_COUNT = 1

    def __init__(self):
        default_settings = TAdsBidderSettings()
        default_settings.ComputeControls = True
        default_settings.Controls.BidNoiseRangeSettings.BidNoiseRange = 50000
        preprod_settings = TAdsBidderSettings()
        experiment_1 = TAutobudgetSettings()
        experiment_1.AdsBidderSettings.ComputeControls = True
        experiment_1.AdsBidderSettings.Controls.BidNoiseRangeSettings.BidNoiseRange = 50000
        experiment_1.AutobudgetExperimentID = 1
        experiment_1.AdsBidderSettings.StartTime = 10

        super().__init__(default_settings, preprod_settings, [experiment_1])

        self.test_cases = [
            {  #
                "Resources": {
                    "AutoBudget": {
                        "AutoBudgetGoalID": 1,
                    },
                    "DirectBannersLogFields": {
                        "ContentType": "mobile_content",
                        "SourceInterface": "uac",
                    },
                },
                "AutobudgetControls": {
                    "DataTimestamp": 10,
                    "ExperimentControls": [
                        {
                            "ExperimentID": 1,
                            "SearchControls": {},
                            "FlatControls": {},
                        },
                    ],
                },
                "Result": {
                    "BidNoiseRange": 50000,
                },
            },
        ]

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = self.test_cases[profile_id]
        self.expected[profile_id] = test_case

        order_body = TOrderResources()
        ParseDict(test_case["Resources"], order_body)
        order_body.Version = time
        yield make_event(profile_id, time - random.randint(0, 60), order_body)

        autobudget_controls = TOrderProfileProto.TAutobudgetResources.TAutobudgetControls()
        ParseDict(test_case["AutobudgetControls"], autobudget_controls)
        fs = make_full_state(TFullState.EFullStateExist.FS_YES, autobudget_controls)
        yield make_service_event(profile_id, time - random.randint(0, 60), {"AutobudgetControls": fs})

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            test_case = self.expected[profile.OrderID]
            expected_result = test_case["Result"]
            control = profile.AutobudgetResources.ComputedAutobudgetControls.ExperimentControls

            controls = list(filter(lambda controls: controls.ExperimentID == 1, control))[0]

            assert controls.FlatControls.BidNoiseRange == expected_result["BidNoiseRange"]
            assert controls.SearchControls.BidNoiseRange == expected_result["BidNoiseRange"]


class TestCaseActiveOrdersAddition(_BaseTestCaseOrders):
    ROUND_TIME = 300

    def __init__(self):
        super().__init__()

        self.order_id = 1337
        self.ts_5min_current = None

    def generate_profile_ids(self, _):
        return {0: [self.order_id]}

    @staticmethod
    def _gen_experiments(order_id, experiment_ids, time):
        autobudget_controls = TOrderProfileProto.TAutobudgetResources.TAutobudgetControls()
        ParseDict(
            {"DataTimestamp": time, "ExperimentControls": [{"ExperimentID": e} for e in experiment_ids]},
            autobudget_controls,
        )
        fs = make_full_state(TFullState.EFullStateExist.FS_YES, autobudget_controls)
        yield make_service_event(order_id, time, {"ComputedAutobudgetControls": fs})

    def make_events(self, time, shard, profile_id):
        ts_current = time
        ts_5min_current = ts_current - ts_current % self.ROUND_TIME
        ts_5min_minus_inf = ts_5min_current - self.ROUND_TIME * 1000
        ts_5min_middle = ts_5min_current - self.ROUND_TIME * 30
        ts_5min_plus_inf = ts_5min_current + self.ROUND_TIME * 1000

        order_counters = TActiveOrderCounters(
            Counters=[
                TActiveOrderCounters.TActiveOrderCounter(
                    RoundedTimestamp=ts_5min_minus_inf, IsRsya=True, AutobudgetExperimentID=0, Counter=1
                ),
                TActiveOrderCounters.TActiveOrderCounter(
                    RoundedTimestamp=ts_5min_middle, IsRsya=True, AutobudgetExperimentID=0, Counter=2
                ),
                TActiveOrderCounters.TActiveOrderCounter(
                    RoundedTimestamp=ts_5min_current, IsRsya=True, AutobudgetExperimentID=0, Counter=4
                ),
                TActiveOrderCounters.TActiveOrderCounter(
                    RoundedTimestamp=ts_5min_plus_inf, IsRsya=True, AutobudgetExperimentID=0, Counter=8
                ),
            ]
        )

        yield make_event(profile_id, ts_current, order_counters)

        self.ts_5min_current = ts_5min_current

    def check_profiles(self, profiles):
        assert len(profiles) == 1
        assert profiles[0].OrderID == self.order_id
        experiments = profiles[0].AutobudgetResources.ComputedAutobudgetControls.ExperimentControls
        assert len(experiments) == 1
        time_counter = experiments[0].FlatControls.ActiveTimeCounter

        expected_first_timestamp = self.ts_5min_current - self.ROUND_TIME * 71
        assert abs(time_counter.FirstTimestamp - expected_first_timestamp) <= 900
        assert time_counter.StartOffset == 0

        assert sum([v != 0 for v in time_counter.TimestampCounter]) == 2
        for value in time_counter.TimestampCounter:
            assert value in (0, 2, 4)


class TestCaseActiveOrdersAdditionMultiple(TestCaseActiveOrdersAddition):
    def __init__(self):
        super().__init__()

        self.test_cases = [
            dict(IsRsya=True, AutobudgetExperimentID=0, Counter=1337),
            dict(IsRsya=False, AutobudgetExperimentID=403, Counter=2),
            dict(IsRsya=True, AutobudgetExperimentID=322, Counter=3),
            dict(IsRsya=False, AutobudgetExperimentID=404, Counter=4),
        ]
        self.expected = {}

    def generate_profile_ids(self, _):
        return {0: [100500 + i for i in range(len(self.test_cases))]}

    def make_events(self, time, shard, profile_id):
        test_case = self.test_cases[profile_id - 100500]
        self.expected[profile_id] = test_case

        ts_current = time if self.ts_5min_current is None else self.ts_5min_current
        ts_5min_current = ts_current - ts_current % self.ROUND_TIME
        ts_5min_middle = ts_5min_current - self.ROUND_TIME * 30

        self.ts_5min_current = ts_5min_current
        order_counters = TActiveOrderCounters(
            Counters=[
                TActiveOrderCounters.TActiveOrderCounter(
                    RoundedTimestamp=ts_5min_middle,
                    IsRsya=test_case["IsRsya"],
                    AutobudgetExperimentID=test_case["AutobudgetExperimentID"],
                    Counter=test_case["Counter"],
                )
            ]
        )
        yield make_event(profile_id, ts_current, order_counters)

    def check_profiles(self, profiles):
        assert len(profiles) == len(self.expected)

        for profile in profiles:
            test_case = self.expected[profile.OrderID]

            if test_case["AutobudgetExperimentID"] != 0:
                assert len(profile.AutobudgetResources.ComputedAutobudgetControls.ExperimentControls) == 0
                continue

            assert len(profile.AutobudgetResources.ComputedAutobudgetControls.ExperimentControls) == 1
            experiment = profile.AutobudgetResources.ComputedAutobudgetControls.ExperimentControls[0]
            assert experiment.ExperimentID == 0

            time_counter = (
                experiment.FlatControls if test_case["IsRsya"] else experiment.SearchControls
            ).ActiveTimeCounter
            assert time_counter.StartOffset == 0

            expected_timestamp = self.ts_5min_current - self.ROUND_TIME * 71
            assert abs(time_counter.FirstTimestamp - expected_timestamp) <= 900

            for value in time_counter.TimestampCounter:
                assert value == 0 or value == test_case["Counter"]
            assert sum([v != 0 for v in time_counter.TimestampCounter]) == 1


class TestCaseOrdersUpdateCounterID(_BaseTestCaseOrders):
    SHARDS_COUNT = 1

    @staticmethod
    def make_counter(counter_id):
        counter = TGoalProfileProto.TCounter()
        counter.CounterID = counter_id
        return counter.SerializeToString()

    @property
    def extra_profiles(self):
        return {
            "Goals": [{"GoalID": 1000, "Counter": self.make_counter(123123123)}],
        }

    def __init__(self):
        super().__init__()
        self.test_cases = [
            {
                "Resources": {
                    "AutoBudget": {
                        "AutoBudgetGoalID": 1000,
                    },
                    "UpdateInfo": True,
                },
                "AutobudgetControls": {
                    "DataTimestamp": 10,
                },
                "CounterID": 123123123,
            },
            {
                "Resources": {
                    "AutoBudget": {
                        "AutoBudgetGoalID": 1001,
                    },
                    "UpdateInfo": True,
                },
                "AutobudgetControls": {
                    "DataTimestamp": 10,
                },
                "CounterID": -1,
            },
            {
                "Resources": {
                    "AutoBudget": {
                        "AutoBudgetGoalID": 13,
                    },
                    "UpdateInfo": True,
                },
                "AutobudgetControls": {
                    "DataTimestamp": 10,
                },
                "CounterID": 0,
            },
            {
                "Resources": {
                    "AutoBudget": {},
                    "UpdateInfo": True,
                },
                "AutobudgetControls": {
                    "DataTimestamp": 10,
                },
                "CounterID": -1,
            },
            {
                "Resources": {
                    "AutoBudget": {
                        "AutoBudgetGoalID": 3e9 + 510,
                    },
                    "UpdateInfo": True,
                },
                "AutobudgetControls": {
                    "DataTimestamp": 10,
                },
                "CounterID": 510,
            },
        ]
        self.iter_case = iter(self.test_cases)

    def generate_profile_ids(self, _):
        return {0: list(range(len(self.test_cases)))}

    def make_events(self, time, shard, profile_id):
        test_case = next(self.iter_case)
        self.expected[profile_id] = test_case["CounterID"]

        order_body = TOrderResources()
        ParseDict(test_case["Resources"], order_body)
        order_body.Version = time
        yield make_event(profile_id, time - random.randint(0, 60), order_body)

        autobudget_controls = TOrderProfileProto.TAutobudgetResources.TAutobudgetControls()
        ParseDict(test_case["AutobudgetControls"], autobudget_controls)
        fs = make_full_state(TFullState.EFullStateExist.FS_YES, autobudget_controls)
        yield make_service_event(profile_id, time - random.randint(0, 60), {"AutobudgetControls": fs})

    def check_profiles(self, profiles):
        assert len(self.test_cases) == len(profiles)

        for profile in profiles:
            expected_result = self.expected[profile.OrderID]
            assert expected_result == profile.AutobudgetResources.ComputedAutobudgetControls.CounterID


class TestCaseSmartDynamicParentInOrder(_BaseTestCaseOrders):
    def make_events(self, time, shard, profile_id):
        parent = TSmartDynamicParentEvent()

        parent.OrderID = profile_id
        parent.GroupBannerID = profile_id
        parent.Source = banner_source_pb2.EBannerSource.BANNER_LAND_SMART

        self.expected[profile_id] = parent

        yield make_event(profile_id, time, parent)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            assert profile.Flags.SmartParentBanner.Value
