# coding=utf-8
import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from simpleapi.data import features
from simpleapi.data import marks
from simpleapi.data import stories
from simpleapi.steps import balance_steps as balance
from simpleapi.steps import check_steps as check

__author__ = 'slppls'

pytestmark = marks.simple_internal_logic


@reporter.feature(features.Methods.FindClient)
class TestCreateFindClient(object):
    @reporter.story(stories.Methods.Call)
    def test_with_service_param(self):
        user, client = balance.user_client(service_id=23)
        relation_info = balance.get_service_client_relation_info(client, user)
        check.check_that(len(relation_info), equal_to(3),
                         step=u'Проверяем что в БД создалась связка клиента и логина по сервису Музыка',
                         error=u'В БД не создалась связка клиента и логина по сервису Музыка')
        resp = balance.find_client(passport_id=user.uid, service_id=23)
        check.check_that(client, equal_to(resp[2][0]['CLIENT_ID']),
                         step=u'Проверяем найден корректный клиент',
                         error=u'Не найден искомый клиент')


if __name__ == '__main__':
    pytest.main()
