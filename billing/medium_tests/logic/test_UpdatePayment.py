# -*- coding: utf-8 -*-

import datetime

from tests import object_builder as ob

PR_DT = datetime.datetime(2019, 1, 2)


def create_trust_payment(session):
    trust_payment = ob.TrustPaymentBuilder().build(session).obj
    trust_payment.trust_payment_id = ob.generate_character_string(23)
    return trust_payment


def create_side_payment(session):
    side_payment = ob.SidePaymentBuilder(
        service_id=1, transaction_id=ob.generate_int(23), dt=datetime.datetime.now()
    ).build(session).obj

    return side_payment


def UpdatePayment_params(session):
    params = []

    # TrustPayment cо старым параметром PayoutReady
    payment = create_trust_payment(session)
    param_id = {'TrustPaymentID': payment.trust_payment_id}
    param_state = {'PayoutReady': PR_DT}
    params.append((payment, param_id, param_state,))

    # TrustPayment c новым параметром PayoutReadyDT
    payment = create_trust_payment(session)
    param_id = {'TrustPaymentID': payment.trust_payment_id}
    param_state = {'PayoutReadyDT': PR_DT}
    params.append((payment, param_id, param_state,))

    # SidePayment
    payment = create_side_payment(session)
    param_id = {
        'ServiceID': payment.service_id,
        'TransactionID': payment.transaction_id
    }
    param_state = {'PayoutReadyDT': PR_DT}
    params.append((payment, param_id, param_state,))
    return params


def test_UpdatePayment(session, medium_xmlrpc):
    """ Работа ручки UpdatePayment """

    params = UpdatePayment_params(session)

    payment, param_id, param_state = params[0]  # Первый вызов
    res = medium_xmlrpc.UpdatePayment(param_id, param_state)
    # ручка возвращает ок
    assert res == {'ERROR_CODE': 0, 'ERROR_MESSAGE': 'OK'}
    # дата проставилась
    assert payment.payout_ready_dt == PR_DT

    # Второй вызов, тот же платеж, другая дата
    param_state = {'PayoutReadyDT': datetime.datetime.now()}
    res = medium_xmlrpc.UpdatePayment(param_id, param_state)
    # ручка возвращает ок
    assert res == {
        'ERROR_CODE': 0,
        'ERROR_MESSAGE': 'Payment already has payout dt - passed value is ignored'
    }
    # дата не изменилась
    assert payment.payout_ready_dt == PR_DT

    # Третий вызов, тот же платеж без даты
    param_state = {'PayoutReadyDT': None}
    res = medium_xmlrpc.UpdatePayment(param_id, param_state)
    # ручка возвращает ошибку
    assert res == {
        'ERROR_CODE': -1,
        'ERROR_MESSAGE': 'Payment already has payout dt - cannot unset'
    }
    # дата не изменилась
    assert payment.payout_ready_dt == PR_DT


def test_UpdatePayment_not_found(medium_xmlrpc):
    param_id = {'TrustPaymentID': 'robot_xxx_666'}
    param_state = {'PayoutReadyDT': PR_DT}

    # Несуществующий платеж
    res = medium_xmlrpc.UpdatePayment(param_id, param_state)
    # ручка возвращает ошибку
    assert res == {'ERROR_CODE': -1, 'ERROR_MESSAGE': 'Payment was not found'}
