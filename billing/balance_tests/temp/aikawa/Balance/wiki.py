# -*- coding: utf-8 -*-

import json
import logging
import re
import urlparse

import requests as req

logging.basicConfig(level=logging.DEBUG)


wiki_api_netloc = 'wiki-api.yandex-team.ru'
wiki_api_path = {'start': '/_api/frontend'}


def get_session():
    session = req.Session()
    session.headers['Content-Type'] = 'application/json'
    session.headers['Authorization'] = 'OAuth ' + token
    return session


def turn_url_to_api_url(url):
    parsed_url = urlparse.urlparse(url)
    path = parsed_url.path
    replaced = parsed_url._replace(netloc=wiki_api_netloc, path=wiki_api_path['start'] + path)
    result = replaced.geturl()
    return result


class WikiMarker(object):
    def __init__(self, element_of_mark_list):
        self.url = element_of_mark_list['docpath'].args[0]
        print self.url
        self.test = element_of_mark_list['item']
        self.text = list(element_of_mark_list['docs'].args)
        self.docstring = element_of_mark_list['docstring']
        print self.text

    def append_to_result_dict(self, dict_append_to):
        if dict_append_to.get(self.url, 0) == 0:
            dict_append_to[self.url] = [{'test': self.test, 'text': self.text, 'docstring': self.docstring}]
        else:
            dict_append_to[self.url].append({'test': self.test, 'text': self.text, 'docstring':self.docstring})
        return dict_append_to

    @staticmethod
    def open_page(url):
        session = get_session()
        response = session.get(turn_url_to_api_url(url + '/.raw'))
        data = response.json()['data']['body']
        return data

    @staticmethod
    def delete_previous_marks(url):
            data = WikiMarker.open_page(url)
            pattern_list = [
                u'\<\{Автотесты[^\>\}]+\}\>'
                , u'\[\[\*[0-9]+\]\]'
                , u'\!\!\(сер\)\[\!\!'
                , u'\!\!\(сер\)\]\!\!'
            ]
            for pattern in pattern_list:
                data = re.sub(pattern, '', data)
            return data

    @staticmethod
    def rewrite_page(url, body_data):
            session = get_session()
            data = dict(body=body_data)
            session.post(turn_url_to_api_url(url), json.dumps(data))

    @staticmethod
    def make_references(data, text_list, test, docstring,count, final_block, not_found_test, founded_text_list):
            number = count.next()
            final_block_row = []
            for text in text_list:
                if text in data:
                    new_value = u'{0} [[*{1}]]'.format(text, number)
                    data = data.replace(text, new_value)
                    final_block_row = [number, test, docstring]
                    founded_text_list.add(text)
                else:
                    not_found_test.append({'test_name': test, 'text': text})
            if final_block_row:
                final_block.append(final_block_row)
            return data, final_block, not_found_test, founded_text_list

    @staticmethod
    def make_brackets(data, founded_text_list):
            for text in founded_text_list:
                if text in data:
                    new_value = u'!!(сер)[!!{0}!!(сер)]!!'.format(text)
                    data = data.replace(text, new_value)
            return data

    # @staticmethod
    # def make_final_block(final_block):
    #         final_block_text = u'{0}'.format(('\n'.join(u'[[#{0}]] {1} !!(сер){2}!!'.format(number, test, (docstring if docstring is not None else '')) for number, test, docstring in final_block)))
    #         final_block = u'==+ ==\n<{Автотесты\n' + final_block_text + ' \n}>'
    #         return final_block

    @staticmethod
    def main(dict):
            print dict
            for url, tests_and_texts in dict.iteritems():
                data_wo_previous_marks = WikiMarker.delete_previous_marks(url)
                data = data_wo_previous_marks
                final_block = []
                not_found_test = []
                not_found_test_result = {}
                founded_text_list = set()
                counter = iter(range(1, 100))
                for test_and_text in tests_and_texts:
                    data, final_block, not_found_test, founded_text_list = WikiMarker.make_references(data, test_and_text['text'], test_and_text['test'], test_and_text['docstring'],counter, final_block, not_found_test, founded_text_list)
                data = WikiMarker.make_brackets(data, founded_text_list)
                if final_block:
                    final_block = WikiMarker.make_final_block(final_block)
                    data = data + final_block
                if not_found_test:
                    not_found_test_result[url] = not_found_test
                WikiMarker.rewrite_page(url, data)


