import pytest

from tests.helpers import get_json


@pytest.mark.parametrize('lang', ['ru', 'en'])
def test_get_names(person_builder, client, lang):
    person = person_builder(
        language=lang,
    )

    to_find = person_builder(
        first_name_ru='И',
        first_name_en='I',
        last_name_ru='Ж',
        last_name_en='G',
        login='a',
    )

    result = get_json(
        client=client,
        path='/frontend/persons/',
        request={
            'persons': [to_find.login]
        },
        login=person.login,
    )

    assert result[to_find.login] == {
        'name': getattr(to_find, 'first_name_' + lang),
        'surname': getattr(to_find, 'last_name_' + lang),
    }
