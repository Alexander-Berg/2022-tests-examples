from plan.common.utils import upd


def validate_truth(data):
    """
    TRUTH_ERROR
    Data is not true
    """
    if not data:
        return {'bad_data': data}


def validate_lie(data):
    """
    TRUTH_ERROR
    Data is not false
    """
    if data:
        return {'bad_data': data}


def test_validation_chain_no_accumulate_first_fail():
    error = upd.validation_chain(
        data=0,
        validators=[
            validate_truth,
            validate_lie,
        ]
    )

    assert error
    err_key, err_val = error
    assert err_key == 'truth'
    assert err_val.code == 'TRUTH_ERROR'
    assert err_val.message == 'Data is not true'
    assert err_val.params == {'bad_data': 0}


def test_validation_chain_no_accumulate_second_fail():
    error = upd.validation_chain(
        data=1,
        validators=[
            validate_truth,
            validate_lie,
        ]
    )

    assert error
    err_key, err_val = error
    assert err_key == 'lie'
