# -*- coding: utf-8 -*-
import calendar
import datetime
import pickle

import mock
import pytest
from dateutil.relativedelta import relativedelta

import tests.object_builder as ob
from balance import contract_notify_email
from balance import mapper, message_generator
from balance.application import getApplication
from balance.constants import FirmId
from balance.contractpage import ContractPage
from balance.processors import contract_notify2

DISTRIB_NET_COL_TYPE_ID = 3090


class FakeMailGen(message_generator.MailGen):
    recipients = set()
    emails = []

    def generate(self, tmpl_name, rcpt, **kwargs):
        recipient = tmpl_name.split('/')[2]
        FakeMailGen.recipients.add(recipient)
        email = super(FakeMailGen, self).generate(tmpl_name, rcpt, **kwargs)
        FakeMailGen.emails.append(email)
        return email


@mock.patch('balance.processors.contract_notify2.MailGen', FakeMailGen)
class TestContractNotifier(object):
    """
    wiki.yandex-team.ru/balance/tz/notificationsandcontractblockingrules/
    """

    @pytest.fixture()
    def contract_notifier_setup(self, session):
        client = ob.ClientBuilder()
        person = ob.PersonBuilder(type="pu", client=client)
        manager = ob.ManagerWithChiefsBuilder().build(session).obj

        return client, person, manager

    def _test_contract(
        self, session, client, person, manager,
        required_recipients, dt,
    ):
        FakeMailGen.recipients = set()

        contract = ob.ContractBuilder(
            manager_code=manager.manager_code, person=person,
            dt=dt, commission=1, client=client, firm=FirmId.YANDEX_OOO, is_signed=None
        ).build(session).obj
        contract.col0.services = {61: True}
        # дальше вызывается хэндлер, который обрабатывает проставленные в экспорт контракты
        # следовательно нужно экспортировать контракт
        # В проде простановка происходит через enqueue_batch,
        # в cluster_tools.contract_notify2.enqueue, который игнорирует ограничение на очереди
        # для контрактов с фирмами (balance.mapper.contracts.Contract.exportable)
        # обычный enqueue такое ограничение не игнорирует, поэтому форсируем простановку в очередь
        contract.enqueue('CONTRACT_NOTIFY', force=True)
        contract_notify2.handle_contract(contract, None)

        assert FakeMailGen.recipients == set(required_recipients)

        FakeMailGen.recipients = set()
        contract_notify2.handle_contract(
            contract,
            contract.exports['CONTRACT_NOTIFY'].input
        )
        assert FakeMailGen.recipients == set([])

        return contract

    def test_suspension_of_faxed_contract(
        self, contract_notifier_setup, session,
    ):
        """
        Проверяет, что по прошествии 3-х месяцев подписанный по факсу договор
        будет приостановлен, и будут отправлены письма менеджеру и клиенту.
        """
        client, person, manager = contract_notifier_setup
        dt = session.now() - relativedelta(months=3)
        is_faxed = session.now() - relativedelta(months=3)
        contract = ob.ContractBuilder(
            manager_code=manager.manager_code, person=person, dt=dt,
            commission=1, is_faxed=is_faxed, client=client, is_signed=None
        ).build(session).obj

        # дальше вызывается хэндлер, который обрабатывает проставленные в экспорт контракты
        # следовательно нужно экспортировать контракт
        contract.enqueue('CONTRACT_NOTIFY')
        contract.col0.services = {61: True}
        FakeMailGen.recipients = set()

        contract_notify2.handle_contract(contract, None)

        assert FakeMailGen.recipients == {"manager", "client"}
        assert hasattr(contract.col0, "is_suspended")

    def test_unsigned_contract(
        self, session, contract_notifier_setup,
    ):
        client, person, manager = contract_notifier_setup

        dt = session.now() - relativedelta(months=2)
        recipients = ['manager']
        self._test_contract(
            session, client, person, manager,
            recipients, dt,
        )

        dt = session.now() - relativedelta(months=2, weeks=3)
        recipients = ['manager', 'chief']
        self._test_contract(
            session, client, person, manager,
            recipients, dt,
        )

        dt = session.now() - relativedelta(months=3)
        recipients = ['manager']
        contract = self._test_contract(
            session, client, person, manager,
            recipients, dt,
        )
        assert hasattr(contract.col0, 'is_cancelled')

    def test_distribution(
        self, session, contract_notifier_setup,
    ):
        """
        https://st.yandex-team.ru/BALANCE-13234
        """
        client, person, manager = contract_notifier_setup

        last_day_of_month = calendar.monthrange(
            session.now().year,
            session.now().month,
        )[1]
        days_till_month_end = last_day_of_month - session.now().day

        contract_notify_email.DISTRIB3090_DELTA = datetime.timedelta(
            days=days_till_month_end
        )

        FakeMailGen.recipients = set()

        from billing.contract_iface.cmeta import distribution
        collateral_type = distribution.collateral_types[DISTRIB_NET_COL_TYPE_ID]

        dt = session.now() - relativedelta(months=3)
        is_faxed = session.now() - relativedelta(months=3)

        contract = ob.ContractBuilder(
            manager_code=manager.manager_code, person=person,
            ctype='DISTRIBUTION', collateral_type=collateral_type,
            dt=dt, is_faxed=is_faxed, client=client
        ).build(session).obj
        # дальше вызывается хэндлер, который обрабатывает проставленные в экспорт контракты
        # следовательно нужно экспортировать контракт
        contract.enqueue('CONTRACT_NOTIFY')

        contract.col0.services = {61: True}

        ob.CollateralBuilder(
            contract=contract, collateral_type=collateral_type, dt=dt,
            is_faxed=is_faxed, is_booked=1, is_booked_dt=is_faxed,
        ).build(session)

        contract_notify2.handle_contract(contract, None)

        if session.now().day == last_day_of_month:
            assert FakeMailGen.recipients == {'recall.mako'}
        else:
            assert FakeMailGen.recipients == {'notify.mako'}

        FakeMailGen.recipients = set()
        contract_notify2.handle_contract(
            contract,
            contract.exports['CONTRACT_NOTIFY'].input,
        )
        assert FakeMailGen.recipients == set([])

    def test_get_finish_dt(self, session):
        finish_dt = session.now() + relativedelta(months=3)
        c = ob.ContractBuilder(
            commission=0, finish_dt=session.now()
        ).build(session).obj

        from billing.contract_iface.cmeta import general
        collateral_type = general.collateral_types[1006]
        col = ob.CollateralBuilder(
            contract=c,
            collateral_type=collateral_type,
            finish_dt=finish_dt,
        ).build(session).obj

        assert contract_notify_email.get_finish_dt(c) == col.finish_dt

    def test_manager_not_found(self, session, contract_notifier_setup):
        """
        Проверяет, что при отсутствии менеджера у договора письмо
        будет сгенерировано и отправлено на специальную рассылку.
        """
        client, person, manager = contract_notifier_setup

        email_for_errors = session.config.CONTRACT_NOTIFY_EMAIL_FOR_ERRORS
        mail_gen = FakeMailGen(getApplication().mako_renderer)
        FakeMailGen.emails = []
        cn = contract_notify_email.ContractNotifier(
            session, mail_gen, email_for_errors
        )

        contract = ob.ContractBuilder(
            person=person, client=client, manager_code=-1,
            commission=1, is_booked=1, firm=FirmId.YANDEX_OOO,
            is_booked_dt=session.now()
        ).build(session).obj
        contract.col0.services = {61: True}
        tmpl_name = "contract_notify/booked/manager/notify_contract.mako"
        cn._send_email(contract, contract.col0, tmpl_name, "manager")

        assert FakeMailGen.emails[0].recepient_address == email_for_errors

    def test_repeated_suspension(
        self, session, contract_notifier_setup,
    ):
        """
        После того, как флаг приостановки был снят, договор должен попасть в очередь
        и снова приостановиться.
        """
        client, person, manager = contract_notifier_setup

        dt = session.now() - relativedelta(months=3)
        is_faxed = session.now() - relativedelta(months=3)
        contract = ob.ContractBuilder(
            manager_code=manager.manager_code, person=person, dt=dt,
            commission=1, is_faxed=is_faxed, client=client, is_signed=None
        ).build(session).obj

        # дальше вызывается хэндлер, который обрабатывает проставленные в экспорт контракты
        # следовательно нужно экспортировать контракт
        contract.enqueue('CONTRACT_NOTIFY')
        contract.col0.services = {61: True}
        FakeMailGen.recipients = set()

        assert not contract.suspended

        contract_notify2.handle_contract(
            contract,
            contract.exports['CONTRACT_NOTIFY'].input,
        )

        assert FakeMailGen.recipients == {"manager", "client"}
        assert contract.suspended

        contract.col0.is_suspended = None
        assert not contract.suspended

        contract_notify2.handle_contract(
            contract,
            contract.exports['CONTRACT_NOTIFY'].input,
        )
        assert contract.suspended


