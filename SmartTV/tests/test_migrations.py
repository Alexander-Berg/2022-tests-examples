import uuid

import pytest
from django.db.utils import IntegrityError

from plus.utils.db.migrations import check_not_created_migrations
from smarttv.droideka.utils import PlatformType

SERIAL_1 = 'serial1'
SERIAL_2 = 'serial2'
WIFI_MAC_1 = '00:25:90:94:2a:f2'
WIFI_MAC_2 = '00:25:90:94:2a:f3'
ETHERNET_MAC_1 = '00:25:90:e4:d0:29'
ETHERNET_MAC_2 = '00:25:90:e4:d0:2a'
SOME_PUID = 123456
OTHER_PUID = 654321


def test_migration_not_created():
    check_not_created_migrations()


def clean_db(state):
    models = (
        'Category',
        'CategoryExtended',
        'CategoryExtendedEditable',
        'Versions',
        'Device',
        'PlatformModel',
    )
    for model_name in models:
        try:
            model = state.apps.get_model('proxy', model_name)
            model.objects.all().delete()
        except Exception:
            continue


@pytest.mark.django_db
def test_migrations_initial(migrator):
    """Ensures that the initial migration works."""
    old_state = migrator.apply_initial_migration(('proxy', None))

    with pytest.raises(LookupError):
        # Models does not yet exist:
        old_state.apps.get_model('proxy', 'Category')
        old_state.apps.get_model('proxy', 'Versions')

    new_state = migrator.apply_tested_migration(('proxy', '0001_initial'))
    # After the initial migration is done, we can use the model state:
    new_state.apps.get_model('proxy', 'Category')
    new_state.apps.get_model('proxy', 'Versions')

    clean_db(new_state)


@pytest.mark.django_db
def test_migrations_screensaver(migrator):
    """Ensures that the initial migration works."""
    old_state = migrator.apply_initial_migration(('proxy', '0001_initial'))

    with pytest.raises(LookupError):
        # Models does not yet exist:
        old_state.apps.get_model('proxy', 'ScreenSaver')

    new_state = migrator.apply_tested_migration(('proxy', '0002_auto_20200111_2251'))
    # After the initial migration is done, we can use the model state:
    new_state.apps.get_model('proxy', 'ScreenSaver')

    clean_db(new_state)


@pytest.mark.django_db
def test_migrations_device(migrator):
    """Ensures that the initial migration works."""
    old_state = migrator.apply_initial_migration(('proxy', '0002_auto_20200111_2251'))

    with pytest.raises(LookupError):
        # Models does not yet exist:
        old_state.apps.get_model('proxy', 'Device')

    new_state = migrator.apply_tested_migration(('proxy', '0003_auto_20200219_1635'))
    # After the initial migration is done, we can use the model state:
    new_state.apps.get_model('proxy', 'Device')

    clean_db(new_state)


@pytest.mark.django_db
def test_migrations_device_unique(migrator):
    """Ensures that the initial migration works."""
    old_state = migrator.apply_initial_migration(('proxy', '0003_auto_20200219_1635'))
    Device = old_state.apps.get_model('proxy', 'Device')

    for _ in range(2):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1).save()
    Device(serial_number=SERIAL_2, wifi_mac=WIFI_MAC_2, ethernet_mac=ETHERNET_MAC_2).save()

    assert Device.objects.filter(serial_number=SERIAL_1).count() == 2
    assert Device.objects.filter(serial_number=SERIAL_2).count() == 1

    new_state = migrator.apply_tested_migration(('proxy', '0004_auto_20200317_1746'))
    Device = new_state.apps.get_model('proxy', 'Device')
    Device.objects.filter(serial_number=SERIAL_1).count() == 1

    # Creating duplicate devices is prohibited
    with pytest.raises(IntegrityError):
        Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1).save()

    # Duplicates are deleted
    assert Device.objects.filter(serial_number=SERIAL_1).count() == 1
    assert Device.objects.filter(serial_number=SERIAL_2).count() == 1

    clean_db(new_state)


@pytest.mark.django_db
def test_migrations_shared_prefs(migrator):
    old_state = migrator.apply_initial_migration(('proxy', '0006_auto_20200414_1112'))
    with pytest.raises(LookupError):
        old_state.apps.get_model('proxy', 'SharedPreferences')
    new_state = migrator.apply_tested_migration(('proxy', '0007_auto_20200518_1535'))
    new_state.apps.get_model('proxy', 'SharedPreferences')

    clean_db(new_state)


