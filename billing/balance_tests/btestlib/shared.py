# coding: utf-8

__author__ = 'torvald'

import ctypes
import inspect
import json
import os
import pickle
import time

import attr

import btestlib.environments as env
import btestlib.reporter as reporter
import btestlib.utils as utils


class SkipSharedBlockBody(Exception):
    pass


class NoTestsForBlock(Exception):
    pass


class MarkTestAsPassed(Exception):
    pass


class WrongOrNoDataInCache(Exception):
    pass


# a-vasin: а это мог быть Enum или ConstantsContainer =(
BEFORE = 'before'
BLOCK = 'block'
AFTER = 'after'
NO_STAGE = 'no_shared_stage'

EXECUTED_BLOCKS = {}

STAGE_START = None

# На стадии 'block' запускаем тест игнорируя любые возможные фильтрации. Считаем, что блок всегда корректен.
SKIP_MARKERS = {'skip', 'skipif', 'xfail'}

NO_ACTIONS = 'no_actions'


def format_s3_path(item):
    current_env = env.balance_env().name
    s3_prefix = env.ENV_TO_S3_PREFIX.get(current_env, current_env)
    return '{}/{}/{}'.format(build_id(), s3_prefix, item)


def get_s3_item_url(item):
    reporter.log('>>>>>[ITEM]: {}'.format(item))
    path = format_s3_path(item)
    reporter.log('>>>>>[PATH]: {}'.format(path))
    #     return s3storage_shared().get_url(path)
    # else:
    #     return ''

    for i in range(5):
        time.sleep(3)
        try:
            if s3storage_shared().is_present(path):
                return s3storage_shared().get_url(path)
        except AssertionError:
            continue
        break

    else:
        return ''


def push_to_s3(item, data):
    try:
        path = format_s3_path(item)
        s3storage_shared().set_string_value(path, data)
    except Exception as exc:
        reporter.log('Error while save data to s3storage_cache: {}'.format(exc))


# todo-igogor методы работы с s3 надо выделить в модуль cache и переиспользовать
def push_data_to_s3(data, item):
    push_to_s3(item, pickle.dumps(data))


def get_data_from_s3(item):
    start = time.time()
    # Получаем список ключей
    try:
        path = format_s3_path(item)
        if s3storage_shared().is_present(path):
            value = pickle.loads(s3storage_shared().get_string_value(path))
            reporter.log('------- Data received from S3 in {0}'.format(time.time() - start))
        else:
            value = None
    except Exception as exc:
        reporter.log('Exception while getting cached data from S3: {}'.format(exc))
        value = None

    return value


def push_block_tests_to_s3(executed_blocks):
    s3storage_shared().set_string_value(key=build_id(), value=json.dumps(executed_blocks))


def get_block_test_from_s3():
    if not s3storage_shared().is_present(key=build_id()):
        raise utils.TestsError("SharedBlocks can't load saved block tests. Contact igogor@")
    return json.loads(s3storage_shared().get_string_value(key=build_id()))


def push_raw_data_to_s3_and_get_url(item, data):
    push_to_s3(item, data)
    return get_s3_item_url(item)


@utils.cached
def s3storage_shared():
    # return utils.S3Storage(bucket_name='balance-autotest-shared', need_logging=False, need_allure=False)
    return utils.S3Storage(bucket_name='balance-autotest-shared')


def get_stage(config):
    return config.getoption("--shared", default=NO_STAGE) or NO_STAGE


def get_block_name(item):
    mark = item.get_marker('shared')
    return mark.kwargs['block'] if mark else None


@utils.cached
def build_id():
    if utils.is_local_launch():
        return "build_local"
    else:
        return "build_{}_{}_{}".format(os.environ['TEAMCITY_PROJECT_NAME'], os.environ['TEAMCITY_BUILDCONF_NAME'],
                                       os.environ['BUILD_NUMBER'])


def pytest_addoption(parser):
    parser.addoption("--shared", metavar="SHARED", choices=[BEFORE, BLOCK, AFTER],
                     help="separate and run tests grouping by blocking action")

    global STAGE_START
    STAGE_START = time.time()


