import unittest
from ya_courier_backend.util import errors
from ya_courier_backend.util.mvrp_solver import check_valid_task_id


class ValidTaskId(unittest.TestCase):

    def test_valid_task_id(self):
        self.assertRaises(errors.UnprocessableEntity, check_valid_task_id, '@')
        self.assertRaises(errors.UnprocessableEntity, check_valid_task_id, 'some_message.')
        self.assertRaises(errors.UnprocessableEntity, check_valid_task_id, '/path')
        self.assertTrue(check_valid_task_id, 'some_message')
        self.assertTrue(check_valid_task_id, '7d9d72e0-a80c-4024-815b-935c6240edc0')
