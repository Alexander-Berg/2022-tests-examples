import os
import shutil
import yatest

from maps.infra.ecstatic.ymtorrent.py import ymtorrent
import maps.pylibs.utils.lib.filesystem as filesystem

tests_data_source = yatest.common.source_path("maps/infra/ecstatic/ymtorrent/py/tests/data")


def test_generate_torrent():
    def prepare_torrent_data(tmp_dir, dir_name):
        torrent_data_path = os.path.join(tmp_dir, dir_name)

        shutil.copytree(os.path.join(tests_data_source, 'test_torrent_data'), torrent_data_path)
        shutil.copy(os.path.join(tests_data_source, 'external_symlink_target'), tmp_dir)

        os.chmod(torrent_data_path, 0o777)
        os.symlink('../external_symlink_target', os.path.join(torrent_data_path, 'symlink'))

        return torrent_data_path

    for dir_name in ['dir_name', 'another_dir_name']:
        with filesystem.temporary_directory() as tmp_dir:
            torrent_data_path = prepare_torrent_data(tmp_dir, dir_name)
            content = ymtorrent.generate_torrent(torrent_data_path, 'tracker.com')

            assert ymtorrent.torrent_total_size(content) == 1199
            assert ymtorrent.compute_torrent_hash(content) == '5bda4bd62989fc0b4f1701cddf14ecb3cb5edd36'
