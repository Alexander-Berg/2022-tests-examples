from plan.common.utils import upd


def add_one(data):
    data['number'] += 1
    return data


def add_something(data):
    data['something'] = 'nice'
    return data


def test_transformation_chain():
    handled = upd.process_chain(
        data={'number': 41},
        procesors=[
            add_one,
            add_something
        ]
    )

    assert handled['number'] == 42
    assert handled['something'] == 'nice'
