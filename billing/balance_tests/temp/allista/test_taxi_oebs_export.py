# -*- coding: utf-8 -*-
from datetime import datetime

import pytest

import btestlib.data.partner_contexts as pc
from balance.balance_objects import Context
from temp.allista.oebs_export import export_context_to_oebs


@pytest.mark.parametrize(
    "context,invoices_for,completions,spendable_contexts",
    [
        # (pc.TAXI_RU_CONTEXT, pc.SCOUTS_RU_CONTEXT),
        # (
        #     pc.TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT,
        #     ["YANDEX_SERVICE"],
        #     [],
        #     [
        #         pc.TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT_SPENDABLE,
        #         pc.TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
        # (
        #     pc.TAXI_ZAM_USD_CONTEXT,
        #     ["YANDEX_SERVICE"],
        #     [],
        #     [
        #         pc.TAXI_ZAM_USD_CONTEXT_SPENDABLE,
        #         pc.TAXI_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
        # (
        #     pc.TAXI_MLU_EUROPE_SWE_SEK_CONTEXT,
        #     ["YANDEX_SERVICE", "AGENT_REWARD"],
        #     [513480],
        #     [
        #         pc.TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_DONATE,
        #         pc.TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
        # (
        #     pc.TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT,
        #     ["YANDEX_SERVICE"],
        #     [],
        #     [
        #         pc.TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE,
        #         pc.TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
        # (
        #     pc.TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT,
        #     ["YANDEX_SERVICE"],
        #     [],
        #     [
        #         pc.TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT_SPENDABLE,
        #         pc.TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
        # (
        #     pc.TAXI_GHANA_USD_CONTEXT,
        #     ["YANDEX_SERVICE"],
        #     [],
        #     [
        #         pc.TAXI_GHANA_USD_CONTEXT_SPENDABLE,
        #         pc.TAXI_GHANA_USD_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
        # (
        #     pc.TAXI_BV_CIV_EUR_CONTEXT,
        #     ["YANDEX_SERVICE"],
        #     [],
        #     [
        #         pc.TAXI_BV_CIV_EUR_CONTEXT_SPENDABLE,
        #         pc.TAXI_BV_CIV_EUR_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
        # (
        #     pc.TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT,
        #     ["YANDEX_SERVICE"],
        #     [],
        #     [
        #         pc.TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE,
        #         pc.TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
        (
            pc.TAXI_UBER_BV_BYN_AZN_USD_CONTEXT,
            ["YANDEX_SERVICE"],
            [],
            [
                pc.TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE,
                pc.TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE_SCOUTS,
            ],
        ),
        # (
        #     pc.TAXI_BV_GEO_USD_CONTEXT,
        #     ["YANDEX_SERVICE"],
        #     [],
        #     [
        #         pc.TAXI_BV_GEO_USD_CONTEXT_SPENDABLE,
        #         pc.TAXI_BV_GEO_USD_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
        # (
        #     pc.TAXI_BV_UZB_USD_CONTEXT,
        #     ["YANDEX_SERVICE"],
        #     [],
        #     [
        #         pc.TAXI_BV_UZB_USD_CONTEXT_SPENDABLE,
        #         pc.TAXI_BV_UZB_USD_CONTEXT_SPENDABLE_SCOUTS,
        #     ],
        # ),
    ],
    ids=lambda v: str(v) if isinstance(v, Context) else None,
)
def test_2_months_oebs_export(context, invoices_for, completions, spendable_contexts):
    export_context_to_oebs(
        context,
        datetime(2022, 2, 1),
        datetime(2022, 2, 28),
        completions,
        invoices_for,
        spendable_contexts,
    )
