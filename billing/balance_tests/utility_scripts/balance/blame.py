# coding: utf-8
from btestlib.secrets import get_secret, Telegram, Tokens

__author__ = 'a-vasin'

import os
import pickle
from collections import defaultdict
from xml.etree import ElementTree

from git import Repo
from telepot import Bot

import btestlib.utils as utils
from balance.tests.conftest import TestStatsCollector
from btestlib import reporter
from btestlib.constants import Users
from btestlib.shared import AFTER
from simpleapi.common.utils import call_http

PROJECT = os.environ.get('TARGET_PROJECT', 'BALANCE')
BUILDCONF = os.environ.get('TARGET_BUILDCONF', 'Full')
BUILD_NUMBER = os.environ.get('TARGET_BUILD_NUMBER', None)

BOT_TOKEN = str(get_secret(*Telegram.BLAME_BOT_TOKEN))
STAFF_TOKEN = str(get_secret(*Tokens.PIPELINER_OAUTH_TOKEN))

# здесь можно указать свой chat_id и получить репорт в личку (бот для chat_id @RawDataBot)
CHAT_ID = -1001067503629

TESTS_FOLDER = 'balance/tests/'
TELEGRAM_LIMIT = 4096

FORMER_PEOPLE_REPLACEMENT_MAP = { 'alshkit': 'atkaya',
                                  'pelmeshka': 'atkaya',
                                  'igogor': 'torvald',
                                  'blubimov': 'torvald'}


def get_last_build_number():
    url = 'https://teamcity.yandex-team.ru/app/rest/buildTypes/' \
          'affectedProject:(name:{}),name:{}'.format(PROJECT, BUILDCONF)

    xml_response = call_http(url, method='GET', auth_user=Users.TESTUSER_BALANCE1)

    build_number = ElementTree.fromstring(xml_response).find(".//property[@name='buildNumberCounter']").attrib['value']
    build_type_id = ElementTree.fromstring(xml_response).attrib['id']
    return int(build_number) - 1, build_type_id


def get_telegram_by_email(email):
    url = 'https://staff-api.yandex-team.ru/v3/persons'
    headers = {
        'Authorization': 'OAuth {}'.format(STAFF_TOKEN)
    }
    params = {
        'work_email': email,
        '_one': 1
    }

    response = call_http(url, method='GET', headers=headers, params=params)

    if response.get('error_message', None):
        return email

    contact = next((u'@' + account['value'] for account in response['accounts'] if account['type'] == u'telegram'),
                   email)
    return contact


def blame_people(build_number):
    key = utils.make_build_unique_key(TestStatsCollector.S3_PREFIX.format(AFTER), build_number, PROJECT, BUILDCONF)

    if not utils.s3storage_stats().is_present(key):
        raise Exception("Statistics were not found for key: {}".format(key))

    with reporter.reporting(level=reporter.Level.NOTHING):
        stats = pickle.loads(utils.s3storage_stats().get_string_value(key))

    blame_dict = defaultdict(lambda: defaultdict(set))
    repo = Repo(utils.project_dir())
    for test_full_name, test_stat in stats['tests'].iteritems():
        if not test_stat['failed']:
            continue

        test_local_path = test_full_name.split('::')[0]
        test_path = utils.project_file(TESTS_FOLDER + test_local_path)
        test_name = test_full_name.split('::')[1].split('[')[0]

        if not os.path.exists(test_path):
            blame_dict['unknown'][test_local_path].add(test_name)
            continue

        file_lines_w_emails = [(line, commit.author.email) for commit, lines in repo.blame('HEAD', test_path)
                               for line in lines]
        file_lines = [line for line, email in file_lines_w_emails]

        test_line_index = next((index for index, line in enumerate(file_lines) if test_name + '(' in line), None)

        if not test_line_index:
            blame_dict['unknown'][test_local_path].add(test_name)
            continue

        next_test_start = next((index for index, line in enumerate(file_lines[test_line_index + 1:])
                                if line.startswith('def') or line.startswith('@')),
                               len(file_lines) - test_line_index - 1)

        line_authors_count = defaultdict(int)
        for _, email in file_lines_w_emails[test_line_index:test_line_index + next_test_start + 1]:
            line_authors_count[email] += 1

        email = sorted(line_authors_count.iteritems(), key=lambda entry: entry[1], reverse=True)[0][0]
        blame_dict[email][test_local_path].add(test_name)

    replace_former_people(blame_dict)

    return blame_dict


def replace_former_people(blame_dict):
    email_postfix = '@yandex-team.ru'
    former_emails_map = {'{}{}'.format(former_login, email_postfix): '{}{}'.format(new_login, email_postfix)
                         for former_login, new_login in FORMER_PEOPLE_REPLACEMENT_MAP.iteritems()}
    for former_email in former_emails_map.keys():
        if former_email in blame_dict.keys():
            former_modules = blame_dict.pop(former_email)
            if former_emails_map[former_email] in blame_dict.keys():
                for module, tests in former_modules.iteritems():
                    if module in blame_dict[former_emails_map[former_email]]:
                        blame_dict[former_emails_map[former_email]][module].update(tests)
                    else:
                        blame_dict[former_emails_map[former_email]].update({module: tests})
            else:
                blame_dict.update({former_emails_map[former_email]: former_modules})


def make_blame_messages(blame_dict):
    blame_messages = []

    for email, file_dict in blame_dict.iteritems():
        blame_message = get_telegram_by_email(email)
        for file_path in sorted(file_dict.keys()):
            blame_message += '\n\n' + file_path
            blame_message += u''.join([u'\n' + test for test in sorted(file_dict[file_path])])

        blame_messages.append((blame_message, None))

    return blame_messages


def get_build_id(build_number):
    url = 'https://teamcity.yandex-team.ru/app/rest/builds/' \
          'affectedProject:(name:{}),buildType(name:{}),number:{}'.format(PROJECT, BUILDCONF, build_number)

    xml_response = call_http(url, method='GET', auth_user=Users.TESTUSER_BALANCE1)

    build_id = ElementTree.fromstring(xml_response).attrib['id']
    return int(build_id)


def make_build_urls_message(build_number, build_type_id):
    build_id = get_build_id(build_number)

    teamcity_url = "[Teamcity build](https://teamcity.yandex-team.ru/viewLog.html?buildId={})".format(build_id)
    allure_url = "[Allure report](https://teamcity.yandex-team.ru/repository/download/" \
                 "Billing_Autotesting_PythonTests_RunTests/{}:id/allure-report.zip%21/allure-report/index.html)".format(
        build_id)

    return '\n'.join((teamcity_url, allure_url)), "Markdown"


def send_messages(messages):
    bot = Bot(BOT_TOKEN)
    for message, parse_mode in messages:
        limited_messages = [message[i: i + TELEGRAM_LIMIT] for i in range(0, len(message), TELEGRAM_LIMIT)]
        for limited_message in limited_messages:
            bot.sendMessage(CHAT_ID, limited_message, parse_mode=parse_mode)


if __name__ == "__main__":
    build_number, build_type_id = int(BUILD_NUMBER) if BUILD_NUMBER else get_last_build_number()
    build_number = int(BUILD_NUMBER) if BUILD_NUMBER else build_number
    blame_dict = blame_people(build_number)
    messages = make_blame_messages(blame_dict)
    messages.append(make_build_urls_message(build_number, build_type_id))

    for message, _ in messages:
        print '\n------------------------------------------\n'
        print message

    if not utils.is_local_launch():
        send_messages(messages)
