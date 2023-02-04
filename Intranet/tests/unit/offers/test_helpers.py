import pytest

from intranet.femida.src.offers.choices import FORM_TYPES
from intranet.femida.src.offers.helpers import has_no_documents


@pytest.mark.parametrize('citizenship, passport_pages, snils, form_type, result', (
    # Международная анкета - документы нужны всегда
    ('RU', ['doc'], 'snils', FORM_TYPES.international, True),
    ('RU', ['doc'], None, FORM_TYPES.international, True),
    ('RU', [], 'snils', FORM_TYPES.international, True),
    ('RU', [], None, FORM_TYPES.international, True),
    ('KZ', ['doc'], 'snils', FORM_TYPES.international, True),
    ('KZ', ['doc'], None, FORM_TYPES.international, True),
    ('KZ', [], 'snils', FORM_TYPES.international, True),
    ('KZ', [], None, FORM_TYPES.international, True),

    # Российская анкета + гражданин РФ - если отсутствует паспорт либо СНИЛС
    ('RU', ['doc'], 'snils', FORM_TYPES.russian, False),
    ('RU', ['doc'], None, FORM_TYPES.russian, True),
    ('RU', [], 'snils', FORM_TYPES.russian, True),
    ('RU', [], None, FORM_TYPES.russian, True),

    # Российская анкета + иностранный гражданин - если отсутствует паспорт
    ('KZ', ['doc'], 'snils', FORM_TYPES.russian, False),
    ('KZ', ['doc'], None, FORM_TYPES.russian, False),
    ('KZ', [], 'snils', FORM_TYPES.russian, True),
    ('KZ', [], None, FORM_TYPES.russian, True),
))
def test_no_documents_check(citizenship, passport_pages, snils, form_type, result):
    assert has_no_documents(citizenship, passport_pages, snils, form_type) == result
