from datetime import datetime as dt
from datetime import timedelta as timedelta

import luigi
import pytest

from dwh.grocery.task.yt_transfer_task import (
    YTMapNodeTransferTask,
    YTTransferTask
)
from dwh.grocery.targets import (
    YTTableTarget,
    YTMapNodeTarget,
)
from dwh.grocery.yt_export import YTExport
from dwh.grocery.tools import to_iso

from . import is_eq
from .strategies import rows_from_schema


class TestYTTransfer:

    def test_transfer(self, hahn_yt_test_root, arnold_yt_test_root, table_content_with_schema):
        content, schema, _ = table_content_with_schema

        table_a = YTTableTarget("transfer_task/a", schema=schema)
        table_b = YTTableTarget("transfer_task/b")
        junk = YTMapNodeTarget("junk/")

        hahn_table_a = hahn_yt_test_root / table_a
        hahn_table_b = hahn_yt_test_root / table_b

        arnold_table_a = arnold_yt_test_root / junk / table_a
        arnold_table_b = arnold_yt_test_root / junk / table_b

        hahn_table_a.create()
        hahn_table_a.write(content)

        hahn_table_b.create()

        assert not arnold_table_a.exists()
        assert not arnold_table_b.exists()

        task = YTTransferTask(
            sources=[hahn_table_a.path, hahn_table_b.path],
            destinations=[arnold_table_a.path, arnold_table_b.path],
            from_cluster="hahn",
            to_cluster="arnold",
        )
        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert is_eq(hahn_table_a, arnold_table_a)
        assert is_eq(hahn_table_b, arnold_table_b)

    def test_update_id_preserve(self, hahn_yt_test_root, arnold_yt_test_root):
        table_a = YTTableTarget("p_transfer_task/a", update_id="hooray!")
        junk = YTMapNodeTarget("junk/")

        hahn_table_a = hahn_yt_test_root / table_a

        arnold_table_a = arnold_yt_test_root / junk / table_a

        hahn_table_a.create()
        hahn_table_a.touch()

        assert not arnold_table_a.exists()

        task = YTTransferTask(
            sources=[hahn_table_a.path],
            destinations=[arnold_table_a.path],
            from_cluster="hahn",
            to_cluster="arnold",
        )
        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert hahn_table_a.stored_update_id == arnold_table_a.stored_update_id

    def test_task_maker(self, hahn_yt_test_root, arnold_yt_test_root, table_content_with_schema):
        content, schema, _ = table_content_with_schema

        table_a = YTTableTarget("make_transfer_task/a", schema=schema)
        table_b = YTTableTarget("make_transfer_task/b")
        junk = YTMapNodeTarget("junk/")

        hahn_table_a = hahn_yt_test_root / table_a
        hahn_table_b = hahn_yt_test_root / table_b

        arnold_table_a = arnold_yt_test_root / junk / table_a
        arnold_table_b = arnold_yt_test_root / junk / table_b

        hahn_table_a.create()
        hahn_table_a.write(content)

        hahn_table_b.create()

        task = YTTransferTask.create_from_table_pairs([
            (hahn_table_a, arnold_table_a),
            (hahn_table_b, arnold_table_b)
        ])
        is_success = luigi.build([task], local_scheduler=True)

        assert is_success
        assert is_eq(hahn_table_a, arnold_table_a)
        assert is_eq(hahn_table_b, arnold_table_b)

    # Выполняется условие на последней строчке теста is_eq(
    #     YTTableTarget(
    #         '//home/balance-test/test/tmp/pytest/transfer_only_new/c', update_id='kekeke',
    #         schema=[{'name': 'a', 'type': 'int64'}], cluster='hahn'
    #     ),
    #     YTTableTarget(
    #         '//home/balance-test/test/tmp/pytest/junk/transfer_only_new/c', update_id='kekeke',
    #         schema=[{'name': 'a', 'type': 'int64'}], cluster='arnold'
    #     )
    # ) а не должно
    @pytest.mark.skip(reason='Something very strange, failing')
    def test_transfer_only_new(self, hahn_yt_test_root, arnold_yt_test_root, table_content_with_schema):
        content, schema, pyschema = table_content_with_schema

        table_a = YTTableTarget(
            "transfer_only_new/a",
            schema=schema,
            update_id="hooray"
        )
        table_b = YTTableTarget(
            "transfer_only_new/b"
        )
        table_c = YTTableTarget(
            "transfer_only_new/c",
            schema=schema,
            update_id="kekeke"
        )
        junk = YTMapNodeTarget("junk/")

        hahn_table_a = hahn_yt_test_root / table_a
        hahn_table_b = hahn_yt_test_root / table_b
        hahn_table_c = hahn_yt_test_root / table_c

        arnold_table_a = arnold_yt_test_root / junk / table_a
        arnold_table_b = arnold_yt_test_root / junk / table_b
        arnold_table_c = arnold_yt_test_root / junk / table_c

        hahn_table_a.create()
        hahn_table_a.write(content)
        hahn_table_a.touch()

        hahn_table_b.create()

        hahn_table_c.create()
        hahn_table_c.write(content)
        hahn_table_c.touch()

        task = YTTransferTask.create_from_table_pairs(
            [
                (hahn_table_a, arnold_table_a),
                (hahn_table_b, arnold_table_b),
                (hahn_table_c, arnold_table_c)
            ],
            only_new=True
        )
        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert arnold_table_a.stored_update_id == "hooray"
        assert arnold_table_c.stored_update_id == "kekeke"
        assert is_eq(hahn_table_a, arnold_table_a)
        assert is_eq(hahn_table_b, arnold_table_b)
        assert is_eq(hahn_table_c, arnold_table_c)

        print("First done")

        new_content = rows_from_schema(10, pyschema).example()
        hahn_table_a.update_id = "azaza"
        hahn_table_a.write(new_content)
        hahn_table_a.touch()
        hahn_table_b.write(new_content)
        hahn_table_c.write(new_content)

        assert arnold_table_a.stored_update_id == "hooray"
        assert hahn_table_a.stored_update_id == "azaza"
        assert arnold_table_c.stored_update_id == "kekeke"
        assert hahn_table_c.stored_update_id == "kekeke"

        is_success = luigi.build([task.clone()], local_scheduler=True)
        assert is_success
        assert arnold_table_a.stored_update_id == "azaza"
        assert is_eq(hahn_table_a, arnold_table_a)
        assert is_eq(hahn_table_b, arnold_table_b)
        assert not is_eq(hahn_table_c, arnold_table_c)

    @pytest.mark.parametrize("fake_tables", [["f_table1", "f_table2", "f_table4"]],
                             indirect=["fake_tables"])
    def test_transferring_all_tables_in_mapnode(self, fake_tables, hahn_yt_test_root,
                                                freud_yt_test_root):
        """
            Проверяет транcфер всех таблиц из map_node (child1)
                           test_mapnode_export
                          //                   \\
                         child1               child2
                        //     \\                    \\
                       //    f_table_1              f_table_4
                    granchild1
                    //
                   f_table_2

        """
        # выгрузка таблиц
        upd_id = to_iso(dt.now())
        child1 = hahn_yt_test_root / YTMapNodeTarget("yt_export/test_mapnode_export/child1/")
        child2 = hahn_yt_test_root / YTMapNodeTarget("yt_export/test_mapnode_export/child2/")
        grandchild1 = hahn_yt_test_root / YTMapNodeTarget(
            "yt_export/test_mapnode_export/child1/grandchild1/")

        task = YTExport(
            update_id=upd_id,
            tables=["F_TABLE1", "F_TABLE2", "F_TABLE4"],
            targets={
                "F_TABLE1": child1.path,
                "F_TABLE2": grandchild1.path,
                "F_TABLE4": child2.path,
            },
            meta_dict={
                "F_TABLE1": {
                    "type": "small",
                    "source_uri": fake_tables["f_table1"].uri,
                    "chunks": {},
                },
                "F_TABLE2": {
                    "type": "small",
                    "source_uri": fake_tables["f_table2"].uri,
                    "chunks": {},
                },
                "F_TABLE4": {
                    "type": "small",
                    "source_uri": fake_tables["f_table4"].uri,
                    "chunks": {},
                },
            }
        )

        # плавающая ошибка при работе в несколько воркеров, поэтому пока ставим 1
        assert luigi.build([task], workers=1)

        # трансфер таблиц из mapnode
        task = YTMapNodeTransferTask(
            from_cluster="hahn",
            to_cluster="freud",
            from_mapnode=[child1.path],
        )

        assert luigi.build([task], workers=8)

        common_path = "//home/balance-test/test/tmp/pytest/yt_export/test_mapnode_export/"
        f_table1 = YTTableTarget(common_path + "child1/F_TABLE1", cluster="freud")
        f_table2 = YTTableTarget(common_path + "child1/grandchild1/F_TABLE2", cluster="freud")
        f_table4 = YTTableTarget(common_path + "child2/F_TABLE4", cluster="freud")

        assert f_table1.exists(), "F_TABLE1 must have been uploaded to freud {}".format(
            f_table1.path)
        assert f_table2.exists(), "F_TABLE2 must have been uploaded to freud {}".format(
            f_table2.path)
        assert not f_table4.exists(), "F_TABLE4 must not have been uploaded to freud {}".format(
            f_table4.path)

    # These targets are equal but shouldn't (flaky?)
    # YTTableTarget('//home/balance-test/test/tmp/pytest/transfer_task/a0', update_id='2021-12-07T08:40:51.867246Z',
    # cluster='hahn'),
    # YTTableTarget('//home/balance-test/test/tmp/pytest/transfer_task/a0', update_id='2021-12-06T08:40:49.555633Z',
    # schema=[{'name': 'a', 'type': 'int64'}],
    # cluster='arnold'))
    @pytest.mark.skip(reason='Something very strange, failing')
    def test_transfer_with_different_schemas(self, hahn_yt_test_root, arnold_yt_test_root, table_content_with_schema):
        content, schema, _ = table_content_with_schema
        table_a0 = YTTableTarget("transfer_task/a0", schema=schema, update_id=to_iso(dt.now()-timedelta(days=1)))

        arnold_table_a = arnold_yt_test_root / table_a0
        arnold_table_a.clear()
        arnold_table_a.create()
        arnold_table_a.write([])

        table_a1 = YTTableTarget("transfer_task/a0",
                                 schema=schema.append(
                                     {
                                         "name": "xxx",
                                         "type": "int64"
                                     }
                                 ),
                                 update_id=to_iso(dt.now())
                                 )

        hahn_table_a = hahn_yt_test_root / table_a1
        hahn_table_a.clear()
        hahn_table_a.create()
        hahn_table_a.write(content)

        assert hahn_table_a.exists()
        assert arnold_table_a.exists()
        assert not is_eq(hahn_table_a, arnold_table_a)

        task = YTTransferTask(
            sources=[hahn_table_a.path],
            destinations=[arnold_table_a.path],
            from_cluster="hahn",
            to_cluster="arnold",
        )

        is_success = luigi.build([task], local_scheduler=True)

        assert is_success
        assert is_eq(hahn_table_a, arnold_table_a)

        hahn_table_a.clear()
        arnold_table_a.clear()
