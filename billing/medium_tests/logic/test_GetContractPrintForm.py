# -*- coding: utf-8 -*-
from datetime import datetime

import pytest

from billing.contract_iface import ContractTypeId
from balance.constants import *
from billing.contract_iface.contract_meta import ContractTypes
from balance.mapper import Contract
from tests.base import MediumTest
from tests.object_builder import PersonBuilder


@pytest.mark.skip(reason='BALANCE-30630: will be moved to balance-tests')
class ContractPDF2MDSTest(MediumTest):

    def _mock_prepare_contract(self):
        """ Договор с ПФ. """
        with self.session.begin():
            contract = Contract(ContractTypes(type='GENERAL'))
            self.session.add(contract)

            col0 = contract.col0
            col0.commission = ContractTypeId.NON_AGENCY
            col0.dt = datetime(2018, 9, 1)
            col0.firm = FirmId.TAXI
            col0.payment_type = PREPAY_PAYMENT_TYPE
            col0.services = {ServiceId.TAXI_CORP: 1, ServiceId.TAXI_CORP_CLIENTS: 1}
            col0.print_template = ContractPrintTpl.TAXI_MARKETING
            col0.maybe_create_barcode()

            contract.external_id = contract.create_new_eid()
            contract.person = PersonBuilder(type='ph').build(self.session).obj

        return contract

    def test_mds_medium_handlers(self):
        # создаём договор с ПФ
        contract = self._mock_prepare_contract()

        # ручка вернёт pdf, кодированный в base64
        self.xmlrpcserver.GetContractPrintForm(contract.id, 'contract')

        # ручка вернёт ссылку на pdf в mds
        self.xmlrpcserver.GetContractPrintForm(contract.id, 'contract', 'mds-link')

        # ручка удаляет инстанцированный шаблон на вики и файл из MDS
        self.xmlrpcserver.EraseContractPrintForm(contract.id, 'contract')

        # проверяем, что ключ в MDS очистился
        contract = self.session\
            .query(Contract)\
            .filter(Contract.id == contract.id)\
            .first()
        self.assertIsNone(getattr(contract.col0, 'print_tpl_mds_key', None), 'MDS key is to be None')

    def test_mds_workflow(self):
        # создаём договор с ПФ
        contract = self._mock_prepare_contract()
        col0 = contract.col0

        # создаём пдф и выгружаем её в MDS
        with self.session.begin():
            from balance.publisher.wiki_handler import WikiHandler

            wiki_handler = WikiHandler(self.session, contract.id, 'contract')
            wiki_handler.pdf_binary_content()

            print_tpl_mds_key = getattr(col0, 'print_tpl_mds_key', None)
            self.assertIsNotNone(print_tpl_mds_key, 'MDS key is to be None')

        # удаляем pdf из MDS, чтобы не забивать квоту
        with self.session.begin():
            wiki_handler.erase_print_form()

            print_tpl_mds_key = getattr(col0, 'print_tpl_mds_key', None)
            self.assertIsNone(print_tpl_mds_key, 'MDS key is to be None')
