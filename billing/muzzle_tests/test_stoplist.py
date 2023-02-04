
import datetime

from balance.exc import PERMISSION_DENIED
from muzzle.api import client as client_api
from tests.base import MuzzleTest
from tests.object_builder import ClientBuilder


dt = datetime.datetime.now()
dt_trunced = datetime.date(year=dt.year, month=dt.month, day=1)


class TestStoplist(MuzzleTest):

    def test_add_and_remove(self):

        ses = self.session
        entries_before = client_api.get_stoplist_entries(ses, dt)
        new_client = ClientBuilder().build(ses).obj
        client_api.add_to_stoplist(ses, new_client.id, dt, 'SOME_TICKET')
        entries_after_adding = client_api.get_stoplist_entries(ses, dt)
        client_api.remove_from_stoplist(ses, new_client.id, dt)
        entries_after_removing = client_api.get_stoplist_entries(ses, dt)

        expected_added_diff = dict(client_id=new_client.id,
                                   ticket='SOME_TICKET')

        self.assertEqual(entries_before + [expected_added_diff],
                         entries_after_adding)
        self.assertEqual(entries_before, entries_after_removing)

        ses.rollback()
        ses.close()

    def test_permissions(self):
        ses = self.session

        sql = ''' delete from t_role_user
                  where passport_id = {}
              '''.format(ses.passport.passport_id)

        ses.execute(sql)

        try:
            client_api.add_to_stoplist(ses, 0, dt, 'SOME_TICKET')
        except PERMISSION_DENIED:
            pass
        else:
            assert False

        try:
            client_api.remove_from_stoplist(ses, 0, dt)
        except PERMISSION_DENIED:
            pass
        else:
            assert False
