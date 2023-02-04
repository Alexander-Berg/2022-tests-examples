import pytest
from abc import ABC, abstractmethod

from smarttv.droideka.proxy.models import PlatformModel, Category2, CategoryExperiment, Versions
from smarttv.droideka.proxy.categories_provider import categories_provider, IncludeFilter, ExcludeFilter, \
    AboveFilter, categories_cache
from smarttv.droideka.utils import PlatformType, PlatformInfo, RequestInfo
from smarttv.droideka.proxy.constants.carousels import VhFeed
from smarttv.droideka.tests.mock import ExperimentsMock

default_platform_info = PlatformInfo()


@pytest.mark.django_db
class BaseDbQueryTestCase(ABC):
    ANY_PLATFORM = 'any'
    ANY_ANDROID_PLATFORM = 'any_android'
    ANY_WEB_PLATFORM = 'any_web'
    ANY_WEBOS = 'any_webos'
    WEBOS_1_3 = 'webos_1_3'
    ANDROID_7_1_0 = 'android_7_1_0'
    ANDROID_7_1_1 = 'android_7_1_1'
    ANDROID_7_1_0_BUILD_1_1 = 'android_7_1_0_build_1_1'
    ANDROID_7_1_0_BUILD_1_2 = 'android_7_1_0_build_1_2'
    ANDROID_7_1_0_BUILD_1_3 = 'android_7_1_0_build_1_3'
    ANDROID_7_1_1_BUILD_1_1 = 'android_7_1_1_build_1_1'
    ANDROID_7_1_1_BUILD_1_2 = 'android_7_1_1_build_1_2'
    ANDROID_7_1_1_BUILD_1_3 = 'android_7_1_1_build_1_3'
    ANDROID_8_0_0_BUILD_1_1 = 'android_8_0_0_build_1_1'
    ANDROID_8_0_0_BUILD_1_2 = 'android_8_0_0_build_1_2'
    ANDROID_8_0_0_BUILD_1_3 = 'android_8_0_0_build_1_3'
    ANDROID_7_1_0_MANUFACTURER_1 = 'android_7_1_0_man1'
    ANDROID_7_1_0_MANUFACTURER_2 = 'android_7_1_0_man2'
    ANDROID_7_1_0_MODEL_1 = 'android_7_1_0_mod1'
    ANDROID_7_1_0_MODEL_2 = 'android_7_1_0_mod2'
    ANDROID_7_1_0_BUILD_1_2_MANUFACTURER_2_MODEL_1 = 'android_7_1_0_build_1_2_man2_mod1'
    ANDROID_7_1_0_MANUFACTURER_2_MODEL_1 = 'android_7_1_0_man2_mod1'
    ANDROID_ANY_MANUFACTURER_1 = 'android_man1'
    ANDROID_ANY_QUASAR_1 = 'android_quasar1'

    def __init__(self, *args, **kwargs):
        super().__init__()
        self.id_value = 0

    @property
    def next_id(self):
        result = self.id_value
        self.id_value += 1
        return result

    @property
    def platforms(self):
        return [
            (
                PlatformModel(
                    platform_type=PlatformType.ANY,
                ),
                self.ANY_PLATFORM
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='',
                    app_version=''
                ),
                self.ANY_ANDROID_PLATFORM
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.1',
                    app_version=''
                ),
                self.ANDROID_7_1_1
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    app_version=''
                ),
                self.ANDROID_7_1_0
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.1',
                    app_version='1.2'
                ),
                self.ANDROID_7_1_1_BUILD_1_2
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.1',
                    app_version='1.1'
                ),
                self.ANDROID_7_1_1_BUILD_1_1
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    app_version='1.2'
                ),
                self.ANDROID_7_1_0_BUILD_1_2
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    device_manufacturer='man1',
                ),
                self.ANDROID_ANY_MANUFACTURER_1
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    device_manufacturer='man1',
                    quasar_platform='quasar1'
                ),
                self.ANDROID_ANY_QUASAR_1
            ),
        ]

    @property
    def category_target_platform(self):
        return None

    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        for platform_model, platform_name in self.platforms:
            platform_model.save()
            category_model = Category2(
                id=self.next_id,
                category_id=platform_name,
                title=platform_name,
                rank=0,
            )
            category_model.save()
            platforms = {
                'include': category_model.include_platforms,
                'exclude': category_model.exclude_platforms,
                'above': category_model.above_platforms,
                'below': category_model.below_platforms,
            }
            platforms[self.category_target_platform].add(platform_model)

        yield
        PlatformModel.objects.all().delete()
        Category2.objects.all().delete()
        Versions.objects.all().delete()

    @abstractmethod
    def get_android_categories(self, **kwargs):
        pass

    def base_android_test(self, category_filter, expected_category_names):
        categories = self.get_android_categories(**category_filter)

        actual_category_names = tuple(cat.category_id for cat in categories)

        assert sorted(expected_category_names) == sorted(actual_category_names)


