# -*- coding: utf-8 -*-

import unittest
import re
from decimal import Decimal as D

from balance.mapper import DomainObject
from balance import xmlizer as xr

class C(DomainObject):
    def __init__(self):
        self.name = 9
        self.addr = 8

class P(DomainObject):
    def __init__(self, p):
        self.qty = p + D('0.1')
        self.amt = p + D('0.2')

class Q(DomainObject):
    def __init__(self):
        self.id = 0
        self.dt = 1
        self.person = C()
        self.invoice_orders = [P(1), P(2)]

class QXmlizer(xr.Xmlizer):
    xmlized_class = Q


t1 = xr.Tree('invoice', None,
          xr.Leaf('id', None),
          xr.Leaf('dt', None),
          xr.Tree('person', None,
               xr.Leaf('name', None),
               xr.Leaf('addr', None),
               ),
          xr.List('invoice_orders', None,
               xr.XTree('invoice_order', None,
                    xr.Leaf('amt', None),
#                   xr.Leaf('qty', None),
                    )
               )
          )

class TestXmlTemplates(unittest.TestCase):
    def test1(self):
        self.assertEqual(
                'Tree(invoice:Leaf(id),Leaf(dt),Tree(person:Leaf(name),'
                'Leaf(addr)),List(invoice_orders:XTree('
                'invoice_order:Leaf(amt))))',
                str(t1))
    def testWork(self):
        xml =  ('<invoice>'
                '<id>0</id>'
                '<dt>1</dt>'
                '<person>'
                '<name>9</name>'
                '<addr>8</addr>'
                '</person>'
                '<invoice-orders>'
                '<invoice-order>'
                '<qty>1.1</qty>'
                '</invoice-order>'
                '<invoice-order>'
                '<qty>2.1</qty>'
                '</invoice-order>'
                '</invoice-orders>'
                '</invoice>')
        #        self.assertEqual(xml, xr.xml2str(xr.xmlize_tree(Q(), t1)))

        # '<invoice node_666_type="tree">'
        # '<id node_666_type="leaf">0</id>'
        # '<dt node_666_type="leaf">1</dt>'
        # '<person node_666_type="tree">'
        # '<name node_666_type="leaf">9</name>'
        # '<addr node_666_type="leaf">8</addr>'
        # '</person>'
        # '<invoice-orders node_666_type="list">'
        # '<invoice-order node_666_type="xtree">'
        # '<qty node_666_type="leaf">1.1</qty>'
        # '</invoice-order>'
        # '<invoice-order node_666_type="xtree">'
        # '<qty node_666_type="leaf">2.1</qty>'
        # '</invoice-order>'
        # '</invoice-orders>'
        # '</invoice>'
        result = xr.xml2str(xr.xmlize_tree(xr.getxmlizer(Q()), t1))
        result = re.sub(' {0}="[a-zA-~]*"'.format(xr.NODE_TYPE), '', result)
        self.assertEqual(xml, result)
