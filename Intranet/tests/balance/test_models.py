# -*- coding: utf-8 -*-
import datetime

from django.utils import timezone
from django.test import TestCase
from django.conf import settings

from unittest.mock import patch, Mock, call

from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme.factories import SurveyFactory
from events.accounts.helpers import YandexClient
from events.balance.factories import TicketFactory, OrderFactory
from events.balance.models import BalanceOrder, Order
from events.balance import exceptions as balance_exceptions


class TestTicket(TestCase):
    fixtures = ['initial_data.json']

    def test_creating_ticket_process_should_send_request_to_balance(self):
        mocked_balance_client = Mock()
        tickets_data = [
            {'currency': 'RUB', 'price': 1000, 'service_product_id': 1, 'name': 'Ticket 1'},
            {'currency': 'EUR', 'price': 5000, 'service_product_id': 2, 'name': 'Ticket 2'},
        ]
        for ticket_data in tickets_data:
            with patch('events.balance.signals.balance_utils.get_balance_client_for_currency', Mock(return_value=mocked_balance_client)):
                ticket = TicketFactory(
                    survey=SurveyFactory(),
                    price=ticket_data['price'],
                    currency=ticket_data['currency'],
                    name=ticket_data['name'],
                )
                ticket.save()
            exp_call_args = call(
                prices=[ticket_data['price']],
                currency=ticket_data['currency'],
                service_product_id=ticket_data['service_product_id'],
                name=ticket_data['name'],
                product_qty=None
            )
            self.assertEqual(mocked_balance_client.create_service_product.call_args, exp_call_args)


