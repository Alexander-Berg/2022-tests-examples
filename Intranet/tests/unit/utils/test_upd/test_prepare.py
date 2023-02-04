from plan.common.utils import upd


def prepare_something(data, prepared):
    return data['number'] + 1


def prepare_something_else(data, prepared):
    return 'WAT'


def test_preparation():
    prepared = upd.preparation_chain(
        data={
            'number': 41
        },
        preparators=[
            prepare_something,
            prepare_something_else,
        ]
    )

    assert prepared == {
        'something': 42,
        'something_else': 'WAT',
    }


def test_preparation_with_initial_data():
    prepared = upd.preparation_chain(
        data={
            'number': 41
        },
        preparators=[
            prepare_something,
            prepare_something_else,
        ],
        preserve_initial_data=True
    )

    assert prepared == {
        'number': 41,
        'something': 42,
        'something_else': 'WAT',
    }
