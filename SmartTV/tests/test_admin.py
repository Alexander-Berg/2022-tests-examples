import pytest
import mock

from django import forms
from django.core.files.uploadedfile import SimpleUploadedFile
from django.contrib.admin import AdminSite
from django.core import exceptions
from smarttv.droideka.proxy.admin import ValidIdentifierAdmin, Category2ModelForm, ensure_version_for_entity_created, \
    publish_categories2
from smarttv.droideka.proxy.models import PlatformModel, Versions, Category2Editable, Category2
from smarttv.droideka.proxy.models import ValidIdentifier
from smarttv.droideka.tests.mock import RequestStub, IdentifierFileContent
from smarttv.droideka.utils import PlatformType
from smarttv.droideka.proxy.constants.carousels import KpCarousel, CarouselType


@pytest.mark.django_db
class TestPublishCategories:
    TEST_CATEGORY_ID_1 = 'some_category_id_1'
    TEST_TITLE_1 = 'some_title_1'
    TEST_ICON_S3_KEY_1 = 'icon_s3_key_1'
    TEST_THUMBNAIL_S3_KEY_1 = 'thumbnail s3 key 1'
    TEST_LOGO_S3_KEY_1 = 'logo s3 key 1'
    TEST_BANNER_S3_KEY_1 = 'banner S3 key 1'
    TEST_DESCRIPTION_1 = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. \n' \
                         'Suspendisse non sollicitudin augue. Donec lobortis erat.'
    TEST_CONTENT_TYPE_1 = 'side_menu'
    TEST_RANK_1 = 1

    TEST_CATEGORY_ID_2 = 'some_category_id_2'
    TEST_TITLE_2 = 'some_title_2'
    TEST_ICON_S3_KEY_2 = 'icon_s3_key_2'
    TEST_THUMBNAIL_S3_KEY_2 = 'thumbnail s3 key 2'
    TEST_LOGO_S3_KEY_2 = 'logo s3 key 2'
    TEST_BANNER_S3_KEY_2 = 'banner S3 key 2'
    TEST_DESCRIPTION_2 = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. \n' \
                         'Suspendisse non sollicitudin augue. Donec lobortis erat.'
    TEST_CONTENT_TYPE_2 = 'side_menu'
    TEST_RANK_2 = 2

    @staticmethod
    def assert_categories_content_same(category_1, category_2):
        assert category_1.category_id == category_2.category_id
        assert category_1.title == category_2.title
        assert category_1.icon_s3_key == category_2.icon_s3_key
        assert category_1.rank == category_2.rank
        assert category_1.thumbnail_s3_key == category_2.thumbnail_s3_key
        assert category_1.logo_s3_key == category_2.logo_s3_key
        assert category_1.banner_S3_key == category_2.banner_S3_key
        assert category_1.description == category_2.description
        assert category_1.carousel_type == category_2.carousel_type

    def make_test_category_1_without_relations(self):
        Category2Editable(
            category_id=self.TEST_CATEGORY_ID_1,
            title=self.TEST_TITLE_1,
            icon_s3_key=self.TEST_ICON_S3_KEY_1,
            rank=self.TEST_RANK_1,
            thumbnail_s3_key=self.TEST_THUMBNAIL_S3_KEY_1,
            logo_s3_key=self.TEST_LOGO_S3_KEY_1,
            banner_S3_key=self.TEST_BANNER_S3_KEY_1,
            description=self.TEST_DESCRIPTION_1,
            content_type=self.TEST_CONTENT_TYPE_1,
            carousel_type=CarouselType.TYPE_SQUARE,
        ).save()

    def make_test_category_2_without_relations(self):
        Category2Editable(
            category_id=self.TEST_CATEGORY_ID_2,
            title=self.TEST_TITLE_2,
            icon_s3_key=self.TEST_ICON_S3_KEY_2,
            rank=self.TEST_RANK_2,
            thumbnail_s3_key=self.TEST_THUMBNAIL_S3_KEY_2,
            logo_s3_key=self.TEST_LOGO_S3_KEY_2,
            banner_S3_key=self.TEST_BANNER_S3_KEY_2,
            description=self.TEST_DESCRIPTION_2,
            content_type=self.TEST_CONTENT_TYPE_2,
        ).save()

    def test_editable_category_without_relations_published(self):
        assert Category2.objects.count() == 0
        assert Category2Editable.objects.count() == 0

        self.make_test_category_1_without_relations()

        publish_categories2(None, None, None)

        expected = Category2Editable.objects.first()
        published = Category2.objects.first()
        self.assert_categories_content_same(expected, published)

    def test_editable_category_with_parent_category_published(self):
        assert Category2.objects.count() == 0
        assert Category2Editable.objects.count() == 0

        self.make_test_category_1_without_relations()
        self.make_test_category_2_without_relations()
        first_category = Category2Editable.objects.filter(category_id=self.TEST_CATEGORY_ID_1).first()
        first_category.parent_category = Category2Editable.objects.filter(
            category_id=self.TEST_CATEGORY_ID_2).first()
        first_category.save()

        publish_categories2(None, None, None)

        first_published_category = Category2.objects.filter(category_id=self.TEST_CATEGORY_ID_1).first()
        second_published_category = Category2.objects.filter(category_id=self.TEST_CATEGORY_ID_2).first()

        assert first_published_category.parent_category == second_published_category

    def test_remove_parent_category_relation(self):
        self.test_editable_category_with_parent_category_published()

        first_category = Category2Editable.objects.filter(category_id=self.TEST_CATEGORY_ID_1).first()
        first_category.parent_category = None
        first_category.save()

        publish_categories2(None, None, None)

        first_published_category = Category2.objects.filter(category_id=self.TEST_CATEGORY_ID_1).first()

        assert first_published_category.parent_category is None

    @pytest.mark.parametrize('relation_attr', [
        'exclude_platforms',
        'include_platforms',
        'above_platforms',
        'below_platforms',
    ])
    def test_add_platform_relation(self, relation_attr):
        assert Category2.objects.count() == 0
        assert Category2Editable.objects.count() == 0
        any_platform = PlatformModel.objects.filter(platform_type=PlatformType.ANY).first()
        none_platform = PlatformModel.objects.filter(platform_type=PlatformType.NONE).first()

        self.make_test_category_1_without_relations()

        first_category = Category2Editable.objects.filter(category_id=self.TEST_CATEGORY_ID_1).first()
        getattr(first_category, relation_attr).add(any_platform)
        getattr(first_category, relation_attr).add(none_platform)

        publish_categories2(None, None, None)

        first_published_category = Category2.objects.filter(category_id=self.TEST_CATEGORY_ID_1).first()
        assert getattr(first_published_category, relation_attr).filter(
            platform_type=PlatformType.ANY).first() == any_platform
        assert getattr(first_published_category, relation_attr).filter(
            platform_type=PlatformType.NONE).first() == none_platform

    def test_invisible_category_not_published(self):
        assert Category2.objects.count() == 0
        assert Category2Editable.objects.count() == 0

        category1 = Category2Editable(category_id='1', title='1', rank=10, content_type='1',)
        category1.save()
        category2 = Category2Editable(category_id='2', title='2', rank=20, content_type='2', visible=False)
        category2.save()
        category3 = Category2Editable(category_id='3', title='3', rank=10, content_type='3',
                                      parent_category_id=category1.id)
        category3.save()
        category4 = Category2Editable(category_id='4', title='4', rank=10, content_type='4',
                                      parent_category_id=category2.id)
        category4.save()

        publish_categories2(None, None, None)

        # published
        assert Category2.objects.filter(category_id='1').first()
        assert Category2.objects.filter(category_id='3').first()

        with pytest.raises(exceptions.ObjectDoesNotExist):
            Category2.objects.get(category_id='2')
        with pytest.raises(exceptions.ObjectDoesNotExist):
            Category2.objects.get(category_id='4')


