import re

import logging
import mongoengine as me
from datetime import datetime
from distutils.version import LooseVersion
from django.conf import settings

from yaphone.advisor.advisor.models.lbs import COUNTRY_CHOICES, DEFAULT_COUNTRY
from yaphone.advisor.advisor.models.profile import Profile, Locale
from yaphone.advisor.common.localization_helpers import UserSpecificConfig
from yaphone.advisor.common.screen_properties import dpi_to_name
from yaphone.advisor.launcher.configs import places_hierarchy
from yaphone.advisor.launcher.serializers import DEFAULT_LANGUAGE
from yaphone.advisor.setup_wizard.models import Phone
from yaphone.localization import LocalizationUser, Translator

DEVICE_PHONE = 'phone'
DEVICE_TABLET = 'tablet'
USER_AGENT_REGEXP = re.compile(r"(?P<app_name>.+)/(?P<app_version_string>.+)\s+"
                               r"\((?P<device_manufacturer>\S+)\s+(?P<device_model>.*);\s+"
                               r"(?P<os_name>.+)\s+(?P<os_version>.*)\)\s*"
                               "(?P<device_type>(?i)({}|{}))?".format(DEVICE_PHONE, DEVICE_TABLET))

clid_first_digit_position = len('clid')

logger = logging.getLogger(__name__)


class BaseClient(object):
    collection_name = 'client'

    _clids = None
    _localization_user = None
    _translator = None
    _user_specific_config = None
    user_agent = None
    application = None
    regions_by_ip = None
    yphone_id = None

    @property
    def localization_user(self):
        if self._localization_user is None:
            self._localization_user = self._create_localization_user()
        return self._localization_user

    def get_localization_project_name(self):
        return self.application

    @property
    def user_specific_config(self):
        if self._user_specific_config is None:
            self._user_specific_config = UserSpecificConfig(self.localization_user,
                                                            self.get_localization_project_name())
        return self._user_specific_config

    @property
    def translator(self):
        if self._translator is None:
            self._translator = Translator(self.localization_user, settings.LOCALIZATION_TRANSLATIONS_PROJECT)
        return self._translator

    def get_locale(self):
        if self.locale:
            return self.locale
        if self.profile:
            return self.profile.android_info.user_settings.locale

    @property
    def language(self):
        locale = self.get_locale()
        if locale:
            return locale.language
        return DEFAULT_LANGUAGE

    def _create_localization_user(self):
        user = LocalizationUser(
            uuid=self.uuid,
            clids=self.get_clids_in_localization_format(),
            created_at=self.created_at,
        )
        locale = self.get_locale()

        if locale:
            user.language = locale.language
            user.country = locale.territory

        if self.user_agent is not None:
            user.app_name = self.user_agent.app_name.encode('utf8')
            user.app_version = self.user_agent.app_version
            user.device_type = self.user_agent.device_type

        if self.yphone_id:
            user.yphone_id = self.yphone_id
            phone = Phone.objects(pk=self.yphone_id).first()
            if phone:
                user.yphone_batch = phone.batch.encode('ascii')

        if self.profile:
            # TODO: remove try-catch when ADVISOR-2358 will be closed
            try:
                user.loyalty = self.profile.crypta.loyalty
            except AttributeError:
                logger.error('Got attribute error, profile: %r', self.profile)
            user.region_ids = self.profile.region_ids
            user.region_ids_init = self.profile.region_ids_init
            user.device_model = self.profile.device_model
            user.device_manufacturer = self.profile.device_vendor
        else:
            user.device_model = self.user_agent.device_model.encode('utf8')
            user.device_manufacturer = self.user_agent.device_manufacturer.encode('utf8')
            if self.regions_by_ip:
                user.region_ids = self.regions_by_ip
                user.region_ids_init = self.regions_by_ip
        return user

    def get_clid(self, clid_name):
        if self.clids:
            return self.clids.get(clid_name)

    def get_clids_in_localization_format(self):
        """
        Transforms {'clid123': '456'} clids dictionary to suitable
        for localization lib integer format {123: 456}.
        Returns empty dictionary if there is no clids
        """
        clids = {}
        if self.clids:
            for key, value in self.clids.iteritems():
                try:
                    clid_id = int(key[clid_first_digit_position:])
                except ValueError:
                    logger.warning('Incorrect clid id format: %r', key)
                    continue
                try:
                    clid_value = int(value)
                except ValueError:
                    logger.warning('Incorrect clid value for %r: %r', key, value)
                    continue
                clids[clid_id] = clid_value
        return clids

    def get_supported_card_types(self, place):
        # We have list in supported_card_types for some documents in database
        # so we have to check that supported_card_types is dict.
        if isinstance(self.supported_card_types, dict) and self.supported_card_types:
            for place_ in places_hierarchy(place):
                card_type = self.supported_card_types.get(place_)
                if card_type:
                    return card_type

    def get_icon_size_name(self, max_dpi=None):
        try:
            dpi = self.profile.android_info.display_metrics.density_dpi
            if max_dpi:
                dpi = min(dpi, max_dpi)
            return dpi_to_name(dpi)
        except AttributeError:
            return 'icon'


