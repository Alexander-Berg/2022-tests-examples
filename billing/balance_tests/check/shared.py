# coding: utf-8
__author__ = 'chihiro'
from btestlib.shared import *


def shared_data_fixture(request):
    shared_data = CheckSharedData(stage=get_stage(request.config), item=request.node)
    if shared_data.stage == AFTER:
        with reporter.step(u'Считываем из кэша данные, подготовленные в BEFORE блоке'):
            shared_data.cache = CheckCache(get_data_from_s3(shared_data.item.nodeid))
    return shared_data


class CheckCache(object):
    def __init__(self, data=None):
        self.data = data

    def __getitem__(self, key):
        if self.data and (self.data.get(key) or self.data.get(key) == []):
            return self.data[key]
        else:
            raise Exception(u'Тест упал на этапе подготовки данных - проверьте лог или перезапустите тесты')

    def get(self, key):
        if self.data and (self.data.get(key) or self.data.get(key) == []):
            return self.data[key]
        else:
            raise Exception(u'Тест упал на этапе подготовки данных - проверьте лог или перезапустите тесты')


class CheckSharedData(SharedData):
    def is_cache_valid(self, cache_vars):
        return self.cache

    def __getitem__(self, key):
        if self.cache and (self.cache.get(key) or self.cache.get(key) == []):
            return self.cache[key]
        else:
            raise Exception(u'Тест упал на этапе подготовки данных')


class CheckSharedBefore(SharedBefore):

    @property
    def cache(self):
        return self.shared_data.cache.data

    def __enter__(self):

        # скипаем блок подготовки если на этапе BLOCK
        if self.shared_data.stage == BLOCK:
            self.optional_block = OptionalBlock(skip=True)
        # скипаем блок подготовки если на этапе AFTER и кэш валиден
        elif self.shared_data.stage == AFTER:
            self.optional_block = OptionalBlock(skip=True)
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

        if self.shared_data.stage == BEFORE or self.shared_data.stage == NO_STAGE:
            with reporter.step(u'Кэшируем подготовленные данные'):
                frame = inspect.currentframe().f_back
                to_cache = {key: frame.f_locals[key] for key in self.cache_vars}
                push_data_to_s3(to_cache, self.item.nodeid)
        if self.shared_data.stage == BEFORE and not self.optional_block.skip:
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
