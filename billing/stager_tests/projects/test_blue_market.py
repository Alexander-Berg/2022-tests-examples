# -*- coding: utf-8 -*-

from decimal import Decimal
from yt.wrapper.mappings import FrozenDict
from tests.stager_tests.base import StagerTest


chips_rate = Decimal(30)
commission_value = Decimal(20)
tl_distrib_value = Decimal(30)


class TestBlue_MarketProject(StagerTest):
    project_name = 'blue_market'
    purpose = 'main'

    def insert_dev_config(self):
        # сессия в тест аппе одна на все треды.
        # все падает в SIGSEGV, если тут что-то делать с self.session
        # поэтому делаю максимально тупо с новой сессией
        session = self.app.real_new_session()
        conf = session.execute("select value_json from bo.t_config where item='TLOG_BLUE_MARKET_CONFIG'").fetchall()
        if len(conf) == 0:
            session.begin()
            session.execute(u"""insert into bo.t_config(item, \"DESC\", value_json)
                                     values ('TLOG_BLUE_MARKET_CONFIG', 'Конфиг миграции синего маркета на тлог',
                                            '{"migration-date": "2020-04-05", "completion-tlog-start-date": "2020-04-05"}')""")
            session.commit()

    def setUp(self):

        super(TestBlue_MarketProject, self).setUp()
        self.insert_dev_config()
        # дата перехода 05.04.2020
        # граница фильтрации 01.05.2020 (расчитывается от даты перехода)

        # 1. transaction_time < граница фильтрации, event_time < дата перехода - отфильтровано
        # 2. transaction_time < граница фильтрации, event_time > дата перехода - съели
        # 3. transaction_time < граница фильтрации, event_time > дата перехода (по мск, но не по UTC) - съели, event_time приведено к Москве
        # 4. transaction_time > граница фильтрации (по МСК, но не по UTC), event_time < дата перехода - съели
        # 5. transaction_time > граница фильтрации, event_time > дата перехода - съели, сагрегировали с 2мя следующими строками
        # 6,7 : разные часовые пояса по event_time, для группировки с 5
        # 8: ignore_in_Balance
        self.mock_table(
            'tl_revenues_in',

            (('transaction_id',                       'event_time',                 'transaction_time', 'service_id', 'client_id', 'product',        'amount', 'currency', 'aggregation_sign',            'key', 'previous_transaction_id', 'ignore_in_balance', 'nds', ),
             # -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
             (               1, '2020-03-31T13:50:27.000000+03:00', '2020-03-31T13:50:27.000000+03:00',          612,        '12',     'fee',       '7.7'    ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               2, '2020-04-06T13:50:27.000000+03:00', '2020-04-25T13:50:27.000000+03:00',          612,        '12',     'fee',      '70.07'   ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               3, '2020-04-04T22:50:27.000000+00:00', '2020-03-04T23:50:27.000000+00:00',          612,        '12',     'fee',     '700.007'  ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               4, '2020-03-30T22:50:27.000000+03:00', '2020-04-30T23:50:27.000000+00:00',          612,        '12',     'fee',    '7000.0007' ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               5, '2020-04-07T22:50:27.000000+03:00', '2020-05-06T23:50:27.000000+03:00',          612,        '12',     'fee',   '70000.00007',       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               6, '2020-04-06T23:50:27.000000+00:00', '2020-04-09T23:50:27.000000+03:00',          612,        '12',     'fee',       '1.0001' ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               7, '2020-04-08T01:54:27.000000+07:00', '2020-02-06T23:50:27.000000+03:00',          612,        '12',     'fee',      '30.003'  ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               8, '2020-04-08T01:54:27.000000+07:00', '2020-02-06T23:50:27.000000+03:00',          612,        '12',     'fee',   '70030.00307',       'RUB',                  1,   'service_data',                    'null',                True,    1, ),

             ))

        self.results = (self.run_project()('tl_revenues_out'))

    def test_aggregation(self):
        results = self.results
        print(results)
        completions = {FrozenDict(c) for c in results['tl_revenues_out']}

        expected = {
            FrozenDict({
                'amount': '70.07',
                'client_id': 12,
                'currency': 'RUB',
                'event_time': '2020-04-06T00:00:00+03:00',
                'last_transaction_id': 2,
                'nds': 1,
                'product': 'fee',
                'service_id': 612,
            }),
            FrozenDict({
                'amount': '700.007',
                'client_id': 12,
                'currency': 'RUB',
                'event_time': '2020-04-05T00:00:00+03:00',
                'last_transaction_id': 3,
                'nds': 1,
                'product': 'fee',
                'service_id': 612,
            }),
            FrozenDict({
                'amount': '7000.0007',
                'client_id': 12,
                'currency': 'RUB',
                'event_time': '2020-03-30T00:00:00+03:00',
                'last_transaction_id': 4,
                'nds': 1,
                'product': 'fee',
                'service_id': 612,
            }),
            FrozenDict({
                'amount': '70031.00317',
                'client_id': 12,
                'currency': 'RUB',
                'event_time': '2020-04-07T00:00:00+03:00',
                'last_transaction_id': 7,
                'nds': 1,
                'product': 'fee',
                'service_id': 612,
            }),
        }
        self.assertEqual(completions, expected)

if __name__ == '__main__':
    TestBlue_MarketProject._call_test('test_aggregation')
