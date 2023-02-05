# coding: utf-8
import Cookie
from nose.tools import ok_, eq_
from urllib import urlencode

#local imports
import tools
from readonly import app

class ReadonlyTestCase(tools.AuthorizationTestCase,
                       tools.LogErrorsTestCase,
                       tools.SendmailTestCase):
    app = app

    def _check_rw(self):
        check_lock = tools.urlopen(app, '/')
        eq_(check_lock['code'], 200)
        ok_(check_lock['body'].find('разрешено') != -1)

    def _check_ro(self):
        check_lock = tools.urlopen(app, '/')
        eq_(check_lock['code'], 200)
        ok_(check_lock['body'].find('запрещено') != -1)
        ok_(check_lock['body'].find('пользователем ' + \
                    str(tools.AuthorizationTestCase.MODERATOR_LOGIN)) != -1)

    def test_ro(self):
        cookie = Cookie.SimpleCookie()
        cookie['Session_id'] = tools.AuthorizationTestCase.MODERATOR_SESSION_ID

        u_cookie = Cookie.SimpleCookie()
        u_cookie['Session_id'] = tools.AuthorizationTestCase.USER_SESSION_ID

        # check lock isn't set yet
        self._check_rw()

        # lock w/o correct rights/params
        do_switch = tools.urlopen(app,
                                '/',
                                method='POST',
                                env={ 'HTTP_COOKIE' : str(cookie) })
        eq_(do_switch['code'], 303)
        self._check_rw()

        do_switch = tools.urlopen(app,
                                '/',
                                post=urlencode({ 'really_switch' : 'yes',
                                                 'action' : 'lock' }))
        eq_(do_switch['code'], 401)
        self._check_rw()

        do_switch = tools.urlopen(app,
                                '/',
                                post=urlencode({ 'really_switch' : 'da',
                                                 'action' : 'lock' }),
                                env={ 'HTTP_COOKIE' : str(cookie) })
        eq_(do_switch['code'], 303)
        self._check_rw()

        do_switch = tools.urlopen(app,
                                  '/',
                                  method='POST',
                                  query=urlencode({ 'really_switch' : 'yes',
                                                    'action' : 'lock' }),
                                  env={ 'HTTP_COOKIE' : str(cookie) })
        eq_(do_switch['code'], 303)
        self._check_rw()

        do_switch = tools.urlopen(app,
                                  '/',
                                  post=urlencode({ 'really_switch' : 'yes',
                                                   'action' : 'lock' }),
                                  env={ 'HTTP_COOKIE' : str(u_cookie) })
        eq_(do_switch['code'], 401)
        self._check_rw()

        # unlock
        do_switch = tools.urlopen(app,
                                  '/',
                                  post=urlencode({ 'really_switch' : 'yes',
                                                   'action' : 'unlock' }),
                                  env={ 'HTTP_COOKIE' : str(cookie) })
        eq_(do_switch['code'], 303)
        self._check_rw()

        # lock
        do_switch = tools.urlopen(app,
                                  '/',
                                  post=urlencode({ 'really_switch' : 'yes',
                                                   'action' : 'lock' }),
                                  env={ 'HTTP_COOKIE' : str(cookie) })
        eq_(do_switch['code'], 303)
        self._check_ro()

        # try remove lock w/o correct rights
        do_switch = tools.urlopen(app, '/', method='POST')
        eq_(do_switch['code'], 401)
        self._check_ro()

        do_switch = tools.urlopen(app,
                                  '/',
                                  method='POST',
                                  env={ 'HTTP_COOKIE' : str(cookie) })
        eq_(do_switch['code'], 303)
        self._check_ro()

        # remove lock
        do_switch = tools.urlopen(app,
                                  '/',
                                  post=urlencode({ 'really_switch' : 'yes',
                                                   'action' : 'unlock' }),
                                  env={ 'HTTP_COOKIE' : str(cookie) })
        eq_(do_switch['code'], 303)
        self._check_rw()

        # again
        do_switch = tools.urlopen(app,
                                  '/',
                                  post=urlencode({ 'really_switch' : 'yes',
                                                   'action' : 'unlock' }),
                                  env={ 'HTTP_COOKIE' : str(cookie) })
        eq_(do_switch['code'], 303)
        self._check_rw()
