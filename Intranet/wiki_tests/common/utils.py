import functools
import mimetypes
from dataclasses import asdict

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.cache import caches
from django.test.utils import override_settings
from pretend import call

from intranet.wiki.tests.wiki_tests.common.ddf_compat import model_has_the_field
from wiki.utils.features.get_features import (
    clear_override_wiki_features,
    override_wiki_features as do_override_wiki_features,
)
from wiki.utils.features.models import WikiFeatures

User = get_user_model()


def override_wiki_features(**patched_settings_dict):
    def decorator(fn):
        @functools.wraps(fn)
        def wrapped_fn(*args, **kwargs):
            old_settings = settings.FEATURES
            settings_dict = asdict(old_settings)
            settings_dict.update(patched_settings_dict)

            try:
                do_override_wiki_features(WikiFeatures(**settings_dict))
                return fn(*args, **kwargs)
            finally:
                clear_override_wiki_features()

        return wrapped_fn

    return decorator


def only_model_fields(dict_like, model_class):
    filtered_kwargs = {}
    for field_name, val in list(dict_like.items()):
        if model_has_the_field(model_class, field_name):
            filtered_kwargs[field_name] = val
    return filtered_kwargs


def celery_eager(class_or_method=None):
    decorator = override_settings(
        CELERY_TASK_ALWAYS_EAGER=True, CELERY_ALWAYS_EAGER=True, CELERY_TASK_EAGER_PROPAGATES_EXCEPTIONS=True
    )
    if class_or_method is None:
        return decorator
    return decorator(class_or_method)


def no_celery(class_or_method=None):
    decorator = override_settings(CELERY_TASK_ALWAYS_EAGER=False, CELERY_ALWAYS_EAGER=False)
    if class_or_method is None:
        return decorator
    return decorator(class_or_method)


def locmemcache(*cache_names):
    """
    Декоратор для тесткейсов, позволяющий на время прогона теста подменить
    бекенд указанного кеша на LocMemCache.
    """

    def deco(cls):
        test_caches = settings.CACHES.copy()
        for cache_name in cache_names:
            test_caches[cache_name] = {
                'BACKEND': 'django.core.cache.backends.locmem.LocMemCache',
                'LOCATION': cache_name,
            }

        # В конце каждого теста очищать кеш
        original_tearDown = getattr(cls, 'tearDown', None)

        def tearDown(self):
            for cache_name in cache_names:
                cache = caches[cache_name]
                cache.clear()
            if original_tearDown:
                original_tearDown(self)

        cls.tearDown = tearDown

        return override_settings(CACHES=test_caches)(cls)

    return deco


def unexpected_call(*args):
    raise Exception('unexpected invocation (%s)' % (args,))


class CallRecorder(object):
    """
    Класс для использования с моками. Записывает вызовы функции (*args и **kwargs) для последующей проверки тестом.
    Свою функцию, которая будет вызываться можно передать в конструктор. Если не передать функцию, будет использоваться
    пустая функция. Но функцию для замещения реальной функции нужно в любом случае взять из метода get_func.

    Вызовы записываются в переменную класса calls – список объектов класса pretend.call.

    Пример 1. Просто проверяем, что был вызов.
        delete_patch = CallRecorder()
        my_real_obj = stub(delete=delete_patch.get_func())
        ...
        self.assertTrue(delete_patch.is_called)

    Пример 2. Проверяем, что было 2 вызова и проверяем их аргументы.
        get_some_patch = CallRecorder(lambda *args, **kwargs: my_some_obj)
        with patch('wiki.myapp.logic.some.get_some', get_some_patch.get_func()):
            ...

        self.assertTrue(get_some_patch.times, 2)

        call_1 = get_some_patch.calls[0]
        self.assertEqual(call_1.args[0], ...)
        self.assertEqual(call_1.args[1], ...)
        ...
        self.assertEqual(call_1.kwargs['...'], ...)
        self.assertEqual(call_1.kwargs['......'], ...)

        call_2 = get_some_patch.calls[1]
        так же проверяем аргументы второго вызова


    Для проверки, что фунция не вызывается ни разу, удобнее использовать готовую фунцию unexpected_call.
    """

    def __init__(self, sub_f=None):
        self.sub_f = sub_f
        self.calls = list()

    def get_func(self):
        """
        Вернуть мок-функцию для подмены реальной функции.
        """

        def f(*args, **kwargs):
            self.calls.append(call(*args, **kwargs))
            if self.sub_f is not None:
                return self.sub_f(*args, **kwargs)

        return f

    @property
    def is_called(self):
        """
        Была ли вызвана функция.

        @rtype bool
        """
        return bool(self.calls)

    @property
    def times(self):
        """
        Сколько раз была вызвана функция.

        @rtype int
        """
        return len(self.calls)


class SimpleStub(object):
    """
    Упрощённый вариант pretend.stub – без __repr__.
    С pretend.stub возникает проблема, когда стабы ссылаются друг на друга, его рекурсивный __repr__ даёт
    бесконечную рекурсию.
    """

    def __init__(self, **kwargs):
        """
        Сетит все kwargs в мемберы инстанса.
        """
        self.__dict__.update(kwargs)


def strip_wf_pos(parsed_json):
    """
    Рекурсивно обходит словарь parsed_json (результат loads(json) для json полученного из wf) и удаляет в нём и
    всех вложенных словарях ключи pos_start и pos_end.

    @type parsed_json dict
    @return None
    """
    if isinstance(parsed_json, list):
        i = 0
        while i < len(parsed_json):
            if len(parsed_json[i]) != 0:
                strip_wf_pos(parsed_json[i])
                if len(parsed_json[i]) == 0:
                    del parsed_json[i]
                    continue
            i += 1
        return

    if not isinstance(parsed_json, dict):
        return

    if 'pos_start' in parsed_json:
        del parsed_json['pos_start']
    if 'pos_end' in parsed_json:
        del parsed_json['pos_end']

    keys = list(parsed_json.keys())
    for key in keys:
        value = parsed_json[key]
        if isinstance(value, dict):
            if len(value) != 0:
                strip_wf_pos(value)
                if len(value) == 0:
                    del parsed_json[key]
        else:
            strip_wf_pos(value)


def get_content_type(filename):
    return mimetypes.guess_type(filename)[0] or 'application/octet-stream'


def encode_multipart_formdata(fields, files):
    """
    fields is a sequence of (name, value) elements for regular form fields.
    files is a sequence of (name, filename, value) elements for data to be uploaded as files
    Return (content_type, body) ready for httplib.HTTP instance
    """
    BOUNDARY = b'----------ThIs_Is_tHe_bouNdaRY_$'
    CRLF = b'\r\n'
    L = []
    for (key, value) in fields:
        key = key.encode('utf-8')
        value = value.encode('utf-8')
        L.append(b'--' + BOUNDARY)
        L.append(b'Content-Disposition: form-data; name="%s"' % key)
        L.append(b'')
        L.append(value)
    for (key, filename, value) in files:
        content_type = get_content_type(filename).encode('utf-8')
        key = key.encode('utf-8')
        filename = filename.encode('utf-8')
        if isinstance(value, str):
            value = value.encode('utf-8')
        L.append(b'--' + BOUNDARY)
        L.append(b'Content-Disposition: form-data; name="%s"; filename="%s"' % (key, filename))
        L.append(b'Content-Type: %s' % content_type)
        L.append(b'')
        L.append(value)
    L.append(b'--' + BOUNDARY + b'--')
    L.append(b'')
    body = CRLF.join(L)
    content_type = 'multipart/form-data; boundary=%s' % BOUNDARY.decode('utf-8')
    return content_type, body
