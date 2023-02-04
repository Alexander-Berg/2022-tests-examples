import pytest

from intranet.crt.utils.cvs import CvsClient
from intranet.crt.exceptions import CrtTimestampError

pytestmark = pytest.mark.django_db


class CvsTestClient(CvsClient):
    @classmethod
    def _execute(cls, command, cwd=''):
        pass


def test_fail_cvs_checkout(settings):
    settings.CRT_OS_USERNAME = 'fail'

    with pytest.raises(CrtTimestampError, match=r'Can not run cvs from user .*'):
        CvsTestClient()
