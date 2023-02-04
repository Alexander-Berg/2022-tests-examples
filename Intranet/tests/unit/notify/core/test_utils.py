import pretend
import pytest

from plan.notify.core import utils


@pytest.fixture
def data():
    return pretend.stub(
        person1=pretend.stub(login='person1', __str__=lambda: 'x', __repr__=lambda: 'y'),
        person2=pretend.stub(login='person2'),
        person3=pretend.stub(login='person3', is_dismissed=True),
    )


def test_normalize_recipients_list(data):
    normalized = utils.normalize_recipients_lists(
        recipients=[data.person1, [data.person2, data.person3]]
    )

    assert normalized == [[data.person1], [data.person2, data.person3]]


def test_filter_appropriate_recps(data):
    normalized = utils.filter_appropriate_recipients(
        recipients=[
            [],
            [None],
            [data.person1],
            [data.person2, data.person2, data.person3],
            [data.person3]
        ]
    )

    assert normalized == [[data.person1], [data.person2]]


def test_build_extended_context_one_recipient(data):
    context = {'a': 1, 'b': 2}

    extended = utils.build_extended_context(
        context,
        recipient_group=[data.person1],
    )

    assert extended == {'a': 1, 'b': 2, 'recipient': data.person1}
    assert context == {'a': 1, 'b': 2}


def test_build_extended_context_many_recipient(data):
    context = {'a': 1}

    extended = utils.build_extended_context(
        context,
        recipient_group=[data.person1, data.person2],
    )

    assert extended == {'a': 1, 'recipients': [data.person1, data.person2]}
    assert context == {'a': 1}
