import unittest
import socket
import logging, logging.handlers

import mock
import pymongo

from genisys.toiler import base, stats, config


class BaseToilerBaseTestCase(unittest.TestCase):
    maxDiff = None
    CONFIG = {}

    def setUp(self):
        super(BaseToilerBaseTestCase, self).setUp()
        self.stats = stats.ToilerStats(host=None, port=None)
        self.mock_ts = mock.patch.object(base, '_get_ts')
        self.mock_ts_return_value = 1444000000
        self.mock_gethostname = mock.patch.object(socket, 'gethostname')
        self.mock_gethostname.start().return_value = 'toilerhost'
        def fake_get_ts():
            self.mock_ts_return_value += 0.01
            return self.mock_ts_return_value
        self.mock_ts.start().side_effect = fake_get_ts
        cfg_keys = (key for key in vars(config)
                    if not key.startswith('_') and key.isupper())
        self.original_config = {key: getattr(config, key) for key in cfg_keys}
        vars(config).update(self.CONFIG)

    def tearDown(self):
        super(BaseToilerBaseTestCase, self).tearDown()
        self.mock_ts.stop()
        self.mock_gethostname.stop()
        for key in vars(config):
            if key.startswith('_') or not key.isupper():
                continue
            if not key in self.original_config:
                delattr(config, key)
            setattr(config, key, self.original_config[key])

    def assertMockCall(self, actual, ename, *eargs, **ekwargs):
        aname, aargs, akwargs = actual
        self.assertEquals((aname, aargs, akwargs), (ename, eargs, ekwargs))


