import logging
import pytest

from .basic_test import BasicTest, RealEnvTest
import lib.remote_access_server as server

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User

logger = logging.getLogger("TestSupportedCars")

CAR_MODELS_WITH_FEATURES = [
    ("Mitsubishi", "Outlander", ["trunk"]),
    ("Nissan", "Qashqai", ["trunk"]),
    ("Nissan", "X-Trail", []),
    ("Toyota", "Camry", []),
    ("Toyota", "RAV4", ["trunk"]),
    ("Lada", "X-Ray", []),
    ("Lada", "Vesta", ["trunk"])
]


class TestCreationOfSupportedCarsModels(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

    @pytest.mark.parametrize("car_model", CAR_MODELS_WITH_FEATURES + [tuple()])
    def test_check_car_model_features(self, car_model):
        is_known = len(car_model) > 0
        features = car_model[2] if is_known else []

        car = Car(brand=car_model[0], model=car_model[1]) if is_known else Car()
        car.registrate(self.user, self.telematics)

        settings = server.get_car(self.user.oauth, car) >> 200
        assert settings["features"] == features


class TestListOfSupportedCars(RealEnvTest):
    def test_supported_cars(self):
        user, _, _ = self.get_test_units()

        supported_cars = server.get_supported_cars(user.oauth) >> 200
        for brand in supported_cars:
            for model in brand["models"]:
                entry = (brand["brand"], model["model"], model["features"])
                assert entry in CAR_MODELS_WITH_FEATURES, entry
