# -*- coding: utf-8 -*-

from balance.mapper import *

from tests.base import BalanceTest


class TestTblConfig(BalanceTest):
    def setUp(self):
        super(TestTblConfig, self).setUp()
        import sys, random
        random.seed(None)
        self.gen_random_item = lambda: 'TEST_ITEM666_%010i' % (random.randint(0, sys.maxint),)
        today = datetime.datetime.today()
        self.test_items = [(Config(item=self.gen_random_item(), value_num=666), 666),
                           (Config(item=self.gen_random_item(), value_dt=today), today),
                           (Config(item=self.gen_random_item(), value_str='XXX666'), 'XXX666'),
                           (Config(item=self.gen_random_item(), value_json='XXX666JSON_STR'), 'XXX666JSON_STR'),
                           (Config(item=self.gen_random_item(), value_json=["XXX666JSON"]), ["XXX666JSON"]),
                           (Config(item=self.gen_random_item(), value_json_clob='STR_JSON_CLOB'), 'STR_JSON_CLOB'),
                           (Config(item=self.gen_random_item(), value_json_clob=["JSON_CLOB"]), ["JSON_CLOB"])]

        for item, _ in self.test_items:
            self.session.add(item)
        self.session.flush()

    def test_all(self):
        config = self.session.config

        for item, ass_val in self.test_items:
            val = getattr(config, item.item)
            print('got session.config.%s = %s (as %s)' % (item.item, val, val.__class__))
            self.assertEqual(val, ass_val)

        print('get session.config._session as %s' % (config._session, ))

        try:
            item_name = self.gen_random_item()
            getattr(config, item_name)
        except AttributeError as err:
            print('got normal exception:', err)

        item_name = '_' + self.gen_random_item()
        setattr(config, item_name, None)

        for item, _ in self.test_items:
            try:
                setattr(config, item.item, None)
            except AttributeError as err:
                print('got normal exception:', err)

        for item, item_value in self.test_items:
            config.set(item.item, item_value)