class ToilTestCase(BaseToilerBaseTestCase):
    CONFIG = dict(
        POSTPONE_BACKOFF_BASE=2,
        MIN_POSTPONE_TIME=31,
    )

    def test_same_value(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'fizz': 'buzz'}),
            'source': base._serialize('source'),
            '_id': '77779',
            'key': 'deadbeef',
            'vtype': 'vtype',
            'meta': {'c': 3},
            'ttime': None
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            self.assertIs(database, db)
            self.assertIsNot(record, rec)
            self.assertEquals(record, rec)
            for i in range(0, 60, 12):
                self.mock_ts_return_value += 12
                yield "test {}".format(i)
            return {'fizz': 'buzz'}, {'c': 3}
        proc.RESULT_TTL = 49
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        self.assertEquals(len(db.mock_calls), 6)
        self.assertMockCall(db.mock_calls[0], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$set': {'etime': 1444000081.03}}
        )
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )
        self.assertMockCall(db.mock_calls[2], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$set': {'etime': 1444000105.06}}
        )
        self.assertMockCall(db.mock_calls[3],
            'volatile.update_one().matched_count.__eq__', 0
        )
        self.assertMockCall(db.mock_calls[4], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$inc': {'tcount': 1, 'ucount': 1},
             '$set': {
                'ecount': 0,
                'pcount': 0,
                'etime': 1444000109.09,
                'last_status': 'same',
                'meta': {'c': 3},
                'proclog': [(1444000000.01, 'started on toilerhost'),
                            (1444000012.02, 'test 0'),
                            (1444000024.03, 'test 12'),
                            (1444000036.05, 'test 24'),
                            (1444000048.06, 'test 36'),
                            (1444000060.08, 'test 48'),
                            (1444000060.09, 'finished processing, spent 60.1 '
                                            'seconds in total, new ttl=49, '
                                            'value has not changed, new '
                                            "status=same, new meta={'c': 3}"),
                            (1444000060.09,
                             'updating record with {"$inc": {"tcount": 1, '
                             '"ucount": 1}, "$set": {"ecount": 0, "etime": '
                             '1444000109.09, "last_status": "same", "lock_id": '
                             'null, "locked": false, "meta": {"c": 3}, "pcount": '
                             '0, "proclog": "<skipped>", "ttime": 1444000060.09, '
                             '"utime": 1444000060.09}}')],
                'lock_id': None,
                'ttime': 1444000060.09,
                'utime': 1444000060.09,
                'locked': False}}
        )
        self.assertMockCall(db.mock_calls[5],
            'volatile.update_one().matched_count.__eq__', 0
        )

    def test_same_value_changed_meta(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'fizz': 'buzz'}),
            'source': base._serialize('source'),
            '_id': '77779',
            'key': 'deadbeef',
            'vtype': 'vtype',
            'meta': {'c': 3},
            'ttime': None
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            yield
            return {'fizz': 'buzz'}, {'meta': 'new'}
        proc.RESULT_TTL = 49
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        self.assertMockCall(db.mock_calls[0], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$set': {
                'ecount': 0,
                'pcount': 0,
                'lock_id': None,
                'etime': 1444000049.03,
                'utime': 1444000000.03,
                'proclog': [(1444000000.01, 'started on toilerhost'),
                            (1444000000.03, "finished processing, spent 0.0 "
                                            "seconds in total, new ttl=49, "
                                            "value has not changed, new status"
                                            "=same, new meta={'meta': 'new'}"),
                            (1444000000.03,
                             'updating record with {"$inc": {"tcount": 1, '
                             '"ucount": 1}, "$set": {"ecount": 0, "etime": '
                             '1444000049.03, "last_status": "same", "lock_id": '
                             'null, "locked": false, "meta": {"meta": "new"}, '
                             '"pcount": 0, "proclog": "<skipped>", "ttime": '
                             '1444000000.03, "utime": 1444000000.03}}')],
                'meta': {'meta': 'new'},
                'ttime': 1444000000.03,
                'locked': False,
                'last_status': 'same'},
             '$inc': {'ucount': 1, 'tcount': 1}}
        )
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )

    def test_postponed(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'fizz': 'buzz'}),
            'source': base._serialize('source'),
            '_id': '77779',
            'key': 'deadbeef',
            'vtype': 'vtype',
            'meta': {'c': 3},
            'ttime': None
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            yield
            return None, {'new': 'meta'}
        proc.RESULT_TTL = 49
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        self.assertMockCall(db.mock_calls[0], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$inc': {'pcount': 1, 'tcount': 1},
             '$set': {'etime': 1444000031.03,
                      'last_status': 'postponed',
                      'lock_id': None,
                      'locked': False,
                      'meta': {'new': 'meta'},
                      'proclog': [(1444000000.01, 'started on toilerhost'),
                                  (1444000000.03,
                                   'finished processing, spent 0.0 seconds in total, new '
                                   'ttl=31, value has not changed, new status=postponed, '
                                   "new meta={'new': 'meta'}"),
                                  (1444000000.03,
                                   'updating record with {"$inc": {"pcount": 1, '
                                   '"tcount": 1}, "$set": {"etime": 1444000031.03, '
                                   '"last_status": "postponed", "lock_id": null, '
                                   '"locked": false, "meta": {"new": "meta"}, "proclog": '
                                   '"<skipped>", "ttime": 1444000000.03}}')],
                      'ttime': 1444000000.03}},
        )
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )

    def test_new_value(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'old': 'value'}),
            'source': base._serialize('source'),
            '_id': '77779',
            'key': 'F00000',
            'vtype': 'vtype',
            'meta': {'old': 'meta'},
            'ttime': None
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            yield None
            return {'new': 'value'}, {'old': 'meta'}
        proc.RESULT_TTL = 49
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        self.assertEquals(len(db.mock_calls), 2)
        self.assertMockCall(db.mock_calls[0], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$inc': {'tcount': 1, 'ucount': 1, 'mcount': 1},
             '$set': {
                'ecount': 0,
                'pcount': 0,
                'etime': 1444000049.03,
                'last_status': 'modified',
                'meta': {'old': 'meta'},
                'lock_id': None,
                'locked': False,
                'mtime': 1444000000.03,
                'proclog': [(1444000000.01, 'started on toilerhost'),
                            (1444000000.03, 'finished processing, spent 0.0 '
                                            'seconds in total, new ttl=49, '
                                            'value has changed, new status='
                                            "modified, new meta={'old': 'meta'}"),
                            (1444000000.03,
                             'updating record with {"$inc": {"mcount": 1, '
                             '"tcount": 1, "ucount": 1}, "$set": {"ecount": 0, '
                             '"etime": 1444000049.03, "last_status": "modified", '
                             '"lock_id": null, "locked": false, "meta": {"old": '
                             '"meta"}, "mtime": 1444000000.03, "pcount": 0, '
                             '"proclog": "<skipped>", "ttime": 1444000000.03, '
                             '"utime": 1444000000.03, "value": "<skipped>"}}')],

                'ttime': 1444000000.03,
                'utime': 1444000000.03,
                'value': base._serialize({'new': 'value'})}
            }
        )
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )
        self.assertEquals(len(db.mock_calls), 2)

    def test_new_value_new_meta(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'old': 'value'}),
            'source': base._serialize('source'),
            '_id': '77779',
            'key': 'F00000',
            'vtype': 'vtype',
            'meta': {'old': 'meta'},
            'ttime': None
        }
        def proc(database, record, forced):
            yield
            return {'new': 'value'}, {'new': 'meta'}
        proc.RESULT_TTL = 49
        base.Toil(db, self.stats, proc, rec, lock_id='12341321', lock_ttl=57).run()
        self.assertEquals(len(db.mock_calls), 2)
        self.assertMockCall(db.mock_calls[0], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$inc': {'tcount': 1, 'ucount': 1, 'mcount': 1},
             '$set': {
                'ecount': 0,
                'pcount': 0,
                'etime': 1444000049.03,
                'last_status': 'modified',
                'meta': {'new': 'meta'},
                'lock_id': None,
                'locked': False,
                'mtime': 1444000000.03,
                'proclog': [(1444000000.01, 'started on toilerhost'),
                            (1444000000.03, 'finished processing, spent 0.0 '
                                            'seconds in total, new ttl=49, '
                                            'value has changed, new status='
                                            "modified, new meta={'new': 'meta'}"),
                            (1444000000.03,
                             'updating record with {"$inc": {"mcount": 1, '
                             '"tcount": 1, "ucount": 1}, "$set": {"ecount": 0, '
                             '"etime": 1444000049.03, "last_status": "modified", '
                             '"lock_id": null, "locked": false, "meta": {"new": '
                             '"meta"}, "mtime": 1444000000.03, "pcount": 0, '
                             '"proclog": "<skipped>", "ttime": 1444000000.03, '
                             '"utime": 1444000000.03, "value": "<skipped>"}}')],
                'ttime': 1444000000.03,
                'utime': 1444000000.03,
                'value': base._serialize({'new': 'value'})}
            }
        )
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )

    def test_proc_error(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'old': 'val'}),
            'source': base._serialize('source'),
            '_id': '7779',
            'key': 'key',
            'vtype': 'vtype',
            'meta': {'a': 1},
            'ttime': None
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            self.mock_ts_return_value += 12
            yield None
            raise base.ProcError('proc error message ')
        proc.RESULT_TTL = 120
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        self.assertEquals(len(db.mock_calls), 2)
        name, args, kwargs = db.mock_calls[0]
        self.assertEquals(name, 'volatile.update_one')
        self.assertEquals(args[0], {'_id': '7779', 'lock_id': '12341321'})
        self.assertEquals(args[1], {
            '$inc': {'tcount': 1, 'ecount': 1, 'pcount': 1},
            '$set': {
                'etime': 1444000043.04,
                'last_status': 'error',
                'meta': {'a': 1},
                'lock_id': None,
                'locked': False,
                'ttime': 1444000012.04,
                'proclog': [(1444000000.01, 'started on toilerhost'),
                            (1444000012.03, 'proc error message '),
                            (1444000012.04,
                             'finished processing, spent 12.0 seconds in '
                             'total, new ttl=31, value has not changed, '
                             "new status=error, new meta={'a': 1}"),
                           (1444000012.04,
                            'updating record with {"$inc": {"ecount": 1, "pcount": '
                            '1, "tcount": 1}, "$set": {"etime": 1444000043.04, '
                            '"last_status": "error", "lock_id": null, "locked": '
                            'false, "meta": {"a": 1}, "proclog": "<skipped>", '
                            '"ttime": 1444000012.04}}')],
            }
        })
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )

    def test_proc_exception(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'old': 'val'}),
            'source': base._serialize('source'),
            '_id': '7779',
            'key': 'key',
            'vtype': 'vtype',
            'meta': {'b': 2},
            'ttime': None
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            self.mock_ts_return_value += 12
            yield None
            1 / 0
        proc.RESULT_TTL = 120
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        self.assertEquals(len(db.mock_calls), 2)
        name, args, kwargs = db.mock_calls[0]
        self.assertEquals(name, 'volatile.update_one')
        self.assertEquals(args[0], {'_id': '7779', 'lock_id': '12341321'})
        proclog = args[1]['$set'].pop('proclog')
        self.assertEquals(args[1], {
            '$inc': {'tcount': 1, 'pcount': 1, 'ecount': 1},
            '$set': {
                'etime': 1444000043.04,
                'last_status': 'error',
                'lock_id': None,
                'locked': False,
                'ttime': 1444000012.04,
                'meta': {'b': 2},
            }
        })
        self.assertEquals(len(proclog), 4)
        self.assertEquals(proclog[0], (1444000000.01, 'started on toilerhost'))
        self.assertEquals(proclog[1][0], 1444000012.03)
        self.assertTrue(proclog[1][1].endswith(
            'ZeroDivisionError: division by zero\n'
        ))
        self.assertEquals(proclog[2], (1444000012.04,
            'finished processing, spent 12.0 seconds in total, new ttl=31, '
            "value has not changed, new status=error, new meta={'b': 2}"
        ))
        self.assertEquals(proclog[3], (1444000012.04,
            'updating record with {"$inc": {"ecount": 1, "pcount": 1, "tcount": 1}, '
            '"$set": {"etime": 1444000043.04, "last_status": "error", "lock_id": null, '
            '"locked": false, "meta": {"b": 2}, "proclog": "<skipped>", "ttime": '
            '1444000012.04}}'
        ))
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )

    def test_value_serialize_error(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'old': 'val'}),
            'source': base._serialize('source'),
            '_id': '7779',
            'key': 'key',
            'vtype': 'vtype',
            'meta': {'b': 2},
            'ttime': None
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            self.mock_ts_return_value += 12
            yield None
            return {'overflow': 1 << 65}, record['meta']
        proc.RESULT_TTL = 120
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        self.assertEquals(len(db.mock_calls), 2)
        name, args, kwargs = db.mock_calls[0]
        self.assertEquals(name, 'volatile.update_one')
        self.assertEquals(args[0], {'_id': '7779', 'lock_id': '12341321'})
        proclog = args[1]['$set'].pop('proclog')
        self.assertEquals(args[1], {
            '$inc': {'tcount': 1, 'pcount': 1, 'ecount': 1},
            '$set': {
                'etime': 1444000043.03,
                'last_status': 'error',
                'lock_id': None,
                'locked': False,
                'ttime': 1444000012.03,
                'meta': {'b': 2},
            }
        })
        self.assertEquals(len(proclog), 4)
        self.assertEquals(proclog[0], (1444000000.01, 'started on toilerhost'))
        self.assertTrue(proclog[1][1].endswith(
            'Python int too large to convert to C unsigned long\n'
        ))
        self.assertEquals(proclog[2], (1444000012.03,
            'finished processing, spent 12.0 seconds in total, new ttl=31, '
            "value has not changed, new status=error, new meta={'b': 2}"
        ))
        self.assertEquals(proclog[3], (1444000012.03,
            'updating record with {"$inc": {"ecount": 1, "pcount": 1, "tcount": 1}, '
            '"$set": {"etime": 1444000043.03, "last_status": "error", "lock_id": null, '
            '"locked": false, "meta": {"b": 2}, "proclog": "<skipped>", "ttime": '
            '1444000012.03}}'
        ))
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )

    def test_missing_record_on_etime_update(self):
        db = mock.MagicMock()
        update_one_result = mock.MagicMock()
        db.volatile.update_one.return_value = update_one_result
        update_one_result.matched_count.__eq__.return_value = True
        rec = {
            'value': base._serialize({'fizz': 'buzz'}),
            'source': base._serialize('source'),
            '_id': '77779',
            'key': 'decafbad',
            'vtype': 'something_real',
            'meta': {'me': 'ta'},
            'ttime': None
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            self.mock_ts_return_value += (
                base.Toil.UPDATE_NOT_MORE_OFTEN_THAN + 1
            )
            yield "bzz"
            self.assertFalse()
        proc.RESULT_TTL = 49
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        self.assertEquals(len(db.mock_calls), 2)
        self.assertMockCall(db.mock_calls[0], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$set': {'etime': 1444000078.02}}
        )
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )

    def test_outdated(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'old': 'value'}),
            'source': base._serialize('source'),
            '_id': '77779',
            'key': 'F00000',
            'vtype': 'vtype',
            'meta': {'old': 'meta'},
            'ttime': 1234560,
            'atime': 120000
        }
        lock_id = '12341321'
        lock_ttl = 57
        proc_called = []
        def proc(database, record, forced):
            proc_called.append(True)
            yield None
        proc.RESULT_TTL = 49
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        self.assertEquals(len(db.mock_calls), 1)
        self.assertMockCall(db.mock_calls[0], 'volatile.delete_one',
            {'key': 'F00000', 'vtype': 'vtype'}
        )
        self.assertFalse(proc_called)

    def test_same_value_forced_update(self):
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'fizz': 'buzz'}),
            'source': base._serialize('source'),
            '_id': '77779',
            'key': 'deadbeef',
            'vtype': 'vtype',
            'meta': {'c': 3},
            'ttime': None
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            self.assertEquals(forced, True)
            yield "log msg"
            return {'fizz': 'buzz'}, {'c': 3}
        proc.RESULT_TTL = 49
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl,
                         forced=True)
        toil.run()
        self.assertEquals(len(db.mock_calls), 2)
        self.assertMockCall(db.mock_calls[0], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$inc': {'tcount': 1, 'ucount': 1, 'mcount': 1},
             '$set': {
                'ecount': 0,
                'pcount': 0,
                'etime': 1444000049.03,
                'last_status': 'modified',
                'meta': {'c': 3},
                'mtime': 1444000000.03,
                'proclog': [(1444000000.01, 'started on toilerhost'),
                            (1444000000.01, 'doing forced update'),
                            (1444000000.02, 'log msg'),
                            (1444000000.03, 'finished processing, spent 0.0 '
                                            'seconds in total, new ttl=49, '
                                            'value has changed, new status='
                                            "modified, new meta={'c': 3}"),
                            (1444000000.03,
                             'updating record with {"$inc": {"mcount": 1, '
                             '"tcount": 1, "ucount": 1}, "$set": {"ecount": 0, '
                             '"etime": 1444000049.03, "last_status": "modified", '
                             '"lock_id": null, "locked": false, "meta": {"c": 3}, '
                             '"mtime": 1444000000.03, "pcount": 0, "proclog": '
                             '"<skipped>", "ttime": 1444000000.03, "utime": '
                             '1444000000.03, "value": "<skipped>"}}')],
                'lock_id': None,
                'ttime': 1444000000.03,
                'utime': 1444000000.03,
                'locked': False,
                'value': base._serialize({'fizz': 'buzz'})
            }}
        )
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )


