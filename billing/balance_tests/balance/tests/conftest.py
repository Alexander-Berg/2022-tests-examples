# coding: utf-8

import collections
import datetime
import inspect
import io
import json
import os
import pickle
import re
import shutil
import sys
import time
import urllib
from xml.etree import ElementTree
import uuid
import copy

import pytest

import btestlib.config as balance_config
import btestlib.environments as env
import btestlib.shared as shared
import btestlib.utils as utils
# from balance import balance_steps as steps
from balance.balance_steps import simpleapi_steps, CommonSteps, FakeTrustApi, SimpleApi, SimpleNewApi
from balance import balance_db as db
from balance.features import Features, AuditFeatures
from balance.wiki import WikiMarker as wm
from btestlib import reporter, secrets
from btestlib.constants import Users
from simpleapi.data.uids_pool import User
from balance.tests.tus import tus_get_account, tus_create_account, tus_unlock_account

log = reporter.logger()


# pytest_xdist_setupnodes
# pytest_configure_node

def pytest_addoption(parser):

    # Monkey patch to prevent bracket format changing [] -> ():
    from teamcity import pytest_plugin
    pytest_plugin.EchoTeamCityMessages.format_test_id = utils.format_test_id

    shared.pytest_addoption(parser)

    CacheInvalidator.pytest_addoption(parser)
    MetricsCollector.pytest_addoption(parser)
    DocsLinker.pytest_addoption(parser)
    TestFilter.pytest_addoption(parser)
    AuditCollector.pytest_addoption(parser)
    OnlyFailedHandler.pytest_addoption(parser)
    TestStatsCollector.pytest_addoption(parser)
    TestsRuntimeSorter.pytest_addoption(parser)
    MutedTestsRunner.pytest_addoption(parser)
    MutedTestsHandler.pytest_addoption(parser)
    FakeTrustActivator.pytest_addoption(parser)
    IntegrationsConfiguration.pytest_addoption(parser)


def pytest_configure(config):
    CacheInvalidator.pytest_configure(config)
    MetricsCollector.pytest_configure(config)
    DocsLinker.pytest_configure(config)
    TestFilter.pytest_configure(config)
    AuditCollector.pytest_configure(config)
    NoParallelLock.pytest_configure(config)
    OnlyFailedHandler.pytest_configure(config)
    TestStatsCollector.pytest_configure(config)
    WaitersHandler.pytest_configure(config)
    MutedTestsHandler.pytest_configure(config)
    TestsRuntimeSorter.pytest_configure(config)
    ErrorCategories.pytest_configure(config)


def pytest_generate_tests(metafunc):
    ParametrizeIdMaker.pytest_generate_tests(metafunc)


# ----------------------------------------------------------------------------------------------------------------------

@pytest.hookimpl(trylast=True)
def pytest_collection_modifyitems(session, config, items):
    pytest.active_tests = [item.nodeid for item in items if not item.get_marker('xfail')]
    DocsLinker.pytest_collection_modifyitems(session, config, items)

    shared.pytest_collection_modifyitems(session, config, items, OnlyFailedHandler.LAST_FAILED_TESTS)

    TestsRuntimeSorter.pytest_collection_modifyitems(session, config, items)
    AuditCollector.pytest_collection_modifyitems(session, config, items)


# ----------------------------------------------------------------------------------------------------------------------

def pytest_runtest_setup(item):
    shared_state = shared.get_stage(item.session.config)

    # На стадии 'block' запускаем тест игнорируя любые возможные фильтрации. Считаем, что блок всегда корректен.
    if shared_state != shared.BLOCK:
        TestFilter.pytest_runtest_setup(item)
        XFailChecker.pytest_runtest_setup(item)
        MutedTestsHandler.pytest_runtest_setup(item)
        MutedTestsRunner.pytest_runtest_setup(item)

    OnlyFailedHandler.pytest_runtest_setup(item)
    NoParallelLock.pytest_runtest_setup(item)

    # Устанавливаем флаг, что мы внутри теста
    sys._running_test_name = item.nodeid


# ----------------------------------------------------------------------------------------------------------------------

def pytest_runtest_protocol(item):
    pass


# ----------------------------------------------------------------------------------------------------------------------

def pytest_sessionstart(session):
    HostIndicator.pytest_sessionstart(session, env.balance_env())


# ----------------------------------------------------------------------------------------------------------------------

def pytest_runtest_teardown(item, nextitem):
    NoParallelLock.pytest_runtest_teardown(item, nextitem)

    # Убираем, флаг о том, что мы внутри теста
    if utils.is_inside_test():
        del sys._running_test_name


# ----------------------------------------------------------------------------------------------------------------------


def pytest_runtest_makereport(item, call):
    shared.pytest_runtest_makereport(item, call)
    ReportHelper.pytest_runtest_makereport(item, call)
    ErrorCategories.pytest_runtest_makereport(item, call)


# ----------------------------------------------------------------------------------------------------------------------

def pytest_runtest_logreport(report):
    ReportHelper.pytest_runtest_logreport(report)


# ----------------------------------------------------------------------------------------------------------------------


def pytest_report_teststatus(report):
    MetricsCollector.pytest_report_teststatus(report)
    OnlyFailedHandler.pytest_report_teststatus(report)
    TestStatsCollector.pytest_report_teststatus(report)


# ----------------------------------------------------------------------------------------------------------------------
def pytest_sessionfinish(session, exitstatus):
    shared.pytest_sessionfinish(session, exitstatus)

    MetricsCollector.pytest_sessionfinish(session, exitstatus)
    OnlyFailedHandler.pytest_sessionfinish(session, exitstatus)
    AuditCollector.pytest_sessionfinish(session, exitstatus)
    TestStatsCollector.pytest_sessionfinish(session, exitstatus)
    ErrorCategories.pytest_sessionfinish(session, exitstatus)
    AllureEnvironmentAttacher.pytest_sessionfinish(session, exitstatus)
    MutedTestsRunner.pytest_sessionfinish(session, exitstatus)


# ----------------------------------------------------------------------------------------------------------------------

def pytest_unconfigure(config):
    WaitersHandler.pytest_unconfigure(config)
    ReportHelper.pytest_unconfigure(config)
    SecretsManager.pytest_unconfigure(config)


# ----------------------------------------------------------------------------------------------------------------------

@pytest.fixture()
def data_cache(request):
    start = time.time()
    item = request.keywords.node.nodeid
    # Получаем список ключей
    try:
        key_prefix = env.ENV_TO_S3_PREFIX[env.balance_env().name]
        key = '{}/{}'.format(key_prefix, item)
        if utils.s3storage_cache().is_present(key):
            with reporter.reporting():
                value = pickle.loads(utils.s3storage_cache().get_string_value(key))
            log.debug('------- Data received from S3 in {0}'.format(time.time() - start))
        else:
            value = None
    except Exception as exc:
        log.error('Exception while getting cached data from S3: {}'.format(exc))
        value = None
    if request.config.getoption('invalidate_cache'):
        return item, None
    return item, value


@pytest.fixture()
def shared_data(request):
    # logger.LOG.debug(u'SHARED_DATA_BY_TEST:\n{}'.format(utils.Presenter.pretty(shared.SHARED_DATA_BY_TEST)))
    sd = shared.shared_data_fixture(request=request)
    # logger.LOG.debug(u'shared_data:\n{}'.format(utils.Presenter.pretty(sd)))
    return sd


# заимствовано в тестах сверок (shared_data)
@pytest.fixture()
def shared_data_cache(request):
    import shared_cache
    # logger.LOG.debug(u'SHARED_DATA_BY_TEST:\n{}'.format(utils.Presenter.pretty(shared.SHARED_DATA_BY_TEST)))
    sd = shared_cache.shared_data_fixture(request=request)
    # logger.LOG.debug(u'shared_data:\n{}'.format(utils.Presenter.pretty(sd)))
    return sd


def save_value(storage, key_prefix, value, additional_info_required=True):
    if not utils.is_local_launch():
        key = utils.make_build_unique_key(key_prefix, additional_info_required=additional_info_required)

        reporter.log("Saving data to key: {}\nData: {}".format(key, utils.Presenter.pretty(value)))

        with reporter.reporting(level=reporter.Level.NOTHING):
            utils.try_to_execute(
                lambda: storage.set_string_value(key, pickle.dumps(value)),
                description="save {}".format(key)
            )


