# -*- coding: utf-8 -*-

from __future__ import with_statement

from tests.base import MediumTest
import tests.object_builder as ob


class TestOrderInfo(MediumTest):
    def test_orders_info_empty(self):
        res = self.xmlrpcserver.GetOrdersInfo(tuple())
        self.assertEqual(res, [])

    def check_order(self, o, order_struct, is_root=False):
        self.assertEqual(str(o.id), str(order_struct['id']))
        self.assertEqual(o.service_order_id, order_struct['ServiceOrderID'])
        self.assertEqual(o.service_id, order_struct['ServiceID'])
        self.assertEqual(is_root, order_struct['IsGroupRoot'])

    def test_orders_info(self):
        o = ob.OrderBuilder().build(self.session).obj
        res = self.xmlrpcserver.GetOrdersInfo([{'ServiceID': o.service_id, 'ServiceOrderID': o.service_order_id}])
        res2 = self.xmlrpcserver.GetOrdersInfo({'ServiceID': o.service_id, 'ServiceOrderID': o.service_order_id})
        self.assertEqual(res, res2)
        self.assertEqual(1, len(res))
        self.check_order(o, res[0])

    def test_orders_info_dict_contract(self):
        o1 = ob.OrderBuilder().build(self.session).obj
        o2 = ob.OrderBuilder(client=o1.client).build(self.session).obj
        c = ob.ContractBuilder(client=o1.client).build(self.session).obj
        o1.contract = c
        o2.contract = c
        o1.parent_group_order = o2
        res = self.xmlrpcserver.GetOrdersInfo({'ContractID': c.id})
        self.assertEqual(2, len(res))
        self.check_order(o1, [o for o in res if str(o['id']) == str(o1.id)][0])
        self.check_order(o2, [o for o in res if str(o['id']) == str(o2.id)][0], is_root=True)
