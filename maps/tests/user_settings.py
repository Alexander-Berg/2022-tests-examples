from __future__ import with_statement
import simplejson
from pdb import set_trace
import stubout
from tests.common import TransactionalTestCase, MockPumper, create_user
from tests.mock_state import State
from WikimapPy.impl import Wikimap
from WikimapPy.models import UserStats
from WikimapPy import db
import yandex.maps.wikimap.pumper_http
from ytools.xml import AssertXML, ET
from ytools.config import Config


class AuthInfo(object):
    def __init__(self, uid = None, isSecure = True, login = 'test-user'):
        self.uid = uid
        self.isSecure = isSecure
        self.login = login



class UserSettings(TransactionalTestCase):
    def __init__(self, *args, **kwargs):
        self.servant = Wikimap()
        self.stubs = stubout.StubOutForTesting()
        self.stubs.Set(yandex.maps.wikimap.pumper_http, 'EventFirer', MockPumper)
        self.uid = 123
        super(UserSettings, self).__init__(*args, **kwargs)

    def testDefaultSettings(self):
        result = self.servant.GetUserSettingsX(self.uid, '')
        AssertXML(result) \
            .equal('/settings/@uid', str(self.uid)) \
            .equal('/settings/email-notification-mode', None) \
            .equal('/settings/auto-subscribe-new-object', 'true') \
            .equal('/settings/auto-subscribe-mod-object', 'true') \
            .equal('/settings/subscribe-env', 'true')

    def testUpdateSettings(self):
        sets = simplejson.dumps({
            'notify-on-comment': True,
            'notify-on-vote': True,
            'notify-on-new-object': True,
            'notify-on-mod-object': True,
            'email-notification-mode': 'daily',
            'auto-subscribe-new-object': False,
            'auto-subscribe-mod-object': False,
            'subscribe-env': False,
            'email': 'art@yandex-team.ru',
        })

        a = AssertXML(self.servant.UpdateUserSettingsX(self.uid, sets))
        token =  a.xml.xpath('wm:token',
                             namespaces = {'wm': "http://maps.yandex.ru/wikimap/1.x"}
                             )[0].text
        a.exist('/ok')

        result = self.servant.GetUserSettingsX(self.uid, token)
        AssertXML(result) \
            .equal('/settings/@uid', str(self.uid)) \
            .equal('/settings/auto-subscribe-new-object', 'false') \
            .equal('/settings/auto-subscribe-mod-object', 'false') \
            .equal('/settings/subscribe-env', 'false') \
            .equal('/settings/email-notification-mode', 'daily') \
            .equal('/settings/email', 'art@yandex-team.ru')

    def testCreateUser(self):
        uid = 123
        auth_info = AuthInfo(uid = uid)
        AssertXML(self.servant.CreateUserX(auth_info)) \
            .equal('/error/@status', 'ACCESS_DENIED')


    def testGetUsers(self):
        uid = 101

        create_user(AuthInfo(uid = uid))

        with db.get_write_session() as session:
            session.add(UserStats(uid = uid))
            session.flush()

        AssertXML(self.servant.GetUsersX('new', 1, 10, ''))\
            .equal('/users/@page', '1')\
            .equal('/users/@per-page', '10')\
            .equal('/users/@total-pages', '1')\
            .equal('/users/@total-count', '1')\
            .equal('/users/user[1]/@uid', str(uid))

    def testGetBannedUsers(self):
        #banned users should not be displayed
        uid1 = 101
        uid2 = 102
        mod = 103

        [create_user(AuthInfo(uid = u)) for u in (uid1, uid2)]

        with db.get_write_session() as session:
            session.add(UserStats(uid = uid1))
            session.add(UserStats(uid = uid2))
            session.flush()

        AssertXML(self.servant.GetUsersX('new', 1, 10, ''))\
            .equal('/users/@page', '1')\
            .equal('/users/@per-page', '10')\
            .equal('/users/@total-pages', '1')\
            .equal('/users/@total-count', '2')

        self.servant.AddModeratorX(self.servant.wiki_user, mod, False)
        self.servant.BanUserX(mod, uid2, 1)

        AssertXML(self.servant.GetUsersX('new', 1, 10, ''))\
            .equal('/users/@page', '1')\
            .equal('/users/@per-page', '10')\
            .equal('/users/@total-pages', '1')\
            .equal('/users/@total-count', '1')\
            .equal('/users/user[1]/@uid', str(uid1))



    def testDoNotShowAutotesters(self):
 
        uid1 = 100
        with db.get_write_session() as session:
            create_user(AuthInfo(uid = uid1))
            session.add(UserStats(uid = uid1))
            config = Config()
            autotesters = [int(u) for u in filter(None, config.get('misc/autotesters', '').split(','))]

            for uid in autotesters:
                create_user(AuthInfo(uid = uid))
                session.add(UserStats(uid = uid))

            session.flush()

        AssertXML(self.servant.GetUsersX('new', 1, 10, ''))\
            .equal('/users/@page', '1')\
            .equal('/users/@per-page', '10')\
            .equal('/users/@total-pages', '1')\
            .equal('/users/@total-count', '1')\
            .equal('/users/user/@uid', [str(uid1)])
