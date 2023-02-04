# coding=utf-8
import os
import sys
import json
import django
import random
import unittest
import pandas as pd
from jsonschema import Draft4Validator
from catalogue.settings import BASE_DIR
from common.logger import Logger

os.environ['DJANGO_SETTINGS_MODULE'] = 'settings'
sys.path.append(BASE_DIR + '/catalogue')
django.setup()

from utilities.utils_controller import UtilsController
from catalogs.controllers.catalogs_controller import CatalogsController


class CatalogsTest(unittest.TestCase):
    logger = Logger.get_logger(__name__)
    with open('data/vins.json') as vins_reader:
        vins = json.load(vins_reader)
    uc = UtilsController()
    catalogs = CatalogsController()

    response_schema = {
        "name": "response",
        "schema": {
            "type": "object",
            "properties": {
                "data": {
                    "type": "object",
                    "properties": {
                        "cars": {"type": "array"},
                        "groups": {"type": ["array", "null"]},
                        "subgroups": {"type": "array"},
                        "variants": {"type": "array"},
                        "items": {"type": "array"},
                        "image": {"type": ["string", "null"]}
                    },
                    "anyOf": [
                        {"required": ["cars"]},
                        {"required": ["groups"]},
                        {"required": ["subgroups"]},
                        {"required": ["variants"]},
                        {"required": ["items", "image"]}
                    ]
                },
                "meta": {
                    "type": "object",
                    "properties": {
                        "source": {"type": "string"},
                        "mark": {"type": ["string", "null"]},
                        "parts_brand": {"type": ["number", "null"]},
                        "current_url": {"type": ["string", "null"]},
                        "url_format": {"type": ["object", "array", "null"]}
                    },
                    "required": ["source", "current_url", "url_format"]
                }
            }
        },
    }

    car_result_schema = {
        "name": "car_result",
        "schema": {
            "type": "object",
            "properties": {
                "cars": {"type": ["array"]},
                "groups": {"type": ["array", "null"]}
            },
            "required": ["cars", "cars"]
        }
    }

    group_schema = {
        "name": "group",
        "schema": {
            "type": "object",
            "properties": {
                "image": {"type": ["string", "null"]},
                "name": {"type": "string"},
                "next_page_url": {"type": "string"}
            },
            "required": ["image", "name", "next_page_url"]
        }
    }
    parts_group_schema = {
        "name": "parts_group",
        "schema": {
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "description": {"type": "string"},
                "parts": {"type": "array"},
            },
            "required": ["name", "description", "parts"]
        }

    }
    part_schema = {
        "name": "part",
        "schema": {
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "description": {"type": ["string", "null"]},
                "number": {"type": "string"},
                "id": {"type": "string"},
                "position": {"type": ["number", "null"]},
                "category_id": {"type": ["number", "null"]},
                "autoru_link": {"type": "string"},
                "image": {"type": "string"}
            },
            "required": ["name", "description", "number", "id", "position", "category_id", "autoru_link"]
        }
    }

    def assertValidateJSON(self, instance, schema):
        v = Draft4Validator(schema["schema"])
        errors = sorted(v.iter_errors(instance), key=str)
        if errors:
            for error in errors:
                self.logger.error(error)
            raise AssertionError('Instance {} is not valid JSON of type {}'.format(
                json.dumps(instance, ensure_ascii=False), schema["name"]
            ))

    def vin_request(self, vin, check_groups=False, check_sub=False, check_items=False, check_limit=1, find_by_id=False):
        if find_by_id:
            vin_result = self.catalogs.find_car_by_id(vin["mark"], vin["carId"], vin["source"])
        else:
            vin_result = self.catalogs.find_car_by_vin_or_frame(vin)
        self.assertValidateJSON(vin_result, self.response_schema)
        vin_result_data = vin_result["data"]
        self.assertValidateJSON(vin_result_data, self.car_result_schema)
        vin_result_meta = vin_result["meta"]
        source_type = vin_result["meta"]["source"]
        result = vin_result

        if "categories" in vin_result_data and check_groups:
            self.logger.info('Checking groups, limit {}'.format(check_limit))
            groups = vin_result_data["groups"]
            for group in groups:
                self.assertValidateJSON(group, self.group_schema)

            if check_sub:
                self.logger.info('Checking subgroups, limit {}'.format(check_limit))
                last_index = check_limit if check_limit < len(groups) else len(groups) - 1
                for group in groups[:last_index]:
                    sub_groups_result = self.catalogs.get_subgroups(
                        group["next_page_url"], source_type,
                        vin_result_meta["url_format"], vin_result_meta["mark"]
                    )
                    sub_groups_result_data = sub_groups_result["data"]
                    sub_groups_result_meta = sub_groups_result["meta"]
                    self.assertValidateJSON(sub_groups_result_data, self.response_schema)
                    result = sub_groups_result
                    sub_groups = sub_groups_result_data["subgroups"]
                    for sub_group in sub_groups:
                        self.assertValidateJSON(sub_group, self.group_schema)

                    if check_items:
                        self.logger.info('Checking items, limit {}'.format(check_limit))
                        last_index = check_limit if check_limit < len(sub_groups) else len(sub_groups) - 1
                        for sub_group in sub_groups[:last_index]:
                            variants_or_items = self.catalogs.check_variants(
                                {"name": sub_group["name"], "next_page_url": sub_group["next_page_url"]},
                                sub_groups_result_meta["current_url"], source_type,
                                sub_groups_result_meta["url_format"], vin_result_meta["mark"],
                                vin_result_meta["parts_brand"]
                            )
                            variants_or_items_data = variants_or_items["data"]

                            if "items" in variants_or_items_data:
                                self.assertValidateJSON(variants_or_items, self.response_schema)
                                result = variants_or_items
                                items = variants_or_items_data["items"]
                                for parts_group in items:
                                    self.assertValidateJSON(parts_group, self.parts_group_schema)
                                    parts = parts_group["parts"]
                                    for part in parts:
                                        self.assertValidateJSON(part, self.part_schema)

                            elif "variants" in variants_or_items_data:
                                self.assertValidateJSON(variants_or_items, self.response_schema)
                                result = variants_or_items
                                variants = variants_or_items_data["variants"]
                                for variant in variants:
                                    self.assertValidateJSON(variant, self.group_schema)
                                last_index = check_limit if check_limit < len(variants) else len(variants) - 1
                                for variant in variants[:last_index]:
                                    items = self.catalogs.get_items(
                                        variant["next_page_url"], source_type,
                                        vin_result_meta["mark"], vin_result_meta["parts_brand"]
                                    )
                                    self.assertValidateJSON(items, self.response_schema)
                                    for parts_group in items["items"]:
                                        self.assertValidateJSON(parts_group, self.parts_group_schema)
                                        for part in parts_group["parts"]:
                                            self.assertValidateJSON(part, self.part_schema)

                            else:
                                raise AssertionError("Unknown response on variants|items request")

        return result, source_type

    def test_random_vins(self):
        if not os.path.exists('data/vins_random.json'):
            vins = random.sample([{"vin": val["vin"], "mark": key, "model": val["model"]}
                                  for key in self.vins.keys() for val in self.vins[key]], 100)
            json.dump(vins, open('data/vins_random.json', 'w'), ensure_ascii=False, indent=4)
        else:
            with open('data/vins_random.json') as vins_reader:
                vins = json.load(vins_reader)

        counter = {
            "pcc": 0,
            "acat": 0,
            "total_checked": 0,
            "total_found": 0
        }
        marks_counter = {}

        for vin in vins:
            self.logger.info('Requesting vin {vin} with mark {mark} with model {model}'.format(**vin))
            vin_result, source_type = self.vin_request(vin["vin"])
            counter["total_checked"] += 1
            if vin["mark"] not in marks_counter:
                marks_counter[vin["mark"]] = {
                    "checked": 0,
                    "found": 0,
                    "pcc": 0,
                    "acat": 0
                }
            marks_counter[vin["mark"]]["checked"] += 1
            if source_type:
                counter["total_found"] += 1
                marks_counter[vin["mark"]]["found"] += 1
                counter[source_type] += 1
                marks_counter[vin["mark"]][source_type] += 1
        self.logger.info('Checked {total_checked} vins, found {total_found} vins, {acat} acat, {pcc} pcc'.
                         format(**counter))
        df = pd.DataFrame(marks_counter)
        df.to_excel('result_data/CheckedVins.xlsx')

    def test_popular_marks(self):
        marks = ['AUDI', 'DAEWOO', 'HYUNDAI', 'MITSUBISHI', 'RENAULT', 'BMW', 'FIAT', 'KIA', 'NISSAN', 'TOYOTA',
                 'CHEVROLET', 'FORD', 'MAZDA', 'OPEL', 'VOLKSWAGEN', 'CITROEN', 'HONDA', 'MERCEDES', 'PEUGEOT']

        if not os.path.exists('data/marks_popular.json'):
            vins = random.sample([{"vin": val["vin"], "mark": key, "model": val["model"]}
                                  for key in self.vins.keys() for val in self.vins[key] if key in marks], 100)
            json.dump(vins, open('data/marks_popular.json', 'w'), ensure_ascii=False, indent=4)
        else:
            with open('data/marks_popular.json') as vins_reader:
                vins = json.load(vins_reader)

        counter = {
            "pcc": 0,
            "acat": 0,
            "total_checked": 0,
            "total_found": 0
        }
        marks_counter = {}

        for vin in vins:
            self.logger.info('Requesting vin {vin} with mark {mark} with model {model}'.format(**vin))
            vin_result, source_type = self.vin_request(vin["vin"], True, True, check_limit=5)
            counter["total_checked"] += 1
            if vin["mark"] not in marks_counter:
                marks_counter[vin["mark"]] = {
                    "checked": 0,
                    "found": 0,
                    "pcc": 0,
                    "acat": 0
                }
            marks_counter[vin["mark"]]["checked"] += 1
            if source_type:
                counter["total_found"] += 1
                marks_counter[vin["mark"]]["found"] += 1
                counter[source_type] += 1
                marks_counter[vin["mark"]][source_type] += 1
        self.logger.info('Checked {total_checked} vins, found {total_found} vins, {acat} acat, {pcc} pcc'.
                         format(**counter))
        df = pd.DataFrame(marks_counter)
        df.to_excel('result_data/CheckedVins.xlsx')

    def test_vin(self):
        marks = ['VOLKSWAGEN', 'ALFA_ROMEO', 'CITROEN', 'BMW', 'MERCEDES', 'KIA']
        for mark in marks:
            vin = self.vins[mark][0]['vin']
            self.vin_request(vin, True, True, True)

    def test_search_by_params(self):
        marks = ['AUDI', 'DAEWOO', 'HYUNDAI', 'MITSUBISHI', 'RENAULT', 'BMW', 'FIAT', 'KIA', 'NISSAN', 'TOYOTA',
                 'CHEVROLET', 'FORD', 'MAZDA', 'OPEL', 'VOLKSWAGEN', 'CITROEN', 'HONDA', 'MERCEDES', 'PEUGEOT']

        params = {
            "sort_popular": True,
            "mark": None,
            "model": None,
            "generation": None,
            "configuration": None,
            "body": None,
            "doors": None,
            "engine": None,
            "gear": None,
            "transmission": None,
            "year": None,
            "horse_power": None,
            "volume": None,
            "tech_param": None,
            "page": 0
        }

        for mark_code in marks:
            params["mark"] = mark_code
            models = self.uc.verba.get_models(mark_code)
            random_models = random.sample(models, 2)

            for model in random_models:
                model_code = model["code"]
                params["model"] = model_code
                generations = self.uc.get_verba_generations(mark_code, model_code)
                if generations["data"]:
                    gen_code = random.choice(generations["data"])["code"]
                    params["generation"] = gen_code
                    configurations = self.uc.get_verba_configurations(mark_code, model_code, gen_code)
                    if configurations["data"]:
                        config = random.choice(configurations["data"])
                        params["configuration"] = config["code"]
                        params["body"] = config["body"]
                        params["doors"] = config["doors"]
                        tech_params = self.uc.get_verba_tech_params(mark_code, model_code, gen_code, config["code"])
                        if tech_params["data"]:
                            engine_filter = tech_params["data"][random.choice(list(tech_params["data"].keys()))]
                            gear_filter = engine_filter[random.choice(list(engine_filter.keys()))]
                            transmission_filter = gear_filter[random.choice(list(gear_filter.keys()))]
                            year_filter = transmission_filter[random.choice(list(transmission_filter.keys()))]
                            tech_param = random.choice(year_filter)
                            params["engine"] = tech_param["engine"]
                            params["gear"] = tech_param["gear"]
                            params["transmission"] = tech_param["transmission"]
                            params["year"] = tech_param["year"]
                            params["tech_param"] = tech_param
                            params["horse_power"] = tech_param["horse_power"]
                            params["volume"] = tech_param["volume"]
                            cars_response = self.catalogs.find_by_tech_params(params, "pcc")
                            self.assertValidateJSON(cars_response, self.response_schema)
                            cars = cars_response["data"]["cars"]

                            if cars:
                                car = random.choice(cars)
                                self.vin_request(
                                    {"mark": cars_response["meta"]["mark"], "carId": car["id"], "source": "pcc"},
                                    find_by_id=True
                                )
                            else:
                                self.logger.info(
                                    'Not found car with params: {}'.format(json.dumps(params, ensure_ascii=False))
                                )


if __name__ == '__main__':
    unittest.main()