@pytest.mark.django_db
def test_migrations_device_kp_gifts_id(migrator):
    """Ensures that the initial migration works."""
    old_state = migrator.apply_initial_migration(('proxy', '0008_auto_20200525_1507'))
    Device = old_state.apps.get_model('proxy', 'Device')

    Device(serial_number=SERIAL_1, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1).save()

    new_state = migrator.apply_tested_migration(('proxy', '0009_auto_20200522_2101'))
    Device = new_state.apps.get_model('proxy', 'Device')
    device = Device.objects.filter(serial_number=SERIAL_1).first()
    assert device.kp_gifts_id is None
    assert device.kp_gifts_given is False

    Device(serial_number=SERIAL_2, wifi_mac=WIFI_MAC_1, ethernet_mac=ETHERNET_MAC_1, kp_gifts_id=uuid.uuid4()).save()
    device = Device.objects.filter(serial_number=SERIAL_2).first()
    assert isinstance(device.kp_gifts_id, uuid.UUID)

    clean_db(new_state)


@pytest.mark.django_db
def test_tables_platform_model_and_category_extended_created(migrator):
    old_state = migrator.apply_initial_migration(('proxy', '0010_auto_20200526_0904'))
    with pytest.raises(LookupError):
        old_state.apps.get_model('proxy', 'PlatformModel')
    with pytest.raises(LookupError):
        old_state.apps.get_model('proxy', 'CategoryExtended')

    new_state = migrator.apply_tested_migration(('proxy', '0011_auto_20200608_1906'))
    new_state.apps.get_model('proxy', 'PlatformModel')
    new_state.apps.get_model('proxy', 'CategoryExtended')

    clean_db(new_state)


@pytest.mark.django_db
def test_initial_platforms_created(migrator):
    old_state = migrator.apply_initial_migration(('proxy', '0011_auto_20200608_1906'))
    PlatformModel = old_state.apps.get_model('proxy', 'PlatformModel')
    assert PlatformModel.objects.all().count() == 0

    new_state = migrator.apply_tested_migration(('proxy', '0012_auto_20200608_1318'))
    PlatformModel = new_state.apps.get_model('proxy', 'PlatformModel')

    assert PlatformModel.objects.all().count() == 2
    PlatformModel.objects.get(platform_type=PlatformType.ANY)
    PlatformModel.objects.get(platform_type=PlatformType.NONE)

    clean_db(new_state)


@pytest.mark.django_db
def test_actual_categories_copied(migrator):
    actual_version = 1
    old_state = migrator.apply_initial_migration(('proxy', '0010_auto_20200526_0904'))

    Versions = old_state.apps.get_model('proxy', 'Versions')
    Versions(entity='Category', version=actual_version).save()  # create actual version for category

    Category = old_state.apps.get_model('proxy', 'Category')
    Category(category_id='1', title='test category 1', rank=1, parent_category_id=None, version=0).save()
    Category(category_id='2', title='test category 2', rank=1, parent_category_id=None, version=actual_version).save()

    old_state = migrator.apply_tested_migration(('proxy', '0011_auto_20200608_1906'))
    CategoryExtended = old_state.apps.get_model('proxy', 'CategoryExtended')
    assert CategoryExtended.objects.count() == 0

    new_state = migrator.apply_tested_migration(('proxy', '0012_auto_20200608_1318'))
    CategoryExtended = new_state.apps.get_model('proxy', 'CategoryExtended')
    assert CategoryExtended.objects.count() == 1

    assert CategoryExtended.objects.first().category_id == '2'

    clean_db(new_state)


@pytest.mark.django_db
def test_completely_remove_category_extended(migrator):
    old_state = migrator.apply_tested_migration(('proxy', '0012_auto_20200608_1318'))

    old_state.apps.get_model('proxy', 'CategoryExtended')

    new_state = migrator.apply_tested_migration(('proxy', '0013_auto_20200618_1556'))

    with pytest.raises(LookupError):
        # Model does not yet exist:
        new_state.apps.get_model('proxy', 'CategoryExtended')

    clean_db(new_state)


@pytest.mark.django_db
def test_create_category_extended(migrator):
    old_state = migrator.apply_tested_migration(('proxy', '0013_auto_20200618_1556'))

    with pytest.raises(LookupError):
        # Models does not yet exist:
        old_state.apps.get_model('proxy', 'CategoryExtended')
        old_state.apps.get_model('proxy', 'CategoryExtendedEditable')

    new_state = migrator.apply_tested_migration(('proxy', '0014_categoryextended_categoryextendededitable'))

    new_state.apps.get_model('proxy', 'CategoryExtended')
    new_state.apps.get_model('proxy', 'CategoryExtendedEditable')

    clean_db(new_state)


