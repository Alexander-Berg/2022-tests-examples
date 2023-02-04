from tests import helpers

URL = '/frontend/tags-suggest/'


def test_suggest(
    client,
    person_review_builder,
):
    person_review_builder(tag_average_mark='asd')
    person_review_builder(tag_average_mark='asd')
    response = helpers.get_json(
        client,
        path=URL,
        request={'text': 's'},
    )
    assert response[0]['value'] == 'asd' and len(response) == 1
