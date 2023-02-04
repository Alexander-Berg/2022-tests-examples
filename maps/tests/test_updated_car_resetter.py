import rstr
import uuid
import unittest
import itertools

from unittest.mock import patch, ANY, DEFAULT
from maps.automotive.tools.key_resetter.pylib.resetter import UpdatedCarResetter
from maps.automotive.tools.statistics_auto.pylib.car import Car
from yql.client.parameter_value_builder import YqlParameterValueBuilder as ValueBuilder

import datetime


class Table(object):
    def __init__(self, cars=[]):
        self.rows = [Table.row_from_car(car) for car in cars]
        self.column_names = ['car_id', 'head_id', 'plate']

    @staticmethod
    def row_from_car(car):
        return [
            car.car_id,
            car.head_id,
            car.plate,
        ]


class ANY_ORDER(object):
    def __init__(self, data):
        self.data = sorted(data)

    def __repr__(self):
        return f'<ANY_ORDER>({self.data.__repr__()})'

    def __eq__(self, data):
        if isinstance(data, type(self.data)):
            return self.data == sorted(data)
        return False


class Cars(object):
    def __init__(self, success_cars=[], passport_failed_cars=[], drive_failed_cars=[]):
        self.success_cars = success_cars
        self.passport_failed_cars = passport_failed_cars
        self.drive_failed_cars = drive_failed_cars

    def passport(self):
        passport_success_cars = self.success_cars + self.drive_failed_cars
        return (passport_success_cars, self.passport_failed_cars)

    def drive(self):
        return (self.success_cars, self.drive_failed_cars)

    def all(self):
        return self.passport_failed_cars + \
            self.drive_failed_cars + \
            self.success_cars


@patch.multiple('maps.automotive.tools.key_resetter.pylib.resetter',
                send_mail=DEFAULT,
                yt_combine_chunks=DEFAULT,
                yql_insert_rows=DEFAULT,
                YqlClient=DEFAULT,
                reset_cars_in_drive=DEFAULT,
                reset_cars_in_passport=DEFAULT,
                yql_run_query_from_resource=DEFAULT)
