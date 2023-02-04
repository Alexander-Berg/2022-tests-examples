import pytest

from django.urls import reverse

from intranet.search.tests.helpers import models_helpers as mh


pytestmark = pytest.mark.django_db(transaction=False)


def _dump_features(features):
    return {f.name: f.value for f in features}


def test_get_user_required(api_client):
    url = reverse('features-get')
    r = api_client.get(url)
    assert r.status_code == 400
    assert 'user' in r.content.decode('utf-8')


def test_get_features(api_client):
    """
    Возвращаются фичи как для пользователя, так и для группы
    """
    user = 'superman'
    group_id = 1

    user_feature = mh.Feature(user=user)
    another_user_feature = mh.Feature(user=user + "1")  # noqa

    groups = [group_id, group_id + 1]
    group_features = [
        mh.Feature(user=None, group_id=groups[0]),
        mh.Feature(user=None, group_id=groups[1])
    ]
    another_group_feature = mh.Feature(group_id=group_id + 2)  # noqa

    url = reverse('features-get')
    data = {
        'user': user,
        'groups': groups,
    }
    r = api_client.get(url, data)

    assert r.status_code == 200
    assert r.json() == _dump_features([user_feature] + group_features)


def test_user_group_features_conflict(api_client):
    """
    При наличии одинаковых фич у пользователя и группы выбирается пользовательская
    """
    name = 'conflict_feature'
    user = 'superman'
    group_id = 1

    user_feature = mh.Feature(
        name=name,
        user=user,
        group_id=None,
        value='user_feature_value',
    )
    mh.Feature(
        name=name,
        user=None,
        group_id=group_id,
        value='group_feature_value',
    )

    url = reverse('features-get')
    data = {
        'user': user,
        'groups': [group_id],
    }
    r = api_client.get(url, data)

    assert r.status_code == 200
    assert r.json() == _dump_features([user_feature])
