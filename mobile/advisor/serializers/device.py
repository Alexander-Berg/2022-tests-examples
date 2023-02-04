import collections

import logging
import rest_framework.serializers as drf
import six
from ylog.context import log_context

from yaphone.advisor.advisor.models import client, lbs, profile

logger = logging.getLogger(__name__)


# noinspection PyUnresolvedReferences,PyProtectedMember
class ModelInMeta(object):
    @property
    def _model(self):
        assert hasattr(self, 'Meta')
        assert hasattr(self.Meta, 'model')
        return self.Meta.model

    @property
    def _id_field(self):
        return self._model._meta['id_field']


# noinspection PyProtectedMember
class DocumentSerializer(ModelInMeta, drf.Serializer):
    def create(self, validated_data):
        return self._model(**validated_data)

    def update(self, instance, validated_data):
        for field in instance._fields_ordered:
            if field in validated_data and field != self._id_field:
                setattr(instance, field, validated_data[field])
        return instance

    def save(self, **kwargs):
        validated_data = dict(
            list(self.validated_data.items()) +
            list(kwargs.items())
        )
        instance = self._model.objects(pk=self.validated_data[self._id_field]).first()
        if instance is None:
            model = self.create(validated_data)
        else:
            model = self.update(instance, validated_data)
        return model.save()


class EmbeddedDocumentSerializer(ModelInMeta, drf.Serializer):
    def to_internal_value(self, data):
        data = super(EmbeddedDocumentSerializer, self).to_internal_value(data)
        return self._model(**data)


class LocaleField(drf.Field):
    def to_representation(self, value):
        return str(value)

    def to_internal_value(self, data):
        return profile.Locale(data)


class UserSettingsSerializer(EmbeddedDocumentSerializer):
    locale = LocaleField()

    class Meta:
        model = profile.UserSettings
        fields = '__all__'


class DisplayMetricsField(EmbeddedDocumentSerializer):
    height_pixels = drf.IntegerField(required=True)
    width_pixels = drf.IntegerField(required=True)
    density = drf.FloatField(required=True)
    density_dpi = drf.FloatField(required=True)
    xdpi = drf.FloatField(required=True)
    ydpi = drf.FloatField(required=True)
    scaled_density = drf.FloatField(default=1.0)

    class Meta:
        model = profile.DisplayMetrics
        fields = '__all__'


class VersionSerializer(EmbeddedDocumentSerializer):
    codename = drf.CharField(required=False)
    incremental = drf.CharField(required=False)
    release = drf.CharField(required=False)
    sdk_int = drf.IntegerField(min_value=1, required=True)

    class Meta:
        model = profile.Version
        fields = '__all__'


class OSBuildSerializer(EmbeddedDocumentSerializer):
    version = VersionSerializer(required=True)
    string_fields = drf.ListField(child=drf.DictField(), required=True)

    class Meta:
        model = profile.OSBuild
        fields = '__all__'


class AndroidInfoSerializer(EmbeddedDocumentSerializer):
    ad_id = drf.UUIDField(required=False, allow_null=True, default=None)
    android_id = drf.CharField(required=False, allow_null=True, default=None)
    user_settings = UserSettingsSerializer(required=False, allow_null=True, default=None)
    display_metrics = DisplayMetricsField(required=False, allow_null=True, default=None)
    features = drf.ListField(child=drf.CharField(), required=False, allow_null=True, default=None)
    shared_libraries = drf.ListField(child=drf.CharField(), required=False, allow_null=True, default=None)
    os_build = OSBuildSerializer(required=False, allow_null=True, default=None)

    gl_extensions = drf.ListField(child=drf.CharField(), required=False, allow_null=True, default=tuple)
    gl_es_version = drf.IntegerField(required=False, allow_null=True)

    class Meta:
        model = profile.AndroidInfo
        fields = '__all__'


class OperatorSerializer(EmbeddedDocumentSerializer):
    carrier_name = drf.CharField(required=False)
    display_name = drf.CharField(required=False)
    country_iso = drf.CharField(required=False, allow_null=True, min_length=2, max_length=2)
    mcc = drf.IntegerField(required=True, min_value=1, max_value=999)
    mnc = drf.IntegerField(required=True, min_value=0, max_value=999)
    data_roaming = drf.BooleanField(required=False)
    sim_slot_index = drf.IntegerField(required=False)
    is_embedded = drf.BooleanField(required=False)
    icc_id = drf.CharField(required=False)

    class Meta:
        model = profile.Operator
        fields = '__all__'

    def to_internal_value(self, data):
        if isinstance(data.get('country_iso'), basestring):
            if len(data['country_iso']) == 0:
                data['country_iso'] = None
            else:
                data['country_iso'] = data['country_iso'].upper()
        if isinstance(data.get('icc_id'), int):
            data['icc_id'] = str(data['icc_id'])
        return super(OperatorSerializer, self).to_internal_value(data)


