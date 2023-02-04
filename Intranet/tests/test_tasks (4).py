from mock import patch, Mock
import pytest

from staff.achievery.tasks import create_achievement_task
from staff.achievery.tests.factories.model import AchievementFactory
from staff.lib.testing import StaffFactory, GroupFactory


class MockedAchievementForm:

    def __init__(self, is_valid=True, cleaned_data=None, errors=None, *args, **kwargs):
        self.is_valid = lambda: is_valid
        self.achievement_data = {}
        self.achievement_icon_data = {}
        self.cleaned_data = cleaned_data or {}
        self.errors = errors or {}


@pytest.mark.django_db
def test_create_achievement_task():
    GroupFactory(url='__wiki__')

    fake_cleaned_data = {'creator': StaffFactory(), 'icon_url': ''}

    patch_achievement_form = patch(
        'staff.achievery.tasks.CreateAchievementForm',
        Mock(return_value=MockedAchievementForm(cleaned_data=fake_cleaned_data)),
    )

    achievement = AchievementFactory()
    patch_create_achievement = patch(
        'staff.achievery.tasks.Achievement.objects.create',
        Mock(return_value=achievement),
    )
    patch_create_icon = patch('staff.achievery.tasks.Icon.objects.create')
    patch_notification = patch('staff.achievery.tasks.AchievementCreated')
    patch_icon_ctl = patch(
        'staff.achievery.tasks.IconCtl',
        Mock(get_icon_data_from_url=Mock(return_value=('', ''))),
    )

    with patch_create_achievement as mocked_achievement_create:
        with patch_create_icon as mocked_icon_create:
            with patch_notification as mocked_notification, patch_achievement_form, patch_icon_ctl:
                create_achievement_task(fake_cleaned_data)

                mocked_achievement_create.assert_called_once()
                mocked_icon_create.assert_called_once()
                mocked_notification.assert_called_once_with(achievement, fake_cleaned_data['creator'])


@pytest.mark.django_db
def test_create_achievement_bad_data():

    patch_create_achievement = patch('staff.achievery.tasks.Achievement.objects.create')
    patch_notification = patch('staff.achievery.tasks.AchievementCreated')
    patch_achievement_form = patch(
        'staff.achievery.tasks.CreateAchievementForm',
        Mock(return_value=MockedAchievementForm(is_valid=False, errors={'field': ['error']})),
    )

    with patch_notification, patch_achievement_form, patch_create_achievement as mocked_create_achievement:
        create_achievement_task({})
        mocked_create_achievement.assert_not_called()