@pytest.mark.django_db
class TestIncludedPlatformsQueryBuilder(BaseDbQueryTestCase):
    TEST_DATA = [
        (
            {PlatformInfo.KEY_PLATFORM_VERSION: '7.1.1', PlatformInfo.KEY_APP_VERSION: '1.2'},
            (
                BaseDbQueryTestCase.ANY_PLATFORM,
                BaseDbQueryTestCase.ANY_ANDROID_PLATFORM,
                BaseDbQueryTestCase.ANDROID_7_1_1,
                BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2,
            )
        ),
        (
            {PlatformInfo.KEY_PLATFORM_VERSION: '7.1.0', PlatformInfo.KEY_APP_VERSION: '1.2'},
            (
                BaseDbQueryTestCase.ANY_PLATFORM,
                BaseDbQueryTestCase.ANY_ANDROID_PLATFORM,
                BaseDbQueryTestCase.ANDROID_7_1_0,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_2,
            )
        ),
        (
            {PlatformInfo.KEY_PLATFORM_VERSION: '8.0.0', PlatformInfo.KEY_APP_VERSION: '1.3'},
            (
                BaseDbQueryTestCase.ANY_PLATFORM,
                BaseDbQueryTestCase.ANY_ANDROID_PLATFORM,
            )
        ),
        (
            {PlatformInfo.KEY_PLATFORM_VERSION: '8.0.0', PlatformInfo.KEY_DEVICE_MANUFACTURER: 'man1'},
            (
                BaseDbQueryTestCase.ANY_PLATFORM,
                BaseDbQueryTestCase.ANY_ANDROID_PLATFORM,
                BaseDbQueryTestCase.ANDROID_ANY_MANUFACTURER_1,
            )
        ),
        (
            {PlatformInfo.KEY_QUASAR_PLATFORM: 'quasar1', PlatformInfo.KEY_PLATFORM_VERSION: '7.1.0'},
            (
                BaseDbQueryTestCase.ANDROID_ANY_QUASAR_1
            )
        )
    ]

    @property
    def category_target_platform(self):
        return 'include'

    def get_android_categories(self, platform_version=None, app_version=None, device_manufacturer=None,
                               device_model=None, include=True):
        platform_info = PlatformInfo(PlatformType.ANDROID, platform_version, app_version, device_manufacturer,
                                     device_model)
        result = []
        for category_obj in Category2.objects.all():
            category = category_obj.to_json()
            categories_cache.patch_categories_platform_info({category_obj.category_id: category})
            if IncludeFilter().is_matched(category, platform_info):
                result.append(category_obj)

        return result

    @pytest.mark.parametrize('category_filter, expected_category_names', TEST_DATA)
    def test_android(self, category_filter, expected_category_names):
        self.base_android_test(category_filter, expected_category_names)


