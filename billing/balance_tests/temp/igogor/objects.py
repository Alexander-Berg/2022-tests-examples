# coding: utf-8
__author__ = 'igogor'

import collections
import datetime

import btestlib.data.person_defaults as person_defaults
import btestlib.utils as utils


class BalanceBase(collections.MutableMapping):
    """A dictionary that applies an arbitrary key-altering
       function before accessing the keys"""

    def __init__(self, *args, **kwargs):
        self.store = dict()
        self.update(dict(*args, **kwargs))  # use the free update to set keys

    def __getitem__(self, key):
        return self.store[key]

    def __setitem__(self, key, value):
        self.store[key] = value

    def __delitem__(self, key):
        del self.store[key]

    def __iter__(self):
        return iter(self.store)

    def __len__(self):
        return len(self.store)

    def __contains__(self, item):
        return self.store.__contains__(item)

    def __repr__(self):
        return repr(self.__dict__)

    def print_repr(self):
        raise NotImplementedError


class StoreProperty(object):
    def __init__(self, key):
        self.key = key

    def __get__(self, instance, owner=None):
        return instance.store.get(self.key, None)

    def __set__(self, instance, value):
        instance.store[self.key] = value

    def __delete__(self, instance):
        del instance.store[self.key]


class Client(BalanceBase):
    @classmethod
    def default(cls, client_id=None):
        return Client(utils.remove_empty({'CLIENT_ID': client_id,  # int
                                          'CLIENT_TYPE_ID': None,  # int constants.ClientTypes.PHYS.id
                                          'NAME': 'balance_test {}'.format(datetime.datetime.now()),
                                          'EMAIL': 'client@in-fo.ru',
                                          'PHONE': '911',
                                          'FAX': '912',
                                          'URL': 'http://client.info/',
                                          'CITY': 'Butt',
                                          'IS_AGENCY': None,  # 0 или 1
                                          'AGENCY_ID': None,  # int
                                          'REGION_ID': None,  # int constants.Regions.RUSSIA.id
                                          'SERVICE_ID': None,  # int constants.Services.DIRECT.id
                                          'CURRENCY': None,  # int constants.Currencies.RUB.iso_code
                                          'MIGRATE_TO_CURRENCY': None,  # datetime
                                          'CURRENCY_CONVERT_TYPE': None,  # 'COPY' или 'MODIFY'
                                          'ONLY_MANUAL_NAME_UPDATE': None  # bool
                                          }))

    id = StoreProperty('CLIENT_ID')

    def modify(self, client_id):
        self.update(utils.remove_empty({'CLIENT_ID': client_id}))
        return self

    def print_repr(self):
        label = 'agency_id' if self.get('IS_AGENCY', 0) else 'client_id'
        return utils.columns(left='{}:{}'.format(label, self.id), right=str(self.store))


class Order(BalanceBase):
    @classmethod
    def default(cls, client_id, service_id=7, product_id=1475):
        return Order(utils.remove_empty({'ClientID': client_id,
                                         'ProductID': product_id,
                                         'ServiceID': service_id,
                                         'ServiceOrderID': None,
                                         'AgencyID': None,
                                         'ManagerUID': None,
                                         'RegionID': None,
                                         'discard_agency_discount': None,
                                         'TEXT': 'Py_Test order',
                                         'GroupServiceOrderID': -1,
                                         'GroupWithoutTransfer': 1
                                         }))

    id = StoreProperty('OrderID')
    service_order_id = StoreProperty('ServiceOrderID')
    service_id = StoreProperty('ServiceID')

    def modify(self, order_id=None, service_order_id=None):
        self.update(utils.remove_empty({'OrderID': order_id,
                                        'ServiceOrderID': service_order_id}))
        return self

    @property
    def url(self):
        # todo сделать получение урла с учетом среды
        return "https://balance-admin.greed-tm1f.yandex.ru/order.xml?order_id={0}".format(self.id)

    def print_repr(self):
        return utils.columns(left='order_id:{} service_id:{} service_order_id:{}'
                             .format(self.id, self.service_id, self.service_order_id),
                             right='order_url:{} {}'.format(self.url, str(self.store)))


