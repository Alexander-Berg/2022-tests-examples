from operator import attrgetter

from django.test import TestCase

from staff.lib.testing import GroupMembershipFactory

from staff.achievery.roles.base import Condition
from staff.lib.testing import GroupFactory, StaffFactory

from .. import models, roles

from .factories.model import (
    AchievementFactory,
    EventFactory,
    GivenAchievementFactory,
    IconFactory,
)


class RolesTestCase(TestCase):

    def _get_role(self, model):
        return self.role_class(self.user, model)

    def setUp(self):
        self.user = StaffFactory()
        self.achievement = AchievementFactory()
        self.given = GivenAchievementFactory(
            achievement=self.achievement, level=1,
        )
        self.icon = IconFactory(achievement=self.achievement, level=1)
        self.event = EventFactory(given_achievement=self.given)


class TestAdmin(RolesTestCase):
    role_class = roles.Admin

    def test_is_applicable_to_achievement(self):
        role = self._get_role(self.achievement)
        self.assertFalse(role.is_applicable())

        self.group = GroupFactory(url='achieveryadmin')
        self.assertFalse(role.is_applicable())

        self.membership = GroupMembershipFactory(group=self.group,
                                                 staff=self.user)
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_given(self):
        role = self._get_role(self.given)
        self.assertFalse(role.is_applicable())

        self.group = GroupFactory(url='achieveryadmin')
        self.assertFalse(role.is_applicable())

        self.membership = GroupMembershipFactory(group=self.group,
                                                 staff=self.user)
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_icon(self):
        role = self._get_role(self.icon)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_event(self):
        role = self._get_role(self.event)
        self.assertFalse(role.is_applicable())

    def test_check(self):
        for model in (self.achievement, self.given):
            for perm in 'rw':
                role = self._get_role(model)
                self.assertTrue(role.check('', perm))

    def test_get_query(self):
        model_classes = (
            AchievementFactory,
            GivenAchievementFactory,
            IconFactory,
            EventFactory,
        )
        for model_class in model_classes:
            self.assertTrue(
                roles.Admin.get_query(self.user, model_class).is_all,
            )


class TestAnybody(RolesTestCase):
    role_class = roles.Anybody

    def test_is_applicable_to_achievement(self):
        role = self._get_role(self.achievement)

        self.achievement.is_active = False
        self.assertFalse(role.is_applicable())

        self.achievement.is_active = True
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_given(self):
        role = self._get_role(self.given)

        self.given.is_active = False
        self.given.is_hidden = False
        self.assertFalse(role.is_applicable())

        self.given.is_active = True
        self.given.is_hidden = False
        self.assertTrue(role.is_applicable())

        self.given.is_active = False
        self.given.is_hidden = True
        self.assertFalse(role.is_applicable())

        self.given.is_active = True
        self.given.is_hidden = True
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_icon(self):
        role = self._get_role(self.icon)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_event(self):
        role = self._get_role(self.event)
        self.assertFalse(role.is_applicable())

    def test_check_achievement(self):
        role = self._get_role(self.achievement)
        for field in map(attrgetter('name'), self.achievement._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertTrue(role.check(field, 'r'), field)

    def test_check_given(self):
        role = self._get_role(self.given)
        for field in map(attrgetter('name'), self.given._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertTrue(role.check(field, 'r'), field)

    def test_check_icon(self):
        role = self._get_role(self.icon)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertTrue(role.check(field, 'r'), field)

    def test_check_event(self):
        role = self._get_role(self.event)
        for field in map(attrgetter('name'), self.event._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertTrue(role.check(field, 'r'), field)

    def test_get_query_for_achievement(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.Achievement),
            Condition(models.Achievement.queries.active),
        )

    def test_get_query_for_given(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.GivenAchievement),
            Condition(models.GivenAchievement.queries.active),
        )

    def test_get_query_for_icon(self):
        condition = self.role_class.get_query(self.user, models.Icon)
        self.assertTrue(condition.is_all, condition)

    def test_get_query_for_event(self):
        condition = self.role_class.get_query(self.user, models.Event)
        self.assertTrue(condition.is_all, condition)


class TestActiveHolder(RolesTestCase):
    role_class = roles.ActiveHolder

    def test_is_applicable_to_achievement(self):
        role = self._get_role(self.achievement)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_given(self):
        role = self._get_role(self.given)
        self.given.is_active = False
        self.given.is_hidden = True
        self.assertFalse(role.is_applicable())

        self.given.person = self.user
        self.assertFalse(role.is_applicable())

        self.given.is_active = True
        self.assertFalse(role.is_applicable())

        self.given.is_hidden = False
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_icon(self):
        role = self._get_role(self.icon)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_event(self):
        role = self._get_role(self.event)
        self.assertFalse(role.is_applicable())

    def test_check_achievement(self):
        role = self._get_role(self.achievement)
        for field in map(attrgetter('name'), self.achievement._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_given(self):
        role = self._get_role(self.given)
        for field in map(attrgetter('name'), self.given._meta.fields):
            if field in ('is_hidden', 'slot', 'revision'):
                self.assertTrue(role.check(field, 'w'), field)
            else:
                self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_icon(self):
        role = self._get_role(self.icon)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_event(self):
        role = self._get_role(self.event)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_get_query_for_achievement(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Achievement).is_none,
        )

    def test_get_query_for_given(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.GivenAchievement),
            Condition(models.GivenAchievement.queries.held_by(self.user)),
        )

    def test_get_query_for_icon(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Icon).is_none,
        )

    def test_get_query_for_event(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Event).is_none,
        )


