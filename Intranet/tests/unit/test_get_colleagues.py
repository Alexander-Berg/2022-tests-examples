import unittest
import pytest
import json

from unittest.mock import patch, MagicMock

from at.aux_.models import Person
from at.pump.tasks import autofriendship_manage

pytestmark = pytest.mark.django_db

USER_LOGIN = 'friendly_user'
USER_UID = 1120000000434185
GROUP_ID = 102863
CLUBS = [4611686018427387915, 4611686018427388043, 4611686018427388363, 4611686018427388195]


@pytest.fixture
def create_persons():
    with open('tests/test_fixtures/colleagues.txt') as file_ids:
        for uid in file_ids.read().split(', '):
            Person.objects.create(login=f"user_{uid[-4:]}", person_id=int(uid), community_type='NONE', has_access=True)
    for club_id in CLUBS:
        Person.objects.create(person_id=club_id, community_type='OPENED_COMMUNITY')
    Person.objects.create(login=USER_LOGIN, person_id=USER_UID, community_type='NONE', has_access=True)


class TestAutoFriendship(unittest.TestCase):
    @staticmethod
    def load_api():
        with open('tests/test_fixtures/person.json') as person_file, \
             open('tests/test_fixtures/group.json') as group_file, \
             open('tests/test_fixtures/members.json') as members_file, \
             open('tests/test_fixtures/parent_group.json') as parent_group_file:
            person = json.load(person_file)
            group = json.load(group_file)
            members = json.load(members_file)
            parent_group = json.load(parent_group_file)
        return person, group, members, parent_group

    def init_mocked_api(self):
        person, group, members, parent_group = self.load_api()

        mocked_api = MagicMock()
        persons_filter = MagicMock()
        group_filter = MagicMock()
        parent_group_filter = MagicMock()
        group_fields = MagicMock()

        staff_api_filter = {
            ('person__uid', 'group__type'): group_filter,
            ('group__id', 'group__type', 'person__official__is_dismissed'): group_filter,
            ('person__official__is_dismissed', 'group__ancestors__id'): parent_group_filter,
            ('person__official__is_dismissed', 'group__id'): parent_group_filter,
        }
        staff_api_fields = {
            ('group', 'group.ancestors'): group,
            ('person',): members,
        }

        def filter_side_effect(**kwargs):
            return staff_api_filter.get(tuple(kwargs.keys()))

        def fields_side_effect(*args):
            return staff_api_fields.get(tuple(*args))

        persons_filter.fields.return_value = person
        group_filter.fields.side_effect = fields_side_effect
        group_fields.sort.side_effect = parent_group
        parent_group_filter.fields.return_value = group_fields

        mocked_api.persons.filter.return_value = persons_filter
        mocked_api.groupmembership.filter.side_effect = filter_side_effect

        return mocked_api

    @pytest.mark.usefixtures('create_persons')
    def test_autofriend(self):
        mocked_staff = MagicMock()
        mocked_handler = MagicMock()
        mocked_staff.return_value = self.init_mocked_api()
        mocked_handler.put_event.return_value = None
        with patch('at.common.staff.Api', new=mocked_staff), \
             patch('at.pump.HandlerRegistry', new=mocked_handler):
            autofriendship_manage(uid=USER_UID)
