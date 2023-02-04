from django.conf import settings

from review.core.models import StoredFilter

from tests import helpers


def test_review_filter_empty(client):
    StoredFilter.objects.all().delete()
    result = helpers.get_json(client, '/frontend/stored-filters/')
    assert result["saved_filters"] == []


def test_review_filter_put(client, marks_scale):
    request = {
        "name": "test_filter_name",
        "value": {
            "mark": ["A"],
            "level_change_from": 10,
        }
    }
    result = helpers.post_json(client, '/frontend/stored-filters/', request)
    stored = StoredFilter.objects.all()
    assert stored.count() == 1
    assert stored.first().value == request["value"]


def test_review_filter_get(client, test_person):
    expected_filters = [
        {
            "id": 1,
            "owner_id": test_person.id,
            "name": "super_name",
            "value": {
                "mark": ["A"],
                "level_change_from": 10,
            }
        }
    ]
    StoredFilter.objects.create(**expected_filters[0])
    result = helpers.get_json(client, '/frontend/stored-filters/')
    result_filters = result["saved_filters"]
    assert result_filters[0]["name"] == expected_filters[0]["name"]
    assert result_filters[0]["value"] == expected_filters[0]["value"]


def test_review_filter_delete(client, test_person):
    new_filter = StoredFilter.objects.create(
        name="dummy_name",
        value={"dummy": "json"},
        owner_id=test_person.id
    )
    helpers.delete_multipart(
        client=client,
        path='/frontend/stored-filters/{}/'.format(new_filter.id),
        login=settings.YAUTH_DEV_USER_LOGIN,
    )
    assert StoredFilter.objects.count() == 0


def test_review_filter_edit(client, test_person, marks_scale):
    new_filter = StoredFilter.objects.create(
        name="test_filter_name_22",
        value={
            "mark": ["B"],
            "level_change_from": 7,
        },
        owner_id=test_person.id
    )
    request = {
        "name": "test_filter_name_99",
        "value": {
            "mark": ["D"],
            "level_change_from": 1,
        }
    }
    result = helpers.post_json(client, '/frontend/stored-filters/{}/'.format(new_filter.id), request)
    updated_filter = StoredFilter.objects.first()
    assert updated_filter.name == request["name"] and updated_filter.value == request["value"]


def test_stored_filters_logins_limitation_cia_1131(
    client,
    person_builder_bulk,
):
    persons = person_builder_bulk(_count=101)
    request = {
        "name": "test_filter_with_101_logins",
        "value": {
            "persons": [p.login for p in persons],
        }
    }
    result = helpers.post_json(
        client=client,
        path='/frontend/stored-filters/',
        request=request,
        expect_status=400,
    )
    helpers.assert_is_substructure(
        {
            'errors': {
                'value': {
                    'code': 'VALIDATION_ERROR',
                    'params': {},
                },
            }
        },
        result
    )
