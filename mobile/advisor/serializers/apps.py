from django.core.validators import RegexValidator
from rest_framework.serializers import (Serializer, CharField, URLField, IntegerField,
                                        Field, FloatField, BooleanField, DictField, ListField, SerializerMethodField,
                                        DateTimeField)

from yaphone.advisor.advisor.serializers.base import BaseSerializer

GOOGLE_PLAY_URL_TEMPLATE = "https://play.google.com/store/apps/details?id={package_name}"


class ImpressionIDField(Field):
    def to_representation(self, obj):
        return str(obj)


class AdnetworkSerializer(BaseSerializer):
    OMIT_FIELDS_IF_EMPTY = 'offer_id',

    name = CharField(required=False, source='adnetwork_name')
    offer_id = CharField(required=False)
    click_url = URLField(required=False)
    on_show_callback_url = URLField(required=False)
    on_click_callback = URLField(required=False)


class ScreenshotInfoSerializer(Serializer):
    width = IntegerField(required=True)
    height = IntegerField(required=True)
    url = URLField(required=True)


class ExtendedScreenshotSerializer(Serializer):
    preview = ScreenshotInfoSerializer(required=True)
    full = ScreenshotInfoSerializer(required=True)


class IconContextColorsSerializer(Serializer):
    card_text = CharField(required=True)
    card_background = CharField(required=True)
    button_text = CharField(required=True)
    button_background = CharField(required=True)


class ScreenshotSerializer(Serializer):
    preview = URLField(required=True)
    full = URLField(required=True)


class RecItemSerializer(BaseSerializer):
    popup_type = CharField(required=True)


class AppSerializer(RecItemSerializer):
    OMIT_FIELDS_IF_EMPTY = ('adnetwork', 'screenshots', 'icon_context_colors', 'content_rating',
                            'disclaimer', 'disclamer')

    package_name = CharField(required=True)
    title = CharField(required=True)
    icon = URLField(required=True)
    icon_context_colors = IconContextColorsSerializer(required=False)
    genres = ListField(child=CharField(), required=True)
    description = CharField(required=True)
    rating = FloatField(required=True)
    rating_count = IntegerField(required=True)
    is_free = BooleanField(required=True)
    use_external_ads = BooleanField(required=False)
    impression_id = ImpressionIDField(required=True)
    adnetwork = AdnetworkSerializer(required=False, source='*')
    screenshots = ListField(child=ScreenshotSerializer(), required=False)

    content_rating = CharField(required=False, source='adult')
    disclaimer = CharField(required=False)
    # https://st.yandex-team.ru/ADVISOR-1417#1518709970000 - misspell in client logic
    disclamer = CharField(required=False, source='disclaimer')


class AppSerializerExtended(AppSerializer):
    mark_as_sponsored = BooleanField(required=True)
    screenshots = ExtendedScreenshotSerializer(many=True, required=False)
    publisher = CharField(required=True)
    download_url = SerializerMethodField()

    @staticmethod
    def get_download_url(app):
        return GOOGLE_PLAY_URL_TEMPLATE.format(package_name=app.package_name)


class HeterogeneousListSerializer(BaseSerializer):
    """ calls serializer() from each item to get its serializer """

    def to_representation(self, data):
        return [item.serializer().to_representation(item) for item in data]


class AppsBlockSerializerExtended(Serializer):
    apps = HeterogeneousListSerializer(required=True)
    title = CharField(required=True)
    subtitle = CharField(required=True)
    mark_as_sponsored = BooleanField(required=True)
    type = CharField(required=True, source='card_type')
    external_ads = ListField(required=False, child=DictField())
    reserve = HeterogeneousListSerializer(required=True, source='reserve_apps')
    icon = URLField(required=False)
    rotation_interval = IntegerField(required=False)
    background_image = URLField(required=False)
    background_preview = CharField(required=False)

    def to_representation(self, instance):
        """ https://st.yandex-team.ru/ADVISOR-1064 """
        group = super(AppsBlockSerializerExtended, self).to_representation(instance)
        if group['mark_as_sponsored']:
            for app in group['apps'] + group['reserve']:
                app['mark_as_sponsored'] = False
        return group


class PlaceholderSetializer(Serializer):
    impression_id = ImpressionIDField(required=True)
    use_external_ads = BooleanField(required=True)


class RecommendationSerializer(BaseSerializer):
    OMIT_FIELDS_IF_EMPTY = 'expire_at',

    blocks = HeterogeneousListSerializer(required=True)
    expire_at = DateTimeField(required=False, format=None)
    title = CharField(required=True)
    next_page = URLField(required=False)


class VangaStatsSerializer(Serializer):
    stats = DictField(required=True)
    expired_at = CharField(required=True)


class VangaQuerySerializer(Serializer):
    packages = ListField(required=True, allow_empty=False)


class DefaultApplicationValidator(Serializer):
    category = IntegerField(required=True)
    package = CharField(required=True)


class ArrangerQueryValidator(Serializer):
    packages = ListField(child=CharField(validators=[RegexValidator('^.*/.*$')]), required=True, allow_empty=False)
    grid_width = IntegerField(required=True)
    grid_height = IntegerField(required=True)
    defaults = DefaultApplicationValidator(many=True, required=False, default=list)
