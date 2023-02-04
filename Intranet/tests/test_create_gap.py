# coding: utf-8

from __future__ import unicode_literals

from datetime import datetime

from freezegun import freeze_time
from mock import patch, call

from uhura.external import gap
from utils import handle_utterance

TYPE_KEYBOARD = ['Отсутствие', 'Обучение', 'Отгул', 'Болезнь', 'Отпуск']
CONFIRM_KEYBOARD = ['Да', 'Нет']


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_1(uid, tg_app):
    with patch('uhura.external.gap.create_gap') as m1:
        m1.return_value = 'ok'
        handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'Обучение',
            'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
            'то уточни время в формате HH:MM)',
            cancel_button=True
        )
        handle_utterance(
            tg_app,
            uid,
            'сегодня',
            'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
            'то уточни время в формате HH:MM)',
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'сегодня', 'Введи комментарий', cancel_button=True)
        handle_utterance(tg_app, uid, 'comment', 'Будешь работать?', CONFIRM_KEYBOARD, cancel_button=True)
        expected_message = '''Создать отсутствие?

Тип: Обучение
Начало: Сегодня (1 Января)
Конец: Сегодня (1 Января)
Комментарий: comment
Будешь работать: да''',
        handle_utterance(tg_app, uid, 'Да', expected_message, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'Да', 'Обучение успешно создано!')
        m1.assert_has_calls([call(
            login='robot-uhura',
            from_date=datetime(2017, 1, 1, 0, 0, 0),
            to_date=datetime(2017, 1, 1, 0, 0, 0),
            message='comment',
            workflow='обучение',
            work_in_absence=True,
            has_sicklist=None
        )])


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_2(uid, tg_app):
    with patch('uhura.external.gap.post_request_to_gap') as m1:
        m1.return_value = None
        with patch('uhura.external.gap.get_gaps') as m2:
            m2.return_value = None
            handle_utterance(
                tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True
            )
            handle_utterance(
                tg_app,
                uid,
                'Обучение',
                'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
                'то уточни время в формате HH:MM)',
                cancel_button=True
            )
            handle_utterance(
                tg_app,
                uid,
                'сегодня',
                'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
                'то уточни время в формате HH:MM)',
                cancel_button=True
            )
            handle_utterance(tg_app, uid, 'завтра', 'Введи комментарий', cancel_button=True)
            handle_utterance(tg_app, uid, 'comment', 'Будешь работать?', CONFIRM_KEYBOARD, cancel_button=True)
            expected_message = '''Создать отсутствие?

Тип: Обучение
Начало: Сегодня (1 Января)
Конец: Завтра (2 Января)
Комментарий: comment
Будешь работать: да''',
            handle_utterance(tg_app, uid, 'Да', expected_message, CONFIRM_KEYBOARD)
            handle_utterance(tg_app, uid, 'Да', 'Гэп не отвечает, попробуй через минуту')

            expected_calls = [
                call('robot-uhura', datetime(2017, 1, 1, 0, 1), datetime(2017, 1, 2, 23, 59))
            ]
            m2.assert_has_calls(expected_calls)


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_3(uid, tg_app):
    with patch('uhura.external.gap.create_gap') as m1:
        m1.side_effect = gap.GapIntersectionError
        handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'Обучение',
            'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
            'то уточни время в формате HH:MM)',
            cancel_button=True
        )
        handle_utterance(
            tg_app,
            uid,
            'сегодня',
            'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
            'то уточни время в формате HH:MM)',
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'завтра в 15:00', 'Введи комментарий', cancel_button=True)
        handle_utterance(tg_app, uid, 'comment', 'Будешь работать?', CONFIRM_KEYBOARD, cancel_button=True)
        expected_message = '''Создать отсутствие?

Тип: Обучение
Начало: Сегодня (1 Января)
Конец: Завтра (2 Января) 15:00
Комментарий: comment
Будешь работать: да''',
        handle_utterance(tg_app, uid, 'Да', expected_message, CONFIRM_KEYBOARD)
        handle_utterance(
            tg_app, uid, 'Да',
            'У тебя есть отсутствия, пересекающимся с этим. Пожалуйста, сначала удали их'
        )


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_4(uid, tg_app):
    with patch('uhura.external.gap.create_gap') as m1:
        m1.return_value = 'ok'
        handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'Обучение',
            'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
            'то уточни время в формате HH:MM)',
            cancel_button=True
        )
        handle_utterance(
            tg_app,
            uid,
            'сегодня',
            'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
            'то уточни время в формате HH:MM)',
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'завтра в 15:00', 'Введи комментарий', cancel_button=True)
        handle_utterance(tg_app, uid, 'comment', 'Будешь работать?', CONFIRM_KEYBOARD, cancel_button=True)
        expected_message = '''Создать отсутствие?

Тип: Обучение
Начало: Сегодня (1 Января)
Конец: Завтра (2 Января) 15:00
Комментарий: comment
Будешь работать: да''',
        handle_utterance(tg_app, uid, 'Да', expected_message, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'Да', 'Обучение успешно создано!')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_5(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'Обучение',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(
        tg_app,
        uid,
        'сегодня',
        'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'завтра в 15:00', 'Введи комментарий', cancel_button=True)
    handle_utterance(tg_app, uid, 'comment', 'Будешь работать?', CONFIRM_KEYBOARD, cancel_button=True)
    expected_message = '''Создать отсутствие?

Тип: Обучение
Начало: Сегодня (1 Января)
Конец: Завтра (2 Января) 15:00
Комментарий: comment
Будешь работать: да''',
    handle_utterance(tg_app, uid, 'Да', expected_message, CONFIRM_KEYBOARD)
    handle_utterance(tg_app, uid, 'Нет', 'Тогда не буду создавать отсутствие')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_6(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'Обучение',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(
        tg_app,
        uid,
        'сегодня',
        'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'завтра в 15:00', 'Введи комментарий', cancel_button=True)
    handle_utterance(tg_app, uid, 'comment', 'Будешь работать?', CONFIRM_KEYBOARD, cancel_button=True)
    handle_utterance(tg_app, uid, 'Отмена', 'Как скажешь!')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_7(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'Обучение',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(
        tg_app,
        uid,
        'сегодня',
        'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'завтра в 15:00', 'Введи комментарий', cancel_button=True)
    handle_utterance(tg_app, uid, 'Отмена', 'Как скажешь!')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_8(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'Обучение',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(
        tg_app,
        uid,
        'сегодня',
        'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'Отмена', 'Как скажешь!')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_9(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'Обучение',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'Отмена', 'Как скажешь!')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_10(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(tg_app, uid, 'Отмена', 'Как скажешь!')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_11(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'Обучение',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'цкуенгш', 'Неверный формат даты. Хочешь попробовать еще раз?', CONFIRM_KEYBOARD)
    handle_utterance(tg_app, uid, 'Нет', 'Тогда не буду создавать отсутствие')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_12(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'Обучение',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'цкуенгш', 'Неверный формат даты. Хочешь попробовать еще раз?', CONFIRM_KEYBOARD)
    handle_utterance(
        tg_app,
        uid,
        'Да',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'Отмена', 'Как скажешь!')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_13(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'Обучение',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(
        tg_app,
        uid,
        'сегодня',
        'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'знмпгршо', 'Неверный формат даты. Хочешь попробовать еще раз?', CONFIRM_KEYBOARD)
    handle_utterance(tg_app, uid, 'Нет', 'Тогда не буду создавать отсутствие')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_14(uid, tg_app):
    handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
    handle_utterance(
        tg_app,
        uid,
        'Обучение',
        'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(
        tg_app,
        uid,
        'сегодня',
        'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'знмпгршо', 'Неверный формат даты. Хочешь попробовать еще раз?', CONFIRM_KEYBOARD)
    handle_utterance(
        tg_app,
        uid,
        'Да',
        'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
        'то уточни время в формате HH:MM)',
        cancel_button=True
    )
    handle_utterance(tg_app, uid, 'Отмена', 'Как скажешь!')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_15(uid, tg_app):
        handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'Обучение',
            'Введи дату начала отсутствия (Если хочешь отсутствие не на весь день, '
            'то уточни время в формате HH:MM)',
            cancel_button=True
        )
        handle_utterance(
            tg_app, uid, 'сегодня в 14:00',
            'Введи дату окончания отсутствия (Если хочешь отсутствие не на весь день, '
            'то уточни время в формате HH:MM)',
            cancel_button=True
        )
        handle_utterance(tg_app, uid, 'сегодня в 13:00', 'Введи комментарий', cancel_button=True)
        handle_utterance(tg_app, uid, 'comment', 'Будешь работать?', CONFIRM_KEYBOARD, cancel_button=True)
        expected_message = '''Создать отсутствие?

Тип: Обучение
Начало: Сегодня (1 Января) 14:00
Конец: Сегодня (1 Января) 13:00
Комментарий: comment
Будешь работать: да'''
        handle_utterance(tg_app, uid, 'Да', expected_message, CONFIRM_KEYBOARD)
        handle_utterance(tg_app, uid, 'Да', 'Дата окончания отсутсвия меньше даты начала отсутствия')


@freeze_time('2017-01-01T00:01:00.123456')
def test_create_gap_16(uid, tg_app):
    with patch('uhura.external.gap.create_gap') as m1:
        m1.return_value = 'ok'
        handle_utterance(tg_app, uid, 'Создай отсутствие', 'Выбери тип отсутствия', TYPE_KEYBOARD, cancel_button=True)
        handle_utterance(
            tg_app,
            uid,
            'Болезнь',
            'Введи дату начала отсутствия',
            cancel_button=True
        )
        handle_utterance(
            tg_app,
            uid,
            'двадцать первое апреля',
            'Введи дату окончания отсутствия',
            cancel_button=True
        )
        handle_utterance(tg_app, uid, '02.04.2018', 'Введи комментарий', cancel_button=True)
        handle_utterance(tg_app, uid, 'comment', 'Будешь работать?', CONFIRM_KEYBOARD, cancel_button=True)
        handle_utterance(tg_app, uid, 'Да', 'Есть больничный?', CONFIRM_KEYBOARD, cancel_button=True)
        expected_message = '''Создать отсутствие?

Тип: Болезнь
Начало: 21 Апреля
Конец: 2 Апреля 2018
Комментарий: comment
Будешь работать: да
Больничный: да'''
        handle_utterance(tg_app, uid, 'Да', expected_message, CONFIRM_KEYBOARD)
        handle_utterance(
            tg_app, uid, 'Да',
            'Отсутствие успешно создано! '
            'Если у тебя есть больничный лист, то не забудь занести его в кадры после выздоровления.'
        )
        m1.assert_has_calls([call(
            login='robot-uhura',
            from_date=datetime(2017, 4, 21, 0, 0, 0),
            to_date=datetime(2018, 4, 2, 0, 0, 0),
            message='comment',
            workflow='болезнь',
            work_in_absence=True,
            has_sicklist=True
        )])