class TestBalanceOrder(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex()
        survey = SurveyFactory()
        self.ticket = TicketFactory(survey=survey, price=1000)
        self.balance_api_url = settings.BALANCE_API_URL

    def test__create_order_in_balance_should_return_service_order_id_if_request_is_with_success_status(self):
        expected_service_order_id = 999
        mocked_creating = Mock(return_value={'status': 'success', 'service_order_id': expected_service_order_id})
        with patch('events.balance.balance_client.BalanceClient.create_order_or_subscription', mocked_creating):
            result = BalanceOrder._create_order_in_balance(self.profile.uid, '127.0.0.1', self.ticket)
        self.assertEqual(result, expected_service_order_id)

    def test_create_should_not_create_balance_order_if_request_to_balance_api_is_not_with_success_status(self):
        expected_service_order_id = 999
        mocked_creating = Mock(return_value={'status': 'error', 'service_order_id': expected_service_order_id})
        self.assertEqual(BalanceOrder.objects.count(), 0)
        with patch('events.balance.balance_client.BalanceClient.create_order_or_subscription', mocked_creating):
            try:
                BalanceOrder.create(
                    self.profile.uid,
                    '127.0.0.1',
                    self.ticket,
                    self.ticket.price,
                )
                raised = False
            except balance_exceptions.BalanceOrderCreatingError:
                raised = True

        msg = 'Если заказ не был успешно создан в Балансе - не нужно создавать его в базе'
        self.assertTrue(raised, msg=msg)
        self.assertEqual(BalanceOrder.objects.count(), 0, msg=msg)

    def test_create_should_create_balance_order_if_request_to_balance_api_with_success_status(self):
        expected_service_order_id = 999
        mocked_creating = Mock(return_value={'status': 'success', 'service_order_id': expected_service_order_id})
        self.assertEqual(BalanceOrder.objects.count(), 0)
        with patch('events.balance.balance_client.BalanceClient.create_order_or_subscription', mocked_creating):
            balance_order = BalanceOrder.create(
                self.profile.uid,
                '127.0.0.1',
                self.ticket,
                self.ticket.price,
            )

        msg = 'Если заказ был успешно создан в Балансе - нужно создать его в базе'
        self.assertEqual(BalanceOrder.objects.count(), 1, msg=msg)

        msg = 'Был записан неверный параметр price при создании заказа'
        self.assertEqual(balance_order.price, self.ticket.price, msg=msg)

        msg = 'Был записан неверный параметр service_order_id при создании заказа'
        self.assertEqual(balance_order.service_order_id, expected_service_order_id, msg=msg)

        msg = 'Был записан неверный параметр quantity при создании заказа'
        self.assertEqual(balance_order.quantity, 1, msg=msg)


@patch('events.balance.balance_client.xmlrpc.client', Mock(return_value=Mock()))
class TestOrder(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex()
        survey = SurveyFactory()
        self.profile_survey_answer = ProfileSurveyAnswerFactory(survey=survey, user=self.profile)
        self.ticket = TicketFactory(survey=survey, price=1000)
        self.balance_api_url = settings.BALANCE_API_URL

    def test__create_should_create_order_in_balance(self):
        expected_service_order_id = 999
        expected_trust_payment_id = 555
        mocked_creating_order = Mock(return_value={'status': 'success', 'service_order_id': expected_service_order_id})
        mocked_creating_basket = Mock(return_value={'status': 'success', 'trust_payment_id': expected_trust_payment_id})
        quantities = {1000: 1}
        with patch('events.balance.balance_client.BalanceClient.create_order_or_subscription', mocked_creating_order):
            with patch('events.balance.balance_client.BalanceClient.create_basket', mocked_creating_basket):
                Order.create(self.ticket, self.profile_survey_answer, quantities, '127.0.0.1')
        expected_call_args = call(
            user_ip='127.0.0.1',
            uid=self.profile.uid,
            payment_timeout=900,
            currency='RUB',
            orders=[{'fiscal_nds': 'nds_18', 'fiscal_title': '', 'service_order_id': 999, 'qty': 1}],
            return_path=None,
        )
        self.assertEqual(mocked_creating_basket.call_args, expected_call_args)

    def test__create_should_create_order_in_balance_with_euro_currency(self):
        expected_service_order_id = 999
        expected_trust_payment_id = 555
        mocked_creating_order = Mock(return_value={'status': 'success', 'service_order_id': expected_service_order_id})
        mocked_creating_basket = Mock(return_value={'status': 'success', 'trust_payment_id': expected_trust_payment_id})
        quantities = {1000: 1}
        with patch('events.balance.balance_client.BalanceClient.create_order_or_subscription', mocked_creating_order):
            with patch('events.balance.balance_client.BalanceClient.create_basket', mocked_creating_basket):
                self.ticket.currency = 'EUR'
                self.ticket.save()
                Order.create(self.ticket, self.profile_survey_answer, quantities, '127.0.0.1')
        expected_call_args = call(
            user_ip='127.0.0.1',
            uid=self.profile.uid,
            payment_timeout=900,
            currency='EUR',
            orders=[{'fiscal_nds': 'nds_18', 'fiscal_title': '', 'service_order_id': 999, 'qty': 1}],
            return_path=None,
        )
        self.assertEqual(mocked_creating_basket.call_args, expected_call_args)

    def test__create_should_return_order_instance(self):
        expected_service_order_id = 999
        expected_trust_payment_id = 555
        mocked_creating_order = Mock(return_value={'status': 'success', 'service_order_id': expected_service_order_id})
        mocked_creating_basket = Mock(return_value={'status': 'success', 'trust_payment_id': expected_trust_payment_id})
        quantities = {1000: 1}
        with patch('events.balance.balance_client.BalanceClient.create_order_or_subscription', mocked_creating_order):
            with patch('events.balance.balance_client.BalanceClient.create_basket', mocked_creating_basket):
                order = Order.create(self.ticket, self.profile_survey_answer, quantities, '127.0.0.1')

        msg = 'Был записан неверный trust_payment_id'
        self.assertEqual(order.trust_payment_id, expected_trust_payment_id, msg=msg)

        msg = 'Был записан неверный profile_survey_answer'
        self.assertEqual(order.profile_survey_answer, self.profile_survey_answer, msg=msg)

        msg = 'Был записан неверный profile_survey_answer'
        self.assertEqual(order.profile_survey_answer, self.profile_survey_answer, msg=msg)

        msg = 'Был записан неверный user_ip'
        self.assertEqual(order.user_ip, '127.0.0.1', msg=msg)

        msg = 'Был записан неверный ticket_info'
        self.assertEqual(order.ticket_info, self.ticket, msg=msg)

    def test__create_should_not_return_order_instance_if_creating_basket_in_balance_is_not_with_success_code(self):
        expected_service_order_id = 999
        expected_trust_payment_id = 555
        mocked_creating_order = Mock(return_value={'status': 'success', 'service_order_id': expected_service_order_id})
        mocked_creating_basket = Mock(return_value={'status': 'error', 'trust_payment_id': expected_trust_payment_id})
        quantities = {1000: 1}
        self.assertEqual(Order.objects.count(), 0)
        with patch('events.balance.balance_client.BalanceClient.create_order_or_subscription', mocked_creating_order):
            with patch('events.balance.balance_client.BalanceClient.create_basket', mocked_creating_basket):
                try:
                    Order.create(self.ticket, self.profile_survey_answer, quantities, '127.0.0.1')
                    raised = False
                except balance_exceptions.BalanceBasketCreatingError:
                    raised = True

        msg = 'Если создание корзины в балансе не удалось - не нужно создавать заказ в базе'
        self.assertTrue(raised, msg=msg)
        self.assertEqual(Order.objects.count(), 0, msg=msg)

    def test_check_status_should_save_status(self):
        expected_service_order_id = 999
        expected_trust_payment_id = 555
        mocked_creating_order = Mock(return_value={'status': 'success', 'service_order_id': expected_service_order_id})
        mocked_creating_basket = Mock(return_value={'status': 'success', 'trust_payment_id': expected_trust_payment_id})
        quantities = {1000: 1}
        with patch('events.balance.balance_client.BalanceClient.create_order_or_subscription', mocked_creating_order):
            with patch('events.balance.balance_client.BalanceClient.create_basket', mocked_creating_basket):
                order = Order.create(self.ticket, self.profile_survey_answer, quantities, '127.0.0.1')

        self.assertEqual(order.payment_status, '')
        self.assertEqual(order.payment_status_code, '')

        mocked_check_basket = Mock(return_value={'status': 'success', 'status_code': '200'})
        with patch('events.balance.balance_client.BalanceClient.check_basket', mocked_check_basket):
            order.check_status()

        fresh_order = Order.objects.get(pk=order.pk)
        msg = 'check_status должен обновлять статус заказа в базе'
        self.assertEqual(fresh_order.payment_status, 'success', msg=msg)
        self.assertEqual(fresh_order.payment_status_code, '200', msg=msg)

    def test_start_payment_process(self):
        order = OrderFactory(profile_survey_answer=self.profile_survey_answer)
        payment_start_date = timezone.now()
        mocked_pay_basket = Mock(return_value={'status': 'payment success', 'start_ts': datetime.datetime.isoformat(payment_start_date)})
        with patch('events.balance.balance_client.BalanceClient.pay_basket', mocked_pay_basket):
            order.start_payment_process()
        fresh_order = Order.objects.get(pk=order.pk)
        self.assertEqual(fresh_order.payment_status, 'payment success')
        self.assertEqual(fresh_order.payment_start_datetime, payment_start_date)
