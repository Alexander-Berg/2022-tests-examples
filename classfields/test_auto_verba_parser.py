# coding=utf-8
import unittest

from analytics.verta.auto_verba_parser import AutoVerbaParser


class AutoVerbaParserTest(unittest.TestCase):
    MARKS_SOURCE = "/Users/baibik/Documents/Analitics/Autoparts/auto_marks.csv"
    MODELS_SOURCE = "/Users/baibik/Documents/Analitics/Autoparts/auto_models.csv"
    GENERATIONS_SOURCE = "/Users/baibik/Documents/Analitics/Autoparts/auto_generations.csv"
    COMPLECTATIONS_SOURCE = "/Users/baibik/Documents/Analitics/Autoparts/auto_complect.csv"

    TRUCKS_MARKS_SOURCE = "/Users/baibik/Documents/Analitics/Autoparts/temp_trucks_marks.csv"
    TRUCKS_MODELS_SOURCE = "/Users/baibik/Documents/Analitics/Autoparts/temp_trucks_models.csv"
    TRUCKS_GENERATIONS_SOURCE = "/Users/baibik/Documents/Analitics/Autoparts/trucks_generations.csv"

    def load_cars_test(self):
        AutoVerbaParser.load_cars(self.MARKS_SOURCE, 21, 35, 33, "auto")


    def load_trucks_test(self):
        AutoVerbaParser.load_cars(self.TRUCKS_MARKS_SOURCE, 13, 26, "trucks")


    def load_cars_models_test(self): # Проверить индексы
        AutoVerbaParser.load_models(self.MODELS_SOURCE, 13, 34, "auto")


    def load_trucks_models_test(self):
        AutoVerbaParser.load_models(self.TRUCKS_MODELS_SOURCE, 12, 38, "trucks")


    def load_cars_generations_test(self):
        AutoVerbaParser.load_generations(self.GENERATIONS_SOURCE, 6, 22, 21, 23, 37, 43, "trucks")


    def load_trucks_generations_test(self):
        AutoVerbaParser.load_generations(self.TRUCKS_GENERATIONS_SOURCE, 14, None, 20, None, None, None, "trucks")


    def update_ts_types_test(self):
        AutoVerbaParser.update_ts_types()