@pytest.mark.django_db
def test_categories_extended_copied(migrator):
    migrator.apply_initial_migration(('proxy', '0011_auto_20200608_1906'))
    old_state = migrator.apply_tested_migration(('proxy', '0012_auto_20200608_1318'))
    PlatformModel = old_state.apps.get_model('proxy', 'PlatformModel')
    assert PlatformModel.objects.all().count() > 0

    old_state = migrator.apply_tested_migration(('proxy', '0014_categoryextended_categoryextendededitable'))

    Versions = old_state.apps.get_model('proxy', 'Versions')
    Versions(entity='Category', version=1).save()

    Category = old_state.apps.get_model('proxy', 'Category')
    original_category = Category(category_id='some_id',
                                 title='some title',
                                 icon_s3_key='some key',
                                 rank=1,
                                 version=1)
    original_category.save()

    CategoryExtended = old_state.apps.get_model('proxy', 'CategoryExtended')
    assert CategoryExtended.objects.count() == 0

    new_state = migrator.apply_tested_migration(('proxy', '0015_auto_20200618_1618'))
    CategoryExtended = new_state.apps.get_model('proxy', 'CategoryExtended')

    assert CategoryExtended.objects.count() == 1
    extended_category = CategoryExtended.objects.first()
    assert extended_category.category_id == original_category.category_id
    assert extended_category.title == original_category.title
    assert extended_category.icon_s3_key == original_category.icon_s3_key
    assert extended_category.rank == original_category.rank

    clean_db(new_state)


@pytest.mark.django_db
def test_device_kp_ids_filling(migrator):
    old_state = migrator.apply_initial_migration(('proxy', '0019_auto_20200625_1512'))
    Device = old_state.apps.get_model('proxy', 'Device')
    device = Device(
        serial_number=SERIAL_1,
        wifi_mac=WIFI_MAC_1,
        ethernet_mac=ETHERNET_MAC_1,
        kp_gifts_given=None
    )
    device.save()
    assert device.kp_gifts_given is None
    assert device.kp_gifts_id is None

    new_state = migrator.apply_tested_migration(('proxy', '0020_auto_20200708_1358'))
    Device = new_state.apps.get_model('proxy', 'Device')
    device = Device.objects.get(serial_number=SERIAL_1)
    assert device.kp_gifts_given is False
    assert device.kp_gifts_id is not None

    clean_db(new_state)


@pytest.mark.django_db
def test_device_hardware_ids_filling(migrator):
    old_state = migrator.apply_initial_migration(('proxy', '0019_auto_20200625_1512'))
    Device = old_state.apps.get_model('proxy', 'Device')
    device = Device(
        serial_number=SERIAL_1,
        wifi_mac=WIFI_MAC_1,
        ethernet_mac=ETHERNET_MAC_1,
        kp_gifts_given=False
    )
    device.save()
    int_state = migrator.apply_tested_migration(('proxy', '0020_auto_20200708_1358'))
    Device = int_state.apps.get_model('proxy', 'Device')
    device = Device.objects.get(serial_number=SERIAL_1)
    assert device.hardware_id is None

    new_state = migrator.apply_tested_migration(('proxy', '0021_auto_20200718_0124'))
    Device = new_state.apps.get_model('proxy', 'Device')
    device = Device.objects.get(serial_number=SERIAL_1)
    assert device.hardware_id is not None

    clean_db(new_state)


@pytest.mark.django_db
def test_no_legacy_categories(migrator):
    state = migrator.apply_initial_migration(('proxy', '0022_auto_20200813_1646'))

    # models exists
    state.apps.get_model('proxy', 'Category')
    state.apps.get_model('proxy', 'CategoryExtended')
    state.apps.get_model('proxy', 'CategoryExtendedEditable')

    after = migrator.apply_tested_migration(('proxy', '0023_delete_category'))

    # model does not exists anymore
    with pytest.raises(LookupError):
        after.apps.get_model('proxy', 'Category')

    # models exists
    state.apps.get_model('proxy', 'CategoryExtended')
    state.apps.get_model('proxy', 'CategoryExtendedEditable')

    clean_db(state)


@pytest.mark.django_db
def test_0026_new_category_model_created(migrator):
    state = migrator.apply_tested_migration(('proxy', '0025_auto_20200914_1731'))

    with pytest.raises(LookupError):
        state.apps.get_model('proxy', 'Category2')
    with pytest.raises(LookupError):
        state.apps.get_model('proxy', 'Category2Editable')

    state = migrator.apply_tested_migration(('proxy', '0026_category2_category2editable'))
    state.apps.get_model('proxy', 'Category2')
    state.apps.get_model('proxy', 'Category2Editable')


