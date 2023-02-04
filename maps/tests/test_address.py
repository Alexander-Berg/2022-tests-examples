from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.search.kind_pb2 import COUNTRY, PROVINCE, LOCALITY, STREET, HOUSE, ENTRANCE, LEVEL, APARTMENT
from yandex.maps.proto.search.address_pb2 import Address, Component

validator = Validator('search')


def test_toponym_address():
    a = Address(
        formatted_address="Россия, Москва, улица Льва Толстого, 7",
        postal_code="119021",
        country_code="RU",
        component=[
            Component(kind=[COUNTRY], name="Россия"),
            Component(kind=[PROVINCE], name="Центральный федеральный округ"),
            Component(kind=[PROVINCE], name="Москва"),
            Component(kind=[LOCALITY], name="Москва"),
            Component(kind=[STREET], name="улица Льва Толстого"),
            Component(kind=[HOUSE], name="7"),
            Component(kind=[ENTRANCE], name="2"),
            Component(kind=[LEVEL], name="8"),
            Component(kind=[APARTMENT], name="68")
        ]
    )

    validator.validate_example(a, 'address.toponym')


def test_business_address():
    a = Address(
        formatted_address="Россия, Москва, Большой Конюшковский переулок, 27А",
        postal_code="123242",
        country_code="RU",
        indoor_level_id="-1",
        component=[
            Component(kind=[COUNTRY], name="Россия"),
            Component(kind=[PROVINCE], name="Центральный федеральный округ"),
            Component(kind=[PROVINCE], name="Москва"),
            Component(kind=[LOCALITY], name="Москва"),
            Component(kind=[STREET], name="Большой Конюшковский переулок"),
            Component(kind=[HOUSE], name="27А"),
            Component(kind=[LEVEL], name="цокольный")
        ]
    )

    validator.validate_example(a, 'address.business')
