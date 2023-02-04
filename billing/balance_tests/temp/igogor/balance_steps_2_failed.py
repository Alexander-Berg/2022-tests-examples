# coding: utf-8
__author__ = 'igogor'

import datetime

import balance.balance_api as api
import balance.balance_db as db
import btestlib.data.defaults as defaults
import btestlib.utils as utils


class ClientSteps(object):
    @staticmethod
    def create(client, passport_uid=defaults.PASSPORT_UID):
        code, status, client_id = api.medium().create_client(passport_uid, client)
        client.id = client_id

        # log.print_object(client)

        return client


class OrderSteps(object):
    @staticmethod
    def create(order, passport_uid=defaults.PASSPORT_UID):
        if not order.service_order_id:
            order.modify(service_order_id=OrderSteps.next_service_order_id(service_id=order.service_id))

        answer = api.medium().create_order(passport_uid, [order.store])
        if answer[0][0] == api.SUCCESS:
            order.id = db.BalanceBO().get_order_id(service_id=order.service_id, service_order_id=order.service_order_id)
            # log.print_object(order)
            return order
        else:
            raise api.FailedXmlRpcCall(answer[0][1])

    @staticmethod
    def next_service_order_id(service_id):
        seq_name = {7: 'S_TEST_SERVICE_ORDER_ID_7',
                    116: 'S_TEST_SERVICE_ORDER_ID_116'}.get(service_id, 'S_TEST_SERVICE_ORDER_ID')

        service_order_id = db.BalanceBO().sequence_nextval(seq_name)
        return service_order_id

    @staticmethod
    def do_campaigns(campaign):
        """
        Sent fair campaigns to order
        """
        if campaign.dt:
            result = api.test_balance().campaigns_on_date(campaign.store, campaign.dt)
        else:
            result = api.test_balance().campaigns(campaign.store)
        # log.print_object(campaign)
        return result

    @staticmethod
    def add_campaigns(campaign):
        '''
        Sent some additional campaigns to order
        '''
        current_campaign = db.BalanceBO().execute(
            'select completion_qty from t_order'
            '  where service_id = :service_id and service_order_id = :service_order_id',
            {'service_id': campaign.service_id, 'service_order_id': campaign.service_order_id},
            single_row=True, fail_empty=True) \
            .get('completion_qty', None)
        if current_campaign:
            campaign.qty += current_campaign
        return OrderSteps.do_campaigns(campaign)


class RequestSteps(object):
    @staticmethod
    def create(request, passport_uid=defaults.PASSPORT_UID):
        # todo надо обеспечить наличие client с заданным id
        lines = [line.store for line in request.lines]  # todo xmlrpc не может работать с классом как со словарем. WHY??

        response = api.medium().create_request(passport_uid, request.client_id, lines, request.store)

        request.id = int(utils.get_url_parameter(url=response[3], param='request_id')[0])
        if request.invoice_dt:
            db.BalanceBO().execute(query='update T_REQUEST '
                                         'set INVOICE_DT = to_date(:invoice_dt,\'DD.MM.YYYY HH24:MI:SS\') '
                                         'where ID = :request_id',
                                   named_params={'request_id': request.id, 'invoice_dt': request.invoice_dt})

        # log.print_object(request)
        return request


class PersonSteps(object):
    @staticmethod
    def create(person, passport_uid=defaults.PASSPORT_UID):
        person.id = api.medium().create_person(passport_uid, person.store)
        # log.print_object(person)
        return person


class InvoiceSteps(object):
    @staticmethod
    def create(invoice, passport_uid=defaults.PASSPORT_UID):
        invoice.id = api.medium().create_invoice(passport_uid, invoice.store)
        if invoice.endbuyer_id:
            db.BalanceBO().insert_extprop(object_id=invoice.id, classname='\'Invoice\'', attrname='\'endbuyer_id\'',
                                          value_num=invoice.endbuyer_id, passport_uid=passport_uid)
        invoice_db_attrs = db.BalanceBO().execute(query='select external_id, total_sum, consume_sum '
                                                        ' from t_invoice '
                                                        ' where id = :invoice_id',
                                                  named_params={'invoice_id': invoice.id}, single_row=True)
        invoice.external_id = invoice_db_attrs['external_id']
        invoice.total_sum = invoice_db_attrs['total_sum']
        # log.print_object(invoice)
        return invoice

    @staticmethod
    def pay(invoice_id, payment_sum=None, payment_dt=None):
        invoice_db_attrs = db.BalanceBO().execute(query="select total_sum, external_id"
                                                        " from t_invoice"
                                                        " where id = :invoice_id",
                                                  named_params={'invoice_id': invoice_id}, single_row=True)

        if not payment_sum:
            payment_sum = invoice_db_attrs['total_sum']
        if not payment_dt:
            payment_dt = datetime.datetime.today()

        db.BalanceBO().insert(table='t_correction_payment',
                              params={'dt': "to_date(:payment_dt, 'DD.MM.YYYY HH24:MI:SS')",
                                      'doc_date': "to_date(:payment_dt, 'DD.MM.YYYY HH24:MI:SS')",
                                      'sum': payment_sum,
                                      'memo': "'Testing'",
                                      'invoice_eid': "'{}'".format(invoice_db_attrs['external_id'])},  # todo уродливо
                              named_params={'payment_dt': payment_dt})

        response = api.test_balance().oebs_payment(invoice_id)
        if response != 0:
            raise api.FailedXmlRpcCall('OEBSPayment failed')  # todo такие вещи лучше зашить в balance_api
            # log.print_str(utils.columns(left='oebs_payment: {} <- {}'.format(invoice_id, payment_sum),
            #                             right='payment_dt: {}'.format(payment_dt)))
