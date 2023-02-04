from django.test import TestCase

from staff.lib.testing import GroupMembershipFactory

from staff.achievery.permissions import RoleRegistry
from staff.lib.testing import StaffFactory, GroupFactory

from .. import domain, exceptions, constants, models, permissions

from .factories import model
from .factories.domain import (
    AchievementFactory,
    GivenAchievementFactory,
    IconFactory,
    OwnerGroupFactory,
    PersonFactory,
)
from .factories.notifications import RouteFactory


class TestAchievement(TestCase):
    def _create_achievement(self, **kwargs):
        return (
            domain.Achievement
            .objects(self.user, self.role_registry)
            .create(owner_group=self.owner_grp, **kwargs)
        )

    def setUp(self):
        self.user = StaffFactory(login='pikachu')
        self.admins_grp = GroupFactory(url='achieveryadmin')
        self._membership = GroupMembershipFactory(staff=self.user,
                                                  group=self.admins_grp)
        self.owner_grp = OwnerGroupFactory(
            user=self.user, model=self.admins_grp)
        self.role_registry = permissions.RoleRegistry(self.user)

    def test_create(self):
        """Create new Achievement"""
        instance = self._create_achievement()
        self.assertEqual(instance.owner_group.id, self.owner_grp.id)

    def test_create_duplicate(self):
        """Create duplicate Achievement"""
        self._create_achievement()
        self.assertRaises(exceptions.DuplicateEntry, self._create_achievement)

    def test_update(self):
        """Update existing Achievement"""
        instance = self._create_achievement(native_lang='ru')
        self.assertFalse(instance.title.ru)
        self.assertFalse(instance.title.en)
        new_title_en = 'New Achievement'
        new_title_ru = 'Ню Ащивмент'
        instance.update(title={'ru': new_title_ru, 'en': new_title_en})
        self.assertEqual(instance.title.ru, new_title_ru)
        self.assertEqual(instance.title.en, new_title_en)

    def test_set_fields(self):
        """Set attributes to Achievement"""
        instance = self._create_achievement(native_lang='de')

        data = {
            'category': constants.CATEGORY.FUN,
            'title.de': 'Du!',
            'description_short.de': 'Du hast!',
            'description.de': 'Du hast mich!',
            'service.name.de': 'Links 2-3-4',
        }

        for k, v in data.items():
            setattr(instance, k, v)
            self.assertEqual(getattr(instance, k), v)

    def test_delete(self):
        """(Soft)Delete existing Achievement"""
        instance = self._create_achievement()
        self.assertTrue(instance.is_active)
        instance.delete()
        self.assertFalse(instance.is_active)

    def test_deleted_duplicate(self):
        """Restore previously deleted Achievement"""
        data = {
            'title': {'ru': 'title__ru', 'en': 'title__en'},
            'native_lang': 'ru',
        }

        instance = self._create_achievement(**data)
        self.assertTrue(instance.is_active)

        instance.delete()
        self.assertFalse(instance.is_active)

        self.assertRaises(exceptions.DuplicateEntry, self._create_achievement, **data)

    def test_restore(self):
        """Restore previously deleted Achievement"""
        data = {
            'title': {'ru': 'title__ru', 'en': 'title__en'},
            'native_lang': 'ru',
        }

        instance = self._create_achievement(**data)
        self.assertTrue(instance.is_active)

        instance.delete()
        self.assertFalse(instance.is_active)

        instance.restore()
        self.assertTrue(instance.is_active)

    def test_is_counter(self):
        """`is_counter` property"""
        instance = self._create_achievement()
        self.assertFalse(instance.is_counter)

        icon1 = IconFactory(model__achievement=instance.model, model__level=-1)
        self.assertFalse(instance.is_counter)

        icon2 = IconFactory(model__achievement=instance.model, model__level=1)
        self.assertTrue(instance.is_counter)

        icon2.delete()
        self.assertFalse(instance.is_counter)

        icon1.delete()
        self.assertFalse(instance.is_counter)