@pytest.mark.django_db
class TestAbovePlatformsQueryBuilder(BaseDbQueryTestCase):
    TEST_DATA = [
        (
            {PlatformInfo.KEY_PLATFORM_VERSION: '7.1.1', PlatformInfo.KEY_APP_VERSION: '1.2.684'},
            (
                BaseDbQueryTestCase.ANDROID_7_1_0,
                BaseDbQueryTestCase.ANDROID_7_1_1,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_1,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_2,
                BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_1,
                BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2,
            )
        ),
        (
            {
                PlatformInfo.KEY_PLATFORM_VERSION: '7.1.0',
                PlatformInfo.KEY_APP_VERSION: '1.1',
            },
            (
                BaseDbQueryTestCase.ANDROID_7_1_0,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_1,
            )
        ),
        (
            {PlatformInfo.KEY_PLATFORM_VERSION: '7.1.0', PlatformInfo.KEY_APP_VERSION: '1.0'},
            (
                BaseDbQueryTestCase.ANDROID_7_1_0,
            )
        ),
        (
            {
                PlatformInfo.KEY_PLATFORM_VERSION: '7.1.0',
                PlatformInfo.KEY_DEVICE_MANUFACTURER: 'man1'
            },
            (
                BaseDbQueryTestCase.ANDROID_7_1_0_MANUFACTURER_1,
                BaseDbQueryTestCase.ANDROID_7_1_0,
            )
        ),
        (
            {
                PlatformInfo.KEY_PLATFORM_VERSION: '7.1.0',
                PlatformInfo.KEY_APP_VERSION: '1.2',
                PlatformInfo.KEY_DEVICE_MANUFACTURER: 'man2'
            },
            (
                BaseDbQueryTestCase.ANDROID_7_1_0_MANUFACTURER_2,
                BaseDbQueryTestCase.ANDROID_7_1_0,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_1,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_2,
            )
        ),
        (
            {
                PlatformInfo.KEY_PLATFORM_VERSION: '7.1.0',
                PlatformInfo.KEY_DEVICE_MODEL: 'mod1'
            },
            (
                BaseDbQueryTestCase.ANDROID_7_1_0_MODEL_1,
                BaseDbQueryTestCase.ANDROID_7_1_0,
            )
        ),
        (
            {
                PlatformInfo.KEY_PLATFORM_VERSION: '7.1.0',
                PlatformInfo.KEY_APP_VERSION: '1.2',
                PlatformInfo.KEY_DEVICE_MODEL: 'mod2'
            },
            (
                BaseDbQueryTestCase.ANDROID_7_1_0_MODEL_2,
                BaseDbQueryTestCase.ANDROID_7_1_0,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_1,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_2,
            )
        ),
        (
            {
                PlatformInfo.KEY_PLATFORM_VERSION: '7.1.0',
                PlatformInfo.KEY_APP_VERSION: '1.2',
                PlatformInfo.KEY_DEVICE_MANUFACTURER: 'man2',
                PlatformInfo.KEY_DEVICE_MODEL: 'mod1',
            },
            (
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_2_MANUFACTURER_2_MODEL_1,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_1,
                BaseDbQueryTestCase.ANDROID_7_1_0_BUILD_1_2,
                BaseDbQueryTestCase.ANDROID_7_1_0_MANUFACTURER_2,
                BaseDbQueryTestCase.ANDROID_7_1_0_MODEL_1,
                BaseDbQueryTestCase.ANDROID_7_1_0,
            )
        ),
        (
            {PlatformInfo.KEY_PLATFORM_VERSION: '7.0.0', PlatformInfo.KEY_APP_VERSION: '1.3'},
            tuple()
        ),
    ]

    @property
    def category_target_platform(self):
        return 'above'

    @property
    def platforms(self):
        return [
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.1'
                ),
                self.ANDROID_7_1_1
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0'
                ),
                self.ANDROID_7_1_0
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.1',
                    app_version='1.3'
                ),
                self.ANDROID_7_1_1_BUILD_1_3
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.1',
                    app_version='1.2'
                ),
                self.ANDROID_7_1_1_BUILD_1_2
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.1',
                    app_version='1.1'
                ),
                self.ANDROID_7_1_1_BUILD_1_1
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    app_version='1.3'
                ),
                self.ANDROID_7_1_0_BUILD_1_3
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    app_version='1.2'
                ),
                self.ANDROID_7_1_0_BUILD_1_2
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    app_version='1.1'
                ),
                self.ANDROID_7_1_0_BUILD_1_1
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='8.0.0',
                    app_version='1.3'
                ),
                self.ANDROID_8_0_0_BUILD_1_3
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='8.0.0',
                    app_version='1.2'
                ),
                self.ANDROID_8_0_0_BUILD_1_2
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='8.0.0',
                    app_version='1.1'
                ),
                self.ANDROID_8_0_0_BUILD_1_1
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    device_manufacturer='man1'
                ),
                self.ANDROID_7_1_0_MANUFACTURER_1
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    device_manufacturer='man2'
                ),
                self.ANDROID_7_1_0_MANUFACTURER_2
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    device_model='mod1'
                ),
                self.ANDROID_7_1_0_MODEL_1
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    device_model='mod2'
                ),
                self.ANDROID_7_1_0_MODEL_2
            ),
            (
                PlatformModel(
                    platform_type=PlatformType.ANDROID,
                    platform_version='7.1.0',
                    app_version='1.2',
                    device_manufacturer='man2',
                    device_model='mod1'
                ),
                self.ANDROID_7_1_0_BUILD_1_2_MANUFACTURER_2_MODEL_1
            ),
        ]

    def get_android_categories(self, platform_version=None, app_version=None, device_manufacturer=None,
                               device_model=None, above=True):
        platform_info = PlatformInfo(PlatformType.ANDROID, platform_version, app_version, device_manufacturer,
                                     device_model)
        result = []
        for category_obj in Category2.objects.all():
            category = category_obj.to_json()
            categories_cache.patch_categories_platform_info({category_obj.category_id: category})
            if AboveFilter().is_matched(category, platform_info):
                result.append(category_obj)

        return result

    @pytest.mark.parametrize('category_filter, expected_category_names', TEST_DATA)
    def test_android(self, category_filter, expected_category_names):
        category_filter['above'] = True
        self.base_android_test(category_filter, expected_category_names)


