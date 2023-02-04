# coding: utf-8
from apikeys.apikeys_api import API
from btestlib.utils import check_that
from apikeys.apikeys_utils import trunc_date
from datetime import datetime, timedelta
from hamcrest import greater_than_or_equal_to, less_than_or_equal_to

# обрезаем по часы, так как статистика сворачивается и записывается с обрезкой по часам
TODAY = datetime.utcnow()

def test_hourly_stat_compression(db_connection):
    """Для данного теста используется заранее подготовденный пользовеатель apikeys-autotest-2777

    """
    stat = 10
    iter = 20
    shift = 5
    key = 'c25fbbc9-da65-49d8-9c53-fbf9460b2ef8'
    token = db_connection['service'].find_one({'_id': 3})['token']
    counters_list = db_connection['autotest_logins'].find_one({'LOGIN': 'apikeys-autotest-2777'})['counter_list']
    new_counter_list = counters_list.copy()
    for counter_name in new_counter_list:
        expected_value = 0
        max_expected_value = 0
        for _ in range(iter):
            # Накручиваем честно статистику через ручку
            result = API.update_counters(token, key, {counter_name: stat}, allow_not_200=True)
            # Так как ошибка от ручки может означать как успешную так и неуспешную вставку статистики
            # мы просто понижаем порог ожидаемого результата одновременно скалируя максимальный
            max_expected_value += stat
            if result.ok:
                expected_value += stat
        stat += shift
        new_counter_list[counter_name]['expected'] = expected_value
        new_counter_list[counter_name]['max_expected'] = max_expected_value

    # обновляем в базе ожидаемую статистику для следующей итерации теста
    db_connection['autotest_logins'].update_one({'LOGIN': 'apikeys-autotest-2777'},
                                                {'$set': {'counter_list': new_counter_list}})

    # дату обрезаем по часы, так как статистика сворачивается и записывается с обрезкой по часам
    t_today = trunc_date(TODAY, 'hour') - timedelta(hours=1)

    # провереем статистику в hourly_stat
    for counter in counters_list.values():
        horly_stat = db_connection['hourly_stat'].find_one({'dt': t_today, 'counter_id': counter['counter_id']})

        if horly_stat:
            check_that(horly_stat['value'],
                       greater_than_or_equal_to(counter['expected']) and less_than_or_equal_to(counter['max_expected']))
        else:
            raise Exception('Ошибка сворачивания статистики на дату {date} по каунтеру {counter}'
                            .format(date=t_today, counter=counter['counter_id']))

def test_overhelmed_stat(db_connection):
    #yndx-api-assessor-177 891172865

    key_main = 'ecddbe4a-8b66-4849-8b68-a718582fdc1d'
    key_second = '1bb766f2-8db6-462c-8e4b-17ad5fae5543'
    stat = 1000
    token = db_connection['service'].find_one({'_id': 3})['token']
    counter_list= ['total']
    for counter in counter_list:
        API.update_counters(token, key_main, {counter: stat}, allow_not_200=True)
        API.update_counters(token, key_second, {counter: stat/10}, allow_not_200=True)
