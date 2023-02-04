# coding: utf-8
import datetime
import json
import xmlrpclib

import pytest

from billing.contract_iface.constants import ContractTypeId
from balance.constants import *


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class TestCreateCommonContractWithApikeysTariffsParam(object):
    @staticmethod
    def contract_params(client_id, person_id, manager_code):
        return dict(
            commission=ContractTypeId.LICENSE,
            currency='RUB',
            firm_id=1,
            client_id=client_id,
            person_id=person_id,
            services=[ServiceId.APIKEYS],
            credit_type=CreditType.PO_SROKU_I_SUMME,
            payment_term=20,
            account_type=0,
            manager_code=manager_code,
            partner_credit=1,
            payment_type=3,
            personal_account=1,
            credit_limit=17,
            dt=datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%S'),
            personal_account_checkpassed=1,
        )

    def test_wrong_apikeys_tariffs_param(self, person, some_manager, tariff):
        contract_params = self.contract_params(person.client.id, person.id, some_manager.manager_code)

        # невалидный json
        tariff_info = 'no_json'
        contract_params.update(apikeys_tariffs_info=tariff_info)

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            self.xmlrpcserver.CreateCommonContract(
                self.session.oper_id,
                contract_params
            )

        assert 'Invalid json' in exc_info.value.faultString

        # в tariff_info отсутствует ключ "group_cc"
        tariff_info = {"tariff_cc": tariff.cc}
        contract_params.update(apikeys_tariffs_info=json.dumps(tariff_info))

        with pytest.raises(xmlrpclib.Fault) as exc_info:
            self.xmlrpcserver.CreateCommonContract(
                self.session.oper_id,
                contract_params
            )

        assert '"group_cc" and "tariff_cc" keys are mandatory.' in exc_info.value.faultString

        # несуществующий group_cc
        bad_group_cc = 'bad_group_cc'
        tariff_info = {"group_cc": bad_group_cc, "tariff_cc": tariff.cc}
        contract_params.update(apikeys_tariffs_info=json.dumps(tariff_info))

        assert 'TariffGroup with "%s" cc does not exists.' % bad_group_cc

        # несуществующий tariff_cc
        bad_tariff_cc = 'bad_tariff_cc'
        tariff_info = {"group_cc": tariff.tariff_group.cc, "tariff_cc": bad_tariff_cc}
        contract_params.update(apikeys_tariffs_info=json.dumps(tariff_info))

        assert 'Tariff with "%s" cc does not exists.' % bad_tariff_cc

    def test_ok_apikeys_tariffs_param(self, person, some_manager, tariff):
        contract_params = self.contract_params(person.client.id, person.id, some_manager.manager_code)

        tariff_info = {"group_cc": tariff.tariff_group.cc, "tariff_cc": tariff.cc}
        contract_params.update(apikeys_tariffs_info=json.dumps(tariff_info))

        contract_info = self.xmlrpcserver.CreateCommonContract(
            self.session.oper_id,
            contract_params
        )
        ex_id = contract_info['EXTERNAL_ID']

        contracts_data = self.xmlrpcserver.GetClientContracts({
            'ClientID': person.client.id,
            'Signed': False,
            'ExternalID': ex_id
        })
        assert len(contracts_data) == 1

        tariffs = contracts_data[0]['APIKEYS_TARIFFS'][0]['TARIFFS']
        assert tariffs == {tariff.tariff_group.cc: tariff.cc}
