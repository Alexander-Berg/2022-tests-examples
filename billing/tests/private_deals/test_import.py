import itertools
import random
import unittest
from collections import namedtuple
from datetime import datetime

import sqlalchemy as sa
from dateutil.relativedelta import relativedelta as rd
from unittest.mock import patch, MagicMock
from sqlalchemy.orm.session import sessionmaker

from ..mocks import MockApplication

from agency_rewards.rewards.private_deals import (
    DATETIME_FORMAT,
    import_deals,
    import_notifications,
)
from agency_rewards.rewards.scheme import deals, deal_notifications
from .scheme import deals_dict, direct_deal_notifications, balance_deal_notifications, private_deals


class TestDeals(unittest.TestCase):
    def setUp(self):
        self.engine = sa.create_engine('sqlite:///:memory:')
        self.engine.execute("attach ':memory:' as bo")
        # create table for unit tests
        Session = sessionmaker(bind=self.engine)
        self.session = Session()
        self.create_tests_tables()
        self.app = MockApplication()

    def create_data_for_deals(self):
        dealExportId = itertools.count()
        ratios = [int(5e4), int(1e5), int(5e5)]
        insert_data = [
            {
                'name': 'test_deal_{}'.format(num),
                'dealExportId': next(dealExportId),
                'agencyRevenueRatio': random.choice(ratios),
            }
            for num in range(10)
        ]
        self.session.execute(deals_dict.insert(), insert_data)

    @patch('agency_rewards.rewards.utils.yql_crutches.Config')
    @patch('agency_rewards.rewards.private_deals.create_yt_client')
    @patch('agency_rewards.rewards.utils.yql_crutches.validate_yql')
    def test_deals(self, validate_yql, create_yt_client, ConfigMock):
        #  YQL всегда возвращает результат
        Service = namedtuple('Service', ['name', 'proxy'])
        ConfigMock.clusters = [Service('FREUD', 'freud.yt.yandex.net')]
        self.create_data_for_deals()
        validate_yql.side_effect = lambda *args, **kwargs: True

        query = """
            insert into `{t1}` (b_external_id, name, agency_rev_ratio)
            select
                dealExportId as b_external_id,
                name,
                agencyRevenueRatio / 1000000. as agency_rev_ratio
            from `{t2}`
            where type = 10
        """.format(
            t1="//home/balance/{}/yb-ar/private-deals".format(self.app.get_current_env_type()),
            t2=self.app.cfg.findtext("BKDealPath"),
        )

        def insert_and_select(*args, **kwargs):
            # чтение данных из YT и возвращение в нужном формате
            self.session.execute(query)
            return [dict(item) for item in self.session.execute(sa.select([private_deals]))]

        yt_client = MagicMock()
        yt_client.read_table.side_effect = insert_and_select
        create_yt_client.side_effect = lambda: yt_client

        import_deals(MagicMock(), self.session, self.app.get_current_env_type(), self.app)

        # проверяю что в private_deals и deals лежат одинаковые данные
        for record in self.session.execute(sa.select([private_deals])):
            assert self.session.query(
                sa.exists().where(
                    sa.and_(
                        deals.c.external_id == record.b_external_id,
                        deals.c.agency_rev_ratio == record.agency_rev_ratio,
                        deals.c.name == record.name,
                    )
                )
            ).scalar()

    def create_data_for_notifications(self):
        ct1 = datetime.now()
        ct2 = datetime.now()
        ct3 = datetime.now()

        deals_data = [
            dict(deal_id=1, creation_time=ct1, client_notification_id=10),
            dict(deal_id=1, creation_time=ct2, client_notification_id=10),
            dict(deal_id=1, creation_time=ct3, client_notification_id=10),
            dict(deal_id=2, creation_time=ct2, client_notification_id=10),
            dict(deal_id=2, creation_time=ct3, client_notification_id=10),
            dict(deal_id=1, creation_time=ct2, client_notification_id=11),
            dict(deal_id=3, creation_time=ct2, client_notification_id=10),
        ]

        self.session.execute(direct_deal_notifications.insert(), deals_data)

    @patch('agency_rewards.rewards.utils.yql_crutches.Config')
    @patch('agency_rewards.rewards.private_deals.create_yt_client')
    @patch('agency_rewards.rewards.utils.yql_crutches.validate_yql')
    def test_notifications(self, validate_yql, create_yt_client, ConfigMock):
        #  YQL всегда возвращает результат
        self.create_data_for_notifications()
        Service = namedtuple('Service', ['name', 'proxy'])
        ConfigMock.clusters = [Service('FREUD', 'freud.yt.yandex.net')]
        validate_yql.side_effect = lambda *args, **kwargs: True
        two_months_ago = (datetime.now().replace(day=1, hour=0, minute=0, second=0) - rd(months=2)).strftime(
            DATETIME_FORMAT
        )

        prepare_deal_notification_dict_query = """
                insert into `{}` (b_external_id, doc_date, doc_number)
                SELECT deal.deal_id as b_external_id,
                                MAX(deal.creation_time) as doc_date,
                                deal.client_notification_id as doc_number
                FROM `{}`as deal
                WHERE creation_time >= "{}"
                GROUP BY deal.deal_id, deal.client_notification_id
            """.format(
            balance_deal_notifications.name, direct_deal_notifications.name, two_months_ago
        )

        def insert_and_select(*args, **kwargs):
            # чтение данных из YT и возвращение в нужном формате
            self.session.execute(prepare_deal_notification_dict_query)
            result = [dict(item) for item in self.session.execute(sa.select([balance_deal_notifications]))]

            for item in result:
                item['doc_date'] = item['doc_date'].strftime(DATETIME_FORMAT)

            return result

        yt_client = MagicMock()
        yt_client.read_table.side_effect = insert_and_select
        create_yt_client.side_effect = lambda: yt_client

        import_notifications(MagicMock(), self.session, self.app.get_current_env_type(), self.app)

        expected_result = [(1, 10), (1, 11), (2, 10), (3, 10)]

        assert self.session.execute(sa.select([sa.func.count()]).select_from(deal_notifications)).scalar() == len(
            expected_result
        )

        for record in self.session.execute(sa.select([deal_notifications]).order_by('external_id', 'doc_date')):
            assert (record.external_id, int(record.doc_number)) in expected_result

    def create_tests_tables(self):
        for model in (
            private_deals,
            deals_dict,
            deals,
            balance_deal_notifications,
            direct_deal_notifications,
            deal_notifications,
        ):
            model.drop(self.session.connection(), checkfirst=True)
            model.create(self.session.connection(), checkfirst=True)
