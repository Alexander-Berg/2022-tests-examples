import pytest

from maps_adv.export.lib.pipeline.xml.transform.phone import phone_transform


@pytest.mark.parametrize(
    ["country", "operator", "number"],
    [
        ["7", "111", "2223344"],
        ["7", "111", "222-33-44"],
        ["375", "11", "2223344"],
        ["375", "11", "222-33-44"],
        ["380", "11", "2223344"],
        ["380", "11", "222-33-44"],
        ["90", "111", "2223344"],
        ["90", "111", "222-33-44"],
    ],
)
@pytest.mark.parametrize(
    "template",
    [
        "+{country}{operator}{number}",
        "+{country}({operator}){number}",
        " +{country} ({operator}) {number} ",
    ],
)
def test_will_transform_phone_number_as_expected(country, operator, number, template):
    phone = template.format(country=country, operator=operator, number=number)
    phone_number = phone_transform(phone)

    assert phone_number == dict(
        telephone=phone,
        formatted=f"+{country} ({operator}) 222-33-44",
        country=country,
        prefix=operator,
        number="2223344",
    )
