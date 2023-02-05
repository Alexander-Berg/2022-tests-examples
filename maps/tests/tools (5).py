# coding: utf-8
import blackbox
import bottle
import unittest
import mox
import smtplib
import stubout
import warnings
import wsgiref
import wsgiref.util
from nose.tools import ok_, eq_, raises, with_setup
from StringIO import StringIO
from WikimapPy import models, db
try:
    from io import BytesIO
except:
    BytesIO = None
    pass

# Локальные импорты
import utils

def tob(data):
    ''' Transforms bytes or unicode into bytes. '''
    return data.encode('utf8') if isinstance(data, unicode) else data


def tobs(data):
    ''' Transforms bytes or unicode into a byte stream. '''
    return BytesIO(tob(data)) if BytesIO else StringIO(tob(data))


def warn(message):
    warnings.warn(message, stacklevel=2)


def urlopen(app, path, query='', method='GET', post='', env=None):
    result = {'code': 0, 'status': 'error', 'header': {}, 'body': tob('')}

    def start_response(status, header):
        result['code'] = int(status.split()[0])
        result['status'] = status.split(None, 1)[-1]
        for name, value in header:
            name = name.title()
            if name in result['header']:
                result['header'][name] += ', ' + value
            else:
                result['header'][name] = value

    env = env if env else {}
    wsgiref.util.setup_testing_defaults(env)
    env['REQUEST_METHOD'] = method.upper().strip()
    env['PATH_INFO'] = path
    env['QUERY_STRING'] = query
    env['REMOTE_ADDR'] = '127.0.0.1'
    env['HTTP_HOST'] = 'localhost'
    if post:
        env['REQUEST_METHOD'] = 'POST'
        env['CONTENT_LENGTH'] = str(len(tob(post)))
        env['wsgi.input'].write(tob(post))
        env['wsgi.input'].seek(0)
    response = app(env, start_response)
    for part in response:
        try:
            result['body'] += part
        except TypeError:
            raise TypeError('WSGI app yielded non-byte object %s', type(part))
    if hasattr(response, 'close'):
        response.close()
        del response
    return result


def assert_status(code, app, route='/', **kargs):
    eq_(code, urlopen(app, route, **kargs)['code'])


def assert_body(body, app, route='/', **kargs):
    eq_(tob(body), urlopen(app, route, **kargs)['body'])


def assert_in_body(body, app, route='/', **kargs):
    result = urlopen(app, route, **kargs)['body']
    ok_(tob(body) in result,
        'The search pattern "%s" is not included in body:\n%s' % (body, result))


def assert_header(name, app, value, route='/', **kargs):
    eq_(value, urlopen(app, route, **kargs)['header'].get(name))


def assert_header_any(name, app, route='/', **kargs):
    ok_(urlopen(app, route, **kargs)['header'].get(name, None))


def assert_in_error(search, app, route='/', **kargs):
    bottle.request.environ['wsgi.errors'].errors.seek(0)
    err = bottle.request.environ['wsgi.errors'].errors.read()
    ok_(search in err,
        'The search pattern "%s" is not included in wsgi.error: %s' % (search, err))


class TransactionalTestCase(unittest.TestCase):
    """ Transactional test case, which makes rollback in tearDown method.
    """

    def setUp(self):
        self.transactions = dict((part, db._pool.get_master_session(part).real_begin())
                                 for part in db.DbMgr.PART_NAMES)
        super(TransactionalTestCase, self).setUp()

    def tearDown(self):
        super(TransactionalTestCase, self).tearDown()
        for transaction in self.transactions.values():
            transaction.real_rollback()


