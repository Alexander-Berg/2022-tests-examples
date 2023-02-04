# -*- coding: utf-8 -*-

import pytest
from xmlrpclib import Fault
from balance.mapper.common import Service
from balance.payments.terminals import Terminal

from tests.base import MediumTest
from tests import object_builder as ob


class TestGetContractInfoByTerminal(MediumTest):

    def setUp(self):
        super(TestGetContractInfoByTerminal, self).setUp()
        client = ob.ClientBuilder(is_agency=False)
        self.contract = ob.ContractBuilder(client=client).build(self.session).obj
        terminal_with_contract = self.session.query(
            Terminal).filter_by(id=96125001).first()
        terminal_with_contract.contract = self.contract
        terminal_with_no_contract = self.session.query(
            Terminal).filter_by(id=96125002).first()
        terminal_with_no_contract.contract = None

    def _get_service(self, session):
        return session.query(Service).filter_by(id=610).first()

    def test_Success(self):
        service = self._get_service(self.session)
        callhash = {'ServiceToken': service.token, 'TerminalID': 96125001}
        res = self.xmlrpcserver.GetContractInfoByTerminal(
            self.session.oper_id, callhash
        )
        self.assertEqual(res, {'external_id': self.contract.external_id})

    def test_contract_not_found(self):
        service = self._get_service(self.session)
        callhash = {'ServiceToken': service.token, 'TerminalID': 96125002}
        with pytest.raises(Fault):
            self.xmlrpcserver.GetContractInfoByTerminal(
                self.session.oper_id, callhash
            )

    def test_terminal_not_found(self):
        service = self._get_service(self.session)
        callhash = {'ServiceToken': service.token, 'TerminalID': 0}
        with pytest.raises(Fault):
            self.xmlrpcserver.GetContractInfoByTerminal(
                self.session.oper_id, callhash
            )

    def test_bad_service_token(self):
        callhash = {'ServiceToken': 'qwer', 'TerminalID': 96125001}
        with pytest.raises(Fault):
            self.xmlrpcserver.GetContractInfoByTerminal(
                self.session.oper_id, callhash
            )

    def test_no_service_token(self):
        callhash = {'TerminalID': 96125001}
        with pytest.raises(Fault):
            self.xmlrpcserver.GetContractInfoByTerminal(
                self.session.oper_id, callhash
            )

if __name__ == '__main__':
    import nose.core
    nose.core.runmodule()
