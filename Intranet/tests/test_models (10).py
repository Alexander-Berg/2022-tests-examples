from django.test import TestCase

from ..models import StaffLeadership, StaffProfile
from .factories import StaffGroupFactory, StaffLeadershipFactory, StaffProfileFactory


class StaffProfileTestCase(TestCase):
    def setUp(self) -> None:
        self.staff_profile = StaffProfileFactory()

    def test_is_head_false(self):
        StaffLeadershipFactory(
            profile=self.staff_profile,
            role=StaffLeadership.ROLE_CHIEF,
            is_active=False
        )
        StaffLeadershipFactory(
            profile=self.staff_profile,
            role=StaffLeadership.ROLE_DEPUTY,
            is_active=True
        )

        self.assertEqual(self.staff_profile.is_head, False)

    def test_is_head_true(self):
        StaffLeadershipFactory(
            profile=self.staff_profile,
            role=StaffLeadership.ROLE_CHIEF,
            is_active=True
        )

        self.assertEqual(self.staff_profile.is_head, True)

    def test_is_deputy_false(self):
        StaffLeadershipFactory(
            profile=self.staff_profile,
            role=StaffLeadership.ROLE_DEPUTY,
            is_active=False
        )
        StaffLeadershipFactory(
            profile=self.staff_profile,
            role=StaffLeadership.ROLE_CHIEF,
            is_active=True
        )

        self.assertEqual(self.staff_profile.is_deputy, False)

    def test_is_deputy_true(self):
        StaffLeadershipFactory(
            profile=self.staff_profile,
            role=StaffLeadership.ROLE_DEPUTY,
            is_active=True
        )

        self.assertEqual(self.staff_profile.is_deputy, True)

    def test_get_group_names(self):
        staff_groups = StaffGroupFactory.create_batch(3)
        group_ids = list(map(lambda group: group.pk, staff_groups))

        names = StaffProfile.get_group_names(group_ids)

        staff_groups[0].name = "some other name"
        staff_groups[0].save()

        names_from_cache = StaffProfile.get_group_names(group_ids)

        self.assertEqual(names, names_from_cache)

    def test_group_str(self):
        result = self.staff_profile.groups_str()
        self.assertEqual(result, "")

        staff_groups = StaffGroupFactory.create_batch(3)
        group_ids = list(map(lambda group: group.pk, staff_groups))
        group_names = list(map(lambda group: group.name, staff_groups))
        group_names.reverse()

        self.staff_profile.groups_tree = group_ids
        self.staff_profile.save()

        result = self.staff_profile.groups_str()
        self.assertEqual(result, " / ".join(group_names))

        result = self.staff_profile.groups_str(start=1)
        self.assertEqual(result, " / ".join(group_names[1:]))

        result = self.staff_profile.groups_str(end=2)
        self.assertEqual(result, " / ".join(group_names[:2]))

        result = self.staff_profile.groups_str(start=1, end=2)
        self.assertEqual(result, " / ".join(group_names[1:2]))