class AuthorizationTestCase(TransactionalTestCase):
    """Базовый класс для юнит-тестов, проверяющих ручки, использующие авторизацию
    через blackbox"""

    USER_SESSION_ID = '1'
    MODERATOR_SESSION_ID = '2'
    MODERATOR_UID = 222
    USER_UID = 111
    MODERATOR_LOGIN = 'kartgisnew'
    USER_LOGIN = 'user'

    USER_BBINFO = {
        'fields': {'login': USER_LOGIN},
        'uid': str(USER_UID),
        'valid': True}
    MODERATOR_BBINFO = {
        'fields': {'login': MODERATOR_LOGIN},
        'uid': str(MODERATOR_UID),
        'valid': True}

    @db.write_session('core')
    def create_moderator(self, uid, is_superviser, session):
        session.add(models.Moderator(uid=uid, is_superviser=is_superviser))

    def setUp(self):
        super(AuthorizationTestCase, self).setUp()
        self.mox = mox.Mox()
        mock_blackbox = self.mox.CreateMock(blackbox.Blackbox)
        self.mox.StubOutWithMock(utils.BlackboxProxy, 'blackbox')
        utils.BlackboxProxy.blackbox().MultipleTimes().AndReturn(mock_blackbox)
        mock_blackbox.sessionid(sessionid=self.USER_SESSION_ID, userip='127.0.0.1', host='localhost')\
            .MultipleTimes().AndReturn(self.USER_BBINFO)
        mock_blackbox.userinfo(uid_or_login=self.USER_UID, userip='127.0.0.1')\
            .MultipleTimes().AndReturn(self.USER_BBINFO)
        mock_blackbox.userinfo(by_login=True, uid_or_login=self.USER_LOGIN, userip='127.0.0.1')\
            .MultipleTimes().AndReturn(self.USER_BBINFO)

        mock_blackbox.sessionid(sessionid=self.MODERATOR_SESSION_ID, userip='127.0.0.1', host='localhost')\
            .MultipleTimes().AndReturn(self.MODERATOR_BBINFO)
        mock_blackbox.userinfo(uid_or_login=self.MODERATOR_UID, userip='127.0.0.1')\
            .MultipleTimes().AndReturn(self.MODERATOR_BBINFO)
        mock_blackbox.userinfo(by_login=True, uid_or_login=self.MODERATOR_LOGIN, userip='127.0.0.1')\
            .MultipleTimes().AndReturn(self.MODERATOR_BBINFO)

        self.mox.ReplayAll()
        self.create_moderator(self.MODERATOR_UID, True)

    def tearDown(self):
        self.mox.UnsetStubs()
        super(AuthorizationTestCase, self).tearDown()


class LogErrorsTestCase(unittest.TestCase):
    def setUp(self):
        super(LogErrorsTestCase, self).setUp()
        self.app.install(utils.LogErrorsPlugin())

    def tearDown(self):
        self.app.uninstall(utils.LogErrorsPlugin)
        super(LogErrorsTestCase, self).tearDown()

class SendmailTestCase(unittest.TestCase):
    class _RecursiveStub:
        def __call__(self, *args, **kwargs):
            return self

        def __getattr__(self, name):
            return self

    def setUp(self):
        super(SendmailTestCase, self).setUp()
        self.stubs = stubout.StubOutForTesting()
        self.stubs.Set(smtplib, 'SMTP', self._RecursiveStub)

    def tearDown(self):
        self.stubs.UnsetAll()
        super(SendmailTestCase, self).tearDown()


# Взято из http://code.activestate.com/recipes/146306-http-client-to-post-using-multipartform-data/
def encode_multipart_formdata(fields, files):
    """
    Создаёт HTTP-запрос, соответствующий отправке формы с enctype="multipart/form-data"

    fields -- словарь ключ -> значение для обычных полей формы
    files -- словарь ключ -> (имя файла, содержание) для полей формы, соответствующих файлам.
    Возвращает пару (content_type, тело запроса)
    """
    BOUNDARY = '----------ThIs_Is_tHe_bouNdaRY_$'
    CRLF = '\r\n'
    L = []
    for key, value in fields.items():
        L.append('--' + BOUNDARY)
        L.append('Content-Disposition: form-data; name="%s"' % key)
        L.append('')
        L.append(value)
    for key, (filename, value) in files.items():
        L.append('--' + BOUNDARY)
        L.append('Content-Disposition: form-data; name="%s"; filename="%s"' % (key, filename))
        L.append('Content-Type: application/octet-stream')
        L.append('')
        L.append(value)
    L.append('--' + BOUNDARY + '--')
    L.append('')
    body = CRLF.join(L)
    content_type = 'multipart/form-data; boundary=%s' % BOUNDARY
    return content_type, body
