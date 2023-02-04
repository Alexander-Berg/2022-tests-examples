import unittest
import logging

from maps.garden.sdk import utils as util


class AddInsertTracebackTest(unittest.TestCase):
    class T:
        pass

    def functionAddingTraceback(self, item, skip_frames):
        util.add_insert_traceback(item, skip_frames)

    def test_add_insert_traceback_reduced_stack(self):
        test_item = self.T()
        self.functionAddingTraceback(test_item, 1)
        self.assertEqual(test_item.insert_traceback.find('in functionAddingTraceback'), -1)

    def test_add_insert_traceback_simple(self):
        test_item = self.T()
        self.functionAddingTraceback(test_item, 0)
        self.assertTrue(hasattr(test_item, 'insert_traceback'))
        self.assertNotEqual(test_item.insert_traceback.find('in functionAddingTraceback'), -1)

    def test_empty_initialized_traceback(self):
        test_item = self.T()
        test_item.insert_traceback = ''
        self.functionAddingTraceback(test_item, 0)
        self.assertNotEqual(test_item.insert_traceback, '')


class DebugLogger(logging.Handler):
    def __init__(self, *args, **kwargs):
        self._messages = []
        logging.Handler.__init__(self, *args, **kwargs)

    def emit(self, record):
        self._messages.append(record.getMessage())

    def messages(self):
        return self._messages


class C:
    class D:
        pass


def test_get_full_class_path_with_objects():
    class_instance = C.D()
    class_name = util.get_full_class_path(class_instance)
    assert class_name.endswith("util_test.C.D")

    str_name = util.get_full_class_path("blah")
    assert str_name == "str"

    int_name = util.get_full_class_path(65535)
    assert int_name == "int"


def test_get_full_class_path_with_types():
    class_type = C.D
    class_name = util.get_full_class_path(class_type)
    assert class_name.endswith("util_test.C.D")

    str_name = util.get_full_class_path(str)
    assert str_name == "str"

    int_name = util.get_full_class_path(int)
    assert int_name == "int"


def test_get_object_url():
    assert util.get_object_url(test_get_object_url).startswith("https://a.yandex-team.ru/arc_vcs/maps")