class OperatorListField(drf.ListField):
    child = OperatorSerializer()

    def to_internal_value(self, data):
        if isinstance(data, (collections.Mapping, six.string_types)) or not hasattr(data, '__iter__'):
            self.fail('not_a_list', input_type=type(data).__name__)

        result = []
        for item in data:
            try:
                item_value = self.child.run_validation(item)
                if item_value is not None:
                    result.append(item_value)
            except drf.ValidationError as e:
                with log_context(operator=item, error=str(e)):
                    logger.warning('Invalid operator value')

        if not self.allow_empty and len(result) == 0:
            self.fail('empty')
        return result


class ProfileSerializer(DocumentSerializer):
    device_id = drf.UUIDField()
    android_info = AndroidInfoSerializer(required=False, default=None)
    operators = OperatorListField(required=False, allow_null=True, default=None)
    phone_id = drf.CharField(required=False, allow_null=True)
    passport_uid = drf.IntegerField(required=False, allow_null=True)

    class Meta:
        model = profile.Profile
        fields = '__all__'


class ViewConfigSerializer(EmbeddedDocumentSerializer):
    card_type = drf.CharField(required=True)
    count = drf.IntegerField(required=False, allow_null=True)

    class Meta:
        model = client.ViewConfig
        fields = '__all__'


def partial_func(self):
    if self.parent:
        return self.parent.partial
    return False


# https://github.com/encode/django-rest-framework/issues/5384
drf.DictField.partial = property(partial_func)


class ClientSerializer(DocumentSerializer):
    uuid = drf.UUIDField(required=True)
    profile = ProfileSerializer(required=False, allow_null=True, default=None)
    clids = drf.DictField(required=False)
    supported_card_types = drf.DictField(child=drf.ListField(child=drf.CharField()), required=False)
    rec_views_config = drf.DictField(child=ViewConfigSerializer(many=True), required=False, allow_null=True)
    locale = LocaleField(required=False, allow_null=True)
    country = drf.ChoiceField(choices=client.COUNTRY_CHOICES, required=False, default=client.DEFAULT_COUNTRY)

    class Meta:
        model = client.Client
        fields = '__all__'

    clid_map = {
        '2246894': '2246892',  # Fly low-end
        '2248004': '2248002',  # DEXP
        '2254537': '2254535',  # Alcatel
        '2258696': '2258694',  # 4Good
        '2258705': '2258703',  # Fly hi-end
        '2263160': '2263158',  # Multilaser
        '2264436': '2248002',  # BMobile
        '2264494': '2264492',  # MTS
        '2265554': '2265552',  # Mobilink
        '2265873': '2265871',  # Irbis
        '2268769': '2268767',  # 4Good S504m
        '2269787': '2269785',  # Posh
        '2270614': '2270612',  # MTS
        '2271538': '2271536',  # STK
    }

    def to_internal_value(self, data):
        # Old versions of Launcher send clids not as a dict,
        # but as a single field "clid", that contains clid1006
        # we use this clid_map to get clid1

        if 'clid' in data and data['clid'] in self.clid_map:
            clid1006 = data['clid']
            data['clids'] = {'clid1': self.clid_map[clid1006], 'clid1006': clid1006}
        return super(ClientSerializer, self).to_internal_value(data)


class TimeZoneSerializer(EmbeddedDocumentSerializer):
    name = drf.CharField(required=True)
    utc_offset = drf.IntegerField(required=True)

    class Meta:
        model = lbs.TimeZone
        fields = '__all__'


class LocationSerializer(EmbeddedDocumentSerializer):
    latitude = drf.FloatField(required=True, min_value=-90, max_value=90)
    longitude = drf.FloatField(required=True, min_value=-180, max_value=180)

    class Meta:
        model = lbs.Location
        fields = '__all__'


class PackageInfoSerializer(drf.Serializer):
    package_name = drf.CharField(required=True)
    first_install_time = drf.IntegerField(required=True)
    last_update_time = drf.IntegerField(required=True)
    is_system = drf.BooleanField(required=True)
    is_disabled = drf.BooleanField(required=True)


class PackagesInfoSerializer(drf.Serializer):
    packages_info = PackageInfoSerializer(many=True)


class GsmSerializer(drf.Serializer):
    country_code = drf.IntegerField(required=True, source='countrycode')
    operator_id = drf.IntegerField(required=True, source='operatorid')
    cell_id = drf.IntegerField(required=True, source='cellid')
    lac = drf.IntegerField(required=True)
    signal_strength = drf.IntegerField(required=True)
    age = drf.IntegerField(required=False, default=0)


class WifiSerializer(drf.Serializer):
    mac = drf.CharField(required=True)
    signal_strength = drf.IntegerField(required=True)
    age = drf.IntegerField(required=False, default=0)


class LbsInfoSerializer(drf.Serializer):
    location = LocationSerializer(required=False)
    time_zone = TimeZoneSerializer(required=True)
    cells = GsmSerializer(many=True, required=False)
    wifi_networks = WifiSerializer(many=True, required=False)
