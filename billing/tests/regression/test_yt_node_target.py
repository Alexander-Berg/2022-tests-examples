from datetime import datetime as dt

import luigi
import pytest
import pandas as pd
import numpy as np
import time

from yt.transfer_manager.client import TransferManager

from dwh.grocery.targets.yt_table_target import (
    YTTableTarget,
    YTMapNodeTarget,
    YTPathError,
)
from dwh.grocery.tools import (
    SECRET
)
from dwh.grocery.tools.datetime import (
    to_iso
)
from dwh.grocery.yt_export import YTExport

from . import is_eq


class TestYTNodeTarget:

    def setup(self):
        self.target = YTTableTarget("//home/balance/prod/dwh/mediagroup-2018-06-20")
        # self.target = YTTableTarget("//home/balance/dev/dwh/read_tst")
        self.write_target = YTTableTarget("//home/balance/dev/dwh/test_dir/wtest")

    def test_paths(self):
        # incorrect paths
        with pytest.raises(YTPathError):
            YTTableTarget("//haha/")
            YTMapNodeTarget("//haha/kek")
        table = YTTableTarget("//ololo/keke")
        dangled_table = YTTableTarget("lol/kekeke")
        map_node = YTMapNodeTarget("//cheburek/")
        dangled_map_node = YTMapNodeTarget("cheburek/")
        with pytest.raises(ValueError):
            map_node / map_node
            map_node / table

        assert (map_node / dangled_map_node).path == "//cheburek/cheburek/"
        assert (map_node / dangled_table).path == "//cheburek/lol/kekeke"
        assert (map_node / dangled_map_node / dangled_table).path == "//cheburek/cheburek/lol/kekeke"

    def test_update_id_preservance(self):
        dangled_table = YTTableTarget("lol/kekeke", update_id=1488)
        map_node = YTMapNodeTarget("//cheburek/")
        assert (map_node / dangled_table).update_id == 1488

    def test_map_node_emptiness(self, yt_test_root):
        dangled_map_node = YTMapNodeTarget("cheburek/")
        dangled_table = YTTableTarget("lol/kekeke")

        temporal: YTMapNodeTarget = yt_test_root / dangled_map_node
        temporal.clear()
        temporal.create()
        assert temporal.is_empty()
        temporal_table = temporal / dangled_table
        temporal_table.clear()
        temporal_table.create()
        assert not temporal.is_empty()
        temporal_table.clear()

    def test_node_move(self, yt_test_root):
        test_table = yt_test_root / YTTableTarget("test_move")
        test_table.create()
        result_table = yt_test_root / YTMapNodeTarget("rip_in_pizza/") / test_table.as_leaf()
        new_result_table = test_table.move(result_table.path, force=True)
        assert new_result_table == result_table
        assert result_table.exists()

    def test_write_read(self):
        rows = [
            {'a': 1, 'b': 2, 'c': 'a'},
            {'a': 2, 'b': 3, 'c': 'b'},
            {'a': 3, 'b': 4, 'c': 'c'},
        ]
        self.write_target.write(rows)
        yt_rows = self.write_target.read()
        assert rows == yt_rows

    def test_write_read_cached(self):
        rows = [
            {'a': 1, 'b': 2, 'c': 'a'},
            {'a': 2, 'b': 3, 'c': 'b'},
            {'a': 3, 'b': 4, 'c': 'c'},
        ]
        self.write_target.write(rows)
        yt_rows = self.write_target.read(cached=True)
        assert rows == yt_rows
        rows = [
            {'a': 1, 'b': 2, 'c': 'a'},
        ]
        self.write_target.write(rows)
        yt_rows = self.write_target.read(cached=True)
        assert rows == yt_rows

    def test_mass_attr_write(self):
        attrs = {
            'aaa': [1, 2, 3],
            'bbb': '2'
        }
        self.write_target.set_attrs(**attrs)
        assert attrs['aaa'] == self.write_target.get_attr('aaa')
        assert attrs['bbb'] == self.write_target.get_attr('bbb')

    def test_dict_attr(self):
        attr = {
            'a': [1, 2, 3],
            'b': {'a': 1, 'b': 2},
            'c': 3,
        }
        self.write_target.set_attr('sync', attr)
        stored = self.write_target.get_attr('sync')
        assert attr == stored, stored

    def test_path_parsing(self):
        t = YTTableTarget("abc/def")
        ta = YTTableTarget("hahn.abc/def")
        tas = YTTableTarget("abc/def", cluster="hahn")
        ololo = YTTableTarget("hahn.abc/def", cluster="arnold")
        assert t != ta
        assert ta == tas
        assert ololo.cluster == "hahn"

    def test_midpath_dot(self):
        t = YTTableTarget("//home/balance/prod/bo.t_partner_turnover_expense/latest")
        ct = YTTableTarget("hahn.//home/balance/prod/bo.t_partner_turnover_expense/latest")
        assert t.cluster is None
        assert ct.cluster == "hahn"
        assert t.path == ct.path

    def test_node_repr(self):
        ta = YTTableTarget("hahn.abc/def")
        assert eval(repr(ta)) == ta

    def test_cluster_specific_creation(self, hahn_yt_test_root, arnold_yt_test_root):
        t = YTTableTarget("cluster_specific_creation/test")
        hahn_t = hahn_yt_test_root / t
        arnold_t = arnold_yt_test_root / t

        assert not hahn_t.exists()
        assert not arnold_t.exists()
        hahn_t.create()
        assert hahn_t.exists()
        assert not arnold_t.exists()
        arnold_t.create()
        assert hahn_t.exists()
        assert arnold_t.exists()

    def test_cluster_specific_read_write(self, hahn_yt_test_root, arnold_yt_test_root, fake_t_product):
        t = YTTableTarget("cluster_specific_rw/test", schema=fake_t_product.get_yt_schema())
        hahn_t = hahn_yt_test_root / t
        arnold_t = arnold_yt_test_root / t

        hahn_t.create()
        arnold_t.create()

        hahn_t.write(fake_t_product.read())
        assert is_eq(hahn_t, fake_t_product)
        assert not is_eq(hahn_t, arnold_t)

        arnold_t.write(hahn_t.read())
        assert is_eq(hahn_t, arnold_t)

    def test_table_inter_cluster_sync(self, hahn_yt_test_root, arnold_yt_test_root):
        t = YTTableTarget("cluster_sync/test")
        # rows = [
        #     {'a': 1, 'b': 2, 'c': 'a'},
        #     {'a': 2, 'b': 3, 'c': 'b'},
        #     {'a': 3, 'b': 4, 'c': 'c'},
        # ]
        hahn_t = hahn_yt_test_root / t
        arnold_t = arnold_yt_test_root / t

        assert not hahn_t.exists()
        assert not arnold_t.exists()

        hahn_t.create()
        assert hahn_t.exists()
        assert not arnold_t.exists()

        transferred_arnold_t, _ = hahn_t.transfer_to_cluster("arnold")
        assert hahn_t.exists()
        assert transferred_arnold_t.exists()
        assert arnold_t.exists()
        assert transferred_arnold_t == arnold_t

    # def test_table_inter_cluster_sync_nonempty(self, hahn_yt_test_root, arnold_yt_test_root):
    #     t = YTTableTarget("cluster_sync/test")
    #     rows = [
    #         {'a': 1, 'b': 2, 'c': 'a'},
    #         {'a': 2, 'b': 3, 'c': 'b'},
    #         {'a': 3, 'b': 4, 'c': 'c'},
    #     ]
    #     hahn_t = hahn_yt_test_root / t
    #     hahn_t.write(rows)
    #     transferred_arnold_t, _ = hahn_t.transfer_to_cluster("arnold")

    def test_table_inter_cluster_sync_async(self, hahn_yt_test_root, arnold_yt_test_root):
        t = YTTableTarget("cluster_sync_async/test")
        # rows = [
        #     {'a': 1, 'b': 2, 'c': 'a'},
        #     {'a': 2, 'b': 3, 'c': 'b'},
        #     {'a': 3, 'b': 4, 'c': 'c'},
        # ]
        hahn_t = hahn_yt_test_root / t
        arnold_t = arnold_yt_test_root / t

        assert not hahn_t.exists()
        assert not arnold_t.exists()

        hahn_t.create()
        assert hahn_t.exists()
        assert not arnold_t.exists()

        transferred_arnold_t, task_id = hahn_t.transfer_to_cluster("arnold", sync=False)
        tm_client = TransferManager(token=SECRET['YT_TOKEN'])
        tm_client.get_task_info(task_id=task_id)

    def test_table_inter_cluster_sync_as_task(self, hahn_yt_test_root, arnold_yt_test_root):
        t = YTTableTarget("cluster_sync_task/test")
        hahn_t = hahn_yt_test_root / t
        hahn_t.update_id = "hooray!"
        hahn_t.touch()

        arnold_t = arnold_yt_test_root / t
        arnold_t.create()

        transfer_task = hahn_t.make_transfer_task("arnold")
        assert not transfer_task.complete()
        transfer_task.run()
        assert transfer_task.complete()
        assert arnold_t.stored_update_id == hahn_t.stored_update_id

    def test_write_df_to_yt(self, yt_test_root):
        df = pd.DataFrame(
            np.random.randint(0, 10, size=(10, 3)),
            columns=list('ABC'),
        )

        result = yt_test_root / YTTableTarget("pandas_df/res")
        result.schema = YTTableTarget.extract_schema_from_df(df)
        result.write(df.to_dict(orient='records'))

    # @pytest.mark.bad
    def test_write_date_df_to_yt(self, yt_test_root):
        df = pd.DataFrame({
            'a': ["2019-06-02", "2019-06-02", "2019-06-02", "2019-06-02"],
            'b': ["a", "b", "c", "d"],
        })
        df['a'] = pd.to_datetime(df['a'])

        df['a'] = df['a'].dt.strftime('%Y-%m-%d')

        result = yt_test_root / YTTableTarget("pandas_df/res_dt")
        result.schema = YTTableTarget.extract_schema_from_df(df)
        result.write(df.to_dict(orient='records'))

    def test_write_nan_df_to_yt(self, yt_test_root):
        df = pd.DataFrame({
            'a': ["2019-06-02", "2019-06-02", "2019-06-02", "2019-06-02"],
            'b': [np.nan, np.nan, np.nan, 2],
        })
        result = yt_test_root / YTTableTarget("pandas_df/res_nan")
        result.schema = YTTableTarget.extract_schema_from_df(df)

        df = df.replace({pd.np.nan: None})

        result.write(df.to_dict(orient='records'))

    @pytest.mark.parametrize("fake_tables", [["f_table1", "f_table2", "f_table3", "f_table4"]],
                             indirect=["fake_tables"])
    def test_map_node_recursive_getattr(self, fake_tables, hahn_yt_test_root):
        """
            Checks for getting attributes recursively from all tables in mapnode

                           test_mapnode_getattr_value
                          //
                         child1
                        //        \\                \\
                       //          \\                \\
                    grandchild1     grandchild2      f_table_1
                    //         \\     \\          \\
                   f_table_2  file   f_table_3  f_table_4
        """
        # uploading test tables to YT
        upd_id = to_iso(dt.now())
        child1 = hahn_yt_test_root / YTMapNodeTarget("yt_export/test_mapnode_getattr/child1/")

        grandchild1 = hahn_yt_test_root / YTMapNodeTarget(
            "yt_export/test_mapnode_getattr/child1/grandchild1/")
        grandchild2 = hahn_yt_test_root / YTMapNodeTarget(
            "yt_export/test_mapnode_getattr/child1/grandchild2/")

        task = YTExport(
            update_id=upd_id,
            tables=["F_TABLE1", "F_TABLE2", "F_TABLE3", "F_TABLE4"],
            targets={
                "F_TABLE1": child1.path,
                "F_TABLE2": grandchild1.path,
                "F_TABLE3": grandchild2.path,
                "F_TABLE4": grandchild2.path,
            },
            meta_dict={
                "F_TABLE1": {
                    "type": "full",
                    "source_uri": fake_tables["f_table1"].uri,
                    "chunks": {},
                },
                "F_TABLE2": {
                    "type": "full",
                    "source_uri": fake_tables["f_table2"].uri,
                    "chunks": {},
                },
                "F_TABLE4": {
                    "type": "full",
                    "source_uri": fake_tables["f_table4"].uri,
                    "chunks": {},
                },
                "F_TABLE3": {
                    "type": "full",
                    "source_uri": fake_tables["f_table3"].uri,
                    "chunks": {},
                },
            }
        )

        time.sleep(5)
        is_success = luigi.build([task], workers=1)

        assert is_success

        # creating file object (path for this file will be ignored)
        child1.client.create("file", child1.path + "file")

        # check that all tables paths are returned
        paths = child1.getattr_recursive_values("path")
        table_paths = [
            child1.path + "F_TABLE1",
            grandchild1.path + "F_TABLE2",
            grandchild2.path + "F_TABLE3",
            grandchild2.path + "F_TABLE4",
            ]

        assert len(paths) == 4, f"There are 4 tables in mapnode {child1.path}"
        for path in table_paths:
            assert path in paths, f"{path} must be in mapnode {child1.path}"

    def teardown(self):
        self.write_target.clear()
#

    # def test_query_read(self):
    #     r = self.target.client.query(f"select * from [{self.target.path}]")
    #     r.run()
    #     res = r.get_results()
    #     for table in res:
    #         table.fetch_full_data()
    #         # print(table)
    #         rows = []
    #         columns = table.raw_refs[0]['Columns']
    #         # out.write("\t".join(columns) + "\n")
    #         for row in table.rows:
    #             # TODO поправить в ыкле преобразование unicode = str на unicode = lambda x: x.decode('utf-8')
    #             out_row = [
    #                 codecs.escape_decode(cell[2: -1])[0].decode('utf-8')
    #                 if isinstance(cell, str) and cell.startswith("b'") else cell
    #                 for cell in row
    #             ]
    #             record = {key: value for key, value in zip(columns, out_row)}
    #             rows.append(record)
    #             # print(row)
    #         assert type(rows[0]) == dict
    #         assert type(rows) == list
    #         assert len(rows) == 248452
