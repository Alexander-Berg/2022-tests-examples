
from datetime import datetime
from xmlrpclib import Fault

from tests.base import MediumTest


SOME_WRONG_PAGE_ID = 987654321
SOME_DT = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)

EXPECTED_EXCEPTION_TYPE = Fault
EXPECTED_EXCEPTION_REGEXP = "Invalid parameter for function: Unhandlable page_id"


class TestGetDistributionMoney(MediumTest):

    def test_wrong_page_exception(self):

        self.assertRaisesRegexp(
            EXPECTED_EXCEPTION_TYPE, EXPECTED_EXCEPTION_REGEXP,
            self.xmlrpcserver.GetDistributionMoney,
            *(SOME_DT, SOME_DT, SOME_WRONG_PAGE_ID)
        )


if __name__ == '__main__':
    import unittest
    unittest.main()
