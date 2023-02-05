import bottle
import Cookie
from nose.tools import ok_, eq_

#local imports
import utils
import tools

app = bottle.Bottle()


@app.route('/auth')
@utils.authorize
def restricted_func(uid):
    return "OK"


@app.route('/moder')
@utils.authorize
@utils.supervisers_only
def super_func(uid):
    return "OK"


class AuthorizationTestCase(tools.AuthorizationTestCase):

    def test_auth(self):
        tools.assert_status(401, app, '/auth')
        cookie = Cookie.SimpleCookie()
        cookie['Session_id'] = tools.AuthorizationTestCase.USER_SESSION_ID
        tools.assert_status(200, app, '/auth', env={'HTTP_COOKIE': str(cookie)})

    def test_access_control(self):
        tools.assert_status(401, app, '/moder')
        cookie = Cookie.SimpleCookie()
        cookie['Session_id'] = tools.AuthorizationTestCase.USER_SESSION_ID
        tools.assert_status(401, app, '/moder', env={'HTTP_COOKIE': str(cookie)})
        moder_cookie = Cookie.SimpleCookie()
        moder_cookie['Session_id'] = tools.AuthorizationTestCase.MODERATOR_SESSION_ID
        tools.assert_status(200, app, '/moder', env={'HTTP_COOKIE': str(moder_cookie)})
