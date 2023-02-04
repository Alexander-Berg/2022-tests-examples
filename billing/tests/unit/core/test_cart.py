from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.cart import derive_cart_id, get_derived_cart_id


def test_derive_cart_id(core_context, entity_cart):
    derive_cart_id(core_context, cart=entity_cart, seed='The quick brown fox jumps over the lazy dog')

    assert_that(entity_cart.cart_id, equal_to('d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592'))


def test_get_derived_cart_id():
    assert_that(
        get_derived_cart_id('The quick brown fox jumps over the lazy dog'),
        equal_to('d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592')
    )