def load_value(storage, key_prefix, build_number=None, additional_info_required=True):
    if not utils.is_local_launch():
        key = utils.make_build_unique_key(key_prefix, build_number, additional_info_required=additional_info_required)

        def helper():
            if storage.is_present(key):
                with reporter.reporting(level=reporter.Level.NOTHING):
                    return pickle.loads(storage.get_string_value(key))
            return None

        return utils.try_to_execute(helper, description="load {}".format(key))

    return None


def get_test_features_set(item):
    # return set(item.get_closest_marker('allure_label').args) \
    #     if 'allure_label' in item.keywords else set()
    features = set(item.function.allure_label.args) if hasattr(item.function, 'allure_label') else set()
    if 'audit' in item.keywords:
        features.add('audit')
    return features


def get_test_markers(item):
    # print 'TEST MARKERS'
    # print item.keywords._markers
    return item.keywords._markers


def get_test_location(item):
    return item.location[0] + '\\' + item.location[2]


def get_test_audit_features_set(item):
    # print 'TEST FEATURES'
    features_set_level_1 = get_test_features_set(item)
    # это очень плохо. надеюсь, больше второго уровня вложенности никто не будет писать.
    # нужно для корректной обработки декоратора @pytest.mark.audit(reporter.AuditFeatures(...)),
    # т.к. в этом случае в списке фич возвращается только audit, без его внутренностей
    features_set_level_2 = set()
    if 'audit' in item.keywords:
        if item.get_closest_marker('audit').args:
            features_set_level_2 = set(item.get_closest_marker('audit').args[0].args)

    features_set = features_set_level_1 | features_set_level_2
    return features_set


@pytest.fixture()
def mock_simple_api(pytestconfig, monkeypatch):
    FakeTrustActivator.mock_simple_api(pytestconfig, monkeypatch)


@pytest.fixture()
def get_free_user(need_cleanup=True, with_tus=True, env='prod'):
    if with_tus:
        user_uids = list()

        def _get_free_user(type=None):
            account = tus_get_account(env)

            if account.get('error') == 'account.not_found':
                tus_create_account()
                account = tus_get_account(env)

            if account.get('status') == 'ok':
                uid = account.get('account').get('uid')
                login = account.get('account').get('login')
                password = account.get('account').get('password')

                user_uids.append(uid)
                reporter.log(u'Получен пользователь с uid ' + uid + u' и логином ' + login)

                try:
                    db.balance().execute(
                        'INSERT INTO T_PASSPORT (PASSPORT_ID, LOGIN, GECOS, EMAIL, DEAD, CLIENT_ID, SIMPLE_CLIENT_ID, OPER_ID, IS_MAIN, INTERNAL) VALUES (:passport_id, :login, \'tus user\', \'tus-user@yandex-team.ru\', 0, null, null, null, 0, null)',
                        {'passport_id': uid, 'login': login}
                    )
                except Exception:
                    pass

                db.balance().execute('delete from t_role_user where passport_id=:passport_id', {'passport_id': uid})

                return User(uid, login, password)
            else:
                raise Exception(account)

        yield _get_free_user

        if need_cleanup:
            for uid in user_uids:
                tus_unlock_account(uid)
    else:
        user_ids = list()

        def _get_free_user(type=None):

            # Блокировка строки при параллельной работе обеспечивается тем, что мы сразу делаем update
            id = str(uuid.uuid4())
            user_ids.append(id)

            query = '''update (
                             select *
                             from t_test_passport
                             where login = (
                               select login
                               from t_test_passport
                               where (usage_dt is null
                                      OR usage_dt < sysdate - 1/24)
                               order by dbms_random.value(1, 100)
                               FETCH NEXT 1 ROWS ONLY)
                             )
                             set usage_dt = sysdate,
                                 uuid = :id
                       '''

            db.balance().execute(query, {'id': id})

            result = db.balance().execute('select * from t_test_passport where uuid=:id', {'id': id})

            if not result:
                raise Exception('No free login of such type: {}'.format(type))

            login = result[0]['login']
            passport_id = int(result[0]['passport_id'])
            password = result[0]['password']
            log.info(login)

            db.balance().execute('delete from t_role_user where passport_id=:passport_id', {'passport_id': passport_id})

            user = User(passport_id, login, password)

            return user

        yield _get_free_user

        if need_cleanup:
            for id in user_ids:
                query = '''update t_test_passport
                    set usage_dt = null, uuid = null
                    where uuid = :id'''

                db.balance().execute(query, {'id': id})

class IntegrationsConfiguration(object):
    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--config", action="store",
                         help="test fixed config")

        parser.addoption("--payments", action="store_true",
                         help="test only payments")

        parser.addoption("--acts", action="store_true",
                         help="test only acts")


class TestStatsCollector(object):
    S3_PREFIX = 'stats_{}'

    STAGE = None
    TESTS_STATS = {'tests': collections.defaultdict(dict)}
    COLLECTED_STATS = {'setup': set(), 'call': set()}

    TESTS_START = None

    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--collect_stats", action="store_true",
                         help="collect statistic for each test and save it to s3-mds")

        TestStatsCollector.TESTS_START = time.time()

    @staticmethod
    def pytest_configure(config):
        config.addinivalue_line("markers", "collect_stats: save tests statistics to s3-mds")

        # a-vasin: нужно для append_stats, чтобы скипать учет тестов на стадии BLOCK
        TestStatsCollector.STAGE = shared.get_stage(config)
        TestStatsCollector.S3_PREFIX = TestStatsCollector.S3_PREFIX.format(shared.get_stage(config))

    @staticmethod
    def pytest_report_teststatus(report):
        # a-vasin: к этому моменту STAGE уже должен быть получен
        if report.when not in ["call", "setup"] or TestStatsCollector.STAGE == shared.BLOCK:
            return

        # a-vasin: из-за SHARED_BLOCK иногда дважды вызывается append для одного и того же
        collected_stats = TestStatsCollector.COLLECTED_STATS

        if report.nodeid in collected_stats[report.when]:
            return
        collected_stats[report.when].add(report.nodeid)

        tests_stats = TestStatsCollector.TESTS_STATS

        duration = tests_stats['tests'][report.nodeid]['duration'] \
            if report.nodeid in tests_stats['tests'] and 'duration' in tests_stats['tests'][report.nodeid] \
            else 0

        tests_stats['tests'][report.nodeid].update({
            'duration': duration + report.duration,
            'failed': report.failed,
            'passed': report.passed,
            'outcome': report.outcome,
            'skipped': report.skipped,
            'error': report.longrepr,
            'when': report.when
        })

    @staticmethod
    def pytest_sessionfinish(session, exitstatus):
        if not session.config.getoption("--collect_stats") or utils.is_local_launch():
            return

        tests_stats = TestStatsCollector.TESTS_STATS

        if TestStatsCollector.STAGE != shared.BLOCK:
            slave_key = TestStatsCollector.S3_PREFIX
            if not utils.is_master_node(session.config):
                utils.save_slave_value(utils.make_build_unique_key(slave_key), tests_stats)
                return

            slave_tests_stats = utils.collect_slave_values(utils.make_build_unique_key(slave_key))

            # a-vasin: это мы так данные об ожидании подтягиваем из потоков
            for process_info in slave_tests_stats.values():
                for test_name, info in process_info['tests'].iteritems():
                    if 'runtime' not in info:
                        continue
                    if 'runtime' not in tests_stats['tests'][test_name]:
                        tests_stats['tests'][test_name]['runtime'] = collections.defaultdict(list)
                    for method_name, runtimes in info['runtime'].iteritems():
                        tests_stats['tests'][test_name]['runtime'][method_name] += runtimes

        tests_stats['duration'] = int(time.time() - TestStatsCollector.TESTS_START)

        tests_stats['build_number'] = balance_config.BUILD_NUMBER
        tests_stats['project_name'] = balance_config.TEAMCITY_PROJECT_NAME
        tests_stats['build_name'] = balance_config.TEAMCITY_BUILDCONF_NAME
        tests_stats['stage'] = TestStatsCollector.STAGE
        tests_stats['datetime'] = datetime.datetime.now()

        tests_stats['total'] = len(tests_stats['tests'])
        tests_stats['failed'] = len([item for item in tests_stats['tests'].values() if item['failed']])
        tests_stats['passed'] = len([item for item in tests_stats['tests'].values() if item['passed']])
        tests_stats['skipped'] = len([item for item in tests_stats['tests'].values() if item['skipped']])

        save_value(utils.s3storage_stats(), TestStatsCollector.S3_PREFIX, TestStatsCollector.TESTS_STATS)


