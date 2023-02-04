import pytest
import os
import sys

parent_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.append(parent_dir)

from api.models import PricePredictor
from api.config import RENT, SELL, ROOMS, APARTMENT

SELL_APARTMENT_FACTORS_LIST = [
    {"offer_type": SELL, "category_type": APARTMENT, "subject_federation_id": 1},
    {"offer_type": SELL, "category_type": APARTMENT, "subject_federation_id": 10174},
    {"offer_type": SELL, "category_type": APARTMENT, "subject_federation_id": 11111},
]

SELL_OTHER_FACTORS_LIST = [
    {"offer_type": SELL, "category_type": ROOMS}
]

RENT_FACTORS_LIST = [
    {"offer_type": RENT, "category_type": APARTMENT},
    {"offer_type": RENT, "category_type": ROOMS},
]


@pytest.fixture
def full_price_predictor():
    """Получение полного PricePredictor"""
    price_predictor = PricePredictor()
    return price_predictor


def test_regular_sell_quantiles_computing(full_price_predictor):
    """Проверка интервальной оценки для продукта"""
    model_type = "regular"
    for factors in SELL_APARTMENT_FACTORS_LIST + SELL_OTHER_FACTORS_LIST:
        price_dict = full_price_predictor.get_prediction(factors, "all", model_type)
        assert price_dict["value"] > price_dict["min"]
        assert price_dict["value"] < price_dict["max"]
        assert price_dict["value"] > price_dict["q05"]
        assert price_dict["value"] < price_dict["q95"]
        assert price_dict["value"] > price_dict["q25"]
        assert price_dict["value"] < price_dict["q75"]

        for quantile_type in ["05_95", "25_75"]:
            price_dict = full_price_predictor.get_prediction(factors, quantile_type, model_type)
            assert price_dict["value"] > price_dict["min"]
            assert price_dict["value"] < price_dict["max"]


def test_moderation_sell_quantiles_computing(full_price_predictor):
    """Проверка специальной интервальной оценки для модерации"""
    model_type = "moderation"
    quantile_type = "plain"
    for factors in SELL_APARTMENT_FACTORS_LIST + SELL_OTHER_FACTORS_LIST:
        price_dict = full_price_predictor.get_prediction(factors, quantile_type, model_type)
        assert price_dict["value"] > price_dict["min"]
        assert price_dict["value"] < price_dict["max"]


def test_custom_sell_quantiles_computing(full_price_predictor):
    """Проверка интервальной оценки для кадастрового отчета"""
    model_type = "custom"
    for factors in SELL_APARTMENT_FACTORS_LIST:
        price_dict = full_price_predictor.get_prediction(factors, "all", model_type)
        assert price_dict["value"] > price_dict["min"]
        assert price_dict["value"] < price_dict["max"]
        assert price_dict["value"] > price_dict["q05"]
        assert price_dict["value"] < price_dict["q95"]
        assert price_dict["value"] > price_dict["q25"]
        assert price_dict["value"] < price_dict["q75"]


def test_landing_sell_quantiles_computing(full_price_predictor):
    """Проверка интервальной оценки для лендинга"""
    model_type = "custom"
    for factors in SELL_APARTMENT_FACTORS_LIST:
        price_dict = full_price_predictor.get_prediction(factors, "all", model_type)
        assert price_dict["value"] > price_dict["min"]
        assert price_dict["value"] < price_dict["max"]
        assert price_dict["value"] > price_dict["q05"]
        assert price_dict["value"] < price_dict["q95"]
        assert price_dict["value"] > price_dict["q25"]
        assert price_dict["value"] < price_dict["q75"]
