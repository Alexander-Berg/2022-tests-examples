from yatest.common import source_path, build_path
import gzip
import mock
import os
import re
import shutil

import maps.jams.tools.tie_xml_jams.pylib.tie_xml_jams as test_module


def test_tie_upload(ytc):
    test_module.CONVERT_BINARY_PATH = build_path('maps/jams/tools/tie-xml-jams/bin/convert/convert')
    with mock.patch.multiple(
        'maps.jams.tools.tie_xml_jams.pylib.tie_xml_jams.ecstatic',
        upload=mock.DEFAULT,
        init_logging=mock.DEFAULT,
    ) as m:
        with mock.patch.multiple(
            'maps.jams.tools.tie_xml_jams.pylib.tie_xml_jams',
            download=mock.DEFAULT,
            now_version=mock.DEFAULT,
            binary_log_sink=mock.DEFAULT,
        ) as t:
            def mock_download(server, user, password, file, dest):
                with open(source_path('maps/jams/tools/tie-xml-jams/tests/data/jams.xml'), 'rb') as src:
                    with gzip.open(dest, 'wb') as dst:
                        shutil.copyfileobj(src, dst)

            def mock_upload(directory, *args, **kwargs):
                assert os.listdir(directory) == [test_module.JAMS_FILE_NAME]

            def mock_binary_log_sink(binary_name, binary_stderr):
                banned_polylines_count = 0
                for l in binary_stderr.read().splitlines():
                    matched_obj = re.search(rb"Banned edges: (\d+)", l)
                    if matched_obj is not None:
                        banned_polylines_count += int(matched_obj.group(1))
                    test_module.logger.getChild(binary_name).info(l)
                assert banned_polylines_count != 0

            t['download'].side_effect = mock_download
            t['now_version'].return_value = 'test-version'
            t['binary_log_sink'].side_effect = mock_binary_log_sink
            m['upload'].side_effect = mock_upload
            test_module.run(
                'ftp.server.com', 'user', 'password', 'jams.xml.gz',
                regions=[213], max_category=5,
                ban_list=source_path('maps/jams/tools/tie-xml-jams/tests/data/ban.yson'),
                in_dataset_dir='dataset',
                yt_context=ytc,
                downloaded_graph_folder="graph_folder"
            )

            t['download'].assert_called_once_with(
                server='ftp.server.com',
                user='user',
                password='password',
                file='jams.xml.gz',
                dest=mock.ANY,
            )
            m['init_logging'].assert_called_once_with(test_module.PACKAGE_NAME, file=None)
            m['upload'].assert_called_once_with(
                directory=mock.ANY,
                dataset_name=test_module.EXTERNAL_JAMS_DATASET_NAME,
                dataset_version='test-version',
                branch='stable',
            )
