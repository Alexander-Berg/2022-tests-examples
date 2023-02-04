# -*- coding: utf-8 -*-

from balance.balance_steps.export_steps import ExportSteps


PRODUCTS = [
    513480,
    513483,
    513491,
    513492,
    513492,
    513485,
    513488,
    513489,
    513482,
    513484,
    513490,
    513486,
    513486,
]


def test_export_swe_taxi_products():
    for product_id in PRODUCTS:
        ExportSteps.export_oebs(product_id=product_id)