class PostponeBackoffTestCase(BaseToilerBaseTestCase):
    CONFIG = dict(
        POSTPONE_BACKOFF_BASE=2,
        MIN_POSTPONE_TIME=5,
        MAX_POSTPONE_TIME=120,
    )

    def _test(self, pcount, expected_postpone):
        self.mock_ts_return_value = 1444000000
        db = mock.MagicMock()
        rec = {
            'value': base._serialize({'fizz': 'buzz'}),
            'source': base._serialize('source'),
            '_id': '77779',
            'key': 'deadbeef',
            'vtype': 'vtype',
            'meta': {'c': 3},
            'ttime': None,
            'pcount': pcount,
        }
        lock_id = '12341321'
        lock_ttl = 57
        def proc(database, record, forced):
            yield
            return None, {'new': 'meta'}
        proc.RESULT_TTL = 49
        toil = base.Toil(db, self.stats, proc, rec, lock_id, lock_ttl)
        toil.run()
        etime = 1444000000.03 + expected_postpone
        self.assertMockCall(db.mock_calls[0], 'volatile.update_one',
            {'_id': '77779', 'lock_id': '12341321'},
            {'$inc': {'pcount': 1, 'tcount': 1},
             '$set': {'etime': etime,
                      'last_status': 'postponed',
                      'lock_id': None,
                      'locked': False,
                      'meta': {'new': 'meta'},
                      'proclog': [(1444000000.01, 'started on toilerhost'),
                                  (1444000000.03,
                                   'finished processing, spent 0.0 seconds in total, new '
                                   'ttl=%d, value has not changed, new status=postponed, '
                                   "new meta={'new': 'meta'}" % (expected_postpone, )),
                                  (1444000000.03,
                                   'updating record with {"$inc": {"pcount": 1, '
                                   '"tcount": 1}, "$set": {"etime": %.2f, '
                                   '"last_status": "postponed", "lock_id": null, '
                                   '"locked": false, "meta": {"new": "meta"}, "proclog": '
                                   '"<skipped>", "ttime": 1444000000.03}}' % (etime, ))],
                      'ttime': 1444000000.03}},
        )
        self.assertMockCall(db.mock_calls[1],
            'volatile.update_one().matched_count.__eq__', 0
        )

    def test(self):
        self._test(0, 5)
        self._test(1, 5 + 2 - 1)
        self._test(2, 5 + 2 ** 2 - 1)
        self._test(5, 5 + 2 ** 5 - 1)
        self._test(6, 5 + 2 ** 6 - 1)
        self._test(10, 120)
        self._test(20, 120)



