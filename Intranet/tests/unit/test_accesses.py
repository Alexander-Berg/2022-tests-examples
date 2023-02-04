# coding: utf-8


import unittest
import pytest

from at.common import dbswitch
from at.common.groups import GroupType
from at.aux_.models import Person
from at.aux_.Accesses import RealAccess, FakeAccess
from at.common import exceptions


pytestmark = pytest.mark.django_db

class FakeEntry(object):
    def __init__(self, **kwargs):
        for k,v in kwargs.items():
            setattr(self, k, v)
        self.is_comment = 'comment_id' in kwargs


class TestAccesses(unittest.TestCase):
    def entry(self, update=None):
        update = update or {}
        data = {
            'feed_id': self.club.person_id,
            'item_no': 123,
            'deleted': False,
            'is_new': True,
            'author_id': self.other.person_id,
        }
        data.update(update)
        return FakeEntry(**data)

    def setUp(self):
        self.user = Person.get_by(login='theigel')
        self.other = Person.get_by(login='kukutz')
        self.dismissed = Person.get_by(login='deadman')
        self.club = Person.get_by(login='testclub')
        self.closedclub = Person.get_by(login='premod_test_club')

    def testFakeMatchesReal(self):
        real = RealAccess._list_operations()
        fake = FakeAccess._list_operations()
        assert real == fake, set(real).difference(set(fake))

    def testFakeCanAll(self):
        a = FakeAccess(self.user, self.club, self.entry())
        for k in FakeAccess._list_operations():
            getattr(a, 'assert_can_' + k)()

    @pytest.mark.skip('TODO: debug')
    def testCanPost(self):
        RealAccess(self.user, self.club).assert_can_post()

    def testDismissedCantDoAnything(self):
        a = RealAccess(self.dismissed, self.club, self.entry())
        for k in RealAccess._list_operations():
            self.assertEqual(False, getattr(a, 'can_' + k)())

    def testCanPostRaisesWithoutPost(self):
        a = RealAccess(self.user, self.club)
        self.assertRaises(RuntimeError, a.assert_can_read_post)

    def testInvalidUser(self):
        self.assertRaises(ValueError, lambda: RealAccess(self.club, self.user))

    @pytest.mark.skip('TODO: not found closedclub')
    def testCantReadPrivatePosts(self):
        a = RealAccess(
            self.user,
            self.closedclub,
            self.entry({'access_group': GroupType.OWNER})
        )
        a._roles = set([GroupType.USER])  # XXX not very clean
        self.assertRaises(
                exceptions.AccessDenied,
                a.assert_can_read_post
        )

    def testCanBanInMyBlog(self):
        a = RealAccess(self.user, self.other, target=self.user)
        a.assert_can_ban()

    def testCantReadOwnPrivatePosts(self):
        a = RealAccess(self.user, self.user, self.entry({'access_group': GroupType.OWNER}))
        a._roles = set([GroupType.OWNER]) # XXX not very clean
        assert a.can_read_post()

    def testCanInvite(self):
        a = RealAccess(self.user, self.other, target=self.club)
        a.user_target_access._roles = GroupType.expand_role(GroupType.MODERATOR)
        a.feed_target_access._roles = []
        a.assert_can_invite(GroupType.MEMBER)
        a.assert_can_invite(GroupType.MODERATOR)

        a.feed_target_access._roles = GroupType.expand_role(GroupType.INVITED)
        self.assertRaises(exceptions.OperationNotDone, lambda: a.can_invite(GroupType.MEMBER))
        a.assert_can_invite(GroupType.MODERATOR)

    def testExpandRoles(self):
        assert GroupType.OWNER in GroupType.expand_role(GroupType.OWNER)
        assert GroupType.MEMBER in GroupType.expand_role(GroupType.OWNER)
        assert GroupType.OWNER not in GroupType.expand_role(GroupType.MEMBER)

    def testUpscaleRoles(self):
        self.assertEqual(
                set([GroupType.MEMBER, GroupType.MODERATOR, GroupType.OWNER]),
                GroupType.upscale_role(GroupType.MEMBER)
                )

    def testExpandBanned(self):
        assert GroupType.MEMBER not in GroupType.expand_roles([GroupType.MEMBER, GroupType.BANNED])
        assert GroupType.MEMBER not in GroupType.expand_roles([GroupType.BANNED, GroupType.MEMBER])

    def testRoles(self):
        a = FakeAccess(self.user, self.user, _fake_roles=[GroupType.OWNER])
        self.assertEqual(sorted(a.roles),
                sorted([GroupType.OWNER, GroupType.MODERATOR, GroupType.MEMBER,
                GroupType.INVITED, GroupType.USER]))
