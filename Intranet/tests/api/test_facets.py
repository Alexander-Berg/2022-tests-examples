import pytest

from collections import defaultdict

from django.urls import reverse

from intranet.search.tests.helpers import models_helpers as mh


pytestmark = pytest.mark.django_db(transaction=False)


def _dump_facets(facets, lang='ru'):
    data = defaultdict(dict)
    for f in facets:
        data[f.facet][str(f.value)] = {
            'id': str(f.id),
            'value': str(f.value),
            'label': getattr(f, 'label_%s' % lang),
        }
    return dict(data)


@pytest.mark.parametrize('http_method', ['get', 'post'])
def test_filter_by_revision(api_client, http_method):
    """
    Фасеты выбираются для конкретной ревизии
    """
    user = 'superman'
    revision = mh.Revision(search='services', status='active')

    facet = mh.create_facet(revision)
    another_facet = mh.create_facet(
        revision=mh.Revision(search=revision.search + '1'),
        facet=facet.facet,
    )

    url = reverse('facets-get-labels')
    data = {
        'revision': revision.id,
        'user': user,
        'language': 'ru',
        'name': facet.facet,
        'values': [facet.value, another_facet.value],

    }
    r = getattr(api_client, http_method)(url, data)

    assert r.status_code == 200
    assert r.json() == _dump_facets([facet])


@pytest.mark.parametrize('http_method', ['get', 'post'])
def test_filter_by_values(api_client, http_method):
    """
    Фасеты выбираются для конкретных значений
    """
    user = 'superman'
    revision = mh.Revision(search='services', status='active')

    facet = mh.create_facet(revision, value=1)
    another_value = mh.create_facet(revision, facet=facet.facet, value=2)  # noqa
    another_type = mh.create_facet(revision, facet=facet.facet + '1', value=facet.value)  # noqa

    url = reverse('facets-get-labels')
    data = {
        'revision': revision.id,
        'user': user,
        'language': 'ru',
        'name': facet.facet,
        'values': [facet.value],
    }
    r = getattr(api_client, http_method)(url, data)

    assert r.status_code == 200
    assert r.json() == _dump_facets([facet])


@pytest.mark.parametrize('http_method', ['get', 'post'])
@pytest.mark.parametrize('lang', ['ru', 'en'])
def test_lang_labels(api_client, http_method, lang):
    """
    Метки выбираются в зависимости от языка
    """
    user = 'superman'
    revision = mh.Revision(search='services', status='active')

    facet = mh.create_facet(revision, label_ru='ru', label_en='en')

    url = reverse('facets-get-labels')
    data = {
        'revision': revision.id,
        'user': user,
        'language': lang,
        'name': facet.facet,
        'values': [facet.value],
    }
    r = getattr(api_client, http_method)(url, data)
    assert r.json() == _dump_facets([facet], lang=lang)


@pytest.mark.parametrize('http_method', ['get', 'post'])
@pytest.mark.parametrize('label_ru,label_en,lang,expected_label', [
    ('ru', '', 'en', 'ru'),
    ('', 'en', 'ru', 'en'),
])
def test_missing_labels(api_client, http_method, label_ru, label_en, lang, expected_label):
    """
    При отсутствии метки на указанном языке выбирается метка на другом языке
    """
    user = 'superman'
    revision = mh.Revision(search='services', status='active')

    facet = mh.create_facet(revision, label_ru=label_ru, label_en=label_en)

    url = reverse('facets-get-labels')
    data = {
        'revision': revision.id,
        'user': user,
        'language': lang,
        'name': facet.facet,
        'values': [facet.value],
    }
    r = getattr(api_client, http_method)(url, data)
    assert r.json()[facet.facet][facet.value]['label'] == expected_label