# TESTBALANCE-1660: мониторинг состава аудируемых тестов
# в S3 хранится статистика аудируемых тестов в виде
# название теста, контрол ('no control', если тест не размечен): [номер рана попадания в стату, дата попадания в стату]
# также хранится статистика удаленных в том же виде
# и дифф между запусками
class AuditCollector(object):
    S3_PREFIX_STATS = 'audit_test'
    S3_PREFIX_DELETED = 'audit_deleted_test'
    S3_PREFIX_DIFF = 'audit_diff_test'
    S3_PREFIX_BUILD_NUM = 'audit_last_build_num'
    AUDIT_FEATURES = set([getattr(AuditFeatures, attr) for attr in dir(AuditFeatures) if not attr.startswith('__')])

    AUDIT_MARKS_ALL_STATS = None
    AUDIT_MARKS_DELETED = None
    AUDIT_MARKS_CURRENT_RUN_STATS = {'add_to_all_stats': {},
                                     'delete_from_all_stats': {},
                                     'add_to_deleted_stats': {},
                                     'delete_from_deleted_stats': {}}
    AUDIT_MARKS_ALL_STATS_MODIFIED = None

    BUILD_AUDIT_FIX = 'Billing_Autotesting_Maintenance_AuditFixReference'
    BUILD_AUDIT_COLLECT = 'Billing_Autotesting_Maintenance_AuditCollector'
    BUILD_ID = None
    BUILDTYPE_ID = None

    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--collect_audit", action="store_true",
                         help="collect audit marks for each test and save it to s3-mds")

    @staticmethod
    def pytest_configure(config):
        config.addinivalue_line("markers", "collect_audit: save tests audit list to s3-mds")
        # получаем и выводим информацию о аудируемых тестах, если запущен нужный профиль
        # TODO: добавить ограничение на профиль
        AuditCollector.BUILDTYPE_ID = config.getoption("--teamcity_buildtype_id")
        AuditCollector.BUILD_ID = balance_config.BUILD_NUMBER
        # AuditCollector.BUILDTYPE_ID = AuditCollector.BUILD_AUDIT_COLLECT
        if AuditCollector.BUILDTYPE_ID == AuditCollector.BUILD_AUDIT_FIX or \
                AuditCollector.BUILDTYPE_ID == AuditCollector.BUILD_AUDIT_COLLECT:
            AuditCollector.AUDIT_MARKS_ALL_STATS = dict(load_value(utils.s3storage(),
                                                                   AuditCollector.S3_PREFIX_STATS,
                                                                   additional_info_required=False))
            AuditCollector.AUDIT_MARKS_DELETED = dict(load_value(utils.s3storage(),
                                                                 AuditCollector.S3_PREFIX_DELETED,
                                                                 additional_info_required=False))
            AuditCollector.AUDIT_MARKS_ALL_STATS_MODIFIED = copy.deepcopy(AuditCollector.AUDIT_MARKS_ALL_STATS)
            reporter.log('AuditCollector: list of tests with audit marks')
            reporter.log('AuditCollector: {} tests: {}'.format(len(AuditCollector.AUDIT_MARKS_ALL_STATS),
                                                               AuditCollector.AUDIT_MARKS_ALL_STATS))
            reporter.log('AuditCollector: list of tests with deleted audit marks or features')
            reporter.log('AuditCollector: {} tests: {}'.format(len(AuditCollector.AUDIT_MARKS_DELETED),
                                                               AuditCollector.AUDIT_MARKS_DELETED))

    @staticmethod
    def pytest_collection_modifyitems(session, config, items):
        if AuditCollector.BUILDTYPE_ID == AuditCollector.BUILD_AUDIT_COLLECT:
            for item in items:
                AuditCollector.analyze_test(item)
        elif AuditCollector.BUILDTYPE_ID == AuditCollector.BUILD_AUDIT_FIX:
            AuditCollector.fix_reference()

    @staticmethod
    def pytest_sessionfinish(session, exitstatus):
        if AuditCollector.BUILDTYPE_ID == AuditCollector.BUILD_AUDIT_COLLECT or \
                AuditCollector.BUILDTYPE_ID == AuditCollector.BUILD_AUDIT_FIX:
            # при сборе статистики все невошедшие в запуск тесты из общей статы записываем в удаленные
            if AuditCollector.BUILDTYPE_ID == AuditCollector.BUILD_AUDIT_COLLECT:
                AuditCollector.add_missing_tests_to_deleted_stats()
                # пишем в s3 номер запуска, чтобы мочь по нему в тестах вытащить добавленные и удаленные тесты
                save_value(utils.s3storage(), AuditCollector.S3_PREFIX_BUILD_NUM, AuditCollector.BUILD_ID,
                           additional_info_required=False)
            AuditCollector.update_stats()
            save_value(utils.s3storage(), AuditCollector.S3_PREFIX_STATS,
                       AuditCollector.AUDIT_MARKS_ALL_STATS,
                       additional_info_required=False)
            save_value(utils.s3storage(), AuditCollector.S3_PREFIX_DELETED,
                       AuditCollector.AUDIT_MARKS_DELETED,
                       additional_info_required=False)
            save_value(utils.s3storage(), AuditCollector.S3_PREFIX_DIFF,
                       AuditCollector.AUDIT_MARKS_CURRENT_RUN_STATS)

    # фиксация эталона = чистим статистику удаленных и удаляем удаленные из общей статистики
    @staticmethod
    def fix_reference():
        # идем по всем удаленным ранее тестам
        for item in AuditCollector.AUDIT_MARKS_DELETED:
            test_location, feature = item
            # удаляем тест из общей статы
            AuditCollector.save_feature_status_to_diff(test_location, feature, destination='delete_from_all_stats',
                                                       status='fix reference', pop=False)
            # удаляем тест из статы удаленных
            AuditCollector.save_feature_status_to_diff(test_location, feature, destination='delete_from_deleted_stats',
                                                       status='fix reference', pop=False)

    @staticmethod
    def analyze_test(item):
        test_markers = get_test_markers(item)
        test_audit_features = get_test_audit_features_set(item) & AuditCollector.AUDIT_FEATURES
        test_location = get_test_location(item)
        test_old_stat = set()
        test_deleted_stat = set()
        test_current_stat = set()

        all_run_stats = AuditCollector.AUDIT_MARKS_ALL_STATS
        deleted_stats = AuditCollector.AUDIT_MARKS_DELETED

        # проверяем, встречается ли тест в общей стате
        for location, feature in all_run_stats:
            if location == test_location:
                test_old_stat.add(feature)
        # проверяем, встречается ли тест в стате удаленных
        for location, feature in deleted_stats:
            if location == test_location:
                test_deleted_stat.add(feature)
        # собираем информацию о тесте в текущем запуске
        if 'audit' in test_markers:
            # если у теста нет контролей, создаем фейковую фичу "Нет контроля"
            if not test_audit_features:
                test_audit_features.add('No control')
            for feature in test_audit_features:
                test_current_stat.add(feature)
        AuditCollector.check_test_feature_status(test_location, test_old_stat, test_deleted_stat, test_current_stat)

    @staticmethod
    def check_test_feature_status(test_location, test_all_features, test_deleted_features, test_current_features):
        new_features = test_current_features - test_all_features
        restored_features = test_current_features & test_deleted_features
        deleted_features = test_all_features - test_deleted_features - test_current_features
        same_features = test_all_features - deleted_features - restored_features

        for feature in new_features:
            AuditCollector.save_feature_status_to_diff(test_location, feature, destination='add_to_all_stats',
                                                       status='featured', pop=False)
        for feature in restored_features:
            AuditCollector.save_feature_status_to_diff(test_location, feature, destination='delete_from_deleted_stats',
                                                       status='restored')
        for feature in deleted_features:
            AuditCollector.save_feature_status_to_diff(test_location, feature, destination='add_to_deleted_stats',
                                                       status='deleted')
        for feature in same_features:
            AuditCollector.save_feature_status_to_diff(test_location, feature, destination=None, status=None)

        if 'No control' in test_all_features:
            if test_current_features - {'No control'}:
                # у теста появились фичи, удаляем 'No control' отовсюду
                # может быть избыточно
                AuditCollector.process_no_control(test_location)

    @staticmethod
    def save_feature_status_to_diff(test_location, feature, destination, status, pop=True):
        if destination:
            AuditCollector.AUDIT_MARKS_CURRENT_RUN_STATS[destination]. \
                update({(test_location, feature): [balance_config.BUILD_NUMBER, datetime.date.today(),
                                                   status]})
        if pop:
            AuditCollector.AUDIT_MARKS_ALL_STATS_MODIFIED.pop((test_location, feature))

    @staticmethod
    def process_no_control(test_location):
        AuditCollector.AUDIT_MARKS_CURRENT_RUN_STATS['delete_from_all_stats']. \
            update({(test_location, 'No control'): [balance_config.BUILD_NUMBER, datetime.date.today(),
                                                    'featured or restored']})
        AuditCollector.AUDIT_MARKS_CURRENT_RUN_STATS['delete_from_deleted_stats']. \
            update({(test_location, 'No control'): [balance_config.BUILD_NUMBER, datetime.date.today(),
                                                    'featured or restored']})
        if (test_location, 'No control') in AuditCollector.AUDIT_MARKS_ALL_STATS_MODIFIED:
            AuditCollector.AUDIT_MARKS_ALL_STATS_MODIFIED.pop((test_location, 'No control'))

    @staticmethod
    def update_stats():
        all = AuditCollector.AUDIT_MARKS_ALL_STATS
        current = AuditCollector.AUDIT_MARKS_CURRENT_RUN_STATS
        deleted = AuditCollector.AUDIT_MARKS_DELETED
        run_data = [balance_config.BUILD_NUMBER, datetime.date.today()]

        for category, lst, data in [('add_to_all_stats', all, run_data),
                                    ('add_to_deleted_stats', deleted, run_data),
                                    ('delete_from_all_stats', all, None),
                                    ('delete_from_deleted_stats', deleted, None)]:
            for item in current[category]:
                lst[item] = data

        for item in current['delete_from_all_stats']:
            if item in all:
                all.pop(item)
        for item in current['delete_from_deleted_stats']:
            if item in deleted:
                deleted.pop(item)

    @staticmethod
    def add_missing_tests_to_deleted_stats():
        for item in AuditCollector.AUDIT_MARKS_ALL_STATS_MODIFIED:
            if item not in AuditCollector.AUDIT_MARKS_DELETED:
                AuditCollector.AUDIT_MARKS_CURRENT_RUN_STATS['add_to_deleted_stats'][item] = \
                    [balance_config.BUILD_NUMBER, datetime.date.today(), 'deleted test']