@pytest.mark.django_db
class TestExcludeFromIncludedPlatforms:
    def __init__(self, *args, **kwargs):
        super().__init__()
        self.id_value = 0

    @property
    def next_id(self):
        result = self.id_value
        self.id_value += 1
        return result

    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        yield
        PlatformModel.objects.all().delete()
        Category2.objects.all().delete()
        Versions.objects.all().delete()

    def test_exclude_included_platform_empty_result(self):
        category = Category2(
            category_id=BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2,
            title=BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2,
            rank=0,
        )
        category.save()

        platform = PlatformModel(
            platform_type=PlatformType.ANDROID,
            platform_version='7.1.1',
            app_version='1.1'
        )
        platform.save()

        category.include_platforms.add(platform)
        category.exclude_platforms.add(platform)

        platform_info = PlatformInfo(PlatformType.ANDROID, '7.1.1', '1.1', None, None)
        result = []
        for category_obj in Category2.objects.all():
            category = category_obj.to_json()
            categories_cache.patch_categories_platform_info({category_obj.category_id: category})
            if IncludeFilter().is_matched(category, platform_info):
                result.append(category_obj)

        assert len(result) == 1
        assert result[0].category_id == BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2

        result = []
        for category_obj in Category2.objects.all():
            category = category_obj.to_json()
            categories_cache.patch_categories_platform_info({category_obj.category_id: category})
            if IncludeFilter().is_matched(category, platform_info) \
               and not ExcludeFilter().is_matched(category, platform_info):
                result.append(category_obj)
        assert len(result) == 0

    def test_exclude_not_related_platform_empty_result(self):
        category = Category2(
            category_id=BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2,
            title=BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2,
            rank=0,
        )
        category.save()

        platform = PlatformModel(
            platform_type=PlatformType.ANDROID,
            platform_version='7.1.1',
            app_version='1.2'
        )
        platform.save()
        category.include_platforms.add(platform)

        platform = PlatformModel(
            platform_type=PlatformType.ANDROID,
            platform_version='6.0.0',
            app_version='1.0'
        )
        platform.save()
        category.exclude_platforms.add(platform)

        platform_info = PlatformInfo(PlatformType.ANDROID, '7.1.1', '1.2', None, None)
        result = []
        for category_obj in Category2.objects.all():
            category = category_obj.to_json()
            categories_cache.patch_categories_platform_info({category_obj.category_id: category})
            if IncludeFilter().is_matched(category, platform_info):
                result.append(category_obj)

        assert len(result) == 1
        assert result[0].category_id == BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2

        result = []
        for category_obj in Category2.objects.all():
            category = category_obj.to_json()
            categories_cache.patch_categories_platform_info({category_obj.category_id: category})
            if IncludeFilter().is_matched(category, platform_info) \
               and not ExcludeFilter().is_matched(category, platform_info):
                result.append(category_obj)

        assert len(result) == 1
        assert result[0].category_id == BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2

    def test_exclude_above_platform_empty_result(self):
        category = Category2(
            id=self.next_id,
            category_id=BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2,
            title=BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2,
            rank=0,
        )
        category.save()

        platform = PlatformModel(
            platform_type=PlatformType.ANDROID,
            platform_version='7.1.1',
            app_version='1.2'
        )
        platform.save()

        category.above_platforms.add(platform)
        category.exclude_platforms.add(platform)

        platform_info = PlatformInfo(PlatformType.ANDROID, '7.1.1', '1.2', None, None)
        result = []
        for category_obj in Category2.objects.all():
            category = category_obj.to_json()
            categories_cache.patch_categories_platform_info({category_obj.category_id: category})
            if AboveFilter().is_matched(category, platform_info):
                result.append(category_obj)

        assert len(result) == 1
        assert result[0].category_id == BaseDbQueryTestCase.ANDROID_7_1_1_BUILD_1_2

        result = []
        for category_obj in Category2.objects.all():
            category = category_obj.to_json()
            categories_cache.patch_categories_platform_info({category_obj.category_id: category})
            if AboveFilter().is_matched(category, platform_info) \
               and not ExcludeFilter().is_matched(category, platform_info):
                result.append(category_obj)

        assert len(result) == 0


