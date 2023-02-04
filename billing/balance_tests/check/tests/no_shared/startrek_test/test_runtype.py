# coding: utf-8
__author__ = 'chihiro'
import requests
from hamcrest import equal_to

from balance import balance_api as api
from btestlib import utils as butils
from check import utils


def test_runtype():
    cmp_id = utils.run_check_new('bua', '1')
    query = 'select issue_key from bua_cmp where id = {cmp_id}'.format(cmp_id=cmp_id)
    issue_key = api.test_balance().ExecuteSQL('cmp', query)[0]['issue_key']

    # Действие доступно только для команды очереди
    issue_queue = issue_key.split('-')[0]
    st_queue = utils.get_tracker_queue(issue_queue)
    uid = st_queue.teamUsers[0].uid

    requests.post(
        'https://yb-dcs-tm.paysys.yandex.net:8024/api/startrek/mark_test',
        json={
            'issue_key': issue_key,
            'uid': str(uid),
        }
    )

    query = 'select runtype from bua_cmp where id = {cmp_id}'.format(cmp_id=cmp_id)
    runtype = api.test_balance().ExecuteSQL('cmp', query)[0]['runtype']
    butils.check_that(runtype, equal_to(2), u'Проверяем, что в таблице проставился тип - тестовый запуск')

    expected_comment = utils.get_db_config_value('api_mark_test_issue_comment')
    ticket = utils.get_check_ticket('bua', cmp_id)

    comment = list(ticket.comments)[-1]
    butils.check_that(comment.text, equal_to(expected_comment),
                      u'Проверяем, что в задаче появился комментарий об изменении статуса')