class TestGiven(TestCase):
    def _create_given(self, **kwargs):
        return (
            domain.GivenAchievement
            .objects(self.user, self.role_registry)
            .create(**(kwargs or self.data))
        )

    def setUp(self):
        self.user = StaffFactory(login='pikachu')
        self.admins_grp = GroupFactory(url='achieveryadmin')
        self._membership = GroupMembershipFactory(staff=self.user,
                                                  group=self.admins_grp)
        self.role_registry = permissions.RoleRegistry(self.user)

        self.holder = PersonFactory(role_registry=self.role_registry)
        self.achievement = AchievementFactory(role_registry=self.role_registry)
        self.data = {
            'level': -1,
            'person': self.holder,
            'achievement': self.achievement,
        }
        self.icon = model.IconFactory(
            level=-1, achievement=self.achievement.model
        )
        self._route = RouteFactory(transport_id='email')

    def test_create(self):
        """Create new GivenAchievement (give achievement to person)"""
        for prop in self.data:
            insufficient_data = {
                k: v for k, v in self.data.items() if k != prop
            }
            self.assertRaises(
                exceptions.RequiredFieldIsMissing,
                self._create_given,
                **insufficient_data
            )

        instance = self._create_given()

        for k, v in self.data.items():
            actual = getattr(instance, k)
            self.assertEqual(actual, v, msg=(k, v, actual))

        self.assertEqual(instance.slot, 1)

    def test_create_duplicate(self):
        """Give achievement that is already given"""
        self._create_given()

        self.assertRaises(
            exceptions.DuplicateEntry, self._create_given, **self.data)

    def test_slot(self):
        """Try to place 2 Givens in one slot"""
        data = {
            'person': self.holder,
            'level': -1,
        }
        getter = models.GivenAchievement.objects.get

        achievement1 = AchievementFactory()
        model.IconFactory(achievement=achievement1.model, level=-1)
        given1 = self._create_given(achievement=achievement1, **data)
        self.role_registry.fill()

        self.assertEqual(given1.slot, 1)
        self.assertEqual(getter(pk=given1.id).slot, 1)

        given1.update(slot=1)
        self.assertEqual(given1.slot, 1)
        self.assertEqual(getter(pk=given1.id).slot, 1)

        given1.save()
        self.assertEqual(given1.slot, 1)
        self.assertEqual(getter(pk=given1.id).slot, 1)

        achievement2 = AchievementFactory()
        model.IconFactory(achievement=achievement2.model, level=-1)
        given2 = self._create_given(achievement=achievement2, **data)
        self.role_registry.fill()

        self.assertEqual(given2.slot, 2)
        self.assertEqual(getter(pk=given2.id).slot, 2)
        self.assertEqual(given1.slot, 1)
        self.assertEqual(getter(pk=given1.id).slot, 1)

        given2.update(slot=1)
        self.assertEqual(given2.slot, 1)
        self.assertEqual(getter(pk=given2.id).slot, 2)
        self.assertEqual(given1.slot, 1)
        self.assertEqual(getter(pk=given1.id).slot, 1)

        given2.save()
        self.assertEqual(given2.slot, 1)
        self.assertEqual(getter(pk=given2.id).slot, 1)
        self.assertEqual(given1.slot, 1)
        self.assertEqual(getter(pk=given1.id).slot, 2)

        given1.update(slot=None)
        self.assertEqual(given1.slot, None)
        self.assertEqual(getter(pk=given1.id).slot, 2)
        self.assertEqual(given2.slot, 1)
        self.assertEqual(getter(pk=given2.id).slot, 1)

        given1.save()
        self.assertEqual(given1.slot, None)
        self.assertEqual(getter(pk=given1.id).slot, None)
        self.assertEqual(given2.slot, 1)
        self.assertEqual(getter(pk=given2.id).slot, 1)

    def test_update(self):
        """Update existing GivenAchievement"""
        new_data = {
            'comment': 'new comment',
            'is_hidden': True,
        }

        instance = self._create_given()

        for k in new_data:
            self.assertFalse(getattr(instance, k), k)

        instance.update(**new_data)

        for k, v in new_data.items():
            self.assertEqual(getattr(instance, k), v)

        instance.save()
        self.assertIsNone(instance.slot)

    def test_delete(self):
        """(Soft)Delete existing GivenAchievement"""
        instance = self._create_given()
        self.assertTrue(instance.is_active)
        self.assertEqual(instance.slot, 1)

        instance.delete()
        self.assertFalse(instance.is_active)
        self.assertIsNone(instance.slot)

    def test_deleted_duplicate(self):
        """Restore previously deleted GivenAchievement"""
        instance = self._create_given()
        self.assertTrue(instance.is_active)
        self.assertEqual(instance.slot, 1)

        instance.delete()
        self.assertFalse(instance.is_active)
        self.assertIsNone(instance.slot)

        self.assertRaises(exceptions.DuplicateEntry, self._create_given, **self.data)

    def test_restore(self):
        """Restore previously deleted GivenAchievement"""
        instance = self._create_given()
        self.assertTrue(instance.is_active)
        self.assertEqual(instance.slot, 1)

        instance.delete()
        self.assertFalse(instance.is_active)
        self.assertIsNone(instance.slot)

        instance.restore()
        self.assertTrue(instance.is_active)
        self.assertIsNone(instance.slot)

    def test_create_events_on_update(self):
        self.assertFalse(models.Event.objects.count())

        instance = self._create_given()
        self.assertEqual(models.Event.objects.count(), 1)

        instance.update(comment='comment1')
        instance.save()
        self.assertEqual(models.Event.objects.count(), 2)
        self.assertEqual(models.Event.objects.all()[1].comment, 'comment1')

        instance.delete()
        self.assertEqual(models.Event.objects.count(), 3)
        self.assertEqual(models.Event.objects.all()[2].is_active, False)

        instance.restore()
        self.assertEqual(models.Event.objects.count(), 4)
        self.assertEqual(models.Event.objects.all()[3].is_active, True)

        model.IconFactory(level=42, achievement=instance.model.achievement)

        instance.update(level=42)
        instance.save()
        self.assertEqual(models.Event.objects.count(), 5)
        self.assertEqual(models.Event.objects.all()[4].level, 42)

        instance.save()
        self.assertEqual(models.Event.objects.count(), 5)


