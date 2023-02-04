# coding: utf-8

import btestlib.reporter as reporter
from btestlib import environments as env
from btestlib.constants import Regions, Domains

__author__ = 'torvald'

# TODO: we will specify domain later (while login) - here we duplicate (by hardcode) this logic. Good or bad?
URL_MAP = {Regions.RU.id: {'domain': Domains.RU, 'url': '{0}/invoice.xml?invoice_id={1}'},
           Regions.TR.id: {'domain': Domains.COM_TR, 'url': '{0}/invoice.xml?invoice_id={1}&LANG=ru'}
           }


def pay_by(payment_method, service, *args, **kwargs):
    region_id = kwargs.get('region_id')
    invoice_id = kwargs.get('invoice_id')

    # Получаем тербуемый домен
    requested_domain = URL_MAP[region_id]['domain']

    # Получаем текущий урл клиентского интерфейса
    base_url = env.balance_env().balance_ci

    # Отрываем от урла текущий домен
    url_parts = base_url.split('.')[:-1]
    url_parts.extend([requested_domain])

    # Собираем урл заново с требуемым доменом
    url = '.'.join(url_parts)

    payment_url = URL_MAP[region_id]['url'].format(url, invoice_id)
    with reporter.step(u'Совершаем оплату счета. Способ оплаты {}'.format(payment_method)):
        payment_method.pay(service=service, payment_url=payment_url, *args, **kwargs)
