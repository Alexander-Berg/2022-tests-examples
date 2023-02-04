import pytest

from maps_adv.geosmb.logoped.server.lib.domain import extract_lemmas

pytestmark = [pytest.mark.asyncio]


async def test_returns_best_lemmas_for_word():
    got = extract_lemmas("Онотоле")

    assert got == (dict(input="Онотоле", lemmas=("онотол", "онотоле")),)


async def test_returns_lemmas_for_every_word_in_sentence():
    got = extract_lemmas("Онотола, онотолб. Онотолв? Онотолг! онотолд; Онотоле...")

    assert got == (
        dict(input="Онотола", lemmas=("онотол", "онотоле")),
        dict(input="онотолб", lemmas=("онотол", "онотоле")),
        dict(input="Онотолв", lemmas=("онотол", "онотоле")),
        dict(input="Онотолг", lemmas=("онотол", "онотоле")),
        dict(input="онотолд", lemmas=("онотол", "онотоле")),
        dict(input="Онотоле", lemmas=("онотол", "онотоле")),
    )


async def test_hyphenated_words_are_parsed_well():
    got = extract_lemmas("Онотоле-онотоле")

    assert got == (dict(input="Онотоле-онотоле", lemmas=("онотол", "онотоле")),)


async def test_uses_required_args(onotole_mock):
    extract_lemmas("Онотоле-онотоле")

    onotole_mock.assert_called_with("Онотоле-онотоле", langs=("ru",), split=False)
