# -*- coding: utf-8 -*-
from balance import balance_web as b_web
from balance.snout_steps import web_steps as web


def test_swagger_ui():
    """
    UI check (Swagger)
    """
    with b_web.Driver() as driver:
        web.SnoutInterface.MainPage.open(driver)
