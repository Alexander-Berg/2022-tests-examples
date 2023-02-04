from datetime import date, datetime, timedelta

from django.test import SimpleTestCase as TestCase
from unittest import mock
from treelib.exceptions import NodeIDAbsentError

from analytics.partners.collect_write_offs_job import CollectWriteOffs, DataPoint, _datapoint_from_clicks_values, \
    _datapoint_from_clicks_count, _datapoint_from_placement, _filter_clicks_rows


class TestCollectWriteOffs(TestCase):
    def test_process_partners(self):
        self.fail()

    def test_federal_region_aggregated_data(self):
        self.fail()

    @mock.patch('analytics.Partners.collectWriteOffsJob.connections')
    def test_placements(self, cons):
        c = self.get_instance()
        execute_mock = mock.MagicMock()
        c.hive.execute = execute_mock
        region = 1
        client = 2
        price = 3
        expected = [[date.today(), region, client, price]]
        cursor_mock = mock.MagicMock()
        cons['salesman'].cursor.return_value = cursor_mock
        transaction_mock = mock.MagicMock()
        cursor_mock.__enter__.return_value = transaction_mock
        transaction_mock.fetchall.return_value = expected
        result = c.placements()
        got, *_ = result
        self.assertEqual(got.client, client)
        self.assertEqual(got.placement_value, price)
        self.assertEqual(got.rid, region)

    def test_clicks_count(self):
        self.fail()

    def test_clicks_values(self):
        self.fail()

    def test_region_by_rid(self):
        c = self.get_instance()
        tree_mock = mock.MagicMock()
        c.loc = tree_mock
        # should raise lookup error when desired node was not found
        tree_mock.rsearch.return_value = []
        self.assertRaises(LookupError, c.region_by_rid, -111)
        tree_mock.rsearch.side_effect = NodeIDAbsentError()
        self.assertRaises(LookupError, c.region_by_rid, -111)
        # should not mask other exc
        tree_mock.rsearch.side_effect = UnicodeWarning()
        self.assertRaises(UnicodeWarning, c.region_by_rid, -111)
        want = 1
        tree_mock.rsearch.side_effect = None
        tree_mock.rsearch.return_value = [want]
        got = c.region_by_rid(111)
        self.assertEqual(want, got)

    @mock.patch("analytics.Partners.collectWriteOffsJob.WriteOffsLog")
    def test_insert_writeoffs(self, writeoffs_mock):
        writeoffs_mock.objects = mock.MagicMock()
        bulk_insert_mock = mock.MagicMock()
        writeoffs_mock.objects.bulk_insert_on_duplicate = bulk_insert_mock
        c = self.get_instance()
        wanted = object()
        c.insert_writeoffs([wanted])
        self.assertEqual(bulk_insert_mock.call_count, 1)
        (_, got, _), kwargs = bulk_insert_mock.call_args
        self.assertIn(wanted, got)

    @mock.patch("analytics.Partners.collectWriteOffsJob.WriteOffsLog")
    def test_not_insert_writeoffs_if_empty(self, writeoffs_mock):
        writeoffs_mock.objects = mock.MagicMock()
        bulk_insert_mock = mock.MagicMock()
        writeoffs_mock.objects.bulk_insert_ignore = bulk_insert_mock
        c = self.get_instance()
        c.insert_writeoffs([])
        bulk_insert_mock.assert_not_called()

    @mock.patch("analytics.Partners.collectWriteOffsJob.NewPartner")
    def test_set_paid_for_partners(self, partner_mock):
        bulk_update_mock = mock.MagicMock()
        partner_mock.objects = mock.MagicMock()
        partner_mock.objects.bulk_update = bulk_update_mock
        c = self.get_instance()
        ids = [1, 2, 3]
        c.set_paid_for_partners(ids)
        (objs, fields, _), *kwargs = bulk_update_mock.call_args
        field, = fields
        self.assertEqual(field, "used_paid")
        self.assertTrue(all(obj.pk in ids and getattr(obj, field) for obj in objs))

    def get_instance(self):
        return CollectWriteOffs(from_date="2019-11-15")