@pytest.mark.django_db
class TestRangePlatformQuery:
    default_request_info = RequestInfo(False, experiments=ExperimentsMock(experiments={}))

    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        any_platform = PlatformModel(platform_type=PlatformType.ANY)
        any_platform.save()
        none_platform = PlatformModel(platform_type=PlatformType.NONE)
        none_platform.save()
        homeapp_1_2 = PlatformModel(platform_type=PlatformType.ANDROID, app_version='1.2')
        homeapp_1_2.save()
        homeapp_1_4 = PlatformModel(platform_type=PlatformType.ANDROID, app_version='1.4')
        homeapp_1_4.save()
        test_category = Category2(id=1, category_id='test', title='Test', rank=10, content_type=VhFeed.TYPE)
        test_category.save()
        test_category.below_platforms.add(homeapp_1_4)
        test_category.above_platforms.add(homeapp_1_2)
        yield
        Category2.objects.all().delete()
        PlatformModel.objects.all().delete()

    @pytest.mark.parametrize('platform_info', [
        PlatformInfo(PlatformType.ANDROID, '7.1.1', '1.2', None, None),
        PlatformInfo(PlatformType.ANDROID, '7.1.1', '1.3', None, None),
        PlatformInfo(PlatformType.ANDROID, '7.1.1', '1.4', None, None),
    ])
    def test_category_in_range(self, platform_info):
        categories = categories_provider.get_categories(platform_info, request_info=self.default_request_info)
        assert len(categories) == 1
        assert categories[0].category_id == 'test'

    @pytest.mark.parametrize('platform_info', [
        PlatformInfo(PlatformType.ANDROID, '7.1.1', '1.1', None, None),
        PlatformInfo(PlatformType.ANDROID, '7.1.1', '1.5', None, None),
    ])
    def test_category_not_in_range(self, platform_info):
        categories = categories_provider.get_categories(platform_info, request_info=self.default_request_info)
        assert len(categories) == 0


@pytest.mark.django_db
class TestCategoryTargetingByRequestInfo:
    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        any_platform = PlatformModel(platform_type=PlatformType.ANY)
        any_platform.save()
        none_platform = PlatformModel(platform_type=PlatformType.NONE)
        none_platform.save()

        test_category1 = Category2(id=1, category_id='test_1', title='Test 1', rank=10,
                                   content_type=VhFeed.TYPE, authorization_required=False)
        test_category1.save()
        test_category1.below_platforms.add(none_platform)
        test_category1.above_platforms.add(none_platform)
        test_category1.exclude_platforms.add(none_platform)
        test_category1.include_platforms.add(any_platform)

        test_category2 = Category2(id=2, category_id='test_2', title='Test 2', rank=20,
                                   content_type=VhFeed.TYPE, authorization_required=True)
        test_category2.save()
        test_category2.below_platforms.add(none_platform)
        test_category2.above_platforms.add(none_platform)
        test_category2.exclude_platforms.add(none_platform)
        test_category2.include_platforms.add(any_platform)

        experiment = CategoryExperiment(value='known_exp')
        experiment.save()
        test_category3 = Category2(id=3, category_id='test_3', title='Test 3', rank=30,
                                   content_type=VhFeed.TYPE)
        test_category3.save()
        test_category3.below_platforms.add(none_platform)
        test_category3.above_platforms.add(none_platform)
        test_category3.exclude_platforms.add(none_platform)
        test_category3.include_platforms.add(any_platform)
        test_category3.category_experiments.add(experiment)
        yield
        Category2.objects.all().delete()
        PlatformModel.objects.all().delete()
        CategoryExperiment.objects.all().delete()

    @pytest.mark.parametrize('authorized, experiments, expected_categories', [
        (False, {}, ('test_1',)),
        (True, {}, ('test_1', 'test_2')),
        (False, {'category_experiments': ['unknown_exp']}, ('test_1',)),
        (False, {'category_experiments': ['known_exp']}, ('test_1', 'test_3')),
    ])
    def test_request_info_targeting(self, authorized, experiments, expected_categories):
        categories = categories_provider.get_categories(
            default_platform_info,
            request_info=RequestInfo(
                authorized=authorized,
                experiments=ExperimentsMock(experiments=experiments)))
        actual_categories = []
        for category in categories:
            actual_categories.append(category.category_id)
        actual_categories = tuple(actual_categories)

        assert expected_categories == actual_categories


