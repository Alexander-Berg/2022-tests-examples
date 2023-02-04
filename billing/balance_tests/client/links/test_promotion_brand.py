# -*- coding: utf-8 -*-

from __future__ import with_statement
from balance import mapper, exc
from tests.base import BalanceTest
from tests.object_builder import GroupBuilder, ClientBuilder


class TestGroups(BalanceTest):
    def test_promotion_brand(self):
        pg = self.session.query(mapper.Group).getone(code='promotion_brand')
        grp1 = GroupBuilder(parent=pg).build(self.session).obj
        grp2 = GroupBuilder(parent=pg).build(self.session).obj
        self.session.flush()

        clnt1 = ClientBuilder().build(self.session).obj
        clnt2 = ClientBuilder().build(self.session).obj
        clnt3 = ClientBuilder().build(self.session).obj
        clnt4 = ClientBuilder().build(self.session).obj
        clnt5 = ClientBuilder().build(self.session).obj
        clnt3.make_equivalent(clnt2)
        clnt4.make_equivalent(clnt5)

        clnt1.groups.append(grp1)
        clnt2.groups.append(grp1)
        clnt4.promotion_brand = grp1
        clnt5.promotion_brand = None
        clnt5.promotion_brand = grp2

        self.assertRaises(exc.DIFFERENT_PB_CANNOT_BE_EQUIVALENT, clnt4.make_equivalent, clnt3)
        assert len(clnt3.promotion_brand.clients) == 2

        self.session.flush()
