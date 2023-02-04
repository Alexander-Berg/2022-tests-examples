from yandex.maps.proto.common2 import (
    response_pb2,
    geometry_pb2
)
from yandex.maps.proto.search import (
    address_pb2,
    geocoder_pb2
)
from yandex.maps.proto.entrance import entrance_pb2

from maps.geoq.hypotheses.entrance.lib import geocode


PUSHKINA_STREET = geocode.AddressInfo(
    addr_id=1,
    lon=0.0, lat=0.0,
    entrances=[
        geocode.Entrance('1', 1e-6, 1e-6),
        geocode.Entrance('2', 2e-6, 2e-6)
    ]
)

TIMURA_FRUNZE_STREET = geocode.AddressInfo(
    addr_id=56697265,
    lon=37.589482, lat=55.733705,
    entrances=[
        geocode.Entrance('', 37.589329776675925, 55.73381362270063)
    ]
)


def add_geo_object(response, address_info, formatted_address):
    geo_object = response.reply.geo_object.add()

    address_point = geometry_pb2.Point(lon=address_info.lon, lat=address_info.lat)
    geo_object.geometry.add(point=address_point)

    address = address_pb2.Address(formatted_address=formatted_address)
    geocoder_metadata = geocoder_pb2.GeoObjectMetadata(
        id=str(address_info.addr_id),
        address=address)

    geo_object_metadata = geo_object.metadata.add().Extensions[
        geocoder_pb2.GEO_OBJECT_METADATA
    ]
    geo_object_metadata.CopyFrom(geocoder_metadata)

    entrances_metadata = geo_object.metadata.add().Extensions[
        entrance_pb2.ENTRANCE_METADATA
    ]
    for entrance in address_info.entrances:
        entrance_point = geometry_pb2.Point(lon=entrance.lon, lat=entrance.lat)
        entrances_metadata.entrance.add(
            name=entrance.name, point=entrance_point)


def generate_geocoder_reponse():
    response = response_pb2.Response()

    add_geo_object(
        response, PUSHKINA_STREET, 'улица Пушкина, дом Колотушкина')
    add_geo_object(
        response, TIMURA_FRUNZE_STREET, 'Москва, улица Тимура Фрунзе, дом 11к2')

    return response.SerializeToString()


def test_parse_geocoder_response_pushkina_street():
    response = generate_geocoder_reponse()
    address_info = geocode.parse_geocoder_response(
        response, PUSHKINA_STREET.lon + 1e-6, PUSHKINA_STREET.lat + 1e-7)

    assert address_info == PUSHKINA_STREET


def test_parse_geocoder_response_timura_frunze_street():
    response = generate_geocoder_reponse()
    address_info = geocode.parse_geocoder_response(
        response, TIMURA_FRUNZE_STREET.lon, TIMURA_FRUNZE_STREET.lat)

    assert address_info == TIMURA_FRUNZE_STREET


def test_parse_geocoder_response_too_far():
    response = generate_geocoder_reponse()
    address_info = geocode.parse_geocoder_response(
        response, 179.999999, 89.999999)

    assert address_info is None
