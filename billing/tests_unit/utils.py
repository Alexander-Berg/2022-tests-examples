# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

from hamcrest import starts_with

from decimal import Decimal as D, ROUND_HALF_UP
from yb_snout_api.utils.formats import format_fixed_field

standard_library.install_aliases()


def match_decimal_value(value, decimals, rounding=ROUND_HALF_UP):
    """
    Until models aren't ready, use this match for decimal strings.
    """
    rounded_value = format_fixed_field(
        value=D(value),
        decimals=decimals,
        rounding=rounding,
    )
    text_rounded_value = rounded_value[:-1]
    return starts_with(text_rounded_value)