class Campaign(BalanceBase):
    service_id = StoreProperty('service_id')
    service_order_id = StoreProperty('service_order_id')

    def __init__(self, *args, **kwargs):
        super(Campaign, self).__init__(*args, **kwargs)
        self.dt = None

    @classmethod
    def default(cls, service_id, service_order_id, unit, qty, campaign_dt=None, do_stop=0):
        campaign = Campaign(utils.remove_empty({'service_id': service_id,
                                                'service_order_id': service_order_id,
                                                unit: qty,
                                                'do_stop': do_stop}))
        campaign.dt = campaign_dt
        return campaign

    @classmethod
    def from_order(cls, order, unit, qty, date=None, do_stop=0):
        return cls.default(order.service_id, order.service_order_id, unit, qty, date, do_stop)

    def print_repr(self):
        return utils.columns(left='do_campaigns: done}', right='{} campaigns_dt: {}'.format(str(self), self.dt))

    @property
    def unit(self):
        # todo не зря же говорят, что когда класс выполняет больше одной функции, то случается какая-то поебень
        units = filter(lambda x: x in ['Bucks', 'Shows', 'Clicks', 'Units', 'Days', 'Money'], self.keys())
        return units[0] if units else None

    @unit.setter
    def unit(self, unit):
        value = self.pop(self.unit, None)
        self[unit] = value

    @property
    def qty(self):
        return self.get(self.unit, None)

    @qty.setter
    def qty(self, qty):
        self[self.unit] = qty


class Temp(object):
    def __init__(self, x):
        self._x = x

    @property
    def x(self):
        return self._x

    @x.setter
    def x(self, value):
        self._x = value


class Line(BalanceBase):
    @classmethod
    def custom(cls, service_id, service_order_id, qty):
        return Line(utils.remove_empty({'ServiceID': service_id,
                                        'ServiceOrderID': service_order_id,
                                        'Qty': qty}))

    @classmethod
    def from_order(cls, order, qty):
        return cls.custom(service_id=order.service_id, service_order_id=order.service_order_id, qty=qty)


class Request(BalanceBase):
    def __init__(self, *args, **kwargs):
        super(Request, self).__init__(*args, **kwargs)
        self.id = None
        self.lines = None
        self.client_id = None
        self.invoice_dt = None

    @classmethod
    def default(cls, client_id, lines, invoice_dt=None, overdraft=None, promo_code=None, force_unmoderated=None,
                adjust_qty=None, return_path=None, qty_is_ammount=None):
        request = Request(utils.remove_empty({'Overdraft': overdraft,
                                              'PromoCode': promo_code,
                                              'ForceUnmoderated': force_unmoderated,
                                              'AdjustQty': adjust_qty,
                                              'ReturnPath': return_path,
                                              'QtyIsAmount': qty_is_ammount}))
        request.client_id = client_id
        request.lines = lines
        request.invoice_dt = invoice_dt
        return request

    @property
    def url(self):
        # todo сделать получение урла с учетом среды
        return "https://balance-admin.greed-tm1f.yandex.ru/paystep.xml?request_id={0}".format(self.id)

    def print_repr(self):
        return utils.columns(left='request_id: {}'.format(self.id),
                             right='url: {} owner: {} lines: {} additional: {}'.format(
                                 self.url, self.client_id, self.lines, self.store))


class Person(BalanceBase):
    @classmethod
    def default(cls, client_id, type_):
        person = Person({'client_id': client_id,
                         'type': type_
                         })
        if type_ not in ['endbuyer_ph', 'endbuyer_ur']:
            person.update({'fname': 'Test1',
                           'lname': 'Test2',
                           'mname': 'Test3'})
        person.update(person_defaults.get_details(type_))
        return person

    id = StoreProperty('person_id')
    client_id = StoreProperty('client_id')
    type = StoreProperty('type')

    def print_repr(self):
        return utils.columns(left='person_id: {}'.format(self.id),
                             right=str(self.store))


class Invoice(BalanceBase):
    def __init__(self, *args, **kwargs):
        super(Invoice, self).__init__(*args, **kwargs)
        self.endbuyer_id = None
        self.id = None
        self.external_id = None
        self.total_sum = None

    @classmethod
    def default(cls, request_id, person_id, paysys_id, contract_id=None, credit=0, overdraft=0, endbuyer_id=None,
                id_=None, external_id=None, total_sum=None):
        invoice = Invoice(utils.remove_empty({'RequestID': request_id,
                                              'PaysysID': paysys_id,
                                              'PersonID': person_id,
                                              'ContractID': contract_id,
                                              'Credit': credit,
                                              'Overdraft': overdraft}))
        invoice.endbuyer_id = endbuyer_id
        invoice.id = id_
        invoice.external_id = external_id
        invoice.total_sum = total_sum

        return invoice

    @property
    def url(self):
        # todo сделать получение урла с учетом среды
        return "https://balance-admin.greed-tm1f.yandex.ru/invoice.xml?invoice_id={0}".format(self.id)

    def print_repr(self):
        return utils.columns(left='invoice_id: {}'.format(self.id),
                             right='url: {} endbuyer_id: {} params: {}'.format(self.url, self.endbuyer_id, str(self)))
