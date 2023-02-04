import pytest

from django.urls import reverse

from intranet.search.tests.helpers import models_helpers as mh


pytestmark = pytest.mark.django_db(transaction=False)


def _dump_group_attrs(attrs, lang='ru'):
    res = dict()
    for ga in attrs:
        res[str(ga.value)] = {
            'url': ga.url,
            'name': ga.name,
            'label': getattr(ga, 'label_%s' % lang),
        }
    return res


def test_filter_by_revision(api_client):
    """
    Групповые атрибуты выбираются для конкретной ревизии
    """
    user = 'superman'
    revision = mh.Revision(search='services', status='active')

    attr = mh.create_group_attr(revision)
    another_attr = mh.create_group_attr(mh.Revision(), name=attr.name)

    url = reverse('group-attrs-get-data')
    data = {
        'revision': revision.id,
        'user': user,
        'language': 'ru',
        'name': attr.name,
        'values': [attr.value, another_attr.value],
    }
    r = api_client.get(url, data)

    assert r.status_code == 200
    assert r.json() == _dump_group_attrs([attr])


def test_filter_by_values(api_client):
    """
    Групповые атрибуты выбираются для конкретных значений
    """
    user = 'superman'
    revision = mh.Revision(search='services', status='active')

    attr = mh.create_group_attr(revision, value='val1')
    another_value = mh.create_group_attr(revision, name=attr.name, value='val2')  # noqa
    another_type = mh.create_group_attr(revision, name=attr.name + '1', value=attr.value)  # noqa

    url = reverse('group-attrs-get-data')
    data = {
        'revision': revision.id,
        'user': user,
        'language': 'ru',
        'name': attr.name,
        'values': [attr.value],
    }
    r = api_client.get(url, data)

    assert r.status_code == 200
    assert r.json() == _dump_group_attrs([attr])


@pytest.mark.parametrize('lang', ['ru', 'en'])
def test_lang_labels(api_client, lang):
    """
    Метки выбираются в зависимости от языка
    """
    user = 'superman'
    revision = mh.Revision(search='services', status='active')

    attr = mh.create_group_attr(revision, label_ru='ru', label_en='en')

    url = reverse('group-attrs-get-data')
    data = {
        'revision': revision.id,
        'user': user,
        'language': lang,
        'name': attr.name,
        'values': [attr.value],
    }
    r = api_client.get(url, data)
    assert r.json() == _dump_group_attrs([attr], lang=lang)


@pytest.mark.parametrize('label_ru,label_en,lang,expected_label', [
    ('ru', '', 'en', 'ru'),
    ('', 'en', 'ru', 'en'),
])
def test_missing_labels(api_client, label_ru, label_en, lang, expected_label):
    """
    При отсутствии метки на указанном языке выбирается метка на другом языке
    """
    user = 'superman'
    revision = mh.Revision(search='services', status='active')

    attr = mh.create_group_attr(revision, label_ru=label_ru, label_en=label_en)

    url = reverse('group-attrs-get-data')
    data = {
        'revision': revision.id,
        'user': user,
        'language': lang,
        'name': attr.name,
        'values': [attr.value],
    }
    r = api_client.get(url, data)
    assert r.json()[attr.value]['label'] == expected_label
