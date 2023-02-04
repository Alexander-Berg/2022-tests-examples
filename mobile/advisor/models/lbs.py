import logging
import mongoengine as me
from cache_memoize import cache_memoize
from datetime import datetime

from yaphone.advisor.common.country_code import all_countries_set
from yaphone.utils import geo

MULTI_MCC_RESULTS_LOG_TEMPLATE = 'mobile_codes.mcc return more than one country, uuid=%s'
IP_DETECTED_NOT_IN_MCC_RESULTS_LOG_TEMPLATE = 'ip detected country not in mobile.codes.mcc results, uuid=%s'

LBS_LOGGER_MSG_TEMPLATE = 'Country detection: %s=%s, by=%s, uuid=%s'

logger = logging.getLogger(__name__)

DEFAULT_COUNTRY = 'US'
COUNTRY_CHOICES = list(all_countries_set)


def region_to_country(region_id):
    if region_id > 0:
        try:
            country_region_id = geo.geobase_lookuper.get_country_id(region_id)
            country = geo.geobase_lookuper.get_region_by_id(country_region_id)['iso_name']
        except RuntimeError:
            logger.warning('Country by region_id geobase lookup failed')
            return None
        if country in all_countries_set:
            return country


def region_can_be_clarified(region_ids, new_region_ids):
    if isinstance(region_ids, list) and isinstance(new_region_ids, list):
        if len(region_ids) < len(new_region_ids):
            return region_ids[::-1] == new_region_ids[::-1][:len(region_ids)]
    return False


def get_region_parents(region_id):
    try:
        parents = geo.geobase_lookuper.get_parents_ids(region_id)[:-2]
    except RuntimeError:  # region_id not found
        parents = [region_id]
    return parents


@cache_memoize(timeout=30)
def get_region_type(region_id):
    try:
        return geo.geobase_lookuper.get_region_by_id(region_id)['type']
    except RuntimeError:
        logger.warning('Region type lookup failed')
        return -1


def set_by(obj, by):
    if obj:
        obj.by = by
    return obj


def location_by_gps(location):
    return set_by(location, 'gps')


def location_by_lbs(cells, networks, uuid, ip):
    if any((cells, networks)):
        pos = geo.lbs_locator.locate(gsm_cells=cells, wifi_networks=networks, uuid=uuid, ip=ip)
        if pos and pos['type'] != 'ip':
            location = Location(latitude=pos['latitude'], longitude=pos['longitude'])
            return set_by(location, 'lbs')


def country_by_location(location):
    try:
        if location:
            region_id = geo.geobase_lookuper.get_region_id_by_location(location.latitude, location.longitude)
            return Country(iso_name=region_to_country(region_id), by='gps')
    except RuntimeError:
        logger.warning('Country by location geobase lookup failed')
    return None


def country_by_lbs(cells, networks, uuid, ip):
    country = country_by_location(location_by_lbs(cells, networks, uuid, ip))
    return set_by(country, 'lbs')


def country_by_ip(ip):
    try:
        region = geo.geobase_lookuper.get_region_by_ip(ip)
        if region:
            return Country(iso_name=region_to_country(region['id']), by='ip')
    except RuntimeError:
        logger.warning('Country by IP geobase lookup failed')
    return None


def country_by_operators(operators):
    if not operators:
        return None

    countries = set(operator.country_iso for operator in operators if operator.country_iso)

    # if all SIM cards belongs to one country, return it
    if len(countries) == 1:
        return Country(iso_name=countries.pop(), by='mcc')

    return None


# noinspection PyClassHasNoInit,PyStringFormat
class Location(me.EmbeddedDocument):
    latitude = me.FloatField(required=True, min_value=-90, max_value=90)
    longitude = me.FloatField(required=True, min_value=-180, max_value=180)

    meta = {'strict': False}

    def __str__(self):
        return "%.3f,%.3f" % (self.latitude, self.longitude)


class Country(object):
    def __init__(self, iso_name, by):
        self.iso_name = iso_name
        self.by = by


# noinspection PyClassHasNoInit
class TimeZone(me.EmbeddedDocument):
    name = me.StringField(required=True)
    utc_offset = me.IntField(required=True)

    meta = {'strict': False}

    def __str__(self):
        return '%s' % self.name


