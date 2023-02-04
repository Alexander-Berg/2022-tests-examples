import subprocess
import os
import shutil


class MigratorWrapper(object):
    ALEMBIC_ENV_DIR_NAME = 'alembic'
    DATA_PATH = 'ads/watchman/timeline/api/migrations'
    BINARY_PATH = 'ads/watchman/timeline/api/migrations/migrator'

    def __init__(self):
        import yatest
        self._data_dir = yatest.common.source_path(self.DATA_PATH)
        self._binary_dir = yatest.common.binary_path(self.BINARY_PATH)

        self._dst_dir = os.path.join(os.getcwd(), self.ALEMBIC_ENV_DIR_NAME)
        src_name = os.path.join(self._data_dir, self.ALEMBIC_ENV_DIR_NAME)
        shutil.copytree(src_name, self._dst_dir)

    def __enter__(self):
        return self

    def __exit__(self, *args):
        shutil.rmtree(self._dst_dir)

    def run_migrations(self, postgres):
        conf_path = os.path.join(self.ALEMBIC_ENV_DIR_NAME, 'alembic.ini')
        args = (self._binary_dir, "--conf", conf_path, "upgrade", "head")
        subprocess.check_call(args, env={'DB_URI': postgres.db_url})