class OnlyFailedHandler(object):
    S3_PREFIX = "failed"
    S3_RERUN_COUNT = "balance_rerun_count"

    FAILED_TESTS = []
    LAST_FAILED_TESTS = None
    LAST_FAILED_TESTS_SORTED = None

    NIGHT_REGRESSIONS = ['Billing_Autotesting_PythonTests_RunTests',
                         'Billing_Autotesting_PythonTests_FullTmNoParallelTrunkOrCandidate',
                         'Billing_Autotesting_PythonTests_FullTc',
                         'Billing_Autotesting_PythonTests_FullTcNoParallel']

    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--only_failed", action="store", metavar="ONLY_FAILED",
                         help="run only failed test from previous build")
        parser.addoption("--triggered_by", help="triggered by")

    @staticmethod
    def pytest_configure(config):
        config.addinivalue_line("markers", "only_failed: re-run only failed tests from specified test launch")
        trigger = config.getoption("--triggered_by")
        teamcity_buildtype_id = config.getoption("--teamcity_buildtype_id")
        build_number = config.getoption("--only_failed")
        # print '!!! DEBUG datetime.datetime.now().hour', datetime.datetime.now().hour
        # в 23.00 стартует пересборка версии, в 00.05 -  запуск полного профиля
        # триггеры на перезапуск упавших (по логике, описанной ниже), срабатывают в 2.00 и в 3.00
        # в 4.00 начинаются Java-тесты
        if trigger == 'Schedule Trigger' and teamcity_buildtype_id in OnlyFailedHandler.NIGHT_REGRESSIONS \
                and 4 > datetime.datetime.now().hour >= 2:
            build_number = -1

        if not utils.is_local_launch() and build_number and OnlyFailedHandler.LAST_FAILED_TESTS is None:
            build_number = int(build_number)
            if build_number < 0:
                build_number += int(balance_config.BUILD_NUMBER)

            OnlyFailedHandler.LAST_FAILED_TESTS = load_value(utils.s3storage(), OnlyFailedHandler.S3_PREFIX,
                                                             build_number)

            reporter.log('OnlyFailedHandler: list of failed tests for {} run:'.format(build_number))
            reporter.log('OnlyFailedHandler: {} tests: {}'.format(len(OnlyFailedHandler.LAST_FAILED_TESTS),
                                                                  OnlyFailedHandler.LAST_FAILED_TESTS))
            print(OnlyFailedHandler.LAST_FAILED_TESTS)

            OnlyFailedHandler.LAST_FAILED_TESTS = [utils.reformat_item(item)
                                                   for item in OnlyFailedHandler.LAST_FAILED_TESTS]

            reporter.log('OnlyFailedHandler: {} REFORMATTED tests: {}'.format(len(OnlyFailedHandler.LAST_FAILED_TESTS),
                                                                  OnlyFailedHandler.LAST_FAILED_TESTS))

            print(OnlyFailedHandler.LAST_FAILED_TESTS)


    @staticmethod
    def pytest_runtest_setup(item):
        if OnlyFailedHandler.LAST_FAILED_TESTS is not None:
            if utils.reformat_item(item.nodeid) not in OnlyFailedHandler.LAST_FAILED_TESTS:
                pytest.skip("Run only tests that failed during previous run")

    @staticmethod
    def pytest_report_teststatus(report):
        if report.when == "call" and report.outcome == "failed":
            OnlyFailedHandler.FAILED_TESTS.append(report.nodeid)

        if report.when == "setup" and report.outcome == "failed":
            OnlyFailedHandler.FAILED_TESTS.append(report.nodeid.replace('_setup[', '['))

    @staticmethod
    def pytest_sessionfinish(session, exitstatus):
        print '[DEBUG]: OnlyFailedHandler: is_local_launch: {}'.format(utils.is_local_launch())
        print '[DEBUG]: OnlyFailedHandler: S3_PREFIX: {}'.format(OnlyFailedHandler.S3_PREFIX)
        print '[DEBUG]: OnlyFailedHandler: FAILED_TESTS: {}'.format(OnlyFailedHandler.FAILED_TESTS)
        if not utils.is_local_launch():
            save_value(utils.s3storage(), OnlyFailedHandler.S3_PREFIX, OnlyFailedHandler.FAILED_TESTS)


class NoParallelLock(object):
    LOCK = None

    @staticmethod
    def pytest_configure(config):
        if not utils.is_master_node(config):
            return

        # a-vasin: на всякий пересоздаем папку с локами
        if os.path.exists(utils.FileLock.LOCK_DIR):
            shutil.rmtree(utils.FileLock.LOCK_DIR)
        os.makedirs(utils.FileLock.LOCK_DIR)

    @staticmethod
    def get_lock_file_name(item):
        lock_file_name = None
        if len(item.keywords._markers['no_parallel'].args) > 0:
            lock_file_name = item.keywords._markers['no_parallel'].args[0]

        if lock_file_name is None:
            lock_file_name = item.fspath.purebasename + "_" + str(hash(item.fspath.strpath))

        return lock_file_name

    @staticmethod
    def pytest_runtest_setup(item):
        if 'no_parallel' not in item.keywords._markers:
            return

        lock_file_name = NoParallelLock.get_lock_file_name(item)

        write = item.keywords._markers['no_parallel'].kwargs.get('write', True)
        NoParallelLock.LOCK = utils.ReadWriteLock(lock_file_name, write)
        # a-vasin: меряем время, понадобившееся на взятие блокировки
        utils.measure_time(_test_name=item.nodeid)(NoParallelLock.LOCK.lock)()

    @staticmethod
    def pytest_runtest_teardown(item, nextitem):
        if NoParallelLock.LOCK:
            NoParallelLock.LOCK.unlock()
            NoParallelLock.LOCK = None


class XFailChecker(object):
    @staticmethod
    def pytest_runtest_setup(item):
        xfail_predicates = getattr(item.module, "XFAIL_PREDICATES", [])
        for predicate in xfail_predicates:
            if predicate.test_name is not None and predicate.test_name not in item.name:
                continue

            attr = item.callspec.params[predicate.attr_name]
            if attr is None:
                continue

            if predicate.predicate(attr):
                pytest.xfail(predicate.reason)