class ToilerAcquireRecordTestCase(BaseToilerBaseTestCase):
    testdb_host = 'localhost'
    testdb_name = 'genisys_unittest'

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def setUp(self):
        super(ToilerAcquireRecordTestCase, self).setUp()
        client = pymongo.MongoClient(self.testdb_host)
        self.database = client[self.testdb_name]
        self.clear_db()
        self.registry = {'foo': None, 'bar': None}

        self.records = {
            'stall_locked': {
                '_id': 1,
                'etime': 1443999999,
                'locked': True,
                'lock_id': 'lalalock',
                'vtype': 'foo',
                'mtime': 1040,
            },
            'expired_unlocked': {
                '_id': 2,
                'etime': 1443999998,
                'locked': False,
                'lock_id': None,
                'vtype': 'bar',
                'mtime': 1030,
            },
            'expired_unsupported': {
                '_id': 3,
                'etime': 1443999997,
                'locked': False,
                'lock_id': None,
                'vtype': 'unsup',
                'mtime': 1020,
            },
            'not_expired_unlocked': {
                '_id': 4,
                'etime': 1444000015,
                'locked': False,
                'lock_id': None,
                'vtype': 'foo',
                'mtime': 1000,
            },
            'not_expired_locked': {
                '_id': 5,
                'etime': 1444000030,
                'locked': True,
                'lock_id': 'ololock',
                'vtype': 'foo',
                'mtime': 1010,
            }
        }
        for rec in self.records.values():
            self.database.volatile.insert_one(rec)

    def tearDown(self):
        super(ToilerAcquireRecordTestCase, self).tearDown()
        self.clear_db()

    def test(self):
        toiler = base.Toiler(self.database, self.registry, self.stats)
        rec, lock_id = toiler._acquire_record()
        self.assertEquals(rec, self.records['stall_locked'])
        rec = self.database.volatile.find_one({'_id': rec['_id']})
        self.assertEquals(rec['locked'], True)
        self.assertTrue(lock_id)
        self.assertEquals(rec['lock_id'], lock_id)

        rec, _ = toiler._acquire_record()
        self.assertEquals(rec, self.records['expired_unlocked'])

        rec, _ = toiler._acquire_record()
        self.assertEquals(rec, None)

        toiler = base.Toiler(self.database, self.registry, self.stats,
                             is_eager=True)
        rec, _ = toiler._acquire_record()
        self.assertEquals(rec, self.records['not_expired_unlocked'])

        rec, _ = toiler._acquire_record()
        self.assertEquals(rec, None)

    def test_forced_update(self):
        toiler = base.Toiler(self.database, self.registry, self.stats,
                             forced_update_from_mtime=1015)
        rec, lock_id = toiler._acquire_record()
        self.assertEquals(rec, None)
        toiler = base.Toiler(self.database, self.registry, self.stats,
                             forced_update_from_mtime=1015, is_eager=True)
        rec, lock_id = toiler._acquire_record()
        self.assertEquals(rec, self.records['not_expired_unlocked'])
        rec, lock_id = toiler._acquire_record()
        self.assertEquals(rec, None)

    def test_forced_update2(self):
        toiler = base.Toiler(self.database, self.registry, self.stats,
                             forced_update_from_mtime=1025)
        rec, lock_id = toiler._acquire_record()
        self.assertEquals(rec, None)

        toiler = base.Toiler(self.database, self.registry, self.stats,
                             forced_update_from_mtime=1031)
        rec, lock_id = toiler._acquire_record()
        self.assertEquals(rec, self.records['expired_unlocked'])
        rec, lock_id = toiler._acquire_record()
        self.assertEquals(rec, None)

        toiler = base.Toiler(self.database, self.registry, self.stats,
                             forced_update_from_mtime=1040)
        rec, lock_id = toiler._acquire_record()
        self.assertEquals(rec, None)

        toiler = base.Toiler(self.database, self.registry, self.stats,
                             forced_update_from_mtime=1041)
        rec, lock_id = toiler._acquire_record()
        self.assertEquals(rec, self.records['stall_locked'])