class Cell(me.EmbeddedDocument):
    mcc = me.IntField(required=True)
    mnc = me.IntField(required=True)

    meta = {'strict': False}

    def __str__(self):
        return 'mcc=%d;mnc=%d' % (self.mcc, self.mnc)


# noinspection PyClassHasNoInit
class LBSInfo(me.EmbeddedDocument):
    location = me.EmbeddedDocumentField(document_type=Location, required=False)
    region_ids = me.ListField(field=me.IntField(), required=False, default=list)
    region_types = me.ListField(field=me.IntField(), required=False, default=list)
    region_ids_init = me.ListField(field=me.IntField(), required=False, default=list)
    region_types_init = me.ListField(field=me.IntField(), required=False, default=list)
    time_zone = me.EmbeddedDocumentField(document_type=TimeZone, required=False)
    country = me.StringField(choices=COUNTRY_CHOICES, required=False, default=DEFAULT_COUNTRY)
    country_init = me.StringField(choices=COUNTRY_CHOICES, required=False, default=DEFAULT_COUNTRY)
    fix_country_init = me.BooleanField(required=False, default=True)
    updated_at = me.DateTimeField(required=False)
    mnc = me.IntField(required=False)
    mcc = me.IntField(required=False)
    cells = me.EmbeddedDocumentListField(document_type=Cell, required=False)

    meta = {'strict': False}

    def update_location(self, location, cells, networks, ip, uuid):
        new_location = location_by_gps(location) or location_by_lbs(cells, networks, uuid, ip)

        region_id = None
        if new_location:
            self.location = new_location
            logger.info(LBS_LOGGER_MSG_TEMPLATE, 'location', location, self.location.by, uuid.hex)
            try:
                region_id = geo.geobase_lookuper.get_region_id_by_location(new_location.latitude,
                                                                           new_location.longitude)
            except RuntimeError:
                logger.warning('Region by location geobase lookup failed')
        elif not self.location:
            try:
                region = geo.geobase_lookuper.get_region_by_ip(ip)
                region_id = region['id']
            except RuntimeError:
                logger.warning('Region by IP geobase lookup failed')

        if region_id:
            new_region_ids = get_region_parents(region_id)
            if self.region_ids != new_region_ids:
                # do not logging in production: debug level for testing
                logger.debug('new region_ids: %s => %s', self.region_ids, new_region_ids)
                self.region_ids = new_region_ids
                self.region_types = map(get_region_type, self.region_ids)
            elif len(self.region_types) != len(self.region_ids):
                # if coordinates are not changed, code below to be sure
                # `self.region_types` already have proper values
                self.region_types = map(get_region_type, self.region_ids)

        if (self.fix_country_init  # manually requested
                or region_can_be_clarified(self.region_ids_init, self.region_ids)):  # could be increased accuracy
            logger.info('new region_ids_init: %s => %s', self.region_ids_init, self.region_ids)
            self.region_ids_init = self.region_ids
            self.region_types_init = self.region_types

    def update_countries(self, location, cells, operators, networks, ip, uuid):
        new_country = (country_by_location(location) or
                       country_by_lbs(cells, networks, uuid, ip) or
                       country_by_ip(ip))

        if new_country:
            self.country = new_country.iso_name
            logger.info(LBS_LOGGER_MSG_TEMPLATE, 'country', new_country.iso_name, new_country.by, uuid.hex)

        if self.fix_country_init:
            new_country_init = country_by_operators(operators) or new_country
            if new_country_init:
                self.fix_country_init = False
                self.country_init = new_country_init.iso_name
                logger.info(LBS_LOGGER_MSG_TEMPLATE, 'country_init', new_country_init.iso_name, new_country_init.by,
                            uuid.hex)

    def update_lbs(self, location, cells, operators, networks, time_zone, ip, uuid):
        logger.info('location=%s, cells=%s, wifi=%s, ip=%s, time_zone=%s, uuid=%s', location, cells, networks, ip,
                    time_zone, uuid.hex)

        self.update_location(location, cells, networks, ip, uuid)
        self.update_countries(location, cells, operators, networks, ip, uuid)
        self.time_zone = time_zone

        if cells:
            self.cells = []
            for cell in cells:
                if 'countrycode' in cell and 'operatorid' in cell:
                    self.cells.append(Cell(mcc=cell['countrycode'], mnc=cell['operatorid']))

        self.updated_at = datetime.utcnow()
