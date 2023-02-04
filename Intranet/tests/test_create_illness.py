# coding: utf-8
from __future__ import unicode_literals

from datetime import datetime

from freezegun import freeze_time
from mock import patch, call

from utils import handle_utterance

TYPE_KEYBOARD = ['Отсутствие', 'Обучение', 'Отгул', 'Болезнь', 'Отпуск', 'Отмена']
CONFIRM_KEYBOARD = ['Да', 'Нет']


@freeze_time('2017-01-01T00:00:00.100')
def test_create_illness(uid, tg_app):
    with patch('uhura.external.gap.create_gap') as m1:
        m1.return_value = 'ok'
        handle_utterance(tg_app, uid, 'болею', 'Введи дату начала отсутствия', cancel_button=True)
        handle_utterance(tg_app, uid, 'сегодня', 'Введи дату окончания отсутствия', cancel_button=True)
        handle_utterance(tg_app, uid, 'сегодня', 'Введи комментарий', cancel_button=True)
        handle_utterance(tg_app, uid, 'comment', 'Будешь работать?', CONFIRM_KEYBOARD, cancel_button=True)
        handle_utterance(tg_app, uid, 'Да', 'Есть больничный?', CONFIRM_KEYBOARD, cancel_button=True)
        expected_message = '''Создать отсутствие?

Тип: Болезнь
Начало: Сегодня (1 Января)
Конец: Сегодня (1 Января)
Комментарий: comment
Будешь работать: да
Больничный: да'''
        handle_utterance(tg_app, uid, 'Да', expected_message, CONFIRM_KEYBOARD)
        handle_utterance(
            tg_app,
            uid,
            'Да',
            'Отсутствие успешно создано! '
            'Если у тебя есть больничный лист, то не забудь занести его в кадры после выздоровления.'
        )
        m1.assert_has_calls([call(
            login='robot-uhura',
            from_date=datetime(2017, 1, 1, 0, 0, 0),
            to_date=datetime(2017, 1, 1, 0, 0, 0),
            message='comment',
            workflow='болезнь',
            work_in_absence=True,
            has_sicklist=True
        )])