day = date.today()
client = 1
rid = 2
placement = 3
clicks_value = 4
clicks_overdraft = 5
views_new = 6
views_old = 7


class TestDataPoint(TestCase):

    @property
    def datapoint(self):
        return DataPoint(day=day, rid=rid, client=client, placement_value=placement, clicks_value=clicks_value,
                         clicks_overdraft=clicks_overdraft,
                         views_new=views_new,
                         views_used=views_old)

    def test_add(self):
        dp1 = self.datapoint
        dp2 = self.datapoint

        got = dp1 + dp2
        self.assertFalse(got is dp1 or got is dp2, 'should return new datapoint on addition')
        self.assertEqual(got.day, dp1.day)
        self.assertEqual(got.client, dp1.client)
        self.assertEqual(got.rid, dp1.rid)
        self.assertEqual(got.placement_value, dp1.placement_value + dp2.placement_value)
        self.assertEqual(got.clicks_value, dp1.clicks_value + dp2.clicks_value)
        self.assertEqual(got.clicks_overdraft, dp1.clicks_overdraft + dp2.clicks_overdraft)
        self.assertEqual(got.views_new, dp1.views_new + dp2.views_new)
        self.assertEqual(got.views_used, dp1.views_used + dp2.views_used)

    def test_cant_merge_dp_with_different_key(self):
        _add = lambda a: a[0] + a[-1]
        self.assertRaises(TypeError, _add, (self.datapoint, 0))
        dp1 = self.datapoint
        dp2 = self.datapoint
        # different client
        dp2.client = dp1.client + 1
        self.assertRaises(ValueError, _add, (dp1, dp2))
        dp2 = self.datapoint
        dp2.day = dp1.day + timedelta(1)
        self.assertRaises(ValueError, _add, (dp1, dp2))
        dp2 = self.datapoint
        dp2.day = dp1.rid + 1
        self.assertRaises(ValueError, _add, (dp1, dp2))

    def test_row(self):
        dp = self.datapoint
        want = len(dp.COLS)
        got = len(dp.row)
        self.assertEqual(want, got, 'want: {}, got: {}'.format(want, got))


class TestMappings(TestCase):
    def test_count_mapping(self):
        d = datetime.now()
        dp = _datapoint_from_clicks_count((d, None, '75', '32620126', 'new'))
        self.assertEqual(dp.clicks_value, 0)
        self.assertEqual(dp.clicks_overdraft, 0)
        self.assertEqual(dp.rid, 75)
        self.assertEqual(dp.client, 32620126)
        self.assertEqual(dp.day, date.today())
        self.assertEqual(dp.views_new, 1)
        self.assertEqual(dp.views_used, 0)

    def test_clicks_overdraft_mapping(self):
        wanted_date = datetime.strptime('2019-11-21', '%Y-%m-%d').date()
        day, rid, client, total_value, total_overdraft = '2019-11-21	75	32620126	170000	54360'.split('\t')
        dp = _datapoint_from_clicks_values((day, rid, client, total_value, total_overdraft))
        self.assertEqual(wanted_date, dp.day)
        self.assertEqual(dp.rid, int(rid))
        self.assertEqual(dp.client, int(client))
        self.assertEqual(dp.clicks_value, int(total_value))
        self.assertEqual(dp.clicks_overdraft, int(total_overdraft))


class TestFilters(TestCase):
    def test_clicks_overdraft_filter(self):
        wanted_date = datetime.strptime('2019-11-21', '%Y-%m-%d').date()
        day, rid, client, total_value, total_overdraft = '2019-11-21	75	32620126	170000	54360'.split('\t')
        result = ()
        self.assertFalse(_filter_clicks_rows((day, None, client, total_value, total_overdraft)))
        self.assertFalse(_filter_clicks_rows((day, rid, None, total_value, total_overdraft)))
        self.assertFalse(_filter_clicks_rows((day, rid, client, None, total_overdraft)))
        self.assertFalse(_filter_clicks_rows((day, rid, client, total_value, None)))
        self.assertTrue(_filter_clicks_rows((day, rid, client, total_value, total_overdraft)))
        self.assertTrue(_filter_clicks_rows((day, rid, client, total_value, 0)))
