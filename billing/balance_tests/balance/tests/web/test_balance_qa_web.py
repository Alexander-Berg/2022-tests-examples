# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

from hamcrest import equal_to
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_web as web
import btestlib.reporter as reporter
from balance.features import Features


# внутри захардкожен tm, т.к. только на нем есть асессорский пакет
@reporter.feature(Features.TM_ONLY)
def test_balance_qa_create_client_tm_only(get_free_user):
    user = get_free_user()
    steps.ClientSteps.unlink_from_login(user.uid)
    with web.Driver(user=user) as driver:
        balqa_page = web.ClientInterface.BalanceQAWeb.open(driver)
        balqa_page.is_create_client_button_present()
        client_id_from_ui = balqa_page.create_client()
        client_id_from_db = steps.ClientSteps.get_client_id_by_passport_id(user.uid)
        utils.check_that(client_id_from_ui, equal_to("client_id = " + str(client_id_from_db)),
                         u'Проверяем, что указанный в ui client_id совпадает с client_id в базе')