@pytest.mark.django_db
def test_0026_new_category_model_copied_from_old_categories(migrator):
    state = migrator.apply_tested_migration(('proxy', '0025_auto_20200914_1731'))
    PlatformModel = state.apps.get_model('proxy', 'PlatformModel')
    any_platform = PlatformModel(platform_type='any')
    any_platform.save()
    none_platform = PlatformModel(platform_type='none')
    none_platform.save()
    platform_android = PlatformModel(platform_type='android', platform_version='7.1.1', app_version='1.2',
                                     device_manufacturer='man1', device_model='mod1')
    platform_android.save()

    CategoryExtended = state.apps.get_model('proxy', 'CategoryExtended')
    cat1 = CategoryExtended(
        category_id='test1', title='title 1', icon_s3_key='s3 1 key', rank=15, position=16, thumbnail_s3_key='thum 1',
        logo_s3_key='logo 1', banner_S3_key='banner 1', description='desc 1')
    cat1.save()
    cat1.exclude_platforms.add(none_platform)
    cat1.include_platforms.add(any_platform)
    cat1.above_platforms.add(none_platform)
    cat1.below_platforms.add(none_platform)

    cat2 = CategoryExtended(
        category_id='test2', title='title 2', icon_s3_key='s3 2 key', rank=18, position=19, thumbnail_s3_key='thum 2',
        logo_s3_key='logo 2', banner_S3_key='banner 2', description='desc 2', parent_category=cat1)
    cat2.save()
    cat2.exclude_platforms.add(none_platform)
    cat2.include_platforms.add(none_platform)
    cat2.above_platforms.add(platform_android)
    cat2.below_platforms.add(none_platform)

    CategoryExtendedEditable = state.apps.get_model('proxy', 'CategoryExtendedEditable')
    editable_category1 = CategoryExtendedEditable(
        category_id='editabletest1', title='editable title 1', icon_s3_key='editable s3 1 key', rank=3, position=4,
        thumbnail_s3_key='editable thum 1', logo_s3_key='editable logo 1', banner_S3_key='editable banner 1',
        description='editable desc 1', visible=False)
    editable_category1.save()
    editable_category1.exclude_platforms.add(any_platform)
    editable_category1.include_platforms.add(none_platform)
    editable_category1.above_platforms.add(none_platform)
    editable_category1.below_platforms.add(none_platform)

    editable_category2 = CategoryExtendedEditable(
        category_id='editable test2', title='editable title 2', icon_s3_key='editable s3 2 key', rank=8, position=11,
        thumbnail_s3_key='editable thum 2', logo_s3_key='editable logo 2', banner_S3_key='editable banner 2',
        description='editable desc 2', parent_category=editable_category1)
    editable_category2.save()
    editable_category2.exclude_platforms.add(none_platform)
    editable_category2.include_platforms.add(none_platform)
    editable_category2.above_platforms.add(none_platform)
    editable_category2.below_platforms.add(platform_android)

    state = migrator.apply_tested_migration(('proxy', '0026_category2_category2editable'))
    prod_models = (state.apps.get_model('proxy', 'CategoryExtended'), state.apps.get_model('proxy', 'Category2'))
    editable_models = (state.apps.get_model('proxy', 'CategoryExtendedEditable'), state.apps.get_model('proxy', 'Category2Editable'))

    for src_model, dst_model in (editable_models, prod_models):
        for src_category in src_model.objects.all():
            dst_category = dst_model.objects.filter(category_id=src_category.category_id).first()
            assert src_category.category_id == dst_category.category_id
            assert src_category.title == dst_category.title
            assert src_category.icon_s3_key == dst_category.icon_s3_key
            assert src_category.rank == dst_category.rank
            assert src_category.position == dst_category.position
            assert src_category.thumbnail_s3_key == dst_category.thumbnail_s3_key
            assert src_category.logo_s3_key == dst_category.logo_s3_key
            assert src_category.banner_S3_key == dst_category.banner_S3_key
            assert src_category.description == dst_category.description
            assert src_category.content_type == dst_category.content_type
            if src_category.parent_category is not None:
                assert src_category.parent_category_id == dst_category.parent_category.category_id
            else:
                assert dst_category.parent_category is None
            assert list(src_category.exclude_platforms.all()) == list(dst_category.exclude_platforms.all())
            assert list(src_category.include_platforms.all()) == list(dst_category.include_platforms.all())
            assert list(src_category.above_platforms.all()) == list(dst_category.above_platforms.all())
            assert list(src_category.below_platforms.all()) == list(dst_category.below_platforms.all())
