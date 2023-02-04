# coding=utf-8

import copy
import binascii

import maps.carparks.libs.geocoding.geocoding as geocoding
from lxml import etree
from yandex.maps import geolib3

_NS = {
    'gml': 'http://www.opengis.net/gml',
    'daps': 'http://maps.yandex.ru/snippets/driving_arrival_points/1.x'
}

_DAP = etree.XPath('daps:DrivingArrivalPoint', namespaces=_NS)
_ID = etree.XPath('daps:id', namespaces=_NS)
_DESCRIPTION = etree.XPath('daps:description', namespaces=_NS)
_RATING = etree.XPath('daps:rating', namespaces=_NS)


def ewkb_hex_point(lon, lat):
    return binascii.hexlify(
        geolib3.Point2(lon, lat).to_EWKB(geolib3.SpatialReference.Epsg4326))


class GeocoderMock(geocoding.Geocoder):
    def __init__(self, result_dict=None):
        self.result_dict = result_dict if result_dict else {}

    def resolve(self, address):
        result = self.result_dict.get(address)
        if isinstance(result, Exception):
            raise result
        return result


def patched_row(template, patch_dict=None):
    row = copy.copy(template)
    if patch_dict:
        row.update(patch_dict)

    return row


def get_id_description_rating(result):
    if not result:
        return []

    root = etree.fromstring(result[0]['value'])
    result = []
    for dap in _DAP(root):
        id = _ID(dap)[0].text if _ID(dap) else None
        description = _DESCRIPTION(dap)[0].text if _DESCRIPTION(dap) else None
        rating = _RATING(dap)[0].text if _RATING(dap) else None
        result.append((id, description, rating))

    return result
