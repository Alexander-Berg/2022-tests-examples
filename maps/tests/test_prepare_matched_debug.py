import mock
import pytest

import copy

from yt.wrapper import ypath_join, YtOperationFailedError

import maps.analyzer.pylibs.envkit as envkit
import maps.analyzer.toolkit.lib as tk

import maps.analyzer.sandbox.prepare_matched_data.lib.operations as ops

from .test_common import cleanup

import os


os.environ["ALZ_API_YQL_TOKEN"] = "test"
envkit.config.SVN_REVISION = "test"
envkit.config.YT_TITLE_PREFIX = "prepare-matched-data"


def test_prepare_matched_data_debugs(ytc):
    with mock.patch('maps.analyzer.pylibs.graphmatching.lib.match_signals') as m:
        with mock.patch('maps.analyzer.toolkit.lib.utils.function_name') as fn_name:
            with mock.patch('maps.analyzer.sandbox.prepare_matched_data.lib.debug.debug_path') as debug_path:
                with mock.patch('maps.analyzer.sandbox.prepare_matched_data.lib.debug.invokation_args') as invoke_args:
                    def mock_invoke_args(fn, args, kwargs):
                        result = copy.copy(kwargs)
                        result.update({
                            'ytc': args[0],
                            'signals': args[1],
                        })
                        return result

                    invoke_args.side_effect = mock_invoke_args
                    debug_path.side_effect = lambda r, f: ypath_join(r, f, '20190925T000000')
                    fn_name.return_value = 'match'
                    m.side_effect = YtOperationFailedError(0, 0, {'message': 'err'}, [], "URL")
                    config = tk.config.read_json_config('/prepare_matched_data.json')
                    cleanup(ytc)
                    with pytest.raises(YtOperationFailedError):
                        ops.run(ytc, config)

                    assert ytc.exists('//analyzer/debug/match/20190925T000000/signals')