class TestFilter(object):
    ENVIRONMENT_PARAMETERS_MAP = {
        "--env_balance": "balance.branch",
        "--env_simpleapi": "simpleapi.custom",
        "--env_apikeys": "apikeys.custom",
        "--env_balalayka": "balalayka.custom"
    }

    PT_SKIPPED_FEATURES = [Features.OEBS, Features.TRUST, Features.INVOICE_PRINT_FORM]
    BRANCH_SKIPPED_FEATURES = [Features.CONTRACT_PRINT_FORM]

    @staticmethod
    def pytest_addoption(parser):
        for custom_env_key in TestFilter.ENVIRONMENT_PARAMETERS_MAP:
            parser.addoption(custom_env_key, action="store", metavar=custom_env_key.upper(),
                             help="environment custom settings")

        parser.addoption("--features", action="store", metavar="FEATURE")
        parser.addoption("--ignored_features", action="store", metavar="IGNORED_FEATURE")

    @staticmethod
    def pytest_configure(config):
        for custom_env_key in TestFilter.ENVIRONMENT_PARAMETERS_MAP:
            env_settings = config.getoption(custom_env_key)
            if env_settings is not None:
                os.environ[TestFilter.ENVIRONMENT_PARAMETERS_MAP[custom_env_key]] = env_settings

        config.addinivalue_line("markers", "features: ")
        config.addinivalue_line("markers", "ignored_features: ")

    @staticmethod
    def pytest_runtest_setup(item):
        test_features = get_test_features_set(item)

        from _pytest.mark import MarkInfo
        reporter.log('[DEBUG] TestFilter: pytest_runtest_setup: marks of function: {}'.format([name for name, ob in vars(item.function).items() if isinstance(ob, MarkInfo)]))
        reporter.log('[DEBUG] TestFilter: pytest_runtest_setup: test_features: {} for item {}'.format(test_features, item.nodeid))
        reporter.log('[DEBUG] TestFilter: pytest_runtest_setup: env.balance_env().name: {}'.format(env.balance_env().name if env.balance_env().name else
                                                                                           ''))
        # Disable all tests with specified allure.feature on some host
        if env.balance_env().name and env.balance_env().name in [env.BalanceHosts.PT.value,
                                                                 env.BalanceHosts.PTY.value,
                                                                 env.BalanceHosts.PTA.value]:
            if set(test_features) & set(TestFilter.PT_SKIPPED_FEATURES):
                message = "Features {} are skipped on PT, has {}".format(
                    TestFilter.PT_SKIPPED_FEATURES,
                    list(set(test_features) & set(TestFilter.PT_SKIPPED_FEATURES)))
                reporter.log(message)
                pytest.skip(message)

        # Disable current item on some host
        if env.balance_env().name:
            ignored_marker = item.get_marker("ignore_hosts")
            ignored_hosts = [x.value for x in ignored_marker.args] if ignored_marker else []
            if env.balance_env().name in ignored_hosts:
                message = "Test marked to skip on environments: {0}".format(ignored_hosts)
                reporter.log(message)
                pytest.skip(message)

        upper_test_features = set([x.upper() for x in test_features])

        features = item.config.getoption("--features")
        if features:
            desired_features = set([x.strip().upper() for x in features.split(',')])
            if not upper_test_features & desired_features:
                message = "Test has no one of the following features: {0}".format(desired_features)
                reporter.log(message)
                pytest.skip(message)

        ignored_features = item.config.getoption("--ignored_features")
        if ignored_features:
            ignored_features = set([x.strip().upper() for x in ignored_features.split(',')])
            if upper_test_features & ignored_features:
                message = "Skip test due to {0} features".format(upper_test_features & ignored_features)
                reporter.log(message)
                pytest.skip(message)


class MetricsCollector(object):
    METRIC_REPORT = {'total': 0, 'passed': 0, 'failed': 0, 'skipped': 0}

    SOURCE_MAPPING = {'Billing_Autotesting_PythonTests_RunTests': 'balance_qa_metrics_autostatus',
                      'Billing_Trust_Tests_Ts_GreenLineBs': 'trust_qa_metrics_autostatus_greenline_bs',
                      'Billing_Autotesting_Apikeys_Tests': 'apikeys_qa_metric_autostatus_full',
                      'Billing_Autotesting_Apikeys_NightRegressionNewDoNotRunManually': 'apikeys_qa_metric_new_autostatus_full',
                      'Billing_Trust_Tests_PaymentsBo': 'trust_qa_metrics_autostatus_payments_bo',
                      'Billing_Trust_Tests_WebTests': 'trust_qa_metrics_autostatus_payments_bs',
                      'Billing_Trust_Tests_Ts_PaymentsBsPostgres_2': 'trust_qa_metrics_autostatus_payments_bs_postgres',
                      'Billing_Trust_Tests_RegistersTests': 'trust_qa_metrics_autostatus_registers'
                      }

    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--publish_results", action="store_true", help="sent run results to Graphite.")
        parser.addoption("--teamcity_buildtype_id", help="teamcity_buildtype_id.")

    @staticmethod
    def pytest_configure(config):
        config.addinivalue_line("markers", "publish_results: sent run results to graphite")
        config.addinivalue_line("markers", "teamcity_buildtype_id: teamcity_buildtype_id")

    @staticmethod
    def pytest_report_teststatus(report):

        if report.when == 'setup':
            MetricsCollector.METRIC_REPORT['total'] += 1

            # Тесты отмеченные как skipif помечаются skipped на шаге setup. call для них не выполняется
            if report.skipped:
                MetricsCollector.METRIC_REPORT['skipped'] += 1

        if report.when == 'call':
            if report.passed:
                MetricsCollector.METRIC_REPORT['passed'] += 1
            elif report.failed:
                MetricsCollector.METRIC_REPORT['failed'] += 1
            elif report.skipped:
                MetricsCollector.METRIC_REPORT['skipped'] += 1

    @staticmethod
    def pytest_sessionfinish(session, exitstatus):
        # send stats to graphite
        need_publish_result = session.config.getoption("--publish_results")
        teamcity_buildtype_id = session.config.getoption("--teamcity_buildtype_id")
        of = session.config.getoption("--only_failed")
        shared_state = shared.get_stage(session.config)

        # Отправляем статистику только для запусков с параметром --publish_results
        if need_publish_result:
            reporter.log('[DEBUG]: Send data to graphit with params:')
            reporter.log('\t\t- publish_result: {0}'.format(need_publish_result))
            reporter.log('\t\t- teamcity_buildtype_id: {0}'.format(teamcity_buildtype_id))
            reporter.log('\t\t- session_markexpr: {0}'.format(session.config.option.markexpr))
            reporter.log('\t\t- only_failed: {0}'.format(of))
            reporter.log('\t\t- shared_state: {0}'.format(shared_state))

            # Send metric for Full profile:
            # Отправляем статистику только если:
            # - это НЕ перезапуск упавших тестов
            # - это запуск в режиме shared на стадии AFTER ИЛИ запуск НЕ в режиме shared
            if of in [None, ''] and shared_state in [shared.AFTER, shared.NO_STAGE, None, '']:

                # source = 'balance_qa_metrics_autostatus'

                source = MetricsCollector.SOURCE_MAPPING[teamcity_buildtype_id]

                # Use now() instead of utcnow(): with utcnow() results will be send to previous day
                time_point = int(time.mktime(datetime.datetime.now().date().timetuple()))

                for key in MetricsCollector.METRIC_REPORT:
                    name = key
                    value = MetricsCollector.METRIC_REPORT[key]

                    utils.GraphitSender.send(source, name, value, time_point)

                # Send metric_trust_python
                with reporter.reporting(level=reporter.Level.MANUAL_ONLY):
                    from utility_scripts.metrics import metric_trust_python
                    metric_trust_python.do()

                # Send metric_tails
                with reporter.reporting(level=reporter.Level.MANUAL_ONLY):
                    from utility_scripts.metrics import metric_tails
                    metric_tails.do()


@pytest.fixture()
def switch_to_trust():
    def _switch(service=None, dbname=None, xmlrpc_url=None):
        env.SimpleapiEnvironment.switch_param(**utils.remove_empty(dict(service=service, dbname=dbname,
                                                                        xmlrpc_url=xmlrpc_url)))

    yield _switch
    # igogor в конце теста возвращаем обратно на оракл
    env.SimpleapiEnvironment.switch_param()


