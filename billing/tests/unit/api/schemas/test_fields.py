import pytest
from marshmallow.validate import Length, Regexp, ValidationError

from hamcrest import assert_that, contains, equal_to, has_entries, has_properties, instance_of

from billing.yandex_pay_plus.yandex_pay_plus.api.schemas.fields import CurrencyField


class TestCurrencyField:
    def test_init(self):
        field = CurrencyField()

        assert_that(
            field,
            has_properties(
                metadata=has_entries(
                    description=equal_to('ISO 4217 alpha code. E.g. RUB, BYN, USD'),
                ),
                validators=contains(instance_of(Regexp)),
            )
        )

    def test_multiple_validators(self):
        field = CurrencyField(validate=Length(min=0))

        assert_that(
            field,
            has_properties(
                validators=contains(
                    instance_of(Length),
                    instance_of(Regexp),
                )
            )
        )

    @pytest.mark.parametrize('currency', ['RUB', 'BYN', 'USD', 'XTS', 'FOO', 'BAR', None])
    def test_validation_passes(self, currency):
        field = CurrencyField(allow_none=True)

        deserialized = field.deserialize(currency)
        assert_that(deserialized, equal_to(currency))

    @pytest.mark.parametrize('currency', ['RU', 'rub', 'Rub', 'ABCD', 'U$D', '123', '', None])
    def test_validation_fails(self, currency):
        field = CurrencyField()

        with pytest.raises(ValidationError):
            field.deserialize(currency)
