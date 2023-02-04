# -*- coding: utf-8 -*-
import os
import unittest
import yt.wrapper as yt_wrapper
from datacloud.dev_utils.yt import yt_utils
from datacloud.dev_utils.configs.testing_config import TEST_YT_CLUSTER


class TestYtUtils(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls._yt_client = yt_utils.get_yt_client(TEST_YT_CLUSTER)
        cls._test_folder = '//tmp/test-yt-utils'

        if cls._yt_client.exists(cls._test_folder):
            if cls._test_folder[:5] == '//tmp':
                cls._yt_client.remove(cls._test_folder, recursive=True)
            else:
                raise Exception('You are trying remove folder not from `//tmp`. Are you sure?' +
                                'Please check that folder is not from production adn remove it manually')

        yt_utils.create_folders([cls._test_folder], cls._yt_client)

    def test_create_folders(self):
        folders_to_create = (
            yt_wrapper.ypath_join(self._test_folder, 'folder1'),
            yt_wrapper.ypath_join(self._test_folder, 'folder2'),
            yt_wrapper.ypath_join(self._test_folder, 'folder3/folder4')
        )
        with self._yt_client.Transaction():
            yt_utils.create_folders(folders_to_create, self._yt_client)
            for folder in folders_to_create:
                self.assertTrue(self._yt_client.exists(folder))
                self._yt_client.remove(folder)
            self._yt_client.remove(yt_wrapper.ypath_join(self._test_folder, 'folder3'))

    def test_check_table_exists(self):
        test_table = yt_wrapper.ypath_join(self._test_folder, 'table-to-check-existance')
        with self._yt_client.Transaction():
            self.assertFalse(yt_utils.check_table_exists(test_table, self._yt_client))
            self._yt_client.create('table', test_table)
            self.assertFalse(yt_utils.check_table_exists(test_table, self._yt_client))
            self._yt_client.write_table(test_table, [{'some_key': 'some_value'}])
            self.assertTrue(yt_utils.check_table_exists, self._yt_client)
            self._yt_client.remove(test_table)

    def test_get_last_table(self):
        target_table = yt_wrapper.ypath_join(self._test_folder, '2018-04-08')
        tables_to_create = (
            target_table,
            yt_wrapper.ypath_join(self._test_folder, '2018-04-02'),
            yt_wrapper.ypath_join(self._test_folder, '2018-04-06'),
            yt_wrapper.ypath_join(self._test_folder, '2018-04-01'),
        )
        with self._yt_client.Transaction():
            received = yt_utils.get_last_table(self._test_folder, yt_client=self._yt_client)
            self.assertEqual(received, '')
            for table in tables_to_create:
                self._yt_client.create('table', table)
            received = yt_utils.get_last_table(self._test_folder, yt_client=self._yt_client)
            self.assertEqual(received, target_table)
            for table in tables_to_create:
                self._yt_client.remove(table)

    def test_remove_table(self):
        table_to_remove = yt_wrapper.ypath_join(self._test_folder, 'table_to_remove')
        with self._yt_client.Transaction():
            self._yt_client.create('table', table_to_remove)
            self.assertTrue(self._yt_client.exists(table_to_remove))
            yt_utils.remove_table(table_to_remove, self._yt_client)
            self.assertFalse(self._yt_client.exists(table_to_remove))

    def test_upload_file(self):
        local_file_path = os.path.abspath(__file__)
        yt_path = yt_wrapper.ypath_join(self._test_folder, 'test_uploaded_file')
        with self._yt_client.Transaction():
            yt_utils.uplaod_file(local_file_path, yt_path, self._yt_client)
            self.assertTrue(self._yt_client.exists(yt_path))
            self._yt_client.remove(yt_path)


class TestDynTable(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        yt_client = yt_utils.get_yt_client(TEST_YT_CLUSTER)
        cls._test_folder = '//tmp/test-dyn-table'

        if yt_client.exists(cls._test_folder):
            if cls._test_folder[:5] == '//tmp':
                yt_client.remove(cls._test_folder, recursive=True)
            else:
                raise Exception('You are trying remove folder not from `//tmp`. Are you sure?' +
                                'Please check that folder is not from production adn remove it manually')

    def test_create_table(self):
        pass

    def test_remove_table(self):
        pass

    def test_insert_row(self):
        pass

    def test_remove_row(self):
        pass

    def test_list_rows(self):
        pass

    def test_get_rows_from_tables(self):
        pass


if __name__ == '__main__':
    unittest.main()