class TestHiddenHolder(RolesTestCase):
    role_class = roles.HiddenHolder

    def test_is_applicable_to_achievement(self):
        role = self._get_role(self.achievement)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_given(self):
        role = self._get_role(self.given)
        self.given.is_active = False
        self.given.is_hidden = False
        self.assertFalse(role.is_applicable())

        self.given.person = self.user
        self.assertFalse(role.is_applicable())

        self.given.is_active = True
        self.assertFalse(role.is_applicable())

        self.given.is_hidden = True
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_icon(self):
        role = self._get_role(self.icon)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_event(self):
        role = self._get_role(self.event)
        self.assertFalse(role.is_applicable())

    def test_check_achievement(self):
        role = self._get_role(self.achievement)
        for field in map(attrgetter('name'), self.achievement._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_given(self):
        role = self._get_role(self.given)
        for field in map(attrgetter('name'), self.given._meta.fields):
            if field in ('is_hidden', 'revision'):
                self.assertTrue(role.check(field, 'w'), field)
            else:
                self.assertFalse(role.check(field, 'w'), field)
            self.assertTrue(role.check(field, 'r'), field)

    def test_check_icon(self):
        role = self._get_role(self.icon)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_event(self):
        role = self._get_role(self.event)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_get_query_for_achievement(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Achievement).is_none,
        )

    def test_get_query_for_given(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.GivenAchievement),
            Condition(models.GivenAchievement.queries.held_by(self.user)),
        )

    def test_get_query_for_icon(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Icon).is_none,
        )

    def test_get_query_for_event(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Event).is_none,
        )


class TestInactiveHolder(RolesTestCase):
    role_class = roles.InactiveHolder

    def test_is_applicable_to_achievement(self):
        role = self._get_role(self.achievement)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_given(self):
        role = self._get_role(self.given)
        self.given.is_active = True
        self.assertFalse(role.is_applicable())

        self.given.person = self.user
        self.assertFalse(role.is_applicable())

        self.given.is_active = False
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_icon(self):
        role = self._get_role(self.icon)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_event(self):
        role = self._get_role(self.event)
        self.assertFalse(role.is_applicable())

    def test_check_achievement(self):
        role = self._get_role(self.achievement)
        for field in map(attrgetter('name'), self.achievement._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_given(self):
        role = self._get_role(self.given)
        for field in map(attrgetter('name'), self.given._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertTrue(role.check(field, 'r'), field)

    def test_check_icon(self):
        role = self._get_role(self.icon)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_event(self):
        role = self._get_role(self.event)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_get_query_for_achievement(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Achievement).is_none,
        )

    def test_get_query_for_given(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.GivenAchievement),
            Condition(models.GivenAchievement.queries.held_by(self.user)),
        )

    def test_get_query_for_icon(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Icon).is_none,
        )

    def test_get_query_for_event(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Event).is_none,
        )


class TestActiveOwner(RolesTestCase):
    role_class = roles.ActiveOwner

    def test_is_applicable_to_achievement(self):
        role = self._get_role(self.achievement)
        self.assertFalse(role.is_applicable())

        self.membership = GroupMembershipFactory(
            group=self.achievement.owner_group, staff=self.user
        )
        self.achievement.user = self.user
        self.achievement.is_active = True
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_given(self):
        role = self._get_role(self.given)
        self.given.is_active = False
        self.given.is_hidden = True
        self.assertFalse(role.is_applicable())

        self.given.achievement.user = self.user
        self.assertFalse(role.is_applicable())

        self.given.is_active = True
        self.assertFalse(role.is_applicable())

        self.given.is_hidden = False
        self.assertFalse(role.is_applicable())

        self.membership = GroupMembershipFactory(
            group=self.achievement.owner_group, staff=self.user
        )
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_icon(self):
        role = self._get_role(self.icon)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_event(self):
        role = self._get_role(self.event)
        self.assertFalse(role.is_applicable())

    def test_check_achievement(self):
        role = self._get_role(self.achievement)
        writable_fields = (
            'description',
            'description_html',
            'description_en',
            'description_html_en',
            'description_short',
            'description_short_en',
            'title',
            'title_en',
        )
        for field in map(attrgetter('name'), self.achievement._meta.fields):
            if field in writable_fields:
                self.assertTrue(role.check(field, 'w'), field)
            else:
                self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_given(self):
        role = self._get_role(self.given)
        for field in map(attrgetter('name'), self.given._meta.fields):
            if field in ('is_active', 'comment', 'comment_html', 'level', 'revision', 'slot'):
                self.assertTrue(role.check(field, 'w'), field)
            else:
                self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_icon(self):
        role = self._get_role(self.icon)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_event(self):
        role = self._get_role(self.event)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_get_query_for_achievement(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.Achievement),
            Condition(models.Achievement.queries.owned_by(self.user)),
        )

    def test_get_query_for_given(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.GivenAchievement),
            Condition(models.GivenAchievement.queries.owned_by(self.user)),
        )

    def test_get_query_for_icon(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Icon).is_none,
        )

    def test_get_query_for_event(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Event).is_none,
        )