class TestEvent(TestCase):
    def _create_event(self, **kwargs):
        data = self.data.copy()
        data.update(kwargs)
        event_model = model.EventFactory(**data)
        return domain.Event(
            user=self.user,
            role_registry=self.role_registry,
            model=event_model,
        )

    def setUp(self):
        self.user = StaffFactory()
        self.role_registry = permissions.RoleRegistry(self.user)
        self.admins_grp = GroupFactory(url='achieveryadmin')
        self._membership = GroupMembershipFactory(staff=self.user,
                                                  group=self.admins_grp)
        self.achievement = AchievementFactory(role_registry=self.role_registry)
        self.given = GivenAchievementFactory(
            user=self.user,
            model=model.GivenAchievementFactory(
                achievement=self.achievement.model,
                person=self.user
            ),
            role_registry=self.role_registry,
        )
        self.data = {
            'level': 1,
            'revision': 0,
            'slot': None,
            'given_achievement': self.given.model,
        }

    def test_previous(self):
        this = self._create_event()
        self.assertIsNone(this.previous)

        that = self._create_event(revision=1)
        self.assertEqual(that.previous, this)

    def test_is_unlocked(self):
        this = self._create_event()
        self.assertTrue(this.is_unlocked)

        that = self._create_event(revision=1)
        self.assertFalse(that.is_unlocked)

    def test_is_levelup(self):
        this = self._create_event(level=1, revision=0)
        self.assertFalse(this.is_levelup)

        that = self._create_event(level=1, revision=1)
        self.assertFalse(that.is_levelup)

        another = self._create_event(level=5, revision=2)
        self.assertTrue(another.is_levelup)

    def test_is_leveldown(self):
        this = self._create_event(level=5, revision=0)
        self.assertFalse(this.is_leveldown)

        that = self._create_event(level=5, revision=1)
        self.assertFalse(that.is_leveldown)

        another = self._create_event(level=4, revision=2)
        self.assertTrue(another.is_leveldown)

    def test_is_taken_away(self):
        this = self._create_event(is_active=True, level=1, revision=1)
        self.assertFalse(this.is_taken_away)

        that = self._create_event(is_active=True, revision=2)
        self.assertFalse(that.is_taken_away)

        another = self._create_event(is_active=False, revision=3)
        self.assertTrue(another.is_taken_away)

    def test_is_returned(self):
        this = self._create_event(is_active=True, level=-1, revision=1)
        self.assertFalse(this.is_returned)

        that = self._create_event(is_active=True, revision=2)
        self.assertFalse(that.is_returned)

        another = self._create_event(is_active=False, revision=3)
        self.assertFalse(another.is_returned)

        another2 = self._create_event(is_active=True, revision=4)
        self.assertTrue(another2.is_returned)


class TestPerson(TestCase):
    def test_free_slots(self):
        user = StaffFactory()
        person = PersonFactory(model=user)
        admins_grp = GroupFactory(url='achieveryadmin')
        GroupMembershipFactory(staff=user, group=admins_grp)
        RouteFactory(transport_id='email')
        roles = RoleRegistry(user)

        self.assertEqual(person.free_slots, [i for i in range(1, 13)])

        def create_given():
            achievement = model.AchievementFactory()
            model.IconFactory(achievement=achievement, level=-1)
            ga_model = model.GivenAchievementFactory.build(
                person=user, achievement=achievement)
            roles.fill()
            ga = GivenAchievementFactory(user=user, model=ga_model,
                                         role_registry=roles)
            ga.save()
            return ga

        given0 = create_given()
        self.assertEqual(given0.slot, 1)
        self.assertEqual(person.free_slots, [i for i in range(2, 13)])

        given1 = create_given()
        self.assertEqual(given1.slot, 2)
        self.assertEqual(person.free_slots, [i for i in range(3, 13)])

        for i in range(10):
            create_given()

        given12 = create_given()
        self.assertIsNone(given12.slot)
        self.assertEqual(person.free_slots, [])