@pytest.mark.django_db
@mock.patch('django.shortcuts.render', mock.Mock(return_value=None))
class TestImportIdentifiers:

    model_admin = ValidIdentifierAdmin(model=ValidIdentifier, admin_site=AdminSite())

    @pytest.fixture(autouse=True)
    def clean_db(self):
        yield
        ValidIdentifier.objects.all().delete()

    def get_request(self, file_content):
        request = RequestStub(method='POST')
        request.FILES['identifiers_file'] = SimpleUploadedFile('identifiers_file', file_content.encode(), 'text/plain')
        return request

    @pytest.mark.parametrize('mac_type, file_content', [
        (ValidIdentifier.WIFI_MAC, IdentifierFileContent.wi_fi_valid_content),
        (ValidIdentifier.ETHERNET_MAC, IdentifierFileContent.ethernet_valid_content),
    ])
    def test_import_from_file(self, mac_type, file_content):
        request = self.get_request(file_content)

        self.model_admin.import_from_file(request)
        identifier: ValidIdentifier = ValidIdentifier.objects.first()
        assert identifier.type == mac_type
        assert identifier.value == IdentifierFileContent.VALID_MAC.lower()

    @pytest.mark.parametrize('file_content', [
        IdentifierFileContent.invalid_mac_type_content,
        IdentifierFileContent.empty_mac_value,
        IdentifierFileContent.invalid_mac_value,
    ])
    def test_invalid_mac_type(self, file_content):
        request = self.get_request(file_content)

        assert ValidIdentifier.objects.count() == 0

        self.model_admin.import_from_file(request)

        assert ValidIdentifier.objects.count() == 0


class TestValidateCategoryFormTest:
    model_form = Category2ModelForm()

    def test_validate_rank_ok(self):
        self.model_form.cleaned_data = {'content_type': '', 'rank': 123}

        assert self.model_form.clean_rank() == 123

    def test_validate_rank_required(self):
        self.model_form.cleaned_data = {'content_type': '', 'rank': None}

        with pytest.raises(forms.ValidationError):
            self.model_form.clean_rank()

    def test_validate_rank_not_required(self):
        self.model_form.cleaned_data = {'content_type': 'some_content_type', 'rank': None}

        self.model_form.clean_rank()

    def test_validate_position_ok(self):
        self.model_form.cleaned_data = {'content_type': KpCarousel.TYPE, 'position': 123}

        assert self.model_form.clean_position() == 123

    def test_validate_position_required(self):
        self.model_form.cleaned_data = {'content_type': KpCarousel.TYPE, 'position': None}

        with pytest.raises(forms.ValidationError):
            self.model_form.clean_position()

    def test_validate_position_not_required(self):
        self.model_form.cleaned_data = {'content_type': 'some_content_type', 'position': None}

        self.model_form.clean_position()


@pytest.mark.django_db
class TestEnsureVersionForEntityCreated:

    entity = 'test_entity'

    @pytest.fixture(autouse=True)
    def clean_db(self):
        yield
        Versions.objects.all().delete()

    def test_created_entity_with_zero_version(self):
        assert Versions.objects.count() == 0

        version = ensure_version_for_entity_created(self.entity)

        assert version == Versions.objects.first()
        assert version.entity == self.entity
        assert version.version == 0

    def test_existing_version_returned(self):
        Versions(entity=self.entity, version=31).save()

        version = ensure_version_for_entity_created(self.entity)

        assert Versions.objects.count() == 1
        assert version.entity == self.entity
        assert version.version == 31
