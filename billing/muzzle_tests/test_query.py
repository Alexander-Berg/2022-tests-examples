# coding=utf-8

import unittest

from muzzle.query import decode_order, ConstSqlPiece, SubstFilter, SubstIter, Query, query_to_sa_text


class TestQuery(unittest.TestCase):
    def testDecodeOrder(self):
        self.assertEqual('desc', decode_order('Desc'))
        self.assertEqual('desc', decode_order('d'))
        self.assertEqual('asc', decode_order('Asc'))
        self.assertEqual('asc', decode_order(''))

    def testQuery(self):
        q = Query()
        q.what += ['external_eid', 'dt']
        q.append_source(ConstSqlPiece('t_invoice'))
        q.append_source(ConstSqlPiece('dual'))
        self.assertEqual(
            'select /*+  */ external_eid, dt from t_invoice, dual',
            q.to_sql(SubstIter())
        )
        q.append_filter(SubstFilter('id', '>'))
        q.append_filter(ConstSqlPiece('external_eid like \'Я-%\''))
        q.append_filter(SubstFilter('dt', '>='))
        self.assertEqual(
            'select /*+  */ external_eid, dt from t_invoice, dual'
            ' where (id > :1) and (external_eid like \'Я-%\')'
            ' and (dt >= :2)',
            q.to_sql(SubstIter())
        )
        q.order_by += [('dt', 'desc'), ('id', '')]
        self.assertEqual(
            'select /*+  */ external_eid, dt from t_invoice, dual'
            ' where (id > :1) and (external_eid like \'Я-%\')'
            ' and (dt >= :2) order by dt desc, id asc',
            q.to_sql(SubstIter())
        )
        self.assertEqual(['id', 'dt'], q.get_subst_list())

    def testGroups(self):
        q = Query()
        q.what += ['client_id', 'paysys_id', 'sum(effective_sum) eff_sum']
        q.append_source(ConstSqlPiece('t_invoice'))
        q.group_by += ['client_id', 'paysys_id']
        self.assertEqual(
            'select /*+  */ client_id, paysys_id, sum(effective_sum) eff_sum from t_invoice'
            ' group by client_id, paysys_id',
            q.to_sql(SubstIter())
        )

    def testGroupSets(self):
        q = Query()
        q.what += ['id', 'dt', 'client_id', 'paysys_id', 'sum(effective_sum) eff_sum']
        q.append_source(ConstSqlPiece('t_invoice'))
        q.group_by_sets += [
            ('id', 'dt', 'client_id', 'paysys_id'),
            ('client_id', 'paysys_id'),
            ('client_id', ),
            (),
        ]
        self.assertEqual(
            'select /*+  */ id, dt, client_id, paysys_id, sum(effective_sum) eff_sum from t_invoice'
            ' group by grouping sets ((id, dt, client_id, paysys_id), '
            '(client_id, paysys_id), (client_id), ())',
            q.to_sql(SubstIter())
        )


class TestSAQuary(unittest.TestCase):
    def test_query_to_sa_text(self):
        params = {'id': '11'}
        res = query_to_sa_text(ConstSqlPiece('id = :id', params))

        self.assertEqual('(id = :p_id)', str(res))
        self.assertEqual({'p_id': '11'}, res.compile().params)

    def test_query_to_sa_with_or(self):
        params = {'firm1': 1, 'firm2': 2}
        res = query_to_sa_text(ConstSqlPiece('firm_id = :firm1 or firm_id = :firm2', params))

        self.assertEqual('(firm_id = :p_firm1 or firm_id = :p_firm2)', str(res))
        self.assertEqual({'p_firm1': 1, 'p_firm2': 2}, res.compile().params)

