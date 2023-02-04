from __future__ import absolute_import

import importlib

from ya.skynet.util.unittest import TestCase, main
from ya.skynet.util.pickle import UnpicklingError

from ya.skynet.services.cqudp.client import unpickling


class TestUnpickling(TestCase):
    def test_whitelist(self):
        for module, attr in (
            ('ya.skynet.util.errors', '_makeError'),
            ('kernel.util.errors', '_makeError'),

            ('ya.skynet.services.cqudp.exceptions', 'CQueueExecutionError'),
            ('ya.skynet.services.cqudp.exceptions', 'CQueueExecutionFailure'),
            ('ya.skynet.services.cqudp.exceptions', 'CQueueNetworkError'),
            ('ya.skynet.services.cqudp.exceptions', 'CQueueAuthenticationError'),
            ('ya.skynet.services.cqudp.exceptions', 'CQueueAuthorizationError'),
            ('ya.skynet.services.cqudp.exceptions', 'CQueueRuntimeError'),
            ('ya.skynet.services.cqudp.exceptions', 'ReadableKeyError'),
            ('ya.skynet.services.cqudp.server.processhandle', 'CommunicationError'),
            ('ya.skynet.services.cqudp.server.processhandle', 'Signalled'),

            ('exceptions', 'StopIteration'),
            ('exceptions', 'MemoryError'),
            ('exceptions', 'SystemError'),
            ('exceptions', 'AttributeError'),
            ('exceptions', 'Exception'),
            ('exceptions', 'SystemExit'),
            ('exceptions', 'TypeError'),
            ('exceptions', 'ImportError'),
            ('__builtin__', 'set'),
            ('collections', 'OrderedDict'),

            ('api.copier.errors', 'ResourceDownloadError'),
            ('api.copier.errors', 'ResourceNotAvailable'),
            ('api.copier.errors', 'ApiError'),
            ('api.copier.errors', 'CopierError'),
        ):
            self.assertTrue(unpickling.check_in_whitelist(module, attr, None, False), str((module, attr)))

    def test_unpickling_skydev1101(self):
        items = (
            ('os', 'system'),
            ('subprocess', 'Popen')
        )

        changeable_items = (
            (__name__, 'TestClass'),
            (__name__, 'TestClass2'),
            (__name__, 'test_fn'),
        )

        for module, attr in items:
            with self.assertRaises(UnpicklingError):
                unpickling.import_if_permitted(module, attr, False)

            unpickling.import_if_permitted(module, attr, True)

        for module, attr in changeable_items:
            obj = getattr(importlib.import_module(module), attr)
            try:
                obj.allow_unpickle = False
                with self.assertRaises(UnpicklingError):
                    unpickling.import_if_permitted(module, attr, True)

                obj.allow_unpickle = True
                unpickling.import_if_permitted(module, attr, False)
            finally:
                if hasattr(obj, 'allow_unpickle'):
                    delattr(obj, 'allow_unpickle')


class TestClass(object):
    pass


class TestClass2:
    pass


def test_fn():
    pass


if __name__ == '__main__':
    raise SystemExit(main())
