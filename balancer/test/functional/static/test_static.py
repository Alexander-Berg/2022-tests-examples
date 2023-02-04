# -*- coding: utf-8 -*-
import os
import stat
import time
import calendar
import email.utils

import balancer.test.plugin.context as mod_ctx

from configs import StaticDirConfig, StaticFileConfig
from balancer.test.util.predef import http
from balancer.test.util.balancer import asserts
from balancer.test.util.stdlib.multirun import Multirun


class StaticContext(object):
    def __init__(self):
        super(StaticContext, self).__init__()

        self.test_html = self.static.abs_path('test.html')
        self.test_content = self.static.read_file(self.test_html)

        self.inner_dir = self.static.abs_path('inner_dir')
        os.symlink(self.test_html, os.path.join(self.inner_dir, 'symlink'))
        self.index_content = self.static.read_file(os.path.join(self.inner_dir, 'index.html'))

        self.file_000 = self.static.abs_path('test_000.html')
        self.static.copy(self.test_html, self.file_000)
        self.static.chmod(self.file_000, 0)

        self.dir_100 = self.static.abs_path('dir_100/')
        os.mkdir(self.dir_100)
        self.static.copy(self.test_html, os.path.join(self.dir_100, 'test.html'))
        self.static.chmod(self.dir_100, stat.S_IXUSR)

        long_dir = self.static.abs_path('0/1/2/3/4/5/6/7/8/9/')
        os.makedirs(long_dir)

        long_path = os.path.join(long_dir, 'test.html')
        self.static.copy(self.test_html, long_path)
        os.mkdir(os.path.join(self.inner_dir, 'tmp/'))


static_ctx = mod_ctx.create_fixture(StaticContext)


def check_test_result(static_ctx, request, static_dir=None, static_file=None, content="", expect=200):
    if static_file:
        static_ctx.start_balancer(StaticFileConfig(static_file))
    else:
        static_ctx.start_balancer(StaticDirConfig(
            static_dir or static_ctx.static.root_dir
        ))
    response = static_ctx.perform_request(request)
    asserts.content(response, content)
    asserts.status(response, expect)

    for run in Multirun():
        with run:
            log = static_ctx.manager.fs.read_file(static_ctx.balancer.config.accesslog)
            assert '[static w:{} succ {}]'.format(len(content), expect) in log


def test_http_1_0_dir(static_ctx):
    """
    Запрос по http/1.0

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает существующий в этой директории файл

    Правильное поведение:
    Отправить содержимое файла
    """
    check_test_result(static_ctx, http.request.get(path='/test.html', version='HTTP/1.1'),
                      content=static_ctx.test_content)


