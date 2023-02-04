import rstr
import random

import lib.remote_access_server as server
import lib.fakeenv as fakeenv
from data_types.car_settings import CarSettings


class Car:
    def __init__(self, car_id=None, brand=None, model=None, year=None, plate=None, features=None):
        self.brand = brand or rstr.letters(6)
        self.model = model or rstr.letters(6)
        self.year = year or (1989 + int(random.random() * 30))
        self.plate = plate or rstr.xeger("[A-Z]\d{3}[A-Z]{2}\d{3}")
        self.name = self.brand + " " + self.model
        self.id = car_id
        self.details = Car.get_default_details(self)

        if features:
            self.details["features"] = features

    @staticmethod
    def get_default_details(car):
        return {
            'alarmStatus': 'unset',
            'alerts': [],
            'brand': car.brand,
            'cabinTemperature': 15,
            'doors': {
                'frontLeft': 'locked',
                'frontRight': 'locked',
                'rearLeft': 'locked',
                'rearRight': 'locked'},
            'engine': {
                'status': 'off',
                'temperature': 13},
            'features': [],
            'fuelLevel': {
                'litres': 11,
                'percents': 22},
            'hood': 'locked',
            'id': car.id if car.id else "",
            'isInServiceMode': False,
            'isMoving': False,
            'isOnline': True,
            'location': {'lat': 55.73312, 'lon': 37.589004},
            'lockStatus': 'locked',
            'model': car.model,
            'name': car.name,
            'plateNumber': car.plate,
            'sim': {
                'balance': {
                    'amount': 464.52,
                    'currency': 'RUB'},
                'number': '+79876543210'},
            'trunk': 'locked',
            'voltage': 11.4,
            'year': car.year}

    @staticmethod
    def from_json(json):
        car = Car()
        car.id = json["id"]
        car.name = json["name"]
        car.brand = json["brand"]
        car.model = json["model"]
        car.year = json["year"]
        car.plate = json["plateNumber"]
        car.details = Car.get_default_details(car)
        return car

    def registrate(self, user, telematics):
        status, response = server.add_car(
            token=user.oauth, hwid=telematics.hwid, car=self)
        assert status == 201, response

        self.id = response["id"]
        self.details["id"] = self.id
        assert self.id

    def share_access(self, owner, another_user):
        server.share_car(
            owner.oauth, self,
            another_user.name, another_user.phone) >> 200

        server.confirm_share_phone(
            owner.oauth, self,
            fakeenv.get_sms_code(another_user.phone)) >> 200

        server.complete_share(
            owner.oauth, self, another_user.phone) >> 200

        server.confirm_share(
            owner.oauth, self,
            fakeenv.get_sms_code(owner.phone)) >> 200

        sms_list = fakeenv.read_sms(another_user.phone)
        assert len(sms_list) == 1

        server.get_cars(another_user.oauth) >> 200

    def add_notifications_phone(self, owner, another_user):
        server.add_notifications_phone(
            token=owner.oauth, car=self,
            phone=another_user.phone, name=another_user.name) >> 200

        code = fakeenv.get_sms_code(another_user.phone)
        response = server.confirm_notifications_phone(
            token=owner.oauth, car=self, code=code) >> 200

        return response[0]["id"]

    def get_settings(self, user):
        car_settings = server.get_car_settings(user.oauth, self) >> 200
        return CarSettings(user, json=car_settings)

    def set_notifications_settings(self, user, notifications_settings):
        server.patch_notification_settings(
            user.oauth, self, notifications_settings) >> 200
