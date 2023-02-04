from __future__ import division

import hashlib
import logging
from collections import defaultdict
from datetime import datetime

import six

from yaphone.utils.parsed_version import ParsedVersion

AUDIENCE_RATIO_EPSILON = 1e-4
MAX_UUID_VALUE = int('F' * 32, 16)

logger = logging.getLogger(__name__)


class ExtendedParam(object):
    value = None
    comparator = None


def compute_hash(s):
    return hashlib.md5(s).hexdigest()


def compute_salted_ratio(uuid, salt):
    return int(compute_hash(six.ensure_binary(salt + uuid)), 16) / MAX_UUID_VALUE


def compute_ratio(uuid):
    return int(uuid, 16) / MAX_UUID_VALUE


def version_matches_range(version, version_range):
    if not version_range or not (version_range.low or version_range.high):
        return True

    if version is None:
        return False

    return ParsedVersion(version_range.low) <= ParsedVersion(version) <= ParsedVersion(version_range.high)


# TODO: move static methods outside
class UserInfo(object):
    uuid = None
    uuid_audience_ratio = None
    uuid_salted_audience_ratio = None
    last_used_salt = None
    language = None
    country = None
    region_ids = None
    region_ids_init = None
    clids = None
    device_type = None
    device_vendor = None
    device_model = None
    app_name = None
    app_version = None
    build_version = None
    os_version = None

    extended_params = None

    def __init__(self, uuid=None, uuid_audience_ratio=None, uuid_salted_audience_ratio=None, last_used_salt=None,
                 language=None, country=None, region_ids=None, region_ids_init=None, clids=None, device_type=None,
                 device_vendor=None, device_model=None, app_name=None, app_version=None, build_version=None,
                 os_version=None):
        if uuid:
            self.uuid = uuid.hex.lower()
        self.uuid_audience_ratio = uuid_audience_ratio
        self.uuid_salted_audience_ratio = uuid_salted_audience_ratio
        self.last_used_salt = last_used_salt
        if language and country:
            self.language = six.text_type(language.lower())
            self.country = six.text_type(country.lower())
        if region_ids:
            self.region_ids = set(region_ids)
        if region_ids_init:
            self.region_ids_init = set(region_ids_init)
        if clids:
            self.clids = {}
            for clid_number, clid_value in six.iteritems(clids):
                try:
                    self.clids[int(clid_number)] = int(clid_value)
                except ValueError:
                    logger.warning('Invalid value of clid %r: %r', clid_number, clid_value)
        self.device_type = device_type
        if device_vendor and device_model:
            # TODO why only both?
            self.device_vendor = device_vendor.lower()
            self.device_model = device_model.lower()
        if app_name and app_version:
            # TODO why only both?
            self.app_name = app_name.lower()
            self.app_version = six.text_type(app_version).lower()
        if os_version:
            self.build_version = six.text_type(build_version).lower()
            self.os_version = six.text_type(os_version).lower()
        # TODO move most fields out of extended params out of extended params

    def setExtendedParam(self, name, value, comparator):
        if self.extended_params is None:
            self.extended_params = defaultdict(ExtendedParam)
        self.extended_params[name].value = value
        self.extended_params[name].comparator = comparator

    def matches_conditions(self, conditions, options):
        for name, cond in self.enumerate_conditions(conditions, options):
            if not cond:
                return False
        return True

    def enumerate_conditions(self, conditions, options):
        yield 'conditions', conditions.enabled
        yield 'locale', self.locale_matches_condition(conditions.language, conditions.country)
        yield 'region_ids_init', self.region_ids_init_matches_condition(
            conditions.region_ids_init,
            conditions.region_ids_init_blacklist,
        )
        yield 'region_ids', self.region_ids_matches_condition(conditions.region_ids, conditions.region_ids_blacklist)
        yield 'application', self.application_matches_condition(conditions.applications)
        yield 'uuid', self.uuid_matches_condition(conditions.uuids)
        yield 'time', self.time_matches_condition(conditions.time_start, conditions.time_end)
        yield 'audience_ratio', self.audience_ratio_matches_condition(
            conditions.audience_ratio,
            conditions.audience_offset,
            options.audience_salt,
        )
        yield 'device_type', self.device_type_matches_condition(conditions.device_types)
        yield 'model', self.model_matches_condition(conditions.models)
        yield 'clids', self.clids_matches_condition(conditions.clids)
        yield 'os_version', version_matches_range(self.os_version, conditions.os_version_range)
        yield 'build_version_in_range', version_matches_range(self.build_version, conditions.build_version_range)
        yield 'build_version_in_list', self.build_version_in_list(conditions.build_version_list)
        yield 'extended_params', self.extended_params_matches_condition(conditions.extended_params)

    @staticmethod
    def locale_part_matches_condition_part(locale_part, condition_part):
        if condition_part == '*':
            return True

        if locale_part is None:
            return False

        if condition_part.startswith('!'):
            return locale_part != condition_part[1:]

        return locale_part == condition_part

    def build_version_in_list(self, build_version_list):
        if not build_version_list:
            return True
        if not self.build_version:
            return False
        return any(self.build_version.lower() == value.lower() for value in build_version_list)

    def locale_matches_condition(self, language, country):
        return self.locale_part_matches_condition_part(self.language, language) and \
            self.locale_part_matches_condition_part(self.country, country)

    @staticmethod
    def is_geo_targeting_succeeded(user_region_ids, item_region_ids, item_region_ids_blacklist):
        # prevent checking uninitialized conditions
        if not (item_region_ids or item_region_ids_blacklist):
            return True

        # user's region is unknown
        if not user_region_ids:
            return False

        # test if one of user's region_id is blacklisted
        if item_region_ids_blacklist and user_region_ids & set(item_region_ids_blacklist):
            return False

        return not item_region_ids or bool(set(item_region_ids) & user_region_ids)

    def region_ids_matches_condition(self, region_ids, region_ids_blacklist):
        return self.is_geo_targeting_succeeded(
            self.region_ids,
            region_ids,
            region_ids_blacklist,
        )

    def region_ids_init_matches_condition(self, region_ids_init, region_ids_init_blacklist):
        return self.is_geo_targeting_succeeded(
            self.region_ids_init,
            region_ids_init,
            region_ids_init_blacklist,
        )

    def application_matches_condition(self, applications):
        if not applications:
            return True

        if self.app_name is None:
            return False

        for condition_app in applications:
            if self.app_name in condition_app.aliases:
                return version_matches_range(self.app_version, condition_app.version)

        return False

    def uuid_matches_condition(self, uuids):
        if not uuids:
            return True

        if self.uuid is None:
            return False

        for uuid in uuids:
            if self.uuid == uuid.lower():
                return True

        return False

    @staticmethod
    def time_matches_condition(time_start, time_end):
        return time_start <= datetime.utcnow() < time_end

    def audience_ratio_matches_condition(self, audience_ratio, audience_offset, audience_salt):
        if audience_ratio + AUDIENCE_RATIO_EPSILON >= 1 and audience_offset <= AUDIENCE_RATIO_EPSILON:
            return True

        if self.uuid is None:
            return False

        if audience_salt:
            if self.uuid_salted_audience_ratio is None or audience_salt != self.last_used_salt:
                self.last_used_salt = audience_salt
                self.uuid_salted_audience_ratio = compute_salted_ratio(uuid=self.uuid, salt=audience_salt)
            effective_ratio = self.uuid_salted_audience_ratio
        else:
            if self.uuid_audience_ratio is None:
                self.uuid_audience_ratio = compute_ratio(self.uuid)
            effective_ratio = self.uuid_audience_ratio
        return audience_offset <= effective_ratio <= audience_ratio + audience_offset

    def device_type_matches_condition(self, device_types):
        if not device_types:
            return True

        if self.device_type is None:
            return False

        return self.device_type in device_types

    def model_matches_condition(self, models):
        if not models:
            return True

        for model in models:
            if (not model.vendor or self.device_vendor == model.vendor) and \
               (not model.name or self.device_model == model.name):
                return True

        return False

    def clids_matches_condition(self, clids):
        """
        :param clids:
        :type clids: Dict[int, Set[int]]
        :return:
        :rtype: bool
        """
        if not clids:
            return True

        if self.clids is None:
            return False  # added

        for clid in clids:
            if clid not in self.clids:
                return False
            if self.clids[clid] not in clids[clid]:
                return False

        return True

    def extended_params_matches_condition(self, extended_params):
        if not extended_params:
            return True

        if self.extended_params is None:
            return False  # added

        for name in extended_params:
            if name not in self.extended_params:
                return False
            user_param = self.extended_params[name]
            if not user_param.comparator(user_param.value, extended_params[name]):
                return False

        return True
