# -*- coding: utf-8 -*-

import collections
import json
import os
import re
import time
import urlparse

import requests

TOKEN = 'AVImnikAAASWQ1c2OpAjTgyHcNpGETvXgg'
SCHEME = 'https'
WIKI_API_NETLOC = 'wiki-api.yandex-team.ru'
WIKI_API_PATH = {'start': '/_api/frontend'}

DOC_CLUSTER = u'https://wiki.yandex-team.ru/Balance/Docs/process/'
# PATH = '.\\balance'
PATH = 'C:\\torvald\\_TOOLS\\tools'

PY_MASK = '.py'
IGNORED_LIST = ['__init__.py']

PY_OPENED_PATTERN = '#<TAG_[1-9a-z]+.*'
PY_CLOSED_PATTERN = '#>TAG_[1-9]+'
# Компилируем паттерн для поиска открывающих и закрывающих тегов
PY_PATTERN = re.compile('({}|{})'.format(PY_OPENED_PATTERN, PY_CLOSED_PATTERN))

WIKI_PATTERN = re.compile('(#(TAG_\d+)(\s\w*?\n|\n)(%%[\s\S]*?%%)?)', re.UNICODE)


def format_url_for_api(url):
    parsed_url = urlparse.urlparse(url)
    path = parsed_url.path
    replaced = parsed_url._replace(scheme=SCHEME, netloc=WIKI_API_NETLOC, path=WIKI_API_PATH['start'] + path)
    result = replaced.geturl()
    return result


class WikiMarker(object):
    def __init__(self):
        self.session = requests.Session()
        self.session.headers['Content-Type'] = 'application/json'
        self.session.headers['Authorization'] = 'OAuth ' + TOKEN

    def open_page(self, url):
        response = self.session.get(format_url_for_api(url + '/.raw'))
        data = response.json()[u'data'][u'body']
        return data

    def get_page_data(self, leaf):
        response = self.session.get(leaf + '/.raw')
        data = response.json()[u'data'][u'body']
        return data

    def get_cluster_tree(self, cluster):
        path = format_url_for_api(cluster)
        result = self.session.get(path + '/.treecluster', params={'sort_order': 'title'})
        return [item[u'url'] for item in result.json()[u'data']]

    def rewrite_page(self, url, body_data):
        data = dict(body=body_data)
        self.session.post(format_url_for_api(url), json.dumps(data))


class CodeProcessor(object):
    @staticmethod
    def is_open_tag(tag):
        return tag.startswith('#<TAG')

    @staticmethod
    def is_close_tag(tag):
        return tag.startswith('#>TAG')

    @staticmethod
    def reformat(tag):
        # Если тег открывающий - отделяем тег от комментариев
        if CodeProcessor.is_open_tag(tag):
            tag = tag.split(' ')[0]

        # Очищаем тег от ведущих символов '#<' или '#>'
        return tag[2:]

    @staticmethod
    def get_all_py_files(path):
        # Для директории path получаем все файлы c расширением PY_MASK, кроме входящих в IGNORED_LIST
        return ['{0}\\{1}'.format(level[0], item) for level in os.walk(path)
                for item in level[2]
                if item.endswith(PY_MASK)
                and item not in IGNORED_LIST]

    @staticmethod
    def process_file(file):
        with open(file, 'r') as f:
            # Храним: номер строки \ список открытых на текущий момент тегов \ список некорректных тегов \ сниппеты
            opened = []
            newborn_tag = []
            incorrect_tags = []
            snippets = collections.defaultdict(str)

            for line_no, line in enumerate(f.readlines()):

                # Ищем открывающий\закрывающий тег и получаем его
                match_obj = PY_PATTERN.search(line)
                if match_obj:
                    tag = match_obj.group()

                    # Если тег открывающий: очищаем его от лишних символов и добавляем в список открытых тегов
                    if CodeProcessor.is_open_tag(tag):
                        newborn_tag.append(CodeProcessor.reformat(tag))

                    # Если тег закрывающий: очищаем его от лишних символов и валидируем
                    if CodeProcessor.is_close_tag(tag):
                        tag = CodeProcessor.reformat(tag)

                        # Валидация: если закрывающему тегу не предшествует открывающих
                        if tag not in opened:
                            incorrect_tags.append(tag)

                        # Разрешаем теги с пересекающимися кусками кода
                        # # Валидация: тег закрывает НЕ последний из добавленных открывающих (ошибка вложенности тегов)
                        # elif opened[-1] != close_tag:
                        #     incorrect_tags.append(close_tag)

                        # Если проверки прошли, удаляем корректно закрывшийся открывающий тег из списка
                        else:
                            opened.remove(tag)

                # Для каждого открытого на данный момент тега добавляем текущую строку в сниппет (с номером строки)
                for tag in opened:
                    snippets[tag] += '{0}\t|{1}'.format(line_no + 1, line)

                if newborn_tag:
                    opened.append(newborn_tag.pop())

            # Валидация: файл закончился, но остались открытые теги
            if opened:
                incorrect_tags.extend(opened)

            # Для некорректных тегов выкидываем сниппеты
            for tag in incorrect_tags:
                if tag in snippets.keys():
                    snippets.pop(tag)

        return snippets, incorrect_tags


