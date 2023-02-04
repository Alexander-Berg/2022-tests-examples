# coding: utf-8

from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance


def test_check_aurora_parking__available(uid, tg_app):
    with patch('uhura.external.intranet.get_request') as m:
        m.return_value = [{'datapoints': [[0.0, 123456789]]}]
        handle_utterance(tg_app, uid, 'парковка аврора', 'На парковке в Авроре места еще есть')


def test_check_aurora_parking__not_available(uid, tg_app):
    with patch('uhura.external.intranet.get_request') as m:
        m.return_value = [{'datapoints': [[1.0, 123456789]]}]
        handle_utterance(tg_app, uid, 'парковка аврора', 'На парковке в Авроре мест нет')


def test_check_aurora_parking__error(uid, tg_app):
    with patch('uhura.external.intranet.get_request') as m:
        m.return_value = None
        handle_utterance(
            tg_app, uid, 'парковка аврора', 'Не смогла проверить количество мест на парковке. Попробуй через минуту'
        )