class ToilerProcessRecordTestCase(BaseToilerBaseTestCase):
    def test_ahead_of_schedule(self):
        rec = {'vtype': 'bzz',
               'key': 'testrec',
               'locked': False,
               'lock_id': None,
               'etime': 1444000001}
        lock_id = 'ololock'
        proc = object()
        database = object()
        toiler = base.Toiler(database, {'bzz': proc, 'foo': None, 'bar': None},
                             self.stats)
        with mock.patch('genisys.toiler.base.Toil') as mock_toil:
            toiler._process_record(rec, lock_id)
        self.assertMockCall(
            mock_toil.mock_calls[0], '', database, self.stats, proc,
            lock_id='ololock', lock_ttl=60, record=rec, forced=False
        )
        self.assertMockCall(mock_toil.mock_calls[1], '().run')
        self.assertEquals(len(mock_toil.mock_calls), 2)

    def test_behind_schedule(self):
        rec = {'vtype': 'bzz',
               'key': 'testrec',
               'locked': False,
               'lock_id': None,
               'etime': 1443999900}
        lock_id = 'ololock'
        proc = object()
        database = object()
        toiler = base.Toiler(database, {'bzz': proc, 'foo': None, 'bar': None},
                             self.stats)
        with mock.patch('genisys.toiler.base.Toil') as mock_toil:
            toiler._process_record(rec, lock_id)
        self.assertMockCall(
            mock_toil.mock_calls[0], '', database, self.stats, proc,
            lock_id='ololock', lock_ttl=60, record=rec, forced=False
        )
        self.assertMockCall(mock_toil.mock_calls[1], '().run')
        self.assertEquals(len(mock_toil.mock_calls), 2)

    def test_forced_update(self):
        rec = {'vtype': 'bzz',
               'key': 'testrec',
               'locked': False,
               'lock_id': None,
               'etime': 1443999900}
        lock_id = 'ololock'
        proc = object()
        database = object()
        toiler = base.Toiler(database, {'bzz': proc, 'foo': None, 'bar': None},
                             self.stats, forced_update_from_mtime=123)
        with mock.patch('genisys.toiler.base.Toil') as mock_toil:
            toiler._process_record(rec, lock_id)
        self.assertMockCall(
            mock_toil.mock_calls[0], '', database, self.stats, proc,
            lock_id='ololock', lock_ttl=60, record=rec, forced=True
        )


