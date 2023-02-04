# -*- coding: utf-8 -*-

import btestlib.utils as utils
import btestlib.reporter as reporter
import balance.balance_db as db
import btestlib.pagediff as pagediff
import balance.balance_web as web
import filters


def get_invoice_dynamic(invoice_id):
    with reporter.step(u'Получаем меняющиеся элементы в счете: {}'.format(invoice_id)):
        query = '''select
                i.id, i.external_id, i.person_id, i.paysys_id, i.request_id, i.contract_id, cnt.external_id as contract_eid, 
                i.client_id, c.name, io.order_id, o.service_order_id, io.text, o.service_id,
                bd.account, bd.bankaccount, bd.bank||bd.bankaddress as address, bd.bankcode
                from t_invoice i
                join t_client c on i.client_id = c.id
                left join t_invoice_order io on i.id = io.invoice_id
                left join t_order o on io.order_id = o.id
                left join t_contract2 cnt on i.contract_id = cnt.id
                left join t_bank_details bd on i.bank_id = bd.bank_id and i.currency = bd.currency
                where i.id =:invoice_id'''
        params = {'invoice_id': invoice_id}
        result = db.balance().execute(query, params)
        result_final = []
        for string in result:
            string = utils.remove_false(string)
            for name, value in string.iteritems():
                result_final.append(value) if value not in result_final else None

        return result_final


def check_publish_page(unique_name, invoice_id, invoice_eid=None, contract_eid=None):
    page_html = pagediff.get_page(
        url=web.ClientInterface.InvoicePublishPage.url(invoice_id=invoice_id),
        prepare_page=lambda driver_, url_: web.ClientInterface.InvoicePublishPage.open_and_wait(driver_, invoice_id))
    pagediff.pagediff(unique_name=unique_name, page_html=page_html,
                      filters=filters.invoice_publish_filters(dynamic_values=get_invoice_dynamic(invoice_id),
                                                              external_invoice_id=invoice_eid,
                                                              external_contract_id=contract_eid))


def check_invoice_page_ci(unique_name, invoice_id):
    page_html = pagediff.get_page(url=web.ClientInterface.InvoicePage.url(invoice_id=invoice_id),
                                  prepare_page=lambda driver_, url_: web.ClientInterface.InvoicePage.open_url(driver_,
                                                                                                              url_))
    pagediff.pagediff(unique_name=unique_name,
                      page_html=page_html,
                      filters=filters.invoice_ci_filters(
                          dynamic_values=get_invoice_dynamic(invoice_id)))


def check_paypreview_page(unique_name, request_data):
    page_html = pagediff.get_page(url=web.ClientInterface.PaypreviewPage.url(**request_data))
    pagediff.pagediff(unique_name=unique_name, page_html=page_html,
                      filters=filters.paypreview_filters(
                          dynamic_values=get_invoice_dynamic(request_data['invoice_id'])))
