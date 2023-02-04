# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import btestlib.utils as utils
from balance.balance_web import DefaultPage
from hamcrest import equal_to
from selenium.webdriver.common.by import By
import btestlib.environments as env


class SnoutInterface(object):
    class MainPage(DefaultPage):
        DESCRIPTION = u'Страница Snout'
        DEFAULT_BLOCKS = [
            'act',
            'client',
            'common',
            'contract',
            'edo',
            'firm',
            'invoice',
            'manager',
            'order',
            'passport_sms',
            'paysys',
            'person',
            'service',
            'user',
        ]

        @classmethod
        def url(cls):
            return u'{base_url}/'.format(base_url=env.balance_env().snout_url)

        @classmethod
        def open(cls, driver):
            return cls.open_url(driver=driver, url=cls.url())

        @classmethod
        def open_url(cls, driver, url):
            driver.get(url, name=u'Страница Snout')
            page_object = cls(driver=driver)
            page_object.wait_for_data()
            return page_object

        def get_blocks(cls):
            data_for_search = []

            for block in cls.DEFAULT_BLOCKS:
                data_for_search.append((
                    By.XPATH,
                    u'//*[@id="operations-tag-{}"]'.format(block),
                    u'Блок {}'.format(block),
                ))

            return data_for_search

        def wait_for_data(cls):
            for block in cls.get_blocks():
                utils.wait_until(lambda: utils.Web.is_element_present(cls.driver, block), equal_to(True))
