# -*- coding: utf-8 -*-
from datetime import datetime, timedelta
from decimal import Decimal as D
from functools import partial

import sqlalchemy as sa

from butils import logger
from balance import application, scheme, batcher
from balance.mapper.mapper_base import DeclarativeObject
from tests.base import BalanceTest

log = logger.get_logger('test_thirdparty_batches')


START = 200000000
TEST_SEQUENCE = scheme.request_order_id_seq


test_table = sa.Table(
    't_test_batcher',
    scheme.meta,
    sa.Column('id', sa.Integer, TEST_SEQUENCE, primary_key=True),
    sa.Column('key_id', sa.Integer),
    sa.Column('amount', scheme.OraclePreciseNumber),
    sa.Column('batch_id', sa.Integer),
    sa.Column('dt', sa.DateTime),
    sa.Column('inc', sa.Integer))


test_batch_table = sa.Table(
    't_test_batcher_batch',
    scheme.meta,
    sa.Column('id', sa.Integer, primary_key=True),
    sa.Column('key_id', sa.Integer),
    sa.Column('batch_dt', sa.DateTime),
    sa.Column('amount', scheme.OraclePreciseNumber))


class Table(DeclarativeObject):
    __table__ = test_table

    def __repr__(self):
        return (u'<{class_name}('
                u'id={id}, '
                u'batch_id={batch_id}, '
                u'batch_dt={batch_dt}, '
                u'key_id={key_id}, '
                u'amount={amount})>').format(
                    class_name=self.__class__.__name__,
                    id=self.id,
                    batch_id=self.batch_id,
                    batch_dt=self.batch_dt,
                    key_id=self.key_id,
                    amount=self.amount)


class BatchTable(DeclarativeObject):
    __table__ = test_batch_table

    def __repr__(self):
        return (u'<{class_name}('
                u'id={id}, '
                u'batch_id={batch_id}, '
                u'batch_dt={batch_dt}, '
                u'key_id={key_id}, '
                u'amount={amount})>').format(
                    class_name=self.__class__.__name__,
                    id=self.id,
                    batch_id=self.batch_id,
                    key_id=self.key_id,
                    amount=self.amount)


class MockBatchSet(batcher.BatchSet):
    _increasing_col = sa.literal_column('inc')

    def __init__(self, *args, **kwargs):
        super(MockBatchSet, self).__init__(*args, **kwargs)


class BatcherTest(BalanceTest):
    def _create_rows(self, amount=100, count=4):
        rows = []
        for x in range(count):
            rows.append(Table(key_id=(x/2 + 1),
                              amount=amount,
                              inc=x))
        self.session.add_all(rows)
        self.session.flush()
        return rows


class TestBatchRange(BatcherTest):
    _test_tables = [Table, BatchTable]

    def test_empty_range(self):
        batch_range = batcher.BatchRange()
        self.assertEqual(str(batch_range.where), '')

    def test_range_dt(self):
        col = test_table.c.dt
        max_val_ni = datetime.now()
        min_val = max_val_ni - timedelta(days=1)
        batch_range = batcher.BatchRange(range_col=col,
                                         min_val=min_val,
                                         max_val_ni=max_val_ni)
        self.assertEqual(str(batch_range.where),
                         str(sa.and_(col >= min_val,
                                     col < max_val_ni)))


class TestBatchSet(BatcherTest):
    _test_tables = [Table, BatchTable]

    def test_empty_batch_set(self):
        batch_set_promise = partial(batcher.BatchSet, auto_lock=False, scheme=test_table, session=self.session)
        self.assertRaises(batcher.NothingToBatch, batch_set_promise)

    def test_batch_set(self):
        self._create_rows()
        batch_set = batcher.BatchSet(session=self.session,
                                     scheme=test_table, auto_lock=False)
        self.assertEqual(str(batch_set.where),
                         str(sa.literal_column('ora_rowscn') <= 0))


class TestBatches(BatcherTest):
    _test_tables = [Table, BatchTable]

    def _get_batcher(self):
        bs = MockBatchSet(session=self.session, scheme=test_table)
        batch = batcher.Batcher(
            self.session,
            batch_table_scheme=test_batch_table,
            keys=[test_table.c.key_id],
            facts=[sa.func.sum(test_table.c.amount).label('amount'),
                   sa.func.sysdate().label('batch_dt')],
            sequence=TEST_SEQUENCE,
            batch_set=bs)
        return batch

    def test_batcher(self):
        self._create_rows()
        b = self._get_batcher()
        b.batch()

        self.assertEqual(len(b.batches), 2)
        for row in b.batches:
            self.assertEqual(row.amount, D(200))
