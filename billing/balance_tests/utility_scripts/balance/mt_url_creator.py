# -*- coding: utf-8 -*-

import btestlib.reporter as reporter
from btestlib import environments as env
from btestlib.data import defaults


def url_formatter(data, type):
    data.update({'prefix': defaults.AUTO_PREFIX,
                 'env': env.balance_env().name,
                 'contract_id': data['contract_id'] if data.get('contract_id', None) is not None else ''})

    # Для каждого
    if type == 'paypreview':
        paypreview = "{prefix}https://balance.greed-{env}.yandex.ru/paypreview.xml?person_id={person_id}" \
                     "&request_id={request_id}&paysys_id={paysys_id}&contract_id={contract_id}&coupon=&mode=ci"
        return paypreview.format(**data)

    # Только для предоплатных счетов (credit = 0)
    if type == 'invoice_print_form':
        invoice_print_form = "{prefix}https://balance-admin.greed-{env}.yandex.ru/invoice-publish.xml?ft=html" \
                             "&object_id={invoice_id}"
        return invoice_print_form.format(**data)

    # Для каждого
    if type == 'invoice_ci':
        invoice_ci = "{prefix}https://balance.greed-{env}.yandex.ru/invoice.xml?invoice_id={invoice_id}"
        return invoice_ci.format(**data)

    # Toлько для постоплатных счетов (credit = 1)
    if type == 'act_print_form':
        act_print_form = "{prefix}https://balance-admin.greed-{env}.yandex.ru/invoice-publish.xml?ft=html&rt=act" \
                         "&object_id={act_id}"
        return act_print_form.format(**data)

    # Для каждого
    if type == 'act_ereport':
        act_ereport = "{prefix}https://balance-admin.greed-{env}.yandex.ru/invoice-publish.xml?ft=html&rt=erep" \
                      "&object_id={act_id}"
        return act_ereport.format(**data)


if __name__ == "__main__":
    from balance.tests.paystep.archive.SERVICES_AG_regression import test_servicesAG_toloka_regression as func

    data = func({'description': u'Russian region: 142734', 'id': '142734'})
    reporter.log(url_formatter(data, 'paypreview'))
    reporter.log(url_formatter(data, 'invoice_print_form'))
    reporter.log(url_formatter(data, 'invoice_ci'))
    reporter.log(url_formatter(data, 'act_print_form'))
    reporter.log(url_formatter(data, 'act_ereport'))