class TestUpdatedCarResetter(unittest.TestCase):
    STORE_TOKEN = 'DUMMY_STORE_TOKEN'
    DRIVE_TOKEN = 'DUMMY_DRIVE_TOKEN'

    QUERY_RESOURCE = 'select_updated_heads_to_reset'
    RESET_HEADS_TABLE = '//home/maps/automotive/broken_head_units/reset_heads'

    FIRMWARE_BUILD_PREFIX = '20201001'

    FROM_ADDR = 'from@yandex.ru'
    TO_ADDR = 'to@yandex.ru'
    SUBJ = 'Test Mail'
    SMTP_SERVER = 'outbound-relay.yandex.net'

    MAILER_CONFIG = {
        'from_address': FROM_ADDR,
        'to_addresses': TO_ADDR,
        'subject': SUBJ,
        'smtp_server': SMTP_SERVER,
    }

    @staticmethod
    def generate_car():
        HEX_DIGITS_LOWER = '0123456789abcdef'
        PLATE_LETTERS = 'авекмнорстух'
        plate = rstr.rstr(PLATE_LETTERS, 1) + rstr.digits(3) + rstr.rstr(PLATE_LETTERS, 2) + '799'
        return Car(
            car_id=uuid.uuid4(),
            head_id=rstr.rstr(HEX_DIGITS_LOWER, 32),
            plate=plate,
        )

    @staticmethod
    def generate_cars(count):
        return [TestUpdatedCarResetter.generate_car() for _ in range(count)]

    def test_cars_are_reset_in_passport(self,
                                        yql_run_query_from_resource,
                                        reset_cars_in_passport,
                                        reset_cars_in_drive,
                                        **kwargs):
        cars = Cars(
            success_cars=TestUpdatedCarResetter.generate_cars(30),
        )

        yql_run_query_from_resource.return_value = [Table(cars=cars.all())]
        reset_cars_in_passport.return_value = cars.passport()
        reset_cars_in_drive.return_value = cars.drive()

        UpdatedCarResetter(
            self.STORE_TOKEN,
            self.DRIVE_TOKEN,
            self.FIRMWARE_BUILD_PREFIX,
            self.MAILER_CONFIG).run()

        reset_cars_in_passport.assert_called_once_with(cars.all(), self.STORE_TOKEN)

    def test_cars_are_reset_in_drive(self,
                                     yql_run_query_from_resource,
                                     reset_cars_in_passport,
                                     reset_cars_in_drive,
                                     **kwargs):
        cars = Cars(
            passport_failed_cars=TestUpdatedCarResetter.generate_cars(10),
            success_cars=TestUpdatedCarResetter.generate_cars(30),
        )

        yql_run_query_from_resource.return_value = [Table(cars=cars.all())]
        reset_cars_in_passport.return_value = cars.passport()
        reset_cars_in_drive.return_value = cars.drive()

        UpdatedCarResetter(
            self.STORE_TOKEN,
            self.DRIVE_TOKEN,
            self.FIRMWARE_BUILD_PREFIX,
            self.MAILER_CONFIG).run()

        pasport_success_cars = cars.passport()[0]
        reset_cars_in_drive.assert_called_once_with(pasport_success_cars, self.DRIVE_TOKEN)

    def test_query_script_called_with_correct_parameters(self, yql_run_query_from_resource, **kwargs):
        yql_run_query_from_resource.return_value = [Table(cars=[])]

        UpdatedCarResetter(
            self.STORE_TOKEN,
            self.DRIVE_TOKEN,
            self.FIRMWARE_BUILD_PREFIX,
            self.MAILER_CONFIG).run()

        reset_reason = 'old_firmware_update_' + self.FIRMWARE_BUILD_PREFIX
        expected_query_params = ValueBuilder.build_json_map({
            "$firmware_build_prefix": ValueBuilder.make_string(self.FIRMWARE_BUILD_PREFIX),
            "$reason": ValueBuilder.make_string(reset_reason),
            "$reset_heads": ValueBuilder.make_string(self.RESET_HEADS_TABLE)
        })
        yql_run_query_from_resource.assert_called_once_with(
            ANY,
            self.QUERY_RESOURCE,
            expected_query_params
        )

    def test_mail_about_reset_cars_is_sent(self,
                                           yql_run_query_from_resource,
                                           reset_cars_in_passport,
                                           reset_cars_in_drive,
                                           send_mail,
                                           **kwargs):
        passport_failed_parameters = [0, 10]
        drive_failed_parameters = [0, 10]
        success_parameters = [0, 10]
        product = itertools.product(passport_failed_parameters,
                                    drive_failed_parameters,
                                    success_parameters)

        for passport_failed_count, drive_failed_count, success_count in product:
            with self.subTest(passport_failed_count=passport_failed_count,
                              drive_failed_count=drive_failed_count,
                              success_count=success_count):
                cars = Cars(
                    passport_failed_cars=TestUpdatedCarResetter.generate_cars(passport_failed_count),
                    drive_failed_cars=TestUpdatedCarResetter.generate_cars(drive_failed_count),
                    success_cars=TestUpdatedCarResetter.generate_cars(success_count),
                )

                yql_run_query_from_resource.return_value = [Table(cars=cars.all())]
                reset_cars_in_passport.return_value = cars.passport()
                reset_cars_in_drive.return_value = cars.drive()
                send_mail.reset_mock()

                UpdatedCarResetter(
                    self.STORE_TOKEN,
                    self.DRIVE_TOKEN,
                    self.FIRMWARE_BUILD_PREFIX,
                    self.MAILER_CONFIG).run()

                there_are_some_cars = (passport_failed_count > 0 or
                                       drive_failed_count > 0 or
                                       success_count > 0)
                if there_are_some_cars:
                    send_mail.assert_called_once_with(
                        self.FROM_ADDR,
                        self.TO_ADDR,
                        self.SUBJ,
                        ANY,
                        self.SMTP_SERVER
                    )
                else:
                    send_mail.assert_not_called()

    def test_reset_cars_are_saved(self,
                                  yql_run_query_from_resource,
                                  reset_cars_in_passport,
                                  reset_cars_in_drive,
                                  yql_insert_rows,
                                  **kwargs):
        passport_failed_parameters = [0, 10]
        drive_failed_parameters = [0, 10]
        success_parameters = [0, 10]
        product = itertools.product(passport_failed_parameters,
                                    drive_failed_parameters,
                                    success_parameters)

        for passport_failed_count, drive_failed_count, success_count in product:
            with self.subTest(passport_failed_count=passport_failed_count,
                              drive_failed_count=drive_failed_count,
                              success_count=success_count):
                cars = Cars(
                    passport_failed_cars=TestUpdatedCarResetter.generate_cars(passport_failed_count),
                    drive_failed_cars=TestUpdatedCarResetter.generate_cars(drive_failed_count),
                    success_cars=TestUpdatedCarResetter.generate_cars(success_count),
                )

                yql_run_query_from_resource.return_value = [Table(cars=cars.all())]
                reset_cars_in_passport.return_value = cars.passport()
                reset_cars_in_drive.return_value = cars.drive()
                yql_insert_rows.reset_mock()

                UpdatedCarResetter(
                    self.STORE_TOKEN,
                    self.DRIVE_TOKEN,
                    self.FIRMWARE_BUILD_PREFIX,
                    self.MAILER_CONFIG).run()

                reset_reason = 'old_firmware_update_' + self.FIRMWARE_BUILD_PREFIX
                date = datetime.date.today().strftime('%Y-%m-%d')
                if success_count > 0:
                    yql_insert_rows.assert_called_once_with(
                        ANY,
                        self.RESET_HEADS_TABLE,
                        ('date', 'head_id', 'reason'),
                        ANY_ORDER([(date, car.head_id, reset_reason) for car in cars.success_cars])
                    )
                else:
                    yql_insert_rows.assert_not_called()

    def test_first_update_reset_reason(self,
                                       yql_run_query_from_resource,
                                       reset_cars_in_passport,
                                       reset_cars_in_drive,
                                       yql_insert_rows,
                                       **kwargs):
        cars = Cars(
            success_cars=TestUpdatedCarResetter.generate_cars(10),
        )

        yql_run_query_from_resource.return_value = [Table(cars=cars.all())]
        reset_cars_in_passport.return_value = cars.passport()
        reset_cars_in_drive.return_value = cars.drive()
        yql_insert_rows.reset_mock()

        UpdatedCarResetter(
            self.STORE_TOKEN,
            self.DRIVE_TOKEN,
            '201910',
            self.MAILER_CONFIG).run()

        reset_reason = 'old_firmware_update'
        date = datetime.date.today().strftime('%Y-%m-%d')
        yql_insert_rows.assert_called_once_with(
            ANY,
            self.RESET_HEADS_TABLE,
            ('date', 'head_id', 'reason'),
            ANY_ORDER([(date, car.head_id, reset_reason) for car in cars.success_cars])
        )

    def test_multiple_cars_reset(self,
                                 yql_run_query_from_resource,
                                 reset_cars_in_passport,
                                 reset_cars_in_drive,
                                 **kwargs):
        cars = Cars(
            success_cars=TestUpdatedCarResetter.generate_cars(1000),
        )

        yql_run_query_from_resource.return_value = [Table(cars=cars.all())]
        reset_cars_in_passport.side_effect = lambda cars, token: (cars, [])
        reset_cars_in_drive.side_effect = lambda cars, token: (cars, [])

        UpdatedCarResetter(
            self.STORE_TOKEN,
            self.DRIVE_TOKEN,
            self.FIRMWARE_BUILD_PREFIX,
            self.MAILER_CONFIG).run()

        passport_reset_cars = []
        for passport_reset_call in reset_cars_in_passport.mock_calls:
            assert len(passport_reset_call.args[0]) < 200
            passport_reset_cars.extend(passport_reset_call.args[0])
        assert len(passport_reset_cars) == len(cars.all())
        assert set(passport_reset_cars) == set(cars.all())

        drive_reset_cars = []
        for drive_reset_call in reset_cars_in_drive.mock_calls:
            assert len(passport_reset_call.args[0]) < 200
            drive_reset_cars.extend(drive_reset_call.args[0])
        assert len(drive_reset_cars) == len(cars.all())
        assert set(drive_reset_cars) == set(cars.all())
