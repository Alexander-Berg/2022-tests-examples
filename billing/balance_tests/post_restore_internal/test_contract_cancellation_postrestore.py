# -*- coding: utf-8 -*-
from maintenance import test_contract_cancellation


# зовем существующие тесты, прокидывая нужный день
def test_cancellation_dsp_contract_postrestore():
    test_contract_cancellation.test_cancellation_dsp_contract(0)


def test_cancellation_rsya_contract_postrestore():
    test_contract_cancellation.test_cancellation_rsya_contract(0)


def test_cancellation_taxi_contract_postrestore():
    test_contract_cancellation.test_cancellation_taxi_contract(0)