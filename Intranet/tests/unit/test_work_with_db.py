import pytest

from at.common.utils import get_connection

from at.common.models import Itemsbulcaincrementer


@pytest.mark.django_db
class TestDbWorking(object):

    def test_select_from_db(self):
        # with get_connection() as conn:
        #     sql = 'select count(*) from ItemsBulcaIncrementer'
        #     conn.execute(sql)
        #     result = conn.fetchone()[0]
        result = Itemsbulcaincrementer.objects.count()
        assert result == 0
