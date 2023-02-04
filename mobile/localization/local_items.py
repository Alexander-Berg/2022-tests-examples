from collections import namedtuple
from datetime import datetime

# TODO remove this logic
try:
    import yaphone.localization.items_cache
except ImportError:
    import items_cache

VersionRange = namedtuple(u'VersionRange', (u'low', u'high'))
Application = namedtuple(u'Application', (u'aliases', u'version'))
Model = namedtuple(u'Model', (u'vendor', u'name'))


class EnableConditions(object):
    def __init__(self, enabled=True, extendedParams=None, audience_offset=0., audience=1.0, clids=None,
                 models=None, time=None, applications=None,
                 region_ids_blacklist=None, region_ids=None, region_ids_init_blacklist=None, region_ids_init=None,
                 uuids=None, deviceTypes=None, locale=None, os_version=None, **kwargs):
        # TODO: add operators, platforms, detectOperatorByIp
        self.enabled = enabled
        self.device_types = deviceTypes
        self.uuids = uuids
        self.region_ids_init = region_ids_init
        self.region_ids_init_blacklist = region_ids_init_blacklist
        self.region_ids = region_ids
        self.region_ids_blacklist = region_ids_blacklist
        locale = locale or {}
        self.language = locale.get(u'language', u'*').lower()
        self.country = locale.get(u'country', u'*').lower()
        self.os_version_range = None
        self.build_version_range = None
        self.build_version_list = []
        if os_version:
            self.os_version_range = VersionRange(low=os_version.get('min_os_version'),
                                                 high=os_version.get('max_os_version'))
            self.build_version_range = VersionRange(low=os_version.get('min_build_version'),
                                                    high=os_version.get('max_build_version'))
            if os_version.get('build_version_list'):
                self.build_version_list = os_version.get('build_version_list').split('|')
        try:
            app_cache = yaphone.localization.items_cache._applications_cache
        except NameError:
            app_cache = items_cache._applications_cache
        self.applications = []
        if applications:
            for application in applications:
                name = application[u'name']
                aliases = [name.lower()]
                if name in app_cache.items:
                    aliases.extend(app_cache.items[name])
                version = application.get(u'version', {})
                app = Application(aliases=aliases, version=VersionRange(low=version.get(u'from'), high=version.get(u'to')))
                self.applications.append(app)
        time = time or {}
        self.time_start = time.get(u'from', datetime.min)
        self.time_end = time.get(u'to', datetime.max)
        self.models = []
        if models:
            for model in models:
                model_name = model.get(u'model')
                if model_name:
                    model_name = model_name.lower()
                self.models.append(Model(name=model_name, vendor=model[u'vendor'].lower()))
        self.clids = {}
        if clids:
            for entry in clids:
                self.clids[entry[u'number']] = set(entry[u'value'])
        self.audience_ratio = audience
        self.audience_offset = audience_offset
        self.extended_params = extendedParams


class ItemLocalization(object):
    def __init__(self, value=None, conditions=None):
        self.value = value
        self.conditions = conditions


class ItemOptions(object):
    def __init__(self, audience_salt=None):
        self.audience_salt = audience_salt
