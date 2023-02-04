# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import allure
from dateutil.relativedelta import relativedelta

import balance.balance_db as db
import btestlib.utils as utils

DISTRIBUTION_PLACE_TYPE = 8
SAVE_DAYS_INTERVAL = 1  # Включая сегодня, т.е. 2 = сохранить за вчера и сегодня


def test_clear_old_distribution_places():
    save_date = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=SAVE_DAYS_INTERVAL - 1)

    with allure.step(u"Удаляем тестовые площадки, созданные до (невключительно): {}".format(save_date)):
        query = "DELETE FROM T_PLACE WHERE URL='pytest.com' AND TYPE=8 AND DT < :save_date"
        params = {'save_date': save_date}
        db.balance().execute(query, params)