def timer(func):
    def wrapper(*args, **kwargs):
        start = time.time()
        result = func(*args, **kwargs)
        print (time.time() - start)
        return result

    return wrapper


@timer
def get_code_snippets():
    # Получаем список всех файлов в указанной директории (с учётом вложенности)
    file_list = CodeProcessor.get_all_py_files(PATH)
    total = len(file_list)

    tags_data = {}
    duplicated_tags = []
    broken_tags = []

    for n, file in enumerate(file_list):
        print 'Search in {} of {}: {}'.format(n + 1, total, file)
        snippets, incorrect_tags = CodeProcessor.process_file(file)

        if incorrect_tags:
            broken_tags.append({'file': file, 'tags': incorrect_tags})

        if snippets:
            for tag, snippet in snippets.items():
                # В случае дублирования тега - сохраняем его первое упоминание и отмечаем факт дублирования
                if tag in tags_data:
                    duplicated_tags.append({'file': file, 'tag': tag, 'snippet': snippet})
                else:
                    tags_data[tag] = {'file': file, 'snippet': snippet, 'line': None}

    return tags_data, broken_tags, duplicated_tags


@timer
def modify_wiki(tags_data):
    wm = WikiMarker()
    # tree = wm.get_cluster_tree(DOC_CLUSTER)
    tree = [u'/balance/docs/process/testtags']

    total = len(tree)
    wiki_tags_data = {}
    changed_tags = []

    for n, leaf in enumerate(tree):
        print 'Search in {} of {}: {}'.format(n + 1, total, leaf)
        url = format_url_for_api(leaf)
        page = wm.get_page_data(url)
        groups = re.findall(WIKI_PATTERN, page)
        for all, tag, comment, snippet in groups:
            wiki_tags_data[tag] = {'url': leaf, 'comment': comment, 'snippet': snippet}

            if tag in tags_data:
                new_snippet = u'%%\n{}\n%%'.format(tags_data[tag]['snippet'].decode('utf-8'))
                if snippet != new_snippet:
                    page = re.sub(re.escape(snippet), new_snippet, page)
                    changed_tags.append(tag)

        if groups:
            wm.rewrite_page(leaf, page)

    wiki_only_tags = list(set(wiki_tags_data.keys()) - set(tags_data.keys()))
    code_only_tags = list(set(tags_data.keys()) - set(wiki_tags_data.keys()))

    return wiki_tags_data, changed_tags, wiki_only_tags, code_only_tags


if __name__ == '__main__':
    code_tags_data, broken_tags, duplicated_tags = get_code_snippets()
    wiki_tags_data, changed_tags, wiki_only_tags, code_only_tags = modify_wiki(code_tags_data)
    pass
