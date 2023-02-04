# coding: utf-8

from __future__ import unicode_literals

from random import randint
from uuid import uuid4

from django.conf import settings
from mock import patch
from vins_core.nlg.filters import pluralize

from uhura import models
from utils import handle_utterance, _staff_get_person_data


def _get_suggest_clinic_example():
    data = {
        'id': uuid4().hex,
        'title': 'Лечебный центр',
        'status': 'прямой',
        'working_hours': '',
        'metro': 'Парк культуры',
        'url': 'https://www.lcenter.ru',
        'phones': ['+7 495 786-45-25'],
        'region': 'Москва',
        'address': 'г Москва, ул Тимура Фрунзе, д 15/1',
        'categories': [
            'Амбулатор-поликлиническое обслуживание',
            'Помощь на дому+скорая помощь',
            'Стоматология'
        ]
    }
    return data


def _get_expected_one_clinic_answer(clinic):
    return 'Да, <b> {title}, {phones[0]}, {address} </b> входит в ДМС в твоем городе.'.format(**clinic)


def _get_expected_several_clinic_answer(clinics):
    text = 'По этому запросу я нашла несколько клиник в твоем городе, которые входят в ДМС:\n'
    for clinic in clinics:
        text += '- {title}, {phones[0]}, {address}\n'.format(**clinic)
    return text.strip()


def _get_expected_many_clinic_answer(clinics, count):
    text = 'По этому запросу я нашла {count} {clinic_label} в твоем городе, которые входят в ДМС. Вот первые пять:\n'
    for clinic in clinics:
        text += '- {title}, {phones[0]}, {address}\n'.format(**clinic)

    text += '\nТы можешь уточнить свой запрос или посмотреть список всех клиник на <a href=\'{wiki_link}\'>вики</a>.'
    return text.format(count=count, clinic_label=pluralize('клиника', count, case='acc'),
                       wiki_link=settings.DMS_CLINICS_URL)


def _get_expected_no_clinic_answer(name, city='Москва'):
    text = ('Извини, я не нашла клинику <b>{name}</b> в городе <b>{city}</b>. '
            'Но ты можешь поискать самостоятельно в списке на <a href=\'{wiki_link}\'>вики</a>.')
    return text.format(name=name, city=city, wiki_link=settings.DMS_CLINICS_URL)


def _get_person_data(city=None, office_code=None):
    person_data = _staff_get_person_data()
    if city:
        person_data['location']['office']['city']['name']['ru'] = city
    if office_code:
        person_data['location']['office']['code'] = office_code
    return person_data


def test_city_from_staff(uid, tg_app):
    city = 'Симферополь'
    person_data = _get_person_data(city=city)

    with patch('uhura.external.suggest._call_suggest') as suggest, \
         patch('uhura.app.TelegramApp.get_person_data_from_staff') as p:
        suggest.return_value = {'clinics': {'result': [], 'pagination': {'count': 0}}}
        p.return_value = person_data
        clinic = 'стоматология'
        handle_utterance(tg_app, uid, 'дмс %s' % clinic, _get_expected_no_clinic_answer(clinic, city))
        assert not models.User.objects.filter(city='Симферополь').exists()


def test_ask_for_city(uid, tg_app):
    person_data = _get_person_data(office_code='home')

    with patch('uhura.external.suggest._call_suggest') as suggest, \
         patch('uhura.app.TelegramApp.get_person_data_from_staff') as p:
        suggest.return_value = {'clinics': {'result': [], 'pagination': {'count': 0}}}
        p.return_value = person_data
        clinic = 'стоматология'
        city = 'ростов на дону'

        handle_utterance(
            tg_app,
            uid,
            'дмс %s' % clinic,
            'Скажи название города, в котором ты хочешь найти клинику.',
            cancel_button=True
        )
        handle_utterance(tg_app, uid, city, _get_expected_no_clinic_answer(clinic, city))


def test_city_from_db(uid, tg_app):
    person_data = _get_person_data(office_code='home')

    city = 'Симферополь'
    user = models.User.objects.create(uid=person_data['uid'], city=city)
    models.TelegramUsername.objects.update(user=user)

    with patch('uhura.external.suggest._call_suggest') as suggest, \
         patch('uhura.app.TelegramApp.get_person_data_from_staff') as p:
        suggest.return_value = {'clinics': {'result': [], 'pagination': {'count': 0}}}
        p.return_value = person_data
        clinic = 'стоматология'
        handle_utterance(tg_app, uid, 'клиника %s' % clinic, _get_expected_no_clinic_answer(clinic, city))


def test_ask_for_clinic_name(uid, tg_app):
    with patch('uhura.external.suggest._call_suggest') as suggest:
        suggest.return_value = {'clinics': {'result': [], 'pagination': {'count': 0}}}
        clinic = 'стоматология'
        handle_utterance(
            tg_app,
            uid,
            'клиника',
            'Скажи название клиники, которую ты хочешь найти.',
            cancel_button=True
        )
        handle_utterance(tg_app, uid, clinic, _get_expected_no_clinic_answer(clinic))


def test_not_find(uid, tg_app):
    with patch('uhura.external.suggest._call_suggest') as suggest:
        suggest.return_value = {'clinics': {'result': [], 'pagination': {'count': 0}}}
        clinic_name = 'стоматология'
        expected = _get_expected_no_clinic_answer(clinic_name)
        handle_utterance(tg_app, uid, 'клиника {}'.format(clinic_name), expected)


def test_find_one(uid, tg_app):
    with patch('uhura.external.suggest._call_suggest') as suggest:
        clinic = _get_suggest_clinic_example()
        suggest.return_value = {'clinics': {'result': [clinic], 'pagination': {'count': 1}}}
        expected = _get_expected_one_clinic_answer(clinic)
        handle_utterance(tg_app, uid, 'дмс стоматология', expected)


def test_find_several(uid, tg_app):
    with patch('uhura.external.suggest._call_suggest') as suggest:
        clinics = [_get_suggest_clinic_example()] * randint(2, 5)
        suggest.return_value = {'clinics': {'result': clinics, 'pagination': {'count': len(clinics)}}}
        expected = _get_expected_several_clinic_answer(clinics)
        handle_utterance(tg_app, uid, 'дмс стоматология', expected)


def test_find_many(uid, tg_app):
    with patch('uhura.external.suggest._call_suggest') as suggest:
        clinics = [_get_suggest_clinic_example()] * randint(2, 5)
        count = randint(5, 100)
        suggest.return_value = {'clinics': {'result': clinics, 'pagination': {'count': count}}}
        expected = _get_expected_many_clinic_answer(clinics, count)
        handle_utterance(tg_app, uid, 'дмс стоматология', expected)


def test_suggest_error(uid, tg_app):
    with patch('uhura.external.suggest._call_suggest') as suggest:
        suggest.return_value = None
        expected = 'Прости, но у меня не получилось поискать клиники. Попробуй еще раз через минуту!'
        handle_utterance(tg_app, uid, 'дмс стоматология', expected)