@pytest.fixture()
def switch_to_pg():
    env.SimpleapiEnvironment.switch_param(dbname=env.TrustDbNames.BS_PG, xmlrpc_url=env.TrustApiUrls.XMLRPC_PG)
    yield
    # igogor в конце теста возвращаем обратно на оракл
    env.SimpleapiEnvironment.switch_param()


class DocsLinker(object):
    BASE_URL = 'https://github.yandex-team.ru/Billing/balance-tests/blob/master/'

    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--docs", action="store_true", help="Wiki integration.")

    @staticmethod
    def pytest_configure(config):
        config.addinivalue_line("markers", "docs: doc integration")

    @staticmethod
    def link_tests_with_wiki(items):
        test_list = [
            {'item': item.nodeid,
             'docs': item.keywords._markers.get('docs'),
             'docpath': item.keywords._markers.get('docpath'),
             'docstring': item.function.__doc__,
             'link': DocsLinker.BASE_URL + '/'.join(item.module.__name__.split('.')) + '.py'
             }
            for item in items if 'docs' in item.keywords._markers]

        if test_list:
            result_dict = collections.defaultdict(tuple)
            for mark in test_list:
                wm.append_to_result_dict(wm(mark), result_dict)
            new_result_dict = {}
            total_count = 0
            for url in result_dict:
                total_count = len(result_dict[url])
                url_data = collections.Counter(result_dict[url])
                new_result_dict[url] = [
                    {'test': item[0].test + (' !!(grey)[tests: {0}]!!'.format(item[1]) if item[1] > 1
                                             else ''),
                     'text': list(item[0].text),
                     'docstring': item[0].docstring,
                     'link': item[0].link
                     } for item in url_data.items()]
                # total_count += sum([item[1] for item in url_data.items()])
            wm.main(new_result_dict, total_count)

    @staticmethod
    def pytest_collection_modifyitems(session, config, items):
        if config.getoption("--docs"):
            DocsLinker.link_tests_with_wiki(items)


class CacheInvalidator(object):
    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--invalidate_cache", action="store_true", help="invalidate all cached data.")

    @staticmethod
    def pytest_configure(config):
        config.addinivalue_line("markers", "invalidate_cache: invalidate all cached data")


# останавливаем разборщик оебс на время выполнения тестов, чтобы не было конфликтов TESTBALANCE-1493
# upd: решили пока не использовать
class OebsProcessorHandler(object):
    OEBS_PROCESSOR = 'oebs-processor'
    is_state_changed = False

    @staticmethod
    def pytest_collection_modifyitems(items):
        tests_features = set.union(*[get_test_features_set(item) for item in items])
        if Features.OEBS in tests_features:
            try:
                if not CommonSteps.is_pycron_task_terminated(task_name=OebsProcessorHandler.OEBS_PROCESSOR):
                    CommonSteps.change_pycron_task_terminate_flag(task_name=OebsProcessorHandler.OEBS_PROCESSOR,
                                                                  terminate_flag=1)
                    OebsProcessorHandler.is_state_changed = True
            except Exception as exc:
                log.error('Exception while terminating {}: {}'.format(OebsProcessorHandler.OEBS_PROCESSOR, exc))

    @staticmethod
    def pytest_sessionfinish():
        if OebsProcessorHandler.is_state_changed:
            CommonSteps.change_pycron_task_terminate_flag(task_name=OebsProcessorHandler.OEBS_PROCESSOR,
                                                          terminate_flag=0)


class ParametrizeIdMaker(object):
    @staticmethod
    def pytest_generate_tests(metafunc):
        if not hasattr(metafunc.function, 'parametrize'):
            return

        params_sets = []
        for mark in metafunc.function.parametrize._marks:
            args, kwargs = mark.args, mark.kwargs
            if callable(kwargs.get('ids', None)) and utils.arguments_count(kwargs['ids']) > 1:
                ids_func = kwargs['ids']
                if utils.arguments_count(ids_func) != len(args[0].split(', ')):
                    raise utils.TestsError('Parametrize ids function has wrong number of arguments ({})'
                                           ' for set: {}'.format(utils.arguments_count(ids_func), args[0]))
                kwargs['ids'] = []
                for params in args[1]:
                    if isinstance(params, pytest.mark.Any().__class__):
                        params, _ = utils.Pytest._unwrap(params)
                    if not isinstance(params, collections.Iterable):
                        params = [params]
                    kwargs['ids'].append(ids_func(*params))

                if not all([isinstance(id_val, str) for id_val in kwargs['ids']]):
                    raise utils.TestsError('Parametrize ids function does not return str for set: {}'.format(args[0]))
            params_sets.append((args, kwargs))

        ordered_args = list(metafunc.fixturenames)
        if all([args[0] in ', '.join(ordered_args) for args, kwargs in params_sets]):
            params_sets = sorted(params_sets, key=lambda params: ordered_args.index(params[0][0].split(', ')[0]))

        delattr(metafunc.function, 'parametrize')
        for args, kwargs in params_sets:
            pytest.mark.parametrize(*args, **kwargs)(metafunc.function)
            # todo-igogor это не работает хотя должно. Возможно у нас почему-то хук вызывается несколько раз.
            # metafunc.parametrize(*args, **kwargs)


class MutedTestsHandler(object):
    MUTED_TESTS = None

    @staticmethod
    def get_muted_tests():
        from simpleapi.common.utils import call_http
        project_name = balance_config.TEAMCITY_PROJECT_NAME or 'BALANCE'
        url = 'https://teamcity.yandex-team.ru/app/rest/testOccurrences?' \
              'locator=count:1000000,currentlyMuted:true,' \
              'affectedProject:(name:{})'.format(project_name)

        with reporter.reporting(level=reporter.Level.NOTHING):
            xml_response = call_http(url, method='GET', auth_user=Users.TESTUSER_BALANCE1)

        muted_tests = set(
            [elem.attrib['name'] for elem in ElementTree.fromstring(xml_response).findall('testOccurrence')])
        reporter.log("Muted tests:\n{}".format(utils.Presenter.pretty(muted_tests)))

        return muted_tests

    @staticmethod
    def check_item(item):
        full_module = item.module.__name__
        name = item.location[2].replace('.', '_').replace('/', '.')  # a-vasin: <3 Teamcity

        for module in reversed(full_module.split('.')):
            name = module + '.' + name
            if name in MutedTestsHandler.MUTED_TESTS:
                return True

        return False

    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--skip_muted", action="store_true", help="skip tests muted in Teamcity")

    @staticmethod
    def pytest_configure(config):
        if not utils.is_local_launch() and (config.getoption("--skip_muted") or config.getoption("--only_muted")):
            MutedTestsHandler.MUTED_TESTS = MutedTestsHandler.get_muted_tests()

    @staticmethod
    def pytest_runtest_setup(item):
        if not item.config.getoption("--only_muted") \
                and MutedTestsHandler.MUTED_TESTS is not None \
                and MutedTestsHandler.check_item(item):
            pytest.skip("Skip muted test")