# noinspection PyClassHasNoInit
class ViewConfig(me.EmbeddedDocument):
    card_type = me.StringField(required=True)
    count = me.IntField(required=False)


class UserAgent(me.EmbeddedDocument):
    app_name = me.StringField(required=False)
    app_version_string = me.StringField(required=False)
    device_model = me.StringField(required=False)
    device_manufacturer = me.StringField(required=False)
    device_type = me.StringField(required=False, default=DEVICE_PHONE)
    os_name = me.StringField(required=False)
    os_version = me.StringField(required=False)
    raw = me.StringField(required=False)

    defective_versions = ('1.5.2', '1.5.3')
    compare_version_len = len(defective_versions[0])

    meta = {
        'strict': False
    }

    def __init__(self, app_name, app_version_string, device_type=DEVICE_PHONE, **kwargs):
        if self.is_defective_ua(app_name, app_version_string):
            app_version_string = self.fix_version(app_version_string)
        if device_type is not None:
            device_type = device_type.lower()
        super(UserAgent, self).__init__(
            app_name=app_name, app_version_string=app_version_string, device_type=device_type, **kwargs
        )

    def is_defective_version(self, version):
        return version[:self.compare_version_len] in self.defective_versions

    def is_defective_ua(self, name, version):
        return name == 'com.yandex.launcher' and self.is_defective_version(version)

    def fix_version(self, version):
        return '.'.join((version[:self.compare_version_len], version[self.compare_version_len:]))

    def __str__(self):
        return '%s/%s (%s %s; %s %s)' % (
            self.app_name, self.app_version, self.device_manufacturer, self.device_model, self.os_name, self.os_version,
        )

    @classmethod
    def from_string(cls, ua_str):
        match = USER_AGENT_REGEXP.match(ua_str)
        if match is not None:
            return cls(raw=ua_str, **match.groupdict())

    @property
    def app_version(self):
        return LooseVersion(self.app_version_string)


class Client(me.Document, BaseClient):
    uuid = me.UUIDField(required=True, primary_key=True)
    profile = me.ReferenceField(Profile, required=False, db_field='device_id', default=None)
    clids = me.DictField(required=False)
    supported_card_types = me.MapField(field=me.ListField(field=me.StringField()), required=False)
    rec_views_config = me.MapField(me.EmbeddedDocumentListField(ViewConfig), required=False)
    updated_at = me.DateTimeField(required=False, default=datetime.utcnow)
    created_at = me.DateTimeField(required=False, default=datetime.utcnow)
    user_agent = me.EmbeddedDocumentField(document_type=UserAgent, required=False)
    locale = me.EmbeddedDocumentField(document_type=Locale, required=False)
    country = me.StringField(choices=COUNTRY_CHOICES, required=False, default=DEFAULT_COUNTRY)

    meta = {
        'db_alias': 'primary_only',
        'strict': False,
        'auto_create_index': False,
        'index_background': True,
        'indexes': [
            'profile',
        ]
    }

    @classmethod
    def load(cls, uuid):
        return cls.objects.get(pk=uuid)

    def __init__(self, *args, **values):
        super(Client, self).__init__(*args, **values)

    def clean(self):
        # We have list in supported_card_types for some documents in database
        # so we have to force supported_card_types to be dict.
        if not isinstance(self.supported_card_types, dict):
            self.supported_card_types = {}
