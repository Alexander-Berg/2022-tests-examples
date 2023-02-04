import pytest

from intranet.trip.src.lib.translit.ru_icao import ICAORussianLanguagePack


@pytest.mark.parametrize('full_name, expected_result', (
    ('Ящук Валентин Вячеславович', 'Iashchuk Valentin Viacheslavovich'),
    ('Щедролосьев Юрий Евгеньевич', 'Shchedrolosev Iurii Evgenevich'),
    ('Хабибуллин Фёдор Павлович', 'Khabibullin Fedor Pavlovich'),
))
def test_translit(full_name, expected_result):
    language_pack = ICAORussianLanguagePack()
    result = language_pack.translit(full_name, reversed=True)
    assert result == expected_result
