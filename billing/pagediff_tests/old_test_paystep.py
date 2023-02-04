# coding: utf-8

import datetime

import pytest

import balance.balance_web as web
import btestlib.pagediff as pagediff
import btestlib.reporter as reporter
import filters
from balance.pagediff_tests.data_preparator import prepare_invoice, invoice_dynamic_values

params = [
    {'print_form': 3, 'paysys_id': 1003, 'person_type': 'ur',
     'date': datetime.datetime(2011, 1, 23), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 7, 'paysys_id': 1013, 'person_type': 'yt',
     'date': datetime.datetime(2016, 5, 23), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 8, 'paysys_id': 1003, 'person_type': 'ur',
     'date': datetime.datetime(2011, 1, 24), 'servics_products': ['7-1475'], 'currency': None},
    {'print_form': 9, 'paysys_id': 1017, 'person_type': 'ua',
     'date': datetime.datetime(2012, 9, 9), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 9, 'paysys_id': 1018, 'person_type': 'pu',
     'date': datetime.datetime(2012, 9, 9), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 10, 'paysys_id': 1001, 'person_type': 'ph',
     'date': datetime.datetime(2011, 9, 15), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 10, 'paysys_id': 1003, 'person_type': 'ur',
     'date': datetime.datetime(2011, 9, 15), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 11, 'paysys_id': 1001, 'person_type': 'ph',
     'date': datetime.datetime(2011, 9, 15), 'services_products': ['5-1475'], 'currency': None},
    {'print_form': 11, 'paysys_id': 1002, 'person_type': 'ph',
     'date': datetime.datetime(2011, 9, 15), 'services_products': ['6-1475'], 'currency': None},
    {'print_form': 11, 'paysys_id': 1003, 'person_type': 'ur',
     'date': datetime.datetime(2011, 9, 15), 'services_products': ['5-1475'], 'currency': None},
    {'print_form': 12, 'paysys_id': 1003, 'person_type': 'ur',
     'date': datetime.datetime.now(), 'services_products': ['114-502981'], 'currency': None},
    {'print_form': 12, 'paysys_id': 1014, 'person_type': 'yt',
     'date': datetime.datetime.now(), 'services_products': ['114-502981'], 'currency': None},
    {'print_form': 14, 'paysys_id': 1043, 'person_type': 'sw_ur',
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 14, 'paysys_id': 1068, 'person_type': 'sw_ph',
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 15, 'paysys_id': 1047, 'person_type': 'sw_yt',
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 15, 'paysys_id': 1071, 'person_type': 'sw_ytph',
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 16, 'paysys_id': 1028, 'person_type': 'usu',
     'date': datetime.datetime(2013, 8, 26), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 16, 'paysys_id': 1029, 'person_type': 'usp',
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 17, 'paysys_id': 1050, 'person_type': 'tru',
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 17, 'paysys_id': 1051, 'person_type': 'trp',
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 18, 'paysys_id': 1017, 'person_type': 'ua',
     'date': datetime.datetime(2012, 9, 10), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 18, 'paysys_id': 1018, 'person_type': 'pu',
     'date': datetime.datetime(2012, 9, 10), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 21, 'paysys_id': 1001, 'person_type': 'ph',
     'date': datetime.datetime.now(), 'services_products': ['48-503363'], 'currency': None},
    {'print_form': 21, 'paysys_id': 1003, 'person_type': 'ur',
     'date': datetime.datetime.now(), 'services_products': ['48-503363'], 'currency': None},
    {'print_form': 22, 'paysys_id': 1003, 'person_type': 'ur',
     'date': datetime.datetime(2013, 8, 25), 'services_products': ['7-503162'], 'currency': 'RUB'},
    {'print_form': 24, 'paysys_id': 1028, 'person_type': 'usu',
     'date': datetime.datetime(2013, 8, 25), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 24, 'paysys_id': 1029, 'person_type': 'usp',
     'date': datetime.datetime(2013, 8, 25), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 28, 'paysys_id': 1201001, 'person_type': 'ph',
     'date': datetime.datetime(2015, 7, 30), 'services_products': ['98-505057'], 'currency': None},
    {'print_form': 28, 'paysys_id': 1201033, 'person_type': 'ur',
     'date': datetime.datetime(2015, 7, 30), 'services_products': ['98-505057'], 'currency': None},
    {'print_form': 32, 'paysys_id': 1201002, 'person_type': 'ph',
     'date': datetime.datetime.now(), 'services_products': ['98-506624'], 'currency': None},
    {'print_form': 32, 'paysys_id': 1201003, 'person_type': 'ur',
     'date': datetime.datetime.now(), 'services_products': ['98-506624'], 'currency': None},
    {'print_form': 33, 'paysys_id': 1201003, 'person_type': 'ur',
     'date': datetime.datetime.now(), 'services_products': ['98-505057'], 'currency': None},
    {'print_form': 34, 'paysys_id': 1201003, 'person_type': 'ur',
     'date': datetime.datetime.now(), 'services_products': ['90-506655'], 'currency': None},
    {'print_form': 35, 'paysys_id': 1201003, 'person_type': 'ur',
     'date': datetime.datetime.now(), 'services_products': ['82-507211'], 'currency': None},
    {'print_form': 36, 'paysys_id': 1014, 'person_type': 'yt',
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 38, 'paysys_id': 1003, 'person_type': 'ur',
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 39, 'paysys_id': 11101014, 'person_type': 'yt',
     'date': datetime.datetime.now(), 'services_products': ['11-2136'], 'currency': None},
    {'print_form': 40, 'paysys_id': 11101023, 'person_type': 'yt',
     'date': datetime.datetime.now(), 'services_products': ['11-2136'], 'currency': None},
    {'print_form': 40, 'paysys_id': 11101013, 'person_type': 'yt',
     'date': datetime.datetime.now(), 'services_products': ['11-2136'], 'currency': None},
    {'print_form': 41, 'paysys_id': 11101001, 'person_type': 'ph',
     'date': datetime.datetime.now(), 'services_products': ['11-2136'], 'currency': None},
    {'print_form': 41, 'paysys_id': 11101003, 'person_type': 'ur',
     'date': datetime.datetime.now(), 'services_products': ['11-2136'], 'currency': None},
    {'print_form': 42, 'paysys_id': 11101060, 'person_type': 'yt_kzu',
     'date': datetime.datetime.now(), 'services_products': ['11-2136'], 'currency': None},
    {'print_form': 43, 'paysys_id': 11101003, 'person_type': 'ur',
     'date': datetime.datetime.now(), 'services_products': ['11-506537'], 'currency': None},
    {'print_form': 43, 'paysys_id': 11101001, 'person_type': 'ph',
     'date': datetime.datetime.now(), 'services_products': ['11-506525'], 'currency': None},
    {'print_form': 44, 'paysys_id': 11101014, 'person_type': 'yt',
     'date': datetime.datetime.now(), 'services_products': ['11-506525'], 'currency': None},
    {'print_form': 45, 'paysys_id': 1013, 'person_type': 'yt',  ##BALANCE-24428
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None},
    {'print_form': 52, 'paysys_id': 1075, 'person_type': 'by_ytph',  ##BALANCE-24437
     'date': datetime.datetime.now(), 'services_products': ['7-1475'], 'currency': None}
]


