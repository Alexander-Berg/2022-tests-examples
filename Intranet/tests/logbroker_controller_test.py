import unittest

from mock import Mock, MagicMock

from staff.emission.logbroker.controller import Controller


class ControllerTest(unittest.TestCase):

    def test_mark_sent(self):
        test_unsent_data = ['test_value']
        storage_mock = Mock()
        unsent_mock = MagicMock()
        unsent_mock.filter = Mock(return_value=['test_value'])
        storage_mock.get_unsent_queryset = Mock(return_value=unsent_mock)

        ctl = Controller(storage=storage_mock)
        ctl.mark_sent(message_ids=[1, 2, 4])

        ctl.storage.mark_sent.assert_called_once_with(test_unsent_data)