class GetVolatilesTestCase(BaseToilerBaseTestCase):
    testdb_host = 'localhost'
    testdb_name = 'genisys_unittest'

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def setUp(self):
        super(GetVolatilesTestCase, self).setUp()
        client = pymongo.MongoClient(self.testdb_host)
        self.database = client[self.testdb_name]
        self.clear_db()

    def tearDown(self):
        super(GetVolatilesTestCase, self).tearDown()
        self.clear_db()

    def test(self):
        self.database.volatile.insert_many([
            {'_id': 1,
             'value': base._serialize('value1'),
             'source': base._serialize('source1'),
             'vtype': 'some',
             'key': 'whtvr',
             'atime': None},
            {'_id': 2,
             'value': base._serialize({'val': 2}),
             'source': base._serialize({'source': 2}),
             'vtype': 'some',
             'key': 'whtvr2',
             'atime': 1234},
            {'_id': 3,
             'value': base._serialize(['val', 3]),
             'source': base._serialize(['source', 3]),
             'vtype': 'someother',
             'key': 'whtvr',
             'atime': 777},
        ])
        res = base.get_volatiles(self.database, 'some', ['whtvr', 'whtvr2'])
        self.assertEquals(res, {
            'whtvr': {
                'value': 'value1',
                'source': 'source1',
                'vtype': 'some',
                'key': 'whtvr',
                'atime': 1444000000.01},
            'whtvr2': {
                'value': {'val': 2},
                'source': {'source': 2},
                'vtype': 'some',
                'key': 'whtvr2',
                'atime': 1444000000.01},
        })

    def test_without_values(self):
        self.database.volatile.insert_many([
            {'_id': 1,
             'source': base._serialize('source1'),
             'vtype': 'some',
             'key': 'whtvr',
             'atime': None},
        ])
        res = base.get_volatiles(self.database, 'some', ['whtvr'],
                                 with_values=False)
        self.assertEquals(res, {
            'whtvr': {
                'source': 'source1',
                'vtype': 'some',
                'key': 'whtvr',
                'atime': 1444000000.01},
        })


class ToilLoggingTestCase(unittest.TestCase):
    def setUp(self):
        self.handler = logging.handlers.BufferingHandler(capacity=20)
        logging.getLogger('genisys.toil').addHandler(self.handler)

    def tearDown(self):
        logging.getLogger('genisys.toil').removeHandler(self.handler)

    def test(self):
        m = mock.MagicMock()
        def proc(*args, **kwargs):
            yield
        proc.RESULT_TTL = 123
        record = {'key': 'mockr', 'value': None, 'vtype': 'whatever',
                  'source': base._serialize(None)}
        toil = base.Toil(database=m, stats=m, processor_cls=proc,
                         record=record, lock_id='lockid1', lock_ttl=m)
        self.handler.flush()
        toil._log_info('test')
        toil._log_error('test2 %(foo)s', foo='bar')
        toil._log_error('test3 %(foo)s')
        msgs = [lr.getMessage() for lr in self.handler.buffer]
        self.assertEquals(msgs, ['key=mockr: test',
                                 'key=mockr: test2 bar',
                                 'key=mockr: test3 %(foo)s'])
