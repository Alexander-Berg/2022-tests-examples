from django.test import TestCase

from staff.lib.testing import GroupMembershipFactory
from staff.lib.testing import StaffFactory, GroupFactory

from .. import serializers

from .factories.domain import GivenAchievementFactory


class GivenSerializerTestCase(TestCase):
    def setUp(self):
        self.user = StaffFactory()
        self.admins_grp = GroupFactory(name='admins')
        self._membership = GroupMembershipFactory(staff=self.user,
                                                  group=self.admins_grp)

    def test_fields(self):
        given = GivenAchievementFactory(user=self.user)

        serializer = serializers.GivenAchievementSerializer(
            instance=given,
            context={'fields': ['id']},
        )

        self.assertDictEqual(serializer.data, {'id': given.id})