class TestContractIntercompanyPartnerCredit(object):
    TEST_EMAIL_ADDRESS = 'test_email@email_test.test'

    @pytest.fixture
    def intercompany(self, session):
        obj = session.query(mapper.Intercompany).first()
        return obj

    @pytest.fixture
    def contract(self, session, intercompany):
        client = ob.ClientBuilder(intercompany=intercompany.flex_value)
        person = ob.PersonBuilder(type="ur", client=client)
        manager = ob.ManagerWithChiefsBuilder().build(session).obj

        contract = ob.ContractBuilder(
            client=client,
            person=person,
            manager_code=manager.manager_code,
            dt=datetime.datetime.now(),
            commission=1,
            firm=FirmId.YANDEX_OOO,
            services=[7],
            partner_credit=1
        ).build(session).obj

        return contract

    @pytest.fixture(autouse=True)
    def config(self, session):
        session.config.__dict__['CONTRACT_NOTIFY_PARTNER_CREDIT'] = [self.TEST_EMAIL_ADDRESS]

    def test(self, session, contract, intercompany):
        page = ContractPage(session, contract.id)
        page._previous_partner_credit = False
        page._previous_suspended = False
        page.notify_of_contract_change(contract)
        session.flush()

        email_message = session.query(mapper.EmailMessage).getone(recepient_address=self.TEST_EMAIL_ADDRESS)
        subject, body = pickle.loads(email_message.data)
        assert email_message.opcode == 10
        assert contract.external_id in subject
        assert contract.external_id in body
        assert str(contract.client_id) in body
        assert intercompany.description in body
