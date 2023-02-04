from unittest.mock import patch

from django.test import TransactionTestCase

from lms.actions.tests.factories import AddAchievementEventFactory


class AddAchievementEventPostSaveHandlerTestCase(TransactionTestCase):
    def test_add_achievement_event_post_save(self):
        with patch('lms.actions.receivers.add_achievement_task.delay') as mock:
            event = AddAchievementEventFactory()
            mock.assert_called_once_with(add_achievement_event_id=event.id)