def test_http_1_1_dir(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает существующий в этой директории файл

    Правильное поведение:
    Отправить содержимое файла
    """
    check_test_result(static_ctx, http.request.get(path='test.html'),
                      content=static_ctx.test_content)


def test_http_1_1_rel_path(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает существующий в этой директории файл, используя относительный путь

    Правильное поведение:
    Отправить содержимое файла
    """
    check_test_result(static_ctx, http.request.get(path='/inner_dir/../test.html'),
                      content=static_ctx.test_content)


def test_http_1_1_symlink(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает симлинк, указывающий на файл, находящийся в этой директории

    Правильное поведение:
    Отправить содержимое файла на который указывает симлинк
    """
    check_test_result(static_ctx, http.request.get(path='/inner_dir/symlink'),
                      content=static_ctx.test_content)


def test_http_1_1_long_path(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает файл, путь до которого содержит много вложенных папок

    Правильное поведение:
    Отправить содержимое файла
    """
    check_test_result(static_ctx, http.request.get(path='/0/1/2/3/4/5/6/7/8/9/test.html'),
                      content=static_ctx.test_content)


def test_http_1_1_index_html(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы, содержащая файл index.html
    Клиент запрашивает корневую директорию

    Правильное поведение:
    Отправить содержимое файла index.html
    """
    check_test_result(static_ctx, http.request.get(),
                      static_dir=static_ctx.inner_dir, content=static_ctx.index_content)


def test_http_1_1_index_html_neg(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает поддиректорию, содержащую index.html

    Правильное поведение:
    Вернуть 404
    """
    check_test_result(static_ctx, http.request.get(path='/inner_dir'),
                      static_dir=static_ctx.inner_dir, expect=404)


def test_http_1_1_file_mode_000(static_ctx):
    """
    SEPE-4376
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает существующий в этой директории файл, для которого нет прав на чтение

    Правильное поведение:
    Вернуть 404
    """
    check_test_result(static_ctx, http.request.get(path='/test_000.html'),
                      static_dir=static_ctx.inner_dir, expect=404)


def test_http_1_1_dir_mode_100(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    В директории есть поддиректория с правами доступа 100,
    то есть получить список файлов в ней нельзя,
    но можно получить содержимое файла, зная полный путь до него
    Клиент запрашивает существующий в поддиректории файл

    Правильное поведение:
    Отправить содержимое файла
    """
    check_test_result(static_ctx, http.request.get(path='/dir_100/test.html'),
                      content=static_ctx.test_content)


def test_http_1_1_request_dir(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает поддиректорию этой директории

    Правильное поведение:
    Вернуть 404
    """
    check_test_result(static_ctx, http.request.get(path='/tmp'),
                      static_dir=static_ctx.inner_dir, expect=404)


def test_http_1_1_not_exist(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает несуществующий файл

    Правильное поведение:
    Вернуть 404
    """
    check_test_result(static_ctx, http.request.get(path='/not_exist'),
                      static_dir=static_ctx.inner_dir, expect=404)


def test_http_1_1_outer_file(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает файл вне этой директории

    Правильное поведение:
    Вернуть 403
    """
    check_test_result(static_ctx, http.request.get(path='/../test.html'),
                      static_dir=static_ctx.inner_dir, expect=403)


def test_http_1_1_outer_symlink(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается директория, из которой берутся файлы
    Клиент запрашивает симлинк, находящийся внутри директории, но указывающий на файл вне этой директории

    Правильное поведение:
    Вернуть 403
    """
    check_test_result(static_ctx, http.request.get(path='/symlink'),
                      static_dir=static_ctx.inner_dir, expect=403)


def test_http_1_1_file(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру задается файл, содержимое которого нужно отадавать в ответ на любой запрос

    Правильное поведение:
    Отправить содержимое указанного файла
    """
    check_test_result(static_ctx, http.request.get(path='/wiruwoeuiwoe'),
                      static_file=static_ctx.test_html, content=static_ctx.test_content)


def test_http_1_1_sf_not_exist(static_ctx):
    """
    Запрос по http/1.1

    Условия:
    Балансеру в конфиге задается несуществующий файл

    Правильное поведение:
    Вернуть 404
    """
    static_file = static_ctx.static.abs_path('nonexistent')
    check_test_result(static_ctx, http.request.get(path='/wiruwoeuiwoe'),
                      static_file=static_file, expect=404)


def test_http_1_1_sf_file_mode_000(static_ctx):
    """
    SEPE-4376
    Запрос по http/1.1

    Условия:
    Балансеру в конфиге задается файл без прав на чтение

    Правильное поведение:
    Вернуть 403
    """
    check_test_result(static_ctx, http.request.get(path='/wiruwoeuiwoe'),
                      static_file=static_ctx.file_000, expect=403)


def test_root_dir(static_ctx):
    """
    SEPE-4480

    Условия:
    Балансеру в конфиге в качестве директории задается корень файловой системы
    Клиент запрашивает файл, на который есть права на чтение

    Правильное поведение:
    Вернуть клиенту содержимое файла
    """
    check_test_result(static_ctx, http.request.get(path=static_ctx.test_html),
                      static_dir='/', content=static_ctx.test_content)


def test_request_content_chunked(static_ctx):
    """
    BALANCER-352
    Если клиент задает запрос с телом, то static должен вычитать тело,
    чтобы можно было задавать запросы по этому же соединению
    Случай chunked запроса
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        conn.perform_request(http.request.post(data=['12345', 'abcde']))
        response = conn.perform_request(http.request.get())

    asserts.content(response, static_ctx.test_content)


def test_request_content_length(static_ctx):
    """
    BALANCER-352
    Если клиент задает запрос с телом, то static должен вычитать тело,
    чтобы можно было задавать запросы по этому же соединению
    Случай content-length запроса
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        conn.perform_request(http.request.post(data='12345'))
        response = conn.perform_request(http.request.get())

    asserts.content(response, static_ctx.test_content)


def test_not_valid_request(static_ctx):
    """
    Задаем не GET/HEAD запрос
    """
    check_test_result(static_ctx, http.request.put(path="test.html"),
                      static_file=static_ctx.test_html, expect=405)


def test_date_request(static_ctx):
    """
    Тестируем Date заголовок
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "Date")
        d1 = int(calendar.timegm(email.utils.parsedate(response.headers["Date"][0])))
        d2 = int(time.time())

        assert -5 < (d2 - d1) < 5


def test_last_modified_request(static_ctx):
    """
    Тестируем Last-Modified заголовок
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "Last-Modified")
        d1 = int(calendar.timegm(email.utils.parsedate(response.headers["Last-Modified"][0])))
        d2 = int(os.stat(static_ctx.test_html).st_mtime)

        assert d1 == d2


def test_expires_request(static_ctx):
    """
    Тестируем Expires заголовок с дефолтным поведением
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "Expires")
        d1 = int(calendar.timegm(email.utils.parsedate(response.headers["Expires"][0])))

        assert d1 == 0


def test_set_expires_request(static_ctx):
    """
    Тестируем Expires заголовок с выставленным значением +2 дня
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html, expires="2d"))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "Expires")
        asserts.header(response, "Cache-Control")
        assert response.headers['cache-control'] == ['public, max-age=%d' % (2 * 24 * 60 * 60)]
        d1 = int(calendar.timegm(email.utils.parsedate(response.headers["Expires"][0])))
        d2 = int(time.time()) + 172800  # two days

        assert -5 < (d2 - d1) < 5


def test_etag_request(static_ctx):
    """
    Тестируем ETag заголовок
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "ETag")

        stat = os.stat(static_ctx.test_html)
        etag1 = response.headers["ETag"][0].lower()
        etag2 = "\"" + hex(int(stat.st_size))[2:] + "-" + hex(int(stat.st_mtime))[2:] + "\""

        assert etag1 == etag2


def test_etag_inode_request(static_ctx):
    """
    Тестируем ETag заголовок с inode
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html, etag_inode=True))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "ETag")

        stat = os.stat(static_ctx.test_html)
        etag1 = response.headers["ETag"][0].lower()
        etag2 = "\"" + hex(int(stat.st_ino))[2:] + "-" + hex(int(stat.st_size))[2:] + "-" + hex(int(stat.st_mtime))[2:] + "\""

        assert etag1 == etag2


