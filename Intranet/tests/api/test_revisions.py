import pytest

from operator import itemgetter

from django.urls import reverse

from intranet.search.core import models

from intranet.search.tests.helpers import models_helpers as mh
from intranet.search.tests.helpers.api_helpers import dump_model_list


pytestmark = pytest.mark.django_db(transaction=False)


def _dump_revisions(revisions):
    keys = ('id', 'search', 'index', 'service', 'backend')
    return dump_model_list(revisions, keys)


def test_get_revisions_success(api_client):
    user = 'superman'
    org = mh.Organization(label='yandex')

    mh.Revision(search='foo', backend='yserver', status='active', organization=org)
    mh.Revision(search='bar', backend='platform', status='active', organization=org)
    mh.Revision(search='buz', index='foobarbuz', backend='yserver', status='active', organization=org)

    url = reverse('revisions-get')
    data = {
        'user': user,
    }
    r = api_client.get(url, data)

    assert r.status_code == 200
    assert r['Content-Type'] == 'application/json'
    assert len(r.json()) == 3


def test_get_active_only(api_client):
    """
    Выбираются только активные ревизии
    """
    user = 'superman'
    org = mh.Organization(label='yandex')

    revisions = []
    active = None

    for status, _ in models.Revision.STATUSES:
        revision = mh.Revision(status=status, organization=org)
        revisions.append(revision)
        if status == 'active':
            active = revision

    url = reverse('revisions-get')
    data = {
        'user': user,
    }
    response = api_client.get(url, data)
    assert response.json() == _dump_revisions([active])


def test_get_latest_active(api_client):
    """
    Если есть несколько активных ревизий, то выбирается последняя по id
    """

    user = 'superman'
    org = mh.Organization(label='yandex')

    active1 = mh.Revision(status='active', organization=org)   # noqa
    active2 = mh.Revision(status='active', organization=org)
    url = reverse('revisions-get')
    data = {
        'user': user,
    }
    response = api_client.get(url, data)
    assert response.json() == _dump_revisions([active2])


def test_respect_db_features(api_client):
    """
    При выборе ревизии учитываются заданные в базе фичи
    """
    user = 'superman'
    org = mh.Organization(label='yandex')

    active = mh.Revision(status='active', search='wiki', organization=org)
    ready = mh.Revision(status='ready', search='doc', organization=org)
    mh.Feature(user=user, name='revisions', value=ready.pk)

    url = reverse('revisions-get')
    data = {
        'user': user,
    }
    r = api_client.get(url, data)
    assert sorted(r.json(), key=itemgetter('id')) == _dump_revisions([active, ready])


def test_respect_params_features(api_client):
    """
    При выборе ревизии учитываются переданные в параметрах фичи
    """
    user = 'superman'
    org = mh.Organization(label='yandex')

    active = mh.Revision(status='active', search='wiki', organization=org)
    ready = mh.Revision(status='ready', search='doc', organization=org)

    url = reverse('revisions-get')
    data = {
        'user': user,
        'feature.revisions': ready.id,
    }
    r = api_client.get(url, data)
    assert sorted(r.json(), key=itemgetter('id')) == _dump_revisions([active, ready])


def test_feature_revisions_conflict(api_client):
    """
    Если в фичах для поиска указана не последняя активная ревизия,
    то выбирается только она
    """
    user = 'superman'
    org = mh.Organization(label='yandex')

    active = mh.Revision(
        status='active',
        search='somesearch',
        index='someindex',
        backend='platform',
        organization=org,
    )
    ready = mh.Revision(
        status='ready',
        search=active.search,
        index=active.index,
        backend=active.backend,
        organization=org,
    )

    url = reverse('revisions-get')
    data = {
        'user': user,
        'feature.revisions': ready.id,
    }
    r = api_client.get(url, data)
    assert sorted(r.json(), key=itemgetter('id')) == _dump_revisions([ready])
