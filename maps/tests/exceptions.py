import unittest

from maps.garden.sdk.core.exceptions import (
    GardenError,
    RetryTaskError, DEFAULT_YT_ERROR_RETRY_BACKOFF_MULTIPLIER,
    ResourceNotExistsPhysicallyError)
from maps.garden.sdk.utils import pickle_utils


class PickleTest(unittest.TestCase):
    def _test_pickle_unpickle(self, ex):
        unpickled = pickle_utils.loads(pickle_utils.dumps(ex))
        self.assertEqual(str(ex), str(unpickled))
        self.assertEqual(repr(ex), repr(unpickled))

    def test_generic(self):
        test = self._test_pickle_unpickle

        test(GardenError("an error has occurred"))

        test(ResourceNotExistsPhysicallyError("name", "key"))

        test(RetryTaskError())
        test(RetryTaskError(countdown=5))
        test(RetryTaskError(exc=GardenError("error")))
        test(RetryTaskError(countdown=5, max_retries=10, exc=GardenError("e")))

    def test_task_retry_error_compatibility(self):
        obj = RetryTaskError(countdown=5, max_retries=10, exc=GardenError("e"))
        obj.args = (obj.args[0], obj.args[1], obj.args[2])
        unpickled = pickle_utils.loads(pickle_utils.dumps(obj))
        self.assertEqual(DEFAULT_YT_ERROR_RETRY_BACKOFF_MULTIPLIER, unpickled.backoff_multiplier)