class TestsRuntimeSorter(object):
    S3_PREFIX = 'runtime_{}'

    BUILDS_NUMBER = 10
    SKIPPED_PERCENT_THRESHOLD = 0.5

    TESTS_RUNTIMES = None

    @staticmethod
    def get_tests_runtimes(build_number):
        builds_counter = 0
        tests_stat = collections.defaultdict(int)

        while build_number > 0 and builds_counter < TestsRuntimeSorter.BUILDS_NUMBER:
            build_number -= 1

            loaded_stats = load_value(utils.s3storage_stats(), TestStatsCollector.S3_PREFIX, build_number)

            if loaded_stats is None \
                    or loaded_stats['skipped'] >= TestsRuntimeSorter.SKIPPED_PERCENT_THRESHOLD * loaded_stats['total']:
                continue

            for name, stats in loaded_stats['tests'].iteritems():
                tests_stat[name] += stats['duration']

                if 'runtime' in stats and 'lock' in stats['runtime']:
                    tests_stat[name] -= sum(stats['runtime']['lock'])

            builds_counter += 1

        return tests_stat

    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--sort_tests", action="store_true", help="sort tests execution order based on their runtime")

    @staticmethod
    def pytest_configure(config):
        TestsRuntimeSorter.S3_PREFIX = TestsRuntimeSorter.S3_PREFIX.format(shared.get_stage(config))

        if not config.getoption("--sort_tests") or not utils.is_master_node(config) or utils.is_local_launch() \
                or shared.get_stage(config) == shared.BLOCK:
            return

        TestsRuntimeSorter.TESTS_RUNTIMES = load_value(utils.s3storage_stats(), TestsRuntimeSorter.S3_PREFIX)

        # reporter.log('[DEBUG]: TestsRuntimeSorter: pytest_configure: {}'.format(TestsRuntimeSorter.S3_PREFIX))
        # reporter.log('[DEBUG]: TestsRuntimeSorter: pytest_configure: initial TESTS_RUNTIMES is {}'.format(TestsRuntimeSorter.TESTS_RUNTIMES))

        if TestsRuntimeSorter.TESTS_RUNTIMES is None:
            TestsRuntimeSorter.TESTS_RUNTIMES = TestsRuntimeSorter.get_tests_runtimes(int(balance_config.BUILD_NUMBER))
            # reporter.log('[DEBUG]: TestsRuntimeSorter: pytest_configure: second TESTS_RUNTIMES is {}'.format(
            #     TestsRuntimeSorter.TESTS_RUNTIMES))
            save_value(utils.s3storage_stats(), TestsRuntimeSorter.S3_PREFIX, TestsRuntimeSorter.TESTS_RUNTIMES)

    @staticmethod
    def pytest_collection_modifyitems(session, config, items):
        if not config.getoption("--sort_tests") or utils.is_local_launch() \
                or shared.get_stage(session.config) == shared.BLOCK:
            return

        if TestsRuntimeSorter.TESTS_RUNTIMES is None:
            TestsRuntimeSorter.TESTS_RUNTIMES = load_value(utils.s3storage_stats(), TestsRuntimeSorter.S3_PREFIX)
        TestsRuntimeSorter.TESTS_RUNTIMES = collections.defaultdict(lambda: sys.maxint,
                                                                    TestsRuntimeSorter.TESTS_RUNTIMES)

        items.sort(key=lambda item: TestsRuntimeSorter.TESTS_RUNTIMES[item.nodeid], reverse=True)


class MutedTestsRunner(object):
    @staticmethod
    def get_mutes():
        from simpleapi.common.utils import call_http
        project_name = "BALANCE"  # balance_config.TEAMCITY_PROJECT_NAME
        url = 'https://teamcity.yandex-team.ru/app/rest/mutes?' \
              'locator=affectedProject:(name:{})'.format(project_name)

        xml_response = call_http(url, method='GET', auth_user=Users.TESTUSER_BALANCE1)

        return ElementTree.fromstring(xml_response.encode('utf-8'))

    @staticmethod
    def delete_mute(mute_id):
        from simpleapi.common.utils import call_http
        url = 'https://teamcity.yandex-team.ru/app/rest/mutes/id:{}'.format(mute_id)
        call_http(url, method='DELETE', auth_user=Users.TESTUSER_BALANCE1)

    @staticmethod
    def make_mute(mute_xml):
        from simpleapi.common.utils import call_http
        url = 'https://teamcity.yandex-team.ru/app/rest/mutes'
        headers = {'Content-Type': 'application/xml'}
        call_http(url, headers=headers, params=mute_xml, auth_user=Users.TESTUSER_BALANCE1)

    @staticmethod
    def unmute_tests(muted_tests):
        mutes = MutedTestsRunner.get_mutes()
        for mute in mutes:
            tests = mute.find('.//tests')

            unmuted_tests = [test for test in tests if test.get("name") in muted_tests]
            if not unmuted_tests:
                continue

            MutedTestsRunner.delete_mute(mute.get("id"))
            if len(unmuted_tests) == int(tests.get("count")):
                continue

            mute.attrib = {}
            tests.set("count", str(int(tests.get("count")) - len(unmuted_tests)))
            map(lambda test: tests.remove(test), unmuted_tests)
            MutedTestsRunner.make_mute(ElementTree.tostring(mute))

    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--only_muted", action="store_true", help="run only muted tests")

    @staticmethod
    def pytest_runtest_setup(item):
        if item.config.getoption("--only_muted") \
                and MutedTestsHandler.MUTED_TESTS is not None \
                and not MutedTestsHandler.check_item(item):
            pytest.skip("Skip not muted test")

    @staticmethod
    def pytest_sessionfinish(session, exitstatus):
        if session.config.getoption("--only_muted") \
                and utils.is_master_node(session.config) \
                and shared.get_stage(session.config) in [shared.AFTER, shared.NO_STAGE]:
            muted_tests = MutedTestsHandler.MUTED_TESTS
            to_be_unmuted = [name.replace(".py::", ".").replace("/", '.')
                             for name, stats in TestStatsCollector.TESTS_STATS['tests'].iteritems()
                             if stats['passed']]
            filtered_muted_tests = [muted_test for muted_test in muted_tests
                                    if any(test in muted_test for test in to_be_unmuted)]
            MutedTestsRunner.unmute_tests(filtered_muted_tests)


class FakeTrustActivator(object):
    @staticmethod
    def pytest_addoption(parser):
        parser.addoption("--mock_trust", action="store_true",
                         help="enable trust mocking where it is possible")

    @staticmethod
    def mock_simple_api(pytestconfig, monkeypatch):
        if not pytestconfig.getoption('mock_trust') and not balance_config.USE_TRUST_MOCKS:
            return
        FakeTrustActivator.apply_mock_simple_api(monkeypatch)

    @staticmethod
    def apply_mock_simple_api(monkeypatch):
        fake_trust = FakeTrustApi()
        monkeypatch.setattr(SimpleApi, 'create_trust_payment', fake_trust.create_trust_payment)
        monkeypatch.setattr(SimpleApi, 'create_refund', fake_trust.create_refund)
        monkeypatch.setattr(SimpleApi, 'postauthorize', fake_trust.postauthorize)
        monkeypatch.setattr(SimpleApi, 'wait_for_export_from_bs', fake_trust.none)
        monkeypatch.setattr(SimpleApi, 'create_multiple_trust_payments', fake_trust.create_multiple_trust_payments)
        monkeypatch.setattr(SimpleApi, 'create_multiple_tickets_payment', fake_trust.create_multiple_tickets_payment)
        monkeypatch.setattr(SimpleApi, 'create_multiple_refunds', fake_trust.create_multiple_refunds)
        monkeypatch.setattr(SimpleApi, 'get_multiple_promocode_payment_ids_by_composite_tag',
                            fake_trust.get_multiple_promocode_payment_ids_by_composite_tag)
        monkeypatch.setattr(simpleapi_steps, 'create_service_product', fake_trust.create_service_product)
        monkeypatch.setattr(simpleapi_steps, 'create_partner', fake_trust.create_partner)
        monkeypatch.setattr(simpleapi_steps, 'process_promocode_creating', fake_trust.process_promocode_creating)
        monkeypatch.setattr(SimpleNewApi, 'create_payment', fake_trust.new_create_payment)
        monkeypatch.setattr(SimpleNewApi, 'create_topup_payment', fake_trust.new_create_topup_payment)
        monkeypatch.setattr(SimpleNewApi, 'create_refund', fake_trust.new_create_refund)
        monkeypatch.setattr(SimpleNewApi, 'create_account_refund', fake_trust.new_create_refund)
        monkeypatch.setattr(SimpleNewApi, 'unhold_payment', fake_trust.new_unhold_payment)
        monkeypatch.setattr(SimpleNewApi, 'resize_multiple_orders', fake_trust.new_resize_multiple_orders)
        monkeypatch.setattr(SimpleNewApi, 'create_multiple_orders_for_payment',
                            fake_trust.new_create_multiple_orders_for_payment)
        monkeypatch.setattr(SimpleNewApi, 'clear_payment', fake_trust.none)