@pytest.mark.django_db
class TestMutuallyExclusiveCategoryTargetingByRequestInfo:
    def __init__(self):
        self.test_category1 = None
        self.test_category2 = None
        self.test_category3 = None

    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        any_platform = PlatformModel(platform_type=PlatformType.ANY)
        any_platform.save()
        none_platform = PlatformModel(platform_type=PlatformType.NONE)
        none_platform.save()

        self.test_category1 = Category2(id=1, category_id='test_1', title='Test 1', rank=10,
                                        content_type=VhFeed.TYPE, authorization_required=False)
        self.test_category1.save()
        self.test_category1.below_platforms.add(none_platform)
        self.test_category1.above_platforms.add(none_platform)
        self.test_category1.exclude_platforms.add(none_platform)
        self.test_category1.include_platforms.add(any_platform)

        self.test_category2 = Category2(id=2, category_id='test_2', title='Test 2', rank=10,
                                        content_type=VhFeed.TYPE, authorization_required=False)
        self.test_category2.save()
        self.test_category2.below_platforms.add(none_platform)
        self.test_category2.above_platforms.add(none_platform)
        self.test_category2.exclude_platforms.add(none_platform)
        self.test_category2.include_platforms.add(any_platform)

        self.test_category3 = Category2(id=3, category_id='test_3', title='Test 3', rank=10,
                                        content_type=VhFeed.TYPE, authorization_required=False)
        self.test_category3.save()
        self.test_category3.below_platforms.add(none_platform)
        self.test_category3.above_platforms.add(none_platform)
        self.test_category3.exclude_platforms.add(none_platform)
        self.test_category3.include_platforms.add(any_platform)

        yield
        Category2.objects.all().delete()
        PlatformModel.objects.all().delete()
        CategoryExperiment.objects.all().delete()

    def add_exp_if_exists(self, models_field, exp_value):
        if exp_value:
            exp = CategoryExperiment(value=exp_value)
            exp.save()
            models_field.category_experiments.add(exp)

    @pytest.mark.parametrize('experiments, categ_2_experiment, categ_3_disable_experiment, expected_categories', [
        ([], None, None, ('test_1', 'test_3')),
        ([], 'a', None, ('test_1', 'test_3')),
        ([], None, 'b', ('test_1', 'test_2', 'test_3')),
        ([], 'a', 'b', ('test_1', 'test_3')),
        (['a'], None, None, ('test_1', 'test_2', 'test_3')),
        (['a'], 'a', None, ('test_1', 'test_2', 'test_3')),
        (['a'], 'a', 'b', ('test_1', 'test_2', 'test_3')),
        (['b'], 'a', None, ('test_1', 'test_2')),
        (['b'], None, 'b', ('test_1', 'test_2')),
        (['b'], 'a', 'b', ('test_1', )),
    ])
    def test_request_info_targeting(
            self, experiments, categ_2_experiment, categ_3_disable_experiment, expected_categories):
        self.add_exp_if_exists(self.test_category2.category_experiments, categ_2_experiment)
        self.add_exp_if_exists(self.test_category3.category_disable_experiments, categ_3_disable_experiment)

        categories = categories_provider.get_categories(
            default_platform_info,
            request_info=RequestInfo(
                authorized=False,
                experiments=ExperimentsMock(experiments={'category_experiments': experiments})))
        actual_categories = []
        for category in categories:
            actual_categories.append(category.category_id)
        actual_categories = tuple(actual_categories)

        assert expected_categories == actual_categories