def pytest_collection_modifyitems(session, config, items, LAST_FAILED_TESTS):
    LAST_FAILED_TESTS = LAST_FAILED_TESTS or [utils.reformat_item(item.nodeid) for item in items]

    reporter.log('pytest_collection_modifyitems_LAST_FAILED_TESTS: {}'.format(len(LAST_FAILED_TESTS)))
    # reporter.log('DEEEEEEBBBBBBUUUUUG LAST_FAILED_TESTS: {}'.format(LAST_FAILED_TESTS))

    items_marked_shared = [item for item in items if get_block_name(item)]
    if get_stage(config) == BEFORE:
        selected_items = [item for item in items_marked_shared if utils.reformat_item(item.nodeid) in LAST_FAILED_TESTS]
        deselected_items = [item for item in items_marked_shared if utils.reformat_item(item.nodeid) not in LAST_FAILED_TESTS]

        config.hook.pytest_deselected(items=deselected_items)
        items[:] = selected_items

    if get_stage(config) == BLOCK:

        # Skip tests with special shared block 'NO_ACTIONS'
        items_marked_shared = [item for item in items_marked_shared if get_block_name(item) != NO_ACTIONS]

        # Temporary comment put and get data for shared blocks from S3
        used_blocks = []
        selected_items = []
        deselected_items = []

        for item in items_marked_shared:
            # На стадии 'block' запускаем тест игнорируя любые возможные фильтрации. Считаем, что блок всегда корректен.
            if utils.reformat_item(item.nodeid) in LAST_FAILED_TESTS and not (set(item.keywords.keys()) & SKIP_MARKERS):
                block = get_block_name(item)
                if block not in used_blocks:
                    used_blocks.append(block)
                    selected_items.append(item)
                else:
                    deselected_items.append(item)
            else:
                deselected_items.append(item)

        # Скрыт на случай, когда среди запущенных тестов нет ни одного Shared.
        # if not selected_items:
        #     raise NoTestsForBlock()

        config.hook.pytest_deselected(items=deselected_items)
        items[:] = selected_items

        reporter.log('DEEEEEEBBBBBBUUUUUG deselected_items: {}'.format(deselected_items))
        reporter.log('DEEEEEEBBBBBBUUUUUG selected_items: {}'.format(selected_items))

        # executed_blocks = get_block_test_from_s3().values()
        # single_item_for_block = [item for item in items_marked_shared if item.nodeid in executed_blocks]
        # session.items = single_item_for_block


def pytest_runtest_makereport(item, call):
    if call.excinfo and (call.excinfo.type is MarkTestAsPassedException):
        call.excinfo = None
        if get_stage(item.config) == BEFORE and get_block_name(item):
            EXECUTED_BLOCKS[get_block_name(item)] = item.nodeid

            # reporter.current_allure_testcase().name += '_1before'
        if get_stage(item.config) == BLOCK and get_block_name(item):
            # reporter.current_allure_testcase().name = 'shared_block_' + get_block_name(item)
            # reporter.add_feature(reporter.current_allure_testcase(), 'block_stage')
            pass

    if get_stage(item.config) == AFTER and get_block_name(item) and call.when == 'teardown':
        # reporter.current_allure_testcase().name += '_2after'
        pass

        # def pytest_sessionfinish(session, exitstatus):
        # Temporary comment put and get data for shared blocks from S3
        # if _get_stage(session.config) == BEFORE:
        #     push_block_tests_to_s3(EXECUTED_BLOCKS)
        # if _get_stage(session.config) == BLOCK:
        #     todo-igogor здесь можно добавить изменение сьютов чтобы все тесты на блок попадали в один сьют


def pytest_sessionfinish(session, exitstatus):
    stage_duration = int(time.time() - STAGE_START)
    SEC_IN_MINUTE = 60
    reporter.log('[DEBUG]:  stage duration: {}:{}'.format(stage_duration / SEC_IN_MINUTE,
                                                          stage_duration % SEC_IN_MINUTE))


def shared_data_fixture(request):
    shared_data = SharedData(stage=get_stage(request.config), item=request.node)
    if shared_data.stage == AFTER:
        with reporter.step(u'Считываем из кэша данные, подготовленные в BEFORE блоке'):
            shared_data.cache = get_data_from_s3(shared_data.item.nodeid)
    return shared_data


class SkipOptionalBlockException(Exception):
    pass


class MarkTestAsPassedException(Exception):
    pass


@attr.s
class SharedData(object):
    stage = attr.ib()
    item = attr.ib()
    cache = attr.ib(default=None)

    @property
    def block_name(self):
        return get_block_name(self.item)

    def is_cache_valid(self, cache_vars):
        return self.cache and set(self.cache) == set(cache_vars)


@attr.s
class OptionalBlock(object):
    skip = attr.ib()
    validated = attr.ib(default=False)

    def validate(self):
        self.validated = True
        if self.skip:
            raise SkipOptionalBlockException()