class ErrorCategories(object):
    REGEX_MAP = {}

    @staticmethod
    def pytest_configure(config):
        from btestlib.error_categories import COMMON_CATEGORY_TO_ERROR_REGEX_MAP

        if not ErrorCategories.good_time_to_work(config):
            return
        ErrorCategories.REGEX_MAP = COMMON_CATEGORY_TO_ERROR_REGEX_MAP

    @staticmethod
    def pytest_runtest_makereport(item, call):
        """
         Сами определяем категорию ошибки и выводим ее в конце теста
        """
        if not ErrorCategories.good_time_to_work(item.config):
            return
        if call.excinfo:
            error_categories = ErrorCategories.get_error_categories(call.excinfo)
            reporter.log('Error categories: {}'.format(error_categories))

    @staticmethod
    def pytest_sessionfinish(session, exitstatus):
        """
         Генерируем файл с регекспами, который будет использоваться аллюром для определения категорий ошибок
        """
        if not ErrorCategories.good_time_to_work(session.config):
            return
        allure_results_dir = ReportHelper.report_dir(session.config)
        if allure_results_dir:
            ErrorCategories.generate_categories_json(allure_results_dir)

    @staticmethod
    def good_time_to_work(config):
        shared_state = shared.get_stage(config)
        return not utils.is_local_launch() \
               and ReportHelper.report_dir(config) \
               and utils.is_master_node(config) \
               and shared_state in [shared.AFTER, shared.NO_STAGE, None, '']

    @staticmethod
    def get_error_categories(excinfo):
        from btestlib.error_categories import ErrorCategory

        error_msg = u'{}: {}'.format(excinfo.typename, excinfo.value)
        categories = []

        # пока у нас не задано ни одного trace_regex, поэтому определяем категорию только по message_regex
        for category, regexes_list in ErrorCategories.REGEX_MAP.iteritems():
            if any(re.match(regexes.message_regex, error_msg) for regexes in regexes_list):
                categories.append(category)

        return categories if categories else [ErrorCategory.PRODUCT_DEFECTS]

    @staticmethod
    def generate_categories_json(file_dir):
        categories = ErrorCategories.to_allure_format(ErrorCategories.REGEX_MAP)

        file_name = 'categories.json'
        file_path = os.path.join(os.getcwd(), file_dir, file_name)
        reporter.log('Save error categories to {}'.format(file_path))
        with io.open(file_path, 'w', encoding='utf8') as json_file:
            # в json.dump() бывают проблемы с unicode, поэтому делаем так
            data = json.dumps(categories, indent=4, ensure_ascii=False)
            json_file.write(unicode(data))

    # названия полей в итоговом json должны быть как здесь
    # https://github.com/allure-framework/allure2/blob/master/allure-generator/src/main/java/io/qameta/allure/category/Category.java
    @staticmethod
    def to_allure_format(categories_dict):
        return [utils.remove_empty(
            {'name': category,
             'messageRegex': err_regexes.message_regex,
             'traceRegex': err_regexes.trace_regex,
             }) for category, regex_list in categories_dict.iteritems()
            for err_regexes in regex_list]


class AllureEnvironmentAttacher(object):
    PACKAGES = ['yb-tools', 'yb-vhost-xs5']

    @staticmethod
    def get_packages_info():
        from simpleapi.common.utils import call_http
        url = 'https://c.yandex-team.ru/api/packages_on_host/greed-{}1h.paysys.yandex.net'.format(
            env.balance_env().name)
        response = call_http(url, method='GET')

        if response == 'No host found':
            return {}

        rows = [row.split('\t') for row in response.split('\n')]
        return {'Package ' + row[0]: row[2] for row in rows if row[0] in AllureEnvironmentAttacher.PACKAGES}

    @staticmethod
    def get_environments_names():
        functions = inspect.getmembers(env, inspect.isfunction)
        return {'Environment ' + name: environment().name for name, environment in functions if 'env' in name}

    @staticmethod
    def get_stages_runtimes():
        # a-vasin: я не несу ответственность за следующую строку =)
        stages = [shared.BEFORE, shared.BLOCK, shared.AFTER, shared.NO_STAGE]
        stats = utils.remove_empty(
            {stage: load_value(utils.s3storage_stats(), 'stats_{}'.format(stage)) for stage in stages})
        return {'Stage {} duration'.format(stage):
                    str(stage_stats['duration'] / 60) + ':' + str(stage_stats['duration'] % 60)
                for stage, stage_stats in stats.iteritems()}

    @staticmethod
    def pytest_sessionfinish(session, exitstatus):
        if utils.is_local_launch() \
                or not ReportHelper.report_dir(session.config) \
                or not utils.is_master_node(session.config) \
                or shared.get_stage(session.config) not in [shared.AFTER, shared.NO_STAGE]:
            return
        reporter.environment(**AllureEnvironmentAttacher.get_stages_runtimes())
        reporter.environment(**AllureEnvironmentAttacher.get_environments_names())
        reporter.environment(**AllureEnvironmentAttacher.get_packages_info())


class HostIndicator(object):
    @staticmethod
    def pytest_sessionstart(session, cur_env):
        if utils.is_master_node(session.config):
            cur_env.log_env_name()


class WaitersHandler(object):
    @staticmethod
    def pytest_configure(config):
        if not utils.is_local_launch() and \
                not os.path.exists(utils.WaiterHolder.WAITER_DIR):
            os.makedirs(utils.WaiterHolder.WAITER_DIR)

    @staticmethod
    def pytest_unconfigure(config):
        if utils.is_master_node(config) and \
                not utils.is_local_launch() and \
                os.path.exists(utils.WaiterHolder.WAITER_DIR):
            shutil.rmtree(utils.WaiterHolder.WAITER_DIR)


class ReportHelper(object):
    @staticmethod
    def pytest_runtest_makereport(item, call):
        if call.excinfo:
            ReportHelper.attach_ticket_creation_links(item, call)

    @staticmethod
    def pytest_runtest_logreport(report):
        ReportHelper.attach_test_logs(report)

    @staticmethod
    def pytest_unconfigure(config):
        if utils.is_master_node(config) and shared.get_stage(config) in [shared.AFTER, shared.NO_STAGE] and \
                ReportHelper.report_dir(config):
            reporter._write_environment(ReportHelper.report_dir(config))

    @staticmethod
    def report_dir(config):
        return config.getoption("--alluredir")

    @staticmethod
    def attach_test_logs(report):
        [reporter.attach(name, contents, log_=False) for (name, contents) in dict(report.sections).items()]

    @staticmethod
    def attach_ticket_creation_links(item, call):
        test = u'Тест: ' + item.nodeid

        # error = u'Ошибка: \n' + allure.utils.present_exception(call.excinfo.value)
        # error = u'Ошибка: \n' + allure_utils.format_exception(call.excinfo.type, call.excinfo.value)
        try:
            error = u'Ошибка: \n' + unicode(call.excinfo.value)
        except Exception:
            error = u'Ошибку получить не удалось, обратитесь к igogor@'
        build_url = u'https://teamcity.yandex-team.ru/viewLog.html?buildId={build_id}' \
                    u'&buildTypeId={project_id}'.format(build_id=balance_config.BUILD_NUMBER,
                                                        project_id=balance_config.TEAMCITY_PROJECT_NAME)
        description = u'{test}\n\n{error}\n\n(({build_url} Ссылка на запуск))\n'.format(
            test=test, error=error, build_url=build_url)

        description = urllib.quote(description.encode('utf-8'))

        st_balance_url = (
            u'Создать тикет в BALANCE',
            u"https://st.yandex-team.ru/createTicket?summary=&description={}&type=1&priority=2" \
            u"&followers=&bugDetectionMethod=Autotests&internalDesign=%D0%9D%D0%B5%D1%82&stage=Testing" \
            u"&testScope=%D0%94%D0%B0&queue=BALANCE&tags=python_regression".format(description))
        st_testbalance_url = (
            u'Создать тикет в TESTBALANCE',
            u"https://st.yandex-team.ru/createTicket?summary=&description={}&type=1&assignee=igogor" \
            u"&priority=2&tags=processed&tags=autotests_errors&queue=TESTBALANCE".format(
                description))

        # если ссылки получаются очень длинные, то может быть вместо них можно делать post-запрос в стартрек?
        reporter.report_urls(u'Ссылки на создание тикетов', st_balance_url, st_testbalance_url)

    @staticmethod
    def pytest_unconfigure(config):
        if not utils.is_local_launch() \
                and utils.is_master_node(config) \
                and shared.get_stage(config) in [shared.AFTER, shared.NO_STAGE] \
                and ReportHelper.report_dir(config):
            reporter._write_environment(ReportHelper.report_dir(config))


class SecretsManager(object):
    @staticmethod
    def pytest_unconfigure(config):
        if utils.is_master_node(config) and shared.get_stage(config) in [shared.NO_STAGE, shared.AFTER]:
            secrets.delete_all_secret_files()
