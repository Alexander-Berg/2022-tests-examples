# coding: utf-8
from __future__ import unicode_literals

from datetime import datetime

from mock import patch

from utils import get_canteen_menu_json, handle_utterance


def test_get_canteen_menu_url_sucess(uid, ):
    with patch('uhura.external.intranet.get_request') as m:
        from uhura.external import wiki
        m.return_value = get_canteen_menu_json()
        url = wiki.get_canteen_menu_url(datetime(2017, 12, 19, 0, 0, 1, 0), 'morozov', 'bzhu')
        assert url == (
            'https://wiki-api.yandex-team.ru/_api/frontend/HR/'
            'Kompensacii/Novaja-stranica-po-pitaniju/menu/.files/1912obedbzhu-1.pdf'
        )


def test_get_canteen_menu_url_non_working_day(uid, tg_app):
    with patch('uhura.external.calendar.get_holidays') as m:
        m.return_value = [datetime.now().date()]
        handle_utterance(tg_app, uid, 'меню', 'Столовая сегодня не работает')
