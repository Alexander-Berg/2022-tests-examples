# coding: utf-8
from pprint import pprint

import hamcrest

__author__ = 'sunshineguy'

"""
    Скрипт предназначен для преобразования логов упавшего теста
    в удобный вид для стартрекера
"""

LOG_FILE_PATH = 'task_log.log'


class LogAsStringList(object):
    def __init__(self, file_string_list=None, need_coments=False):
        self.file_string_list = file_string_list or self.generate_file()
        if not need_coments:
            self.delete_coments()

    def generate_file(self):
        file_list = list()
        with open(LOG_FILE_PATH) as file:
            for line in file:
                file_list.append(line)
        return file_list

    def get_file_string_list(self):
        return self.file_string_list

    def delete_coments(self):
        for line in self.file_string_list:
            if not (line[0] == ' ' or line.find('BalanceSimple') == -1):
                self.file_string_list.remove(line)


# for line in LogAsStringList(need_coments=True).get_file_string_list():
#     print line

for line in LogAsStringList().get_file_string_list():
    print line

for line in LogAsStringList().get_file_string_list():
    print line
    if line[0] == ' ':
        print line
