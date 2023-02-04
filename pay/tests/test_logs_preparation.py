import json
import pytest
import yatest
import payplatform.spirit.prepare_yt_logs.lib.process_data as pd


def load_test_data():
    with open(yatest.common.runtime.work_path('data.json')) as datafile:
        return json.load(datafile)


@pytest.mark.parametrize(
    argnames='data',
    argvalues=load_test_data(),
    ids=[case['comment'] for case in load_test_data()]
)
def test_logs_preparation(data):
    actual_data_for_update, actual_data_for_move = pd.process_data(data['test_yt_data'], data['test_current_data'])
    assert actual_data_for_move == data['expected_data_for_move']
    assert actual_data_for_update == data['expected_data_for_update']
