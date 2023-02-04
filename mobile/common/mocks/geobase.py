from yaphone.http_geobase_lookuper.httpgeobase import HttpGeobaseException

location_to_region = {
    (43, 41): 2,
    (55, 37): 162180,
}
region_iso_names = {
    2: 'AB',
    5: 'US',
}

region_types = {
    1: 5,
    2: 6,
    3: 4,
    213: 6,
    225: 3,
    206: 3,
    20279: 8,
    98614: 10,
    120542: 13,
    162180: -1,
}


# noinspection PyMethodMayBeStatic
class LookupMock(object):
    def __init__(self, country_id=None):
        self._country_id = country_id

    def get_parents_ids(self, region_id):
        return [region_id, 98614, 1, 3, 225, 10000, 10001]

    def get_region_by_id(self, region_id):
        return {'id': region_id, 'iso_name': region_iso_names.get(region_id, 'RU'), 'type': region_types[region_id]}

    def get_region_id_by_location(self, lat, lon):
        return location_to_region.get((int(lat), int(lon)), 225)

    def get_country_id(self, region_id):
        if self._country_id:
            return self._country_id
        return region_id

    def get_region_by_ip(self, ip):
        if ip == '127.0.0.1':
            # Localhost!
            raise HttpGeobaseException()
        return {'id': 225}
