from pprint import pprint
from typing import List, Dict, Optional

import pytest
from django.conf import settings
from django.urls import URLPattern, URLResolver
from ninja import Schema
from pydantic import Field
from pydantic import ValidationError
from wiki.api_v2.schemas import AllOptional, Slug


class TestSchema(Schema):
    id: int
    optional_b: Optional[str]


class TestSchemaOptional(TestSchema, metaclass=AllOptional):
    id: int
    optional_b: Optional[str]


URLCONF = __import__(settings.ROOT_URLCONF, {}, {}, [''])

availble_methods = ['get', 'post', 'put', 'delete']


def list_urls(patterns, path=None):
    """recursive"""
    if not path:
        path = []
    result = []
    for pattern in patterns:
        if isinstance(pattern, URLPattern):
            cls_name = 'func'
            methods = ['get']
            try:
                cls = pattern.callback.cls
            except AttributeError:
                cls_name = str(pattern.callback)
                pass
            else:
                cls_name = cls.__name__
                methods.clear()
                for method in availble_methods:
                    try:
                        getattr(cls, method)
                        methods.append(method)
                    except AttributeError:
                        pass

            result.append([''.join(path) + str(pattern.pattern), cls_name, methods])
        elif isinstance(pattern, URLResolver):
            result += list_urls(pattern.url_patterns, path + [str(pattern.pattern)])
    return result


#
# def list_all_routes():
#     urlconf = __import__(settings.ROOT_URLCONF, {}, {}, [''])
#     str = []
#     for path, method, kw in list_urls(urlconf.urlpatterns):
#         path = path.replace('(?P<tag>((\w|\-)[\w\-\.:\+]*/)*(\w|\-)[\w\-\.:\+]*/?)?', '<SLUG>/')
#         path = path.replace('(?P<tag>((\w|\-)[\w\-\.:\+]*/)*(\w|\-)[\w\-\.:\+]*/?)', '<SLUG>')
#         path = path.replace('\.', '.')
#         path = path.replace('^', '')
#         path = path.replace('$', '')
#         if path.startswith('_admin/'):
#             continue
#         if path.startswith('api/v2'):
#             continue
#         kw = set(kw)
#         kw_d = ['-' if i in kw else '.' for i in availble_methods]
#         k = '; '.join(kw_d)
#         # str.append(f'{path:<80} {k:<20} {method}')
#         str.append(f'{path}; {k}; {method}')
#     str = sorted(str)
#     print('\n'.join(str))
#
#     pass


@pytest.mark.django_db
def test_optional_schema():
    with pytest.raises(ValidationError):
        TestSchema(optional_b='123')

    TestSchemaOptional(optional_b='123')


class SlugSchema(Schema):
    desired_slug: Slug


@pytest.mark.django_db
def test_slug_field():
    q = SlugSchema(desired_slug='/foo/bar/лолкек///...')
    assert q.desired_slug == 'foo/bar/lolkek'

    with pytest.raises(ValidationError):
        SlugSchema(desired_slug='/')


@pytest.mark.django_db
def test_me(client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)
    response = client.get('/api/v2/public/me')
    assert response.status_code == 200


@pytest.mark.django_db
def test_validation(client, wiki_users):
    client.login(wiki_users.thasonic)
    response = client.post('/api/v2/public/diag/validate_me?query_param_a=-1&query_param_b=aaaaaaaa')
    assert response.status_code == 400
    pprint(response.json())


@pytest.mark.django_db
def test_di(client, wiki_users):
    client.login(wiki_users.thasonic)

    response = client.post(
        '/api/v2/public/diag/validate_me?query_param_a=1&query_param_b=a',
        data={'required_list': [1, 2, 3], 'required_str': 'foobar'},
    )

    assert response.status_code == 200


@pytest.mark.django_db
def test_autodocs(client, wiki_users):
    # проверим что схема собралась  без ошибок
    response = client.get(
        '/api/v2/openapi.json',
    )

    assert response.status_code == 200

    # проверим что подтянулась тема
    response = client.get(
        '/api/v2/docs',
    )

    assert response.status_code == 200
    assert 'redoc' in response.content.decode()


def page_exists_in_list(arr: List[Dict], page, args):
    """
    arr: [{'supertag': 'hello', 'id': 10}, {...}]
    """
    for el in arr:
        match = True
        for a in args:
            assert hasattr(page, a)
            if getattr(page, a) != el[a]:
                match = False
                break
        if match:
            return True
    return False


class DemoModel:
    def __init__(self):
        self.regular_field = 1
        self.rename_this_field = 123

    # в отличие от DRF, его не вызвать при сериализации...
    def func_field(self):
        return 123

    # .. но prop можно
    @property
    def prop_field(self):
        return 123


class DemoModelSerializer(Schema):
    regular_field: int
    this_field: int = Field(alias='rename_this_field')
    prop_field: int
    alt_prop_field: int = Field(alias='prop_field')


def test_from_orm():
    DemoModelSerializer.from_orm(DemoModel())
