from datetime import datetime
from btestlib.utils import Date
from balance import balance_steps as steps
from balance.tests.integrations import utils
from decimal import Decimal as D

from btestlib.constants import TransactionType, PartnerPaymentType


def get_dates_for_acts(period):
    payment_dt = Date.first_day_of_month()

    if period.get('month'):
        payment_dt = datetime.strptime(period.get('month'), '%Y.%m')
    elif period.get('month_offset'):
        payment_dt = Date.shift_date(payment_dt, months=period.get('month_offset'))

    end_dt = Date.last_day_of_month(payment_dt)
    return payment_dt, end_dt


class PartnerCompletion(object):
    pass


class OebsCompletion(object):
    pass


# "tpt_rows" (in month_closing)
class TptRow(object):
    def __init__(self, tpt_row):
        self.amount = tpt_row['amount']
        self.transaction_type = tpt_row['transaction_type']


class InputPeriod(object):
    def __init__(self, period):
        self.payment_dt, self.end_dt = get_dates_for_acts(period)
        self.tpt_rows = period.get('tpt_rows', [])
        self.oebs_completions = period.get('oebs_completions', [])
        self.partner_completions = period.get('partner_completions', [])

    def build_tpt_rows(self, service_id, payment_type):
        rows = []
        for row in self.tpt_rows:
            rows += [
                {
                    'amount': row['amount'],
                    'transaction_type': (
                        TransactionType.PAYMENT if row['transaction_type'] == 'payment' else TransactionType.REFUND),
                    'internal': 1,
                    'service_id': service_id,
                    'payment_type': row.get('payment_type', payment_type),
                }
            ]
        return rows

    def build_tpt_rows_for_general(self, context, contract_id, client_id, sum_key):
        rows = []
        for row in self.tpt_rows:
            tpt_row = {
                'transaction_type': (
                    TransactionType.PAYMENT if row['transaction_type'] == 'payment' else TransactionType.REFUND),
                'service_id': context.service.id,
                sum_key: row['amount'],
            }
            if row.get('set_product_id'):
                main_product_id = utils.get_main_product_id_by_query(row.get('product_id'), context)
            else:
                main_product_id = None

            if row.get('set_invoice_eid'):
                invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code)
            else:
                invoice_eid = None

            tpt_row['invoice_eid'] = invoice_eid
            tpt_row['product_id'] = main_product_id

            if row.get('payment_type'):
                tpt_row['payment_type'] = row['payment_type']

            rows.append(tpt_row)
        return rows

    def build_oebs_completions(self, context, product_id, payment_dt):
        compls_dicts = []
        for completion in self.oebs_completions:
            compls_dicts += [
                {
                    'service_id': context.service.id,
                    'amount': D(completion['amount']),
                    'product_id': product_id,
                    'dt': payment_dt,
                    'transaction_dt': payment_dt,
                    'currency': context.currency.iso_code,
                    'accounting_period': payment_dt
                }
            ]
        return compls_dicts

    def build_partner_completions(self, context, client_id, payment_dt):
        for completion in self.partner_completions:
            # todo: understand what is type K50OrderType (GENERATOR, OPTIMIZATOR)
            steps.PartnerSteps.create_fake_product_completion(
                payment_dt,
                product_id=completion.get('product_id'),
                transaction_dt=payment_dt,
                client_id=client_id,
                service_id=context.service.id,
                service_order_id=0,
                amount=completion.get('amount'),
                type=completion.get('order_type'),
                transaction_type=TransactionType.PAYMENT.name,
                payment_type=PartnerPaymentType.WALLET,
                currency=context.currency.iso_code,
            )


class ResultPeriod(object):
    def __init__(self, period):
        self.payment_dt, self.end_dt = get_dates_for_acts(period)
        self.invoice = period['invoice']
        self.acts = period['acts']

    def build_expected_data_for_spendable(self, context, client_id, contract_id, page, payment_dt, end_dt):
        expected_data = []
        for expected_act_data in self.acts:
            if not expected_act_data:
                expected_data.append([])
            else:
                expected_data.append(
                    steps.CommonData.create_expected_pad(context, client_id, contract_id, payment_dt,
                                                         partner_reward=expected_act_data['amount'],
                                                         nds=context.nds,
                                                         description=page.desc,
                                                         page_id=page.id,
                                                         type_id=context.pad_type_id,
                                                         end_dt=end_dt)
                )
        return expected_data

    def build_expected_act_data(self, context, end_dt):
        expected_data = []

        for act_data in self.acts:
            if not act_data:
                expected_data.append([])
            else:
                expected_data.append(
                    steps.CommonData.create_expected_act_data(
                        act_data['amount'],
                        end_dt,
                        context=context
                    )
                )
        return expected_data

    def build_expected_invoice_data(self, context, contract_id, person_id, contract_start_dt):
        expected_invoice_data = []
        for invoice_data in self.invoice:
            if not invoice_data:
                expected_invoice_data.append([])
            else:
                expected_invoice_data.append(
                    steps.CommonData.create_expected_invoice_data_by_context(
                        context=context,
                        contract_id=contract_id,
                        person_id=person_id,
                        amount=invoice_data['amount'],
                        dt=contract_start_dt,
                    )
                )
        return expected_invoice_data
