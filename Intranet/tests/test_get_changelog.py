# coding: utf-8
from __future__ import unicode_literals

from mock import patch

from utils import handle_utterance


def test_get_changelog_success(uid, tg_app):
    with patch('uhura.external.wiki.get_wiki_grid') as m:
        m.return_value = {'rows': [[{'raw': 10.0}, {'raw': '2017-10-10'}, {'raw': 'описание\nописание'}]]}
        handle_utterance(
            tg_app,
            uid,
            'что нового',
            'В последнем релизе 10.0 (2017-10-10) появились:\n\nописание\nописание\n\n'
            'Полный лог релизов: https://wiki.yandex-team.ru/intranet/uhura/releases/'
        )


def test_get_changelog_error(uid, tg_app):
    with patch('uhura.external.wiki.get_wiki_grid') as m:
        m.return_value = None
        handle_utterance(tg_app, uid, 'что нового', 'Не могу связаться с вики. Попробуй через минуту')
