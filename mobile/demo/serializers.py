from rest_framework.serializers import Serializer, CharField, URLField, BooleanField


# noinspection PyAbstractClass
class ProvisioningSerializer(Serializer):
    device_owner_url = CharField(allow_blank=False, required=True)
    device_owner_hash = CharField(allow_blank=False, required=True)


# noinspection PyAbstractClass
class ConfigureAppsSerializer(Serializer):
    package_name = CharField(allow_blank=False, required=True)
    enable = BooleanField(required=True)


# noinspection PyAbstractClass
class DeviceOwnerSerializer(Serializer):
    video_url = URLField(allow_blank=False, required=True)
    configure_apps = ConfigureAppsSerializer(required=True, many=True)
    organization_name = CharField(allow_blank=False, max_length=80, required=True)
    demo_user_name = CharField(allow_blank=False, max_length=40, required=True)
    disable_factory_reset = BooleanField(required=True)
    preferred_launcher = CharField(allow_blank=False, required=True)
