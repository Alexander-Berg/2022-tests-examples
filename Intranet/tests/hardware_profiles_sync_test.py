from mock import patch
import pytest

from django.conf import settings

from staff.lib.sync_tools import DataDiffMerger, Updater
from staff.preprofile.models import HardwareProfile
from staff.preprofile.sync_hardware_profiles import ProfilesDataSource, ProfilesDataGenerator
from staff.preprofile.tests.utils import HardwareFactory


sample_result = [
    {
        'id': 119,
        'url': 'yandex_exp_9053',
        'name': 'Я.Облако стандартный профиль (Linux + YubiKey)',
        'comment': '1',
        'owner': 'agrebenyuk',
        'confirm': '1',
        'type': 'default',
    },
    {
        'id': 117,
        'url': 'yandex_exp_9053',
        'name': 'Я.Облако стандартный профиль (MAC + YubiKey)',
        'comment': '1',
        'owner': 'agrebenyuk',
        'confirm': '1',
        'type': 'default',
    },
]


def _create_updater() -> Updater:
    data_source = ProfilesDataSource(settings)
    data_generator = ProfilesDataGenerator(data_source)
    data_diff_merger = DataDiffMerger(data_generator)
    updater = Updater(data_diff_merger, None)
    updater.source_type = 'Hardware profiles'
    return updater


@pytest.mark.django_db
@patch('staff.preprofile.sync_hardware_profiles.ProfilesDataSource.get_all_profiles')
def test_updater(get_all_profiles_mock):
    # given
    get_all_profiles_mock.return_value = sample_result
    updater = _create_updater()

    # when
    updater.run_sync()

    # then
    assert HardwareProfile.objects.count() == 2


@pytest.mark.django_db
@patch('staff.preprofile.sync_hardware_profiles.ProfilesDataSource.get_all_profiles')
def test_updater_is_not_deleting_existing_profiles(get_all_profiles_mock):
    # given
    HardwareFactory(
        profile_id='119',
        url='yandex_exp_9053',
        name='Я.Облако стандартный профиль (Linux + YubiKey)',
        comment='1',
    )
    get_all_profiles_mock.return_value = sample_result
    updater = _create_updater()

    # when
    updater.run_sync()

    # then
    assert HardwareProfile.objects.count() == 2
