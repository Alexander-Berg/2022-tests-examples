# coding: utf-8

__author__ = 'a-vasin'

import balance.balance_api as api
from btestlib import reporter, utils
from hamcrest import has_entry
from btestlib.constants import YtCluster


class StagerSteps(object):

    @staticmethod
    @utils.CheckMode.result_matches(has_entry('status', 'ok'))
    def run_stager(stager_project, start_dt, end_dt=None, custom_paths=None, proxy=YtCluster.HAHN, wait=False):
        start_dt_str = start_dt.strftime('%Y-%m-%d')
        end_dt_str = (end_dt or start_dt).strftime('%Y-%m-%d')

        with reporter.step(
                u"Запускаем stager для проекта '{}' за даты {} - {}".format(stager_project, start_dt_str, end_dt_str)):
            return api.test_balance().RunStager(
                stager_project,
                start_dt_str,
                end_dt_str if end_dt else None,
                custom_paths,
                '{}.yt.yandex.net'.format(proxy),
                wait
            )

    @staticmethod
    def check_stager(request_id):
        with reporter.step(u"Получаем актуальный статус stager по request_id: {}".format(request_id)):
            return api.test_balance().CheckStager(request_id)

    @staticmethod
    def kill_stager(request_id):
        with reporter.step(u"Останавливаем работу stager по request_id: {}".format(request_id)):
            return api.test_balance().KillStager(request_id)

    @staticmethod
    def wait_completion(request_id):
        utils.wait_until(
            lambda: StagerSteps.check_stager(request_id),
            success_condition=has_entry('status', 'success'),
            failure_condition=has_entry('status', 'failed'),
            timeout=30 * 60  # 30 минут =(
        )
