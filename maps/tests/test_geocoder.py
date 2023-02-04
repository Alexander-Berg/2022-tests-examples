from maps.doc.proto.testhelper.common import add_metadata
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.search.geocoder_pb2 import GeoObjectMetadata, GEO_OBJECT_METADATA
from yandex.maps.proto.search.kind_pb2 import COUNTRY, PROVINCE, LOCALITY, STREET, HOUSE
from yandex.maps.proto.search.precision_pb2 import EXACT
from yandex.maps.proto.search.address_pb2 import Address, Component
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject

validator = Validator('search')


def geo_object(md):
    o = GeoObject()
    add_metadata(o.geo_object.add(), GEO_OBJECT_METADATA).CopyFrom(md)
    return o


def test_geocoder_metadata():
    md = GeoObjectMetadata(
        house_precision=EXACT,
        id="56696783",
        address=Address(
            formatted_address="Россия, Москва, улица Льва Толстого, 16",
            postal_code="119021",
            country_code="RU",
            component=[
                Component(kind=[COUNTRY], name="Россия"),
                Component(kind=[PROVINCE], name="Центральный федеральный округ"),
                Component(kind=[PROVINCE], name="Москва"),
                Component(kind=[LOCALITY], name="Москва"),
                Component(kind=[STREET], name="улица Льва Толстого"),
                Component(kind=[HOUSE], name="16")
            ]
        )
    )

    validator.validate_example(geo_object(md), 'geocoder')
