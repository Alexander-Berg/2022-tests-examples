# coding: utf-8

import balance.balance_db as db
from objects3 import *


def client_creation():
    default_client = Client.default()
    cusomized_default_client = Client.default(agency_id=222)
    client = Client(name='custom name')

    # client = steps.ClientSteps.create(client)

    db_params_dict = db.balance().execute('select * from t_client where id = 2234352')[0]
    client_from_db = Client.from_db(**db_params_dict)

    agency = client_from_db.agency

    api_params_dict = {'CLIENT_ID': 111,  # int
                       'CLIENT_TYPE_ID': None,  # int constants.ClientTypes.PHYS.id
                       'NAME': 'balance_test {}'.format(datetime.now()),
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
                       }
    client_from_api = Client.from_api(**api_params_dict)

    pass


def client_attributes():
    client = Client()

    id = client.id
    client.id = 667

    print client

    client.add_attribute(name='phone', value='666-55-55')
    phone = client.phone

    client.fax = '55543'
    fax = client.fax

    db_params = client.to_db()
    api_params = client.to_api()

    pass


def order_usage():
    order = Order()

    id = order.id
    order.id = 5454

    order2 = Order(id=111, client_id=222, service_id=7, service_order_id=333, product_id=444)

    db_params = order.to_db()
    api_params = order.to_api()
    dict = order.to_dict()

    order.product = Products.DIRECT_1475

    pass


def linked_object_usage():
    agency = Client(id=1232323, name='loshara agency')
    client = Client(id=12345, name="loshara", agency_id=agency.id)

    order = Order(id=2221, client_id=client.id)

    client2 = order.client

    client3 = Client()
    client3.agency = agency

    pass


def person_usage():
    person = Person.Ur()

    print utils.Presenter.pretty(Person.Ur.attributes())

    default = Person.Ur.default(email='custom@yandex.ru')

    print 'PersonBase: {} , Person.Ur: {}'.format(isinstance(default, Person.BasePerson),
                                                  isinstance(default, Person.Ur))

    print utils.Presenter.pretty(default.to_db())
    print utils.Presenter.pretty(default.to_api())

    pass


def attributes_inheritance_usage():
    @attributes()
    class A(ModelBase):
        primary = PrimaryAttribute()
        first = Attribute()
        second = Attribute()

    @attributes()
    class B(A):
        primary = PrimaryAttribute(apiname='renamed_primary')
        first = Attribute(apiname='renamed')
        third = Attribute()

    print utils.Presenter.pretty(A.attributes())
    print utils.Presenter.pretty(B.attributes())
    b = B()

    pass


def request_usage():
    client = Client(id=12345, name="loshara")

    order = Order(id=111, client_id=222, service_id=7, service_order_id=333, product_id=444)

    pass


def not_whole_object_is_parameter():
    pass


if __name__ == '__main__':
    # client_creation()
    # client_attributes()
    # order_usage()
    # linked_object_usage()
    # person_usage()
    # attributes_inheritance_usage()
    request_usage()