class TestHiddenOwner(RolesTestCase):
    role_class = roles.HiddenOwner

    def test_is_applicable_to_achievement(self):
        role = self._get_role(self.achievement)
        self.assertFalse(role.is_applicable())

        self.membership = GroupMembershipFactory(
            group=self.achievement.owner_group, staff=self.user
        )
        self.achievement.user = self.user
        self.achievement.is_active = True
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_given(self):
        role = self._get_role(self.given)
        self.given.is_active = False
        self.given.is_hidden = False
        self.assertFalse(role.is_applicable())

        self.given.achievement.user = self.user
        self.assertFalse(role.is_applicable())

        self.given.is_active = True
        self.assertFalse(role.is_applicable())

        self.given.is_hidden = True
        self.assertFalse(role.is_applicable())

        self.membership = GroupMembershipFactory(
            group=self.achievement.owner_group, staff=self.user
        )
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_icon(self):
        role = self._get_role(self.icon)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_event(self):
        role = self._get_role(self.event)
        self.assertFalse(role.is_applicable())

    def test_check_achievement(self):
        role = self._get_role(self.achievement)
        writable_fields = (
            'description',
            'description_html',
            'description_en',
            'description_html_en',
            'description_short',
            'description_short_en',
            'title',
            'title_en',
        )
        for field in map(attrgetter('name'), self.achievement._meta.fields):
            if field in writable_fields:
                self.assertTrue(role.check(field, 'w'), field)
            else:
                self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_given(self):
        role = self._get_role(self.given)
        for field in map(attrgetter('name'), self.given._meta.fields):
            if field in ('is_active', 'comment', 'comment_html', 'level', 'revision', 'slot'):
                self.assertTrue(role.check(field, 'w'), field)
            else:
                self.assertFalse(role.check(field, 'w'), field)
            self.assertTrue(role.check(field, 'r'), field)

    def test_check_icon(self):
        role = self._get_role(self.icon)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_event(self):
        role = self._get_role(self.icon)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_get_query_for_achievement(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.Achievement),
            Condition(none=True),
        )

    def test_get_query_for_given(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.GivenAchievement),
            Condition(models.GivenAchievement.queries.owned_by(self.user)),
        )

    def test_get_query_for_icon(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Icon).is_none,
        )

    def test_get_query_for_event(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Event).is_none,
        )


class TestInactiveOwner(RolesTestCase):
    role_class = roles.InactiveOwner

    def test_is_applicable_to_achievement(self):
        role = self._get_role(self.achievement)
        self.assertFalse(role.is_applicable())

        self.membership = GroupMembershipFactory(
            group=self.achievement.owner_group, staff=self.user
        )
        self.achievement.user = self.user
        self.achievement.is_active = False
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_given(self):
        role = self._get_role(self.given)
        self.given.is_active = True
        self.assertFalse(role.is_applicable())

        self.given.achievement.user = self.user
        self.assertFalse(role.is_applicable())

        self.given.is_active = False
        self.assertFalse(role.is_applicable())

        self.membership = GroupMembershipFactory(
            group=self.achievement.owner_group, staff=self.user
        )
        self.assertTrue(role.is_applicable())

    def test_is_applicable_to_icon(self):
        role = self._get_role(self.icon)
        self.assertFalse(role.is_applicable())

    def test_is_applicable_to_event(self):
        role = self._get_role(self.event)
        self.assertFalse(role.is_applicable())

    def test_check_achievement(self):
        role = self._get_role(self.achievement)
        for field in map(attrgetter('name'), self.achievement._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_given(self):
        role = self._get_role(self.given)
        for field in map(attrgetter('name'), self.given._meta.fields):
            if field in ('is_active', 'revision', 'comment', 'comment_html'):
                self.assertTrue(role.check(field, 'w'), field)
            else:
                self.assertFalse(role.check(field, 'w'), field)
            self.assertTrue(role.check(field, 'r'), field)

    def test_check_icon(self):
        role = self._get_role(self.icon)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_check_event(self):
        role = self._get_role(self.icon)
        for field in map(attrgetter('name'), self.icon._meta.fields):
            self.assertFalse(role.check(field, 'w'), field)
            self.assertFalse(role.check(field, 'r'), field)

    def test_get_query_for_achievement(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.Achievement),
            Condition(models.Achievement.queries.owned_by(self.user)),
        )

    def test_get_query_for_given(self):
        self.assertEqual(
            self.role_class.get_query(self.user, models.GivenAchievement),
            Condition(models.GivenAchievement.queries.owned_by(self.user)),
        )

    def test_get_query_for_icon(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Icon).is_none,
        )

    def test_get_query_for_event(self):
        self.assertTrue(
            self.role_class.get_query(self.user, models.Event).is_none,
        )
