# -*- coding: utf-8 -*-
from balance.mapper import *
from tests.object_builder import generate_character_string


class TestCaseTblConfig(object):
    def test_all(self, session):
        today = datetime.datetime.today()
        test_items = [
            (Config(item=generate_character_string(), value_num=666), 666),
            (Config(item=generate_character_string(), value_dt=today), today),
            (Config(item=generate_character_string(), value_str='XXX666'), 'XXX666'),
        ]

        for item, _ in test_items:
            session.add(item)
        session.flush()

        config = session.config

        for item, ass_val in test_items:
            val = getattr(config, item.item)
            print 'got session.config.%s = %s (as %s)' % (item.item, val, val.__class__)
            assert val == ass_val

        print 'get session.config._session as %s' % (config._session,)

        try:
            item_name = generate_character_string()
            getattr(config, item_name)
        except AttributeError, err:
            print 'got normal exception:', err

        item_name = '_' + generate_character_string()
        setattr(config, item_name, None)

        for item, _ in test_items:
            try:
                setattr(config, item.item, None)
            except AttributeError, err:
                print 'got normal exception:', err

    def test_config_history_updated_rows(self, session):
        """Запись об изменённых строках сохраняется если установлен флаг SKIP_HISTORY"""
        test_configs = [
            Config(item=generate_character_string(), skip_history=0),
            Config(item=generate_character_string(), skip_history=1),
        ]
        for config in test_configs:
            session.add(config)
        session.flush()

        for config in test_configs:
            config.value_num = 696
        session.flush()

        for config in test_configs:
            history_record = session.execute(
                "SELECT * FROM t_config_history WHERE ITEM = '{}'".format(
                    config.item,
                ),
            ).first()
            if config.skip_history:
                assert history_record is None, "No hist records sould be created! config={}".format(config)
            else:
                assert history_record is not None, "History record not found! config={}".format(config)

    def test_config_history_deleted_rows(self, session):
        """Запись об удалённых строках сохраняется не зависимо от флага SKIP_HISTORY"""
        test_configs = [
            Config(item=generate_character_string(), skip_history=0),
            Config(item=generate_character_string(), skip_history=1),
        ]
        for config in test_configs:
            session.add(config)
        session.flush()

        for config in test_configs:
            session.delete(config)
        session.flush()

        for config in test_configs:
            history_record = session.execute(
                "SELECT * FROM t_config_history WHERE ITEM = '{}'".format(
                    config.item,
                ),
            ).first()
            assert history_record is not None, "History record not found! config={}".format(config)