# Контекст-менеджер для кеширования сущностей, которые можно переиспользовать между запусками тестов.
class SharedBefore(object):
    def __init__(self, shared_data, cache_vars):
        self.shared_data = shared_data
        self.cache_vars = cache_vars
        self.optional_block = None

    @property
    def item(self):
        return self.shared_data.item

    @property
    def cache(self):
        return self.shared_data.cache

    def __enter__(self):

        # скипаем блок подготовки если на этапе BLOCK
        if self.shared_data.stage == BLOCK:
            self.optional_block = OptionalBlock(skip=True)
        # скипаем блок подготовки если на этапе AFTER и кэш валиден
        elif self.shared_data.stage == AFTER:
            if self.shared_data.is_cache_valid(self.cache_vars):
                self.optional_block = OptionalBlock(skip=True)
            else:
                raise WrongOrNoDataInCache()
        # во все остальных случаях выполняем блок подготовки
        else:
            self.optional_block = OptionalBlock(skip=False)

        # logger.LOG.debug(u'SharedBefore __enter__: {}'.format(self.optional_block))
        return self.optional_block

    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type and exc_type != SkipOptionalBlockException:
            return False  # если произошли какие-то эксепшены кроме ожидаемых выполнять дальше бессмысленно

        if not self.optional_block.validated:
            raise utils.TestsError(u'В блоке {} пропущен обязательный вызов before.validate()'.format(type(self)))

        if self.shared_data.stage == BEFORE and not self.optional_block.skip:
            with reporter.step(u'Кэшируем подготовленные данные'):
                frame = inspect.currentframe().f_back
                to_cache = {key: frame.f_locals[key] for key in self.cache_vars}
                push_data_to_s3(to_cache, self.item.nodeid)
            with reporter.step(u'Успешно завершаем выполнение BEFORE блока'):
                raise MarkTestAsPassedException()
        if self.shared_data.stage == AFTER and self.optional_block.skip:
            with reporter.step(u'Записываем в переменные данные из кэша, подготовленные в BEFORE блоке'):
                frame = inspect.currentframe().f_back
                frame.f_locals.update(self.cache)
                ctypes.pythonapi.PyFrame_LocalsToFast(ctypes.py_object(frame), ctypes.c_int(0))
                # чистим кэш
                push_data_to_s3(dict(), self.item.nodeid)

        return True


class SharedBlock(object):
    def __init__(self, shared_data, before, block_name):
        self.shared_data = shared_data
        self.before = before
        self.optional_block = None

        if self.shared_data.block_name != block_name:
            raise utils.TestsError(u'Тест помечен как shared(block="{}"), но в нем вызывается действие {}'.format(
                self.shared_data.block_name, block_name
            ))

    def __enter__(self):
        # В стадии before пропускаем тело контекст-менеджера
        # В стадии block заходим в тело контекст-менеджера (выполняем длительные операции)
        # В стадии after понимаем, были ли взяты данные из кеша:
        # - если before.skip: данные взяты из кеша, обновлять матвью ещё раз не надо
        # - если not before.skip: что-то пошло не так и данные заново создавались в стадии after, обновляем матвью.

        # скипаем блок общего действия на этапе BEFORE
        if self.shared_data.stage == BEFORE:
            self.optional_block = OptionalBlock(skip=True)
        # скипаем блок общего действия на этапе AFTER если подготовка данных не перевыполнялась
        elif self.shared_data.stage == AFTER and self.before.skip:
            self.optional_block = OptionalBlock(skip=True)
        else:
            self.optional_block = OptionalBlock(skip=False)

        # logger.LOG.debug(u'SharedBlock __enter__: {}'.format(self.optional_block))
        return self.optional_block

    def __exit__(self, exc_type, exc_val, exc_tb):
        # В стадии shared=before мы не доходим до этого кода - выходим в контекст-менеджере SharedBefore
        # В стадии shared=block после выхода из контекст-менеджера выбрасываем исключение для остановки на этом этапе
        # В стадии shared=after продолжаем выполнение после пропуска тела контекст-менеджера

        if exc_type and exc_type != SkipOptionalBlockException:
            return False  # если произошли какие-то эксепшены кроме ожидаемых выполнять дальше бессмысленно

        if not self.optional_block.validated:
            raise utils.TestsError(u'В блоке {} пропущен обязательный вызов block.validate()'.format(type(self)))

        if self.shared_data.stage == BLOCK and not self.optional_block.skip:
            with reporter.step(u'Успешно завершаем выполнение BLOCK блока'):
                raise MarkTestAsPassedException()

        return True
