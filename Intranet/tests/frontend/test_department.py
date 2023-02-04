import pytest

from tests.helpers import get_json


@pytest.mark.parametrize('lang', ['ru', 'en'])
def test_get_department(person_builder, department_root_builder, client, lang):
    person = person_builder(
        language=lang,
    )

    to_find = department_root_builder(
        slug='a',
        name_en='I',
        name_ru='Ж',
    )

    result = get_json(
        client=client,
        path='/frontend/departments/',
        request={
            'slugs': [to_find.slug]
        },
        login=person.login
    )

    assert result[to_find.slug] == getattr(to_find, 'name_' + lang)