def test_etag_if_match_request(static_ctx):
    """
    Тестируем If-Match заголовок. Если ETag * или находится в списке и при этом не weak,
    то заголовок должен быть пропущен. Иначе должен сработать If-Modified-Since и заодно проверим пропускается ли If-Unmodified-Since.
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "ETag")

        stat = os.stat(static_ctx.test_html)
        etag1 = response.headers["ETag"][0].lower()
        etag2 = "\"" + hex(int(stat.st_size))[2:] + "-" + hex(int(stat.st_mtime))[2:] + "\""

        assert etag1 == etag2

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Match": etag1, "If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 304)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Match": "{0}, \"111\"".format(etag1), "If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 304)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Match": "*", "If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 304)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Match": "W/{0}".format(etag1), "If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 200)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Match": "", "If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 200)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Match": etag1, "If-Unmodified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 200)


def test_etag_if_none_match_request(static_ctx):
    """
    Тестируем If-None-Match заголовок. Если ETag * или находится в списке и при этом не weak,
    то файл не модифицирован. Так же мы должны игнорировать If-Modified-Since.
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "ETag")

        stat = os.stat(static_ctx.test_html)
        etag1 = response.headers["ETag"][0].lower()
        etag2 = "\"" + hex(int(stat.st_size))[2:] + "-" + hex(int(stat.st_mtime))[2:] + "\""

        assert etag1 == etag2

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-None-Match": etag1, "If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 304)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-None-Match": "{0}, \"111\"".format(etag1), "If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 304)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-None-Match": "*", "If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 304)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-None-Match": "", "If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 200)


def test_if_modified_since_request(static_ctx):
    """
    Тестируем If-Modified-Since заголовок. Если дата модификации файла меньше даты заголовка,
    то файл не модифицирован.
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "ETag")

        stat = os.stat(static_ctx.test_html)
        etag1 = response.headers["ETag"][0].lower()
        etag2 = "\"" + hex(int(stat.st_size))[2:] + "-" + hex(int(stat.st_mtime))[2:] + "\""

        assert etag1 == etag2

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Modified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 304)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Modified-Since": "Sat, 24 Jan 2065 05:20:00 GMT"}))
        asserts.status(response, 200)


def test_if_unmodified_since_request(static_ctx):
    """
    Тестируем If-Unmodified-Since заголовок. Если дата модификации файла меньше даты заголовка,
    то возвращаем ошибку 412 Precondition Failed
    """
    static_ctx.start_balancer(StaticFileConfig(static_ctx.test_html))

    with static_ctx.create_http_connection() as conn:
        response = conn.perform_request(http.request.get(path="test.html"))

        asserts.status(response, 200)
        asserts.header(response, "ETag")

        stat = os.stat(static_ctx.test_html)
        etag1 = response.headers["ETag"][0].lower()
        etag2 = "\"" + hex(int(stat.st_size))[2:] + "-" + hex(int(stat.st_mtime))[2:] + "\""

        assert etag1 == etag2

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Unmodified-Since": "Thu, 01 Jan 1970 00:00:01 GMT"}))
        asserts.status(response, 412)

        response = conn.perform_request(http.request.get(path="test.html", headers={"If-Unmodified-Since": "Sat, 24 Jan 2065 05:20:00 GMT"}))
        asserts.status(response, 200)
