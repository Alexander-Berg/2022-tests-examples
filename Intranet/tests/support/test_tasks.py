# -*- coding: utf-8 -*-
from django.test import TestCase
from unittest.mock import patch

from events.support import tasks


class TestSupportTasks(TestCase):
    def test_change_owner(self):
        with patch('events.support.utils._change_owner') as mock_change:
            tasks.change_owner(123, email='user@yandex.ru')
        mock_change.assert_called_once_with(123, None, None, 'user@yandex.ru')
