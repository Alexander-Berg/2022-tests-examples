from datetime import datetime
from functools import partial

from unittest import mock
import pytest
import pytz

from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys import tariff_executor
from billing.apikeys.apikeys.tarifficator.events import BillDateEventUnit


class TestToCommercialTariffSwitchEventUnit:
    @pytest.fixture
    def trial_tariff(self, simple_service):
        tariff_cc = simple_service.cc + '_trial'
        tariff = {
            "service_id": simple_service.id,
            "cc": tariff_cc,
            "name": "Пробный",
            "description": "бесплатно, 14 дней, 1000 запросов в день",
            "contractless": True,
            "client_access": False,
            "next_tariff_immediately": True,
            "tarifficator_config": [
                {"unit": "TemporaryActivatorUnit", "params": {
                    "days": 14, "period_mask": "",
                    "ban_reason": 124, "unban_reason": 125
                }},
                {"unit": "StaticLimitsUnit",
                 "params": {"limit_id": simple_service.cc + "_total_daily", "limit": "1000"}},
                {"unit": "ToCommercialTariffSwitchEventUnit", "params": {}},
            ],
            "personal_account": {
                'product': '508899',
                'firm_id': 1,
                'default_paysys': 1128,
                'default_paysys_by_person_type': {'ph': 1128, 'ur': 1117},
                'default_replenishment_amount': '0'
            },
        }
        return mapper.Tariff(**tariff).save()

    @pytest.fixture
    def commercial_tariff_settings(self):

        class Settings:
            weight = '115'
            limit = '1000'
            subs_cost = '102000'
            over_cost = '102'
            rep_limit = '1 000'
            rep_subs_cost = '102 000'

        return Settings()

    @pytest.fixture
    def commercial_tariff(self, simple_service, commercial_tariff_settings):
        tariff_cc = simple_service.cc + '_commercial'
        tariff = {
            "service_id": simple_service.id,
            "cc": tariff_cc,
            "name": "до " + commercial_tariff_settings.rep_limit + " запросов в сутки со скидкой 15%",
            "description": "Доступно по цене подписки до " + commercial_tariff_settings.rep_subs_cost
                           + " запросов в сутки. Стоимость запросов свыше подписки рассчитывается за каждую неполную "
                             "1000 запросов. Подключается по оферте.",
            "contractless": True,
            "client_access": True,
            "weight": commercial_tariff_settings.weight,
            "info_for_table": {
                '_group': 'special_discount',
                'payed_daily_limit': {'_type': 'number', 'value': commercial_tariff_settings.limit},
                'year_subscribe': {'_type': 'money', 'value': commercial_tariff_settings.subs_cost, 'currency': 'RUR'},
                'overhead_per_1000_cost': {'_type': 'money', 'value': commercial_tariff_settings.over_cost, 'currency': 'RUR'}
            },
            "personal_account": {
                'product': '508899',
                'firm_id': 1,
                'default_paysys': 1128,
                'default_paysys_by_person_type': {'ph': 1128, 'ur': 1117},
                'default_replenishment_amount': commercial_tariff_settings.subs_cost
            },
            "tarifficator_config": [
                {"unit": "UnconditionalActivatorUnit", "params": {}},
                {"unit": "StaticLimitsUnit",
                 "params": {"limit_id": simple_service.cc + "_total_daily", "limit": "-1"}},
                {"unit": "PrepayPeriodicallyUnit", "params": {
                    "period_mask": "0 0 x x * *", "truncate_period_mask": "0 0 * * * *",
                    "time_zone": "Europe/Moscow",
                    "product_id": "508899", "product_value": commercial_tariff_settings.subs_cost,
                    "autocharge_personal_account": True,
                    "ban_reason": 110, "unban_reason": 111
                }},
            ]
        }
        return mapper.Tariff(**tariff).save()

    class FakeBalancePersonalAccount:

        @classmethod
        def update_tarifficator_state(cls, link, tarifficator_state, receipt_sum=0):
            personal_account = {
                'act_sum': '0',
                'consume_sum': '0',
                'currency': 'RUB',
                'date': None,
                'receipt_sum': str(receipt_sum)
            }
            tarifficator_state.state['personal_account'] = personal_account

    @pytest.fixture
    def link_with_current_trial_and_scheduled_commercial_tariff(self, project, trial_tariff, commercial_tariff):
        link = project.attach_to_service(trial_tariff.service)
        link.config.tariff = trial_tariff.cc
        link.config.scheduled_tariff = commercial_tariff.cc
        link.config.scheduled_tariff_date = datetime.now(pytz.utc)

        return link.save()

    @mock.patch.object(tariff_executor.TariffStateExec, '_apply_activator', lambda x: None)
    def test_not_enough_funds(self, mongomock, link_with_current_trial_and_scheduled_commercial_tariff,
                              trial_tariff, commercial_tariff_settings):
        """Недостаточно средств для перехода на коммерческий тариф."""

        with mock.patch('billing.apikeys.apikeys.mapper.contractor.BalancePersonalAccount.update_tarifficator_state',
                        new=partial(self.FakeBalancePersonalAccount.update_tarifficator_state,
                                    receipt_sum=int(commercial_tariff_settings.subs_cost) - 1)):
            mapper.TarifficatorTask(link=link_with_current_trial_and_scheduled_commercial_tariff)._do_task()
            assert link_with_current_trial_and_scheduled_commercial_tariff.config.tariff == trial_tariff.cc

    @mock.patch.object(tariff_executor.TariffStateExec, '_apply_activator', lambda x: None)
    def test_enough_funds(self, mongomock, link_with_current_trial_and_scheduled_commercial_tariff,
                              commercial_tariff, commercial_tariff_settings):
        """Достаточно средств для перехода на коммерческий тариф."""

        with mock.patch('billing.apikeys.apikeys.mapper.contractor.BalancePersonalAccount.update_tarifficator_state',
                        new=partial(self.FakeBalancePersonalAccount.update_tarifficator_state,
                                    receipt_sum=int(commercial_tariff_settings.subs_cost))):
            with mock.patch.object(mapper.User, 'get_client', lambda x: {'NAME': 'Fake'}):
                mapper.TarifficatorTask(link=link_with_current_trial_and_scheduled_commercial_tariff)._do_task()
                assert link_with_current_trial_and_scheduled_commercial_tariff.config.tariff == commercial_tariff.cc