@pytest.mark.parametrize('casedata', params,
                         ids=lambda x: "pf-{}_paysys-{}".format(x['print_form'], x['paysys_id']))
def old_test_invoice_page_ci(casedata):
    invoice_data = prepare_invoice(**casedata)
    unique_name = "invoice_ci_pf-{}_paysys-{}".format(casedata['print_form'], casedata['paysys_id'])
    page_html = pagediff.get_page(
        url=web.ClientInterface.InvoicePage.url(invoice_id=invoice_data['invoice_id']),
        prepare_page=lambda driver_, url_: web.ClientInterface.InvoicePage.open_url(driver_, url_))
    pagediff.pagediff(unique_name=unique_name,
                      page_html=page_html,
                      filters=filters.invoice_ci_filters(dynamic_values=invoice_dynamic_values(invoice_data)))


@pytest.mark.parametrize('casedata', params,
                         ids=lambda x: "pf-{}_paysys-{}".format(x['print_form'], x['paysys_id']))
def old_test_invoice_publish_page(casedata):
    if casedata['print_form'] != 52:
        invoice_data = prepare_invoice(**casedata)
        unique_name = "invoice_publish_pf-{}_paysys-{}".format(casedata['print_form'], casedata['paysys_id'])
        page_html = pagediff.get_page(
            url=web.ClientInterface.InvoicePublishPage.url(invoice_id=invoice_data['invoice_id']),
            prepare_page=lambda driver_, url_: web.ClientInterface.InvoicePublishPage.open_and_wait(driver_,
                                                                                                    invoice_data[
                                                                                                        'invoice_id']))
        pagediff.pagediff(unique_name=unique_name,
                          page_html=page_html,
                          filters=filters.invoice_publish_filters(dynamic_values=invoice_dynamic_values(invoice_data)))
    else:
        reporter.log('This invoice has no print form')


@pytest.mark.parametrize('casedata', params,
                         ids=lambda x: "pf-{}_paysys-{}".format(x['print_form'], x['paysys_id']))
def test_paypreview_page(casedata):
    invoice_data = prepare_invoice(**casedata)
    unique_name = "paypreview_pf-{}_paysys-{}".format(casedata['print_form'], casedata['paysys_id'])
    page_html = pagediff.get_page(url=web.ClientInterface.PaypreviewPage.url(**invoice_data))
    pagediff.pagediff(unique_name=unique_name,
                      page_html=page_html,
                      filters=filters.paypreview_filters(dynamic_values=invoice_dynamic_values(invoice_data)))
