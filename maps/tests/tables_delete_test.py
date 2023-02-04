import os
import tempfile
import yatest.common

from datetime import datetime
from test_file_system import TestFileSystem


TESTING_DATE = datetime(2021, 12, 7).date()


def test_mined_tables_cleanup():
    with tempfile.TemporaryDirectory() as temp:
        file_system = TestFileSystem(
            starting_date=TESTING_DATE,
            base_path=os.path.join(temp, "daps"),
            resource_name='mined_tables',
            mined_hard_delete_age=50,
            mined_soft_delete_age=10,
            snippets_delete_age=1)

        file_system.clean_mined_tables()
        file_system.dump_fs(output_file='mined_tables')

        return yatest.common.canonical_file('mined_tables', local=True)


def test_snippets_tables_cleanup():
    with tempfile.TemporaryDirectory() as temp:
        file_system = TestFileSystem(
            starting_date=TESTING_DATE,
            base_path=os.path.join(temp, "daps"),
            resource_name='snippets_tables',
            mined_hard_delete_age=1,
            mined_soft_delete_age=1,
            snippets_delete_age=15)

        file_system.clean_snippets_tables()
        file_system.dump_fs(output_file='snippets_result')

        return yatest.common.canonical_file('snippets_result', local=True)
