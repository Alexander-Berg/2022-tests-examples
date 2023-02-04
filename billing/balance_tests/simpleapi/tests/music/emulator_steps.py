import time

from appstore_cli import AppStoreClient
from simpleapi.common import logger
from simpleapi.data.defaults import Music

__author__ = 'fellow'

log = logger.get_logger()

USER = {'_id': 'f6c5626e-23c6-46d0-a0c0-2c83269fc254'}

STORE_URL = 'https://trust-dev.paysys.yandex.net:8020/store/api/v1/'

iface = AppStoreClient(STORE_URL)


def crete_user(user):
    """Registers user.
    """
    log.debug('Register InApp user')
    resp = iface.register_user(user)
    assert 'value' in resp and resp['value'] == 'Registered$', \
        'Error while create user, response: %s' % resp


def subscribe(user, period, subscription_plan, created_product_id=None):
    """Creates InApp autorenewable subscription with @period sec period.
    and subscribes @user according to @subscription_plan.
    """
    log.debug('Creates InApp autorenewable subscription')
    product_id = None
    if not created_product_id:
        product_id = Music.INAPP_TMPL['product_id'] % str(time.time())
        inapp = dict(Music.INAPP_TMPL)
        inapp['product_id'] = product_id
        inapp['period'] = period
        iface.create_inapp(inapp)
    else:
        product_id = created_product_id
    iface.subscribe(user['_id'], product_id, subscription_plan)
    return product_id


def restore(user, product_id):
    log.debug('Restore InApp subscription')
    return iface.restore(user['_id'], product_id)


def verify(receipt):
    log.debug('Verify InApp subscription')
    return iface.verify(receipt)


def full_cycle(period, subs_plan):
    crete_user(Music.USER)
    product_id = subscribe(Music.USER, period, subs_plan)
    receipt = restore(Music.USER, product_id)['receipts'][0]
    verify(receipt)
    return receipt['transaction_id'], receipt, product_id
