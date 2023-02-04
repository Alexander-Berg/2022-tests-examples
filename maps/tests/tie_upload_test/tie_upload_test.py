from yatest.common import source_path, build_path
import gzip
import mock
import os
import shutil

import maps.analyzer.services.jams_external.pylib.tie_here_jams.tie_here_jams as test_module


def test_tie_upload():
    TEST_REGION = 'test_region'
    CONVERT_BINARY_PATH = build_path('maps/analyzer/services/jams_external/bin/convert/convert')
    TEST_REGION_MMS = source_path('maps/analyzer/services/jams_external/tests/data/test_region.mms')

    with mock.patch.multiple(
        'maps.analyzer.services.jams_external.pylib.tie_here_jams.tie_here_jams.ecstatic',
        upload=mock.DEFAULT,
        init_logging=mock.DEFAULT,
    ) as m:
        with mock.patch.multiple(
            'maps.analyzer.services.jams_external.pylib.tie_here_jams.tie_here_jams',
            download=mock.DEFAULT,
            now_version=mock.DEFAULT,
            convert=mock.DEFAULT,
            try_enable_yasm_sending=mock.DEFAULT
        ) as t:
            def mock_download(dest_dir, *args, **kwargs):
                with open(source_path('maps/analyzer/services/jams_external/tests/data/jams.xml'), 'rb') as src:
                    dest = os.path.join(dest_dir, TEST_REGION + '.xml.gz')
                    with gzip.open(dest, 'wb') as dst:
                        shutil.copyfileobj(src, dst)

            def mock_convert(convert_binary_path, xml_dir, tied_graph_files, jams_pb_file, *args, **kwargs):
                with open(source_path('maps/analyzer/services/jams_external/tests/data/jams.bin'), 'rb') as src:
                    with open(jams_pb_file, 'wb') as dst:
                        shutil.copyfileobj(src, dst)

            def mock_upload(directory, *args, **kwargs):
                assert os.listdir(directory) == [test_module.JAMS_FILE_NAME]

            t['download'].side_effect = mock_download
            t['convert'].side_effect = mock_convert
            t['now_version'].return_value = 'test-version'
            m['upload'].side_effect = mock_upload

            test_module.run(
                'user', 'password', CONVERT_BINARY_PATH, {TEST_REGION: TEST_REGION_MMS},
                {TEST_REGION: 'here.url.for.downloading.com'}, regions=[TEST_REGION],
                yasmagent_port=11005,
                ban_list=source_path('maps/jams/tools/tie-xml-jams/tests/data/ban.yson')
            )
            t['try_enable_yasm_sending'].assert_called_once_with(11005)
            t['download'].assert_called_once_with(
                user='user',
                password='password',
                regions=[TEST_REGION],
                here_urls={TEST_REGION: 'here.url.for.downloading.com'},
                dest_dir=mock.ANY,
            )
            m['init_logging'].assert_called_once_with(test_module.PACKAGE_NAME, file=None)
            m['upload'].assert_called_once_with(
                directory=mock.ANY,
                dataset_name=test_module.EXTERNAL_JAMS_DATASET_NAME,
                dataset_version='test-version',
                branch='stable',
            )
            t['convert'].assert_called_once_with(
                convert_binary_path=CONVERT_BINARY_PATH,
                xml_dir=mock.ANY,
                tied_graph_files={TEST_REGION: TEST_REGION_MMS},
                jams_pb_file=mock.ANY,
                regions=[TEST_REGION],
                debug=False,
                ban_list=source_path('maps/jams/tools/tie-xml-jams/tests/data/ban.yson')
            )