class TestBillDateEventUnit:

    def test_bill_date_event_triggered(self):
        unit = BillDateEventUnit(days_before_next_consume="30", products_filter=["508208"], tariffication=None)
        state = {
            "activated_date": datetime(2019, 4, 29, 16, 17, tzinfo=pytz.utc),
            "is_active": True,
            "last_run": datetime(2020, 4, 12, 23, 15, tzinfo=pytz.utc),
            "products": {
                "508208": {
                    "next_consume_value": "850000",
                    "consumed": "850000",
                    "needle_credited": "850000",
                    "credited": "850000",
                    "next_consume_date": datetime(2020, 4, 28, 21, 0, tzinfo=pytz.utc),
                    "credited_deficit": "850000"
                },
                "508207": {
                    "consumed": "0",
                    "credited": "0"
                }
            },
            "ban_units": {},
            "events": {"BillDateEventUnit": []}
        }
        unit({}, state)
        assert state['events'] == {
            unit.id: [
                {'type': unit.type, 'date': datetime(2020, 3, 29, 21, 0, tzinfo=pytz.utc), 'info': {'508208': '850000'}}
            ]
        }

    def test_bill_date_event_two_products(self):
        unit1 = BillDateEventUnit(days_before_next_consume="30", products_filter=["xxx"], tariffication=None, scope="one")
        unit2 = BillDateEventUnit(days_before_next_consume="30", products_filter=["yyy"], tariffication=None, scope="two")
        state = {
            "activated_date": datetime(2019, 4, 29, 16, 17, tzinfo=pytz.utc),
            "is_active": True,
            "last_run": datetime(2020, 4, 12, 23, 15, tzinfo=pytz.utc),
            "products": {
                "xxx": {
                    "consumed": "500",
                    "credited": "500",
                    "next_consume_value": "500",
                    "needle_credited": "500",
                    "next_consume_date": datetime(2020, 4, 28, 21, 0, tzinfo=pytz.utc),
                    "credited_deficit": "500"
                },
                "yyy": {
                    "consumed": "800",
                    "credited": "800",
                    "next_consume_value": "800",
                    "needle_credited": "800",
                    "next_consume_date": datetime(2020, 4, 28, 21, 0, tzinfo=pytz.utc),
                    "credited_deficit": "800"
                },
                "zzz": {
                    "consumed": "0",
                    "credited": "0"
                }
            },
            "ban_units": {},
            "events": {}
        }
        unit1({}, state)
        unit2({}, state)
        assert state['events'] == {
            "BillDateEventUnit_one": [
                {"type": unit1.type, "date": datetime(2020, 3, 29, 21, 0, tzinfo=pytz.utc), "info": {"xxx": "500"}},
            ],
            "BillDateEventUnit_two": [
                {"type": unit2.type, "date": datetime(2020, 3, 29, 21, 0, tzinfo=pytz.utc), "info": {"yyy": "800"}},
            ]
        }

    @pytest.mark.parametrize("products_filter", ["*", ["xxx", "yyy"]])
    def test_bill_date_event_wildcard(self, products_filter):
        unit = BillDateEventUnit(days_before_next_consume="30", products_filter=products_filter, tariffication=None)
        state = {
            "activated_date": datetime(2019, 4, 29, 16, 17, tzinfo=pytz.utc),
            "is_active": True,
            "last_run": datetime(2020, 4, 12, 23, 15, tzinfo=pytz.utc),
            "products": {
                "xxx": {
                    "consumed": "500",
                    "credited": "500",
                    "next_consume_value": "500",
                    "needle_credited": "500",
                    "next_consume_date": datetime(2020, 4, 28, 21, 0, tzinfo=pytz.utc),
                    "credited_deficit": "500"
                },
                "yyy": {
                    "consumed": "800",
                    "credited": "800",
                    "next_consume_value": "800",
                    "needle_credited": "800",
                    "next_consume_date": datetime(2020, 4, 28, 21, 0, tzinfo=pytz.utc),
                    "credited_deficit": "800"
                },
                "zzz": {
                    "consumed": "0",
                    "credited": "0"
                }
            },
            "ban_units": {},
            "events": {}
        }
        unit({}, state)
        assert state['events'] == {
            "BillDateEventUnit": [
                {"type": unit.type, "date": datetime(2020, 3, 29, 21, 0, tzinfo=pytz.utc),
                 "info": {"xxx": "500", "yyy": "800"}},
            ]
        }

        state["products"]["xxx"]["next_consume_date"] = datetime(2020, 4, 29, 21, 0, tzinfo=pytz.utc)
        unit({}, state)
        assert state['events'] == {
            "BillDateEventUnit": [
                {"type": unit.type, "date": datetime(2020, 3, 30, 21, 0, tzinfo=pytz.utc), "info": {"xxx": "500"}},
                {"type": unit.type, "date": datetime(2020, 3, 29, 21, 0, tzinfo=pytz.utc), "info": {"yyy": "800"}},
            ]
        }
