import pytest

pytestmark = [pytest.mark.asyncio]

url = "/v1/extract-lemmas/"


async def test_returns_best_lemmas_for_word(api):
    got = await api.post(url, json={"input": "Онотоле"}, expected_status=200)

    assert got == [{"input": "Онотоле", "lemmas": ["онотол", "онотоле"]}]


async def test_returns_best_lemmas_for_every_word_in_sentence(api):
    got = await api.post(
        url,
        json={"input": "Онотола, онотолб. Онотолв? Онотолг! онотолд; Онотоле..."},
        expected_status=200,
    )

    assert got == [
        {"input": "Онотола", "lemmas": ["онотол", "онотоле"]},
        {"input": "онотолб", "lemmas": ["онотол", "онотоле"]},
        {"input": "Онотолв", "lemmas": ["онотол", "онотоле"]},
        {"input": "Онотолг", "lemmas": ["онотол", "онотоле"]},
        {"input": "онотолд", "lemmas": ["онотол", "онотоле"]},
        {"input": "Онотоле", "lemmas": ["онотол", "онотоле"]},
    ]


@pytest.mark.parametrize(
    "input_, expected",
    (
        ({"lol": "kek"}, {"input": ["Missing data for required field."]}),
        ({"input": ""}, {"input": ["Shorter than minimum length 1."]}),
        ({"input": None}, {"input": ["Field may not be null."]}),
    ),
)
async def test_raises_for_wrong_input(input_, expected, api):
    got = await api.post(url, json=input_, expected_status=400)

    assert got == expected
