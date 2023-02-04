# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from datetime import datetime

import pytest
import hamcrest as hm

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Services, Products, Paysyses, Firms, NdsNew
from btestlib.matchers import equal_to_casted_dict

USER1 = {'passport_id': 675282919, 'passport_uid': 'yb-atst-user-29'}
USER2 = {'passport_id': 675282932, 'passport_uid': 'yb-atst-user-30'}


# 23 сервис остался в проде в 1ой фирме https://st.yandex-team.ru/PAYSUP-415463
@reporter.feature(Features.INVOICE, Features.TRUST)
@pytest.mark.parametrize('service_id, product_id, paysys_id, user, expected_data',
                         [
                             # Выключен т.к на данный момент оплата через мобильные платежи не используется
                             # pytest.param(
                             #     Services.MUSIC_MEDIASERVICE.id, Products.MUSIC_MEDIASERVICE_SUBSCRIBE.id,
                             #     Paysyses.CC_WSTORE_INAPP_MEDIASERVICES.id, USER1,
                             #     {'paysys_id': Paysyses.CC_WSTORE_INAPP_MEDIASERVICES.id, 'nds': 0,
                             #      'nds_pct': NdsNew.NOT_RESIDENT, 'bank_details_id': 978},
                             #     id='CreateFastInvoice for MusicSubscription', marks=pytest.mark.skip('Unusable')),
                             pytest.param(
                                 Services.MUSIC.id, Products.MUSIC_SUBSCRIBE.id, Paysyses.CC_WSTORE_INAPP.id, USER1,
                                 {'paysys_id': Paysyses.CC_WSTORE_INAPP.id, 'nds': 0,
                                  'nds_pct': NdsNew.NOT_RESIDENT, 'bank_details_id': 5},
                                 id='CreateFastInvoice for Music'),
                             pytest.param(
                                 Services.MUSIC_PROMO.id, Products.MUSIC_PROMO.id, Paysyses.YM_PH_RUB.id, USER2,
                                 {'paysys_id': Paysyses.YM_MEDIASERVICES.id, 'nds': 1, 'nds_pct': NdsNew.DEFAULT,
                                  'bank_details_id': 977},
                                 id='CreateFastInvoice for MusicPromoCodes')
                         ])
def test_create_fast_invoice_music(service_id, product_id, paysys_id, user, expected_data, get_free_user):
    user = get_free_user()
    user = {'passport_id': user.uid, 'passport_uid': user.login}

    client_id = steps.ClientSteps.create()
    client_data_to_remove = api.medium().FindClient({'PassportID': user['passport_id']})[2]
    if client_data_to_remove:
        client_to_remove = client_data_to_remove[0]['CLIENT_ID']
        api.medium().RemoveUserClientAssociation(user['passport_id'], client_to_remove, user['passport_id'])
    db.balance().execute(
        "DELETE FROM t_service_client WHERE service_id = :service_id AND passport_id = :passport_id",
        {'service_id': service_id, 'passport_id': user['passport_id']})
    db.balance().execute(
        "insert into t_service_client (PASSPORT_ID, SERVICE_ID, CLIENT_ID) values (:p_id, :s_id, :c_id)",
        {'p_id': user['passport_id'], 's_id': service_id, 'c_id': client_id})
    api.medium().CreateUserClientAssociation(user['passport_id'], client_id, user['passport_id'])
    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                            service_id=service_id)
    api.medium().CreateOrUpdateOrdersBatch(user['passport_id'], ({'ClientID': client_id, 'ProductID': product_id,
                                                                  'ServiceOrderID': service_order_id},),
                                           'music_039128f74eaa55f94617c329b4c06e65')
    invoice_resp = api.medium().CreateFastInvoice({'back_url': 'http://music.mt.yandex.ru',
                                                   'login': user['passport_uid'],
                                                   'mobile': False,
                                                   'overdraft': False,
                                                   'paysys_id': paysys_id,
                                                   'qty': 30,
                                                   'service_id': service_id,
                                                   'service_order_id': service_order_id})
    hm.assert_that(invoice_resp, hm.has_key('invoice_id'))
    invoice_id = invoice_resp['invoice_id']
    invoice_data = db.balance().execute(
        "select paysys_id, bank_details_id, nds, nds_pct, firm_id from t_invoice where id = :invoice_id",
        {'invoice_id': invoice_id})
    hm.assert_that(invoice_data, hm.has_length(1))
    invoice_data = invoice_data[0]
    expected_invoice_data = {
        'firm_id': Firms.YANDEX_1.id if service_id == Services.MUSIC.id else Firms.MEDIASERVICES_121.id,
    }
    expected_invoice_data.update(expected_data)
    expected_invoice_data['nds_pct'] = expected_invoice_data['nds_pct'].pct_on_dt(datetime.now())
    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data), 'Сравниваем данные из счета с шаблоном')
