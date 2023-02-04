import pytest

from staff.person.models import Staff as Person
from staff.emails.models import Email
from staff.emails.tests.factories import EmailFactory

from staff.lib.testing import StaffFactory
from staff.lib.sync_tools.diff_merger import DataDiffMerger
from staff.lib.sync_tools.datagenerators import DataGenerator


class TestDataGenerator(DataGenerator):

    all_ext_data = [
        {
            'person__uid': '1',
            'email': '2left',
            'is_main': False,
            'source_type': 'passport'
        },   # same
        {
            'person__uid': '1',
            'email': '2insert',
            'is_main': False,
            'source_type': 'passport'
        },  # insert
        {
            'person__uid': '1',
            'email': '2update',
            'is_main': False,
            'source_type': 'passport'
        }   # update
    ]

    queryset = Email.passport.all()

    sync_fields = ('email', 'person__uid', 'source_type')
    diff_fields = ('email', 'is_main')

    def set_related_object(self, obj, data):
        obj.person = Person.objects.get(uid=data['person__uid'])

    def ext_data_gen(self):
        return list(self.all_ext_data)


@pytest.mark.django_db
def test_data_diff_merger():
    person = StaffFactory(uid=1, login='pupkin')

    email_kw = dict(person=person, source_type='passport')

    EmailFactory(email='2update', is_main=True, **email_kw)
    EmailFactory(email='2delete', is_main=False, **email_kw)
    EmailFactory(email='2left', is_main=False, **email_kw)

    data_generator = TestDataGenerator(None)
    data_diff_merger = DataDiffMerger(data_generator)
    data_diff_merger.execute(True, True, True)

    assert data_diff_merger.created == 1
    assert data_diff_merger.updated == 1
    assert data_diff_merger.deleted == 1
    assert data_diff_merger.skipped == 1

    emails = {email['email']: email for email in Email.objects.values()}

    assert '2delete' not in emails
    assert '2left' in emails
    assert '2insert' in emails
    assert '2update' in emails
    assert emails['2update']['is_main'] is False
