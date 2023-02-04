# coding: utf-8
from apikeys.apikeys_api import API
from btestlib.utils import check_that
from apikeys.apikeys_utils import trunc_date
from datetime import datetime, timedelta
from hamcrest import greater_than_or_equal_to, less_than_or_equal_to

# обрезаем по часы, так как статистика сворачивается и записывается с обрезкой по часам
TODAY = datetime.utcnow()


def test_tk_12(db_connection):
    #yndx-api-assessor-177 891172865

    key_main = '0c70d621-62f2-4881-862e-878bf068d17c'
    key_second = 'f2b5f33d-7770-4c7e-a98a-5d1e4e455e75'
    stat = 100
    shift = 200
    token = db_connection['service'].find_one({'_id': 3})['token']
    counter_list= ['hits', 'total', 'search_hits', 'router_hits', 'panorama_locate_hits', 'geocoder_hits', 'suggest_hits']
    for counter in counter_list:
        API.update_counters(token, key_main, {counter: stat}, allow_not_200=True)
        API.update_counters(token, key_second, {counter: stat/10}, allow_not_200=True)
        stat += shift
