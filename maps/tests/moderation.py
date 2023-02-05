# -*- coding: utf-8 -*-
import json
import stubout
from tests.common import TransactionalTestCase, MockPumper, Comments, create_user
from WikimapPy.impl import Wikimap
from WikimapPy import db
from WikimapPy.models import ModerationTask, ModerationTaskAction

import yandex.maps.wikimap.pumper_http
from ytools.xml import AssertXML, ET


class AuthInfo(object):
    def __init__(self, uid=None, isSecure=True, login='test-user'):
        self.uid = uid
        self.isSecure = isSecure
        self.login = login


class ModerationTest(TransactionalTestCase):

    def __init__(self, *args, **kwargs):
        self.servant = Wikimap()
        self.comments = Comments()
        self.servant.servant_factory.replace('bind/comments', self.comments)
        self.stubs = stubout.StubOutForTesting()
        self.stubs.Set(yandex.maps.wikimap.pumper_http, 'EventFirer', MockPumper)
        super(ModerationTest, self).__init__(*args, **kwargs)

    def testModeratorsPage(self):
        u1, n1 = 1, 'user1'
        u2, n2 = 2, 'user2'
        u3, n3 = 3, 'user3'

        AssertXML(self.servant.IsUserModeratorX(u1, ''))\
            .equal('/moderator', 'false')

        AssertXML(self.servant.AddModeratorX(self.servant.wiki_user, u1, False)).exist('/ok')
        AssertXML(self.servant.AddModeratorX(self.servant.wiki_user, u2, True)).exist('/ok')
        AssertXML(self.servant.IsUserModeratorX(u1, ''))\
            .equal('/moderator', 'true')

        AssertXML(self.servant.GetModeratorsX(token='', per_page=2, page=1, reverse=True))\
            .equal('/moderators/@page', '1')\
            .equal('/moderators/@per-page', '2')\
            .equal('/moderators/@total-pages', '1')\
            .equal('/moderators/@total-count', '2')\
            .equal('/moderators/moderator/@uid', [str(u) for u in (u2, u1)])

        AssertXML(self.servant.AddModerationTaskX(1, 1, u1, ModerationTask.T_COMPLAIN))\
                                                     .exist('/ok')

    def testModTaskList(self):
        oid = 4
        rev = 5
        uid1 = 11
        mod = 21
        action = ModerationTaskAction.A_DELETE

        AssertXML(self.servant.AddModeratorX(self.servant.wiki_user, mod, True)).exist('/ok')

        AssertXML(self.servant.RegisterUserVoteX(uid1, oid, rev, -1, '')).exist('/ok')
        res = self.servant.AddModerationTaskX(oid, rev, mod, ModerationTask.T_COMPLAIN)
        AssertXML(res).exist('/ok')
        task_id = int(ET.XML(res).find('moderation-task').get('id'))

        AssertXML(self.servant.LogModerationActionX(task_id, mod, action, 0)).exist('/ok')

        msgs = self.servant.GetModerationTasksListX(
                    json.dumps(dict(uid=mod, task_state='closed', page=1, per_page=10,
                                task_type='complain')))
        AssertXML(msgs)\
            .equal('/tasks/@page', '1')\
            .equal('/tasks/@per-page', '10')\
            .equal('/tasks/@total-count', '1')\
            .equal('/tasks/moderation-task[1]/@id', str(task_id))\
            .equal('/tasks/moderation-task[1]/votes/vote[1]/@oid', str(oid))\
            .equal('/tasks/moderation-task[1]/mod-actions/mod-task-action[1]/@uid', str(mod))\
            .equal('/tasks/counters/group[@by="type"]/counter[@by="complain"]', '1')

        AssertXML(self.servant.GetModerationTasksListX(
            json.dumps(dict(uid=mod, task_state='closed', page=1, per_page=0,
                            task_type='complain'))))\
            .equal('/tasks/@page', '1')\
            .equal('/tasks/@per-page', '0')\
            .equal('/tasks/@total-count', '1')\
            .equal('/tasks/counters/group[@by="type"]/counter[@by="complain"]', '1')

    def testBan(self):
        mod = 100
        user = 101
        create_user(AuthInfo(uid=user))
        AssertXML(self.servant.GetUserSettingsX(user, '')).equal('/settings/@banned', 'false')
        AssertXML(self.servant.AddModeratorX(self.servant.wiki_user, mod, True)).exist('/ok')
        AssertXML(self.servant.BanUserX(mod, user, 1)).exist('/ok')
        AssertXML(self.servant.GetUserSettingsX(user, '')).equal('/settings/@banned', 'true')\
            .equal('/settings/banned-by', str(mod))

        AssertXML(self.servant.GetBannedUsersX(1, 10, ''))\
            .equal('/user-bans/@page', '1')\
            .equal('/user-bans/@total-count', '1')\
            .equal('/user-bans/user[1]/settings/@uid', str(user))

        AssertXML(self.servant.UnbanUserX(mod, user)).exist('/ok')
        AssertXML(self.servant.GetUserSettingsX(user, '')).equal('/settings/@banned', 'false')

    def testBanAccessControl(self):
        user = 100
        mod = 101
        mod2 = 102
        sup = 103
        [create_user(AuthInfo(uid=u)) for u in (user, mod2)]
        self.servant.AddModeratorX(self.servant.wiki_user, mod, False)
        self.servant.AddModeratorX(self.servant.wiki_user, mod2, False)
        self.servant.AddModeratorX(self.servant.wiki_user, sup, True)
        #moderator can bau user
        AssertXML(self.servant.BanUserX(mod, user, 1)).exist('/ok')
        #moderator can't ban another moderator
        AssertXML(self.servant.BanUserX(mod, mod2, 1)).exist('/error')
        #unless it is superviser
        AssertXML(self.servant.BanUserX(sup, mod2, 1)).exist('/ok')

    def testEditModerator(self):
        sup, mod = 100, 101
        [create_user(AuthInfo(uid=u)) for u in (sup, mod)]
        self.servant.AddModeratorX(self.servant.wiki_user, sup, True)
        self.servant.AddModeratorX(self.servant.wiki_user, mod, False)

        AssertXML(self.servant.IsUserModeratorX(sup, ''))\
            .equal('/moderator', 'true')\
            .equal('/moderator/@is-superviser', 'true')

        AssertXML(self.servant.IsUserModeratorX(mod, ''))\
            .equal('/moderator', 'true')\
            .equal('/moderator/@is-superviser', 'false')

        AssertXML(self.servant.EditModeratorX(mod, sup,
                                json.dumps(dict(is_superviser=False)))).exist('/error')

        AssertXML(self.servant.EditModeratorX(sup, mod,
                    json.dumps(dict(is_superviser=True)))).exist('/ok')
        AssertXML(self.servant.IsUserModeratorX(mod, ''))\
            .equal('/moderator', 'true')\
            .equal('/moderator/@is-superviser', 'true')

        AssertXML(self.servant.EditModeratorX(sup, mod,
                        json.dumps(dict(active=False)))).exist('/ok')
        AssertXML(self.servant.GetModeratorInfoX(mod, ''))\
            .equal('/moderator/active', 'false')

    def testModerationRegions(self):

        @db.write_session('core')
        def add_region_object(session):
            session.execute(
                """INSERT INTO core.objects (id, layer_id, the_geom, zmax, modified) VALUES (1, 2,
                ST_GeomFromText('POLYGON((4177088.99484866 7393320.89525124,4175254.50619069 7393454.66006695,4174432.80815989 7394123.48402158,4173553.78226112 7395480.24120788,4173037.83233998 7396626.79666459,4172655.64725856 7399435.85745586,4174107.9507238 7400544.19442672,4175101.63213859 7399818.04262961,4176611.26346906 7400792.61476424,4177700.49106798 7400677.95923561,4179477.65196931 7402225.80911293,4181063.72038558 7402894.63293553,4181656.10727848 7402359.57380562,4182420.47755264 7399474.07593211,4182019.18314479 7398136.42796312,4181579.67030672 7396149.06521092,4180853.51846279 7393473.76929271,4180261.13156989 7393148.9118696,4179057.24834635 7393148.9118696,4177088.99484866 7393320.89525124))'), 15, NOW())""")

            session.execute(
                """INSERT INTO core.objects (id, layer_id, the_geom, zmax, modified, screen_label) VALUES (2, 2,ST_GeomFromText('POLYGON((4177088.99484866 7393320.89525124,4175254.50619069 7393454.66006695,4174432.80815989 7394123.48402158,4173553.78226112 7395480.24120788,4173037.83233998 7396626.79666459,4172655.64725856 7399435.85745586,4174107.9507238 7400544.19442672,4175101.63213859 7399818.04262961,4176611.26346906 7400792.61476424,4177700.49106798 7400677.95923561,4179477.65196931 7402225.80911293,4181063.72038558 7402894.63293553,4181656.10727848 7402359.57380562,4182420.47755264 7399474.07593211,4182019.18314479 7398136.42796312,4181579.67030672 7396149.06521092,4180853.51846279 7393473.76929271,4180261.13156989 7393148.9118696,4179057.24834635 7393148.9118696,4177088.99484866 7393320.89525124))'), 15, NOW(), 'yyy')""")

        sup, mod, mod2 = 100, 101, 102
        [create_user(AuthInfo(uid=u)) for u in (sup, mod, mod2)]
        self.servant.AddModeratorX(self.servant.wiki_user, sup, True)
        self.servant.AddModeratorX(self.servant.wiki_user, mod, False)
        self.servant.AddModeratorX(self.servant.wiki_user, mod2, False)
        add_region_object()

        AssertXML(self.servant.CreateModerationRegionX(sup, 1, 'xxx')).exist('/ok')

        a = AssertXML(self.servant.GetModerationRegionsListX(1, 10, ''))\
            .equal('/regions/@page', '1')\
            .equal('/regions/@per-page', '10')\
            .equal('/regions/@total-pages', '1')\
            .equal('/regions/@total-count', '1')\
            .equal('/regions/region', 'xxx')

        region_id = int(a.xml.xpath('/regions/region/@id')[0])

        AssertXML(self.servant.AssignRegionX(sup, mod, region_id)).exist('/ok')

        AssertXML(self.servant.GetModeratorTaskCountByRegionX(mod, ''))\
            .equal('/regions/region', 'xxx')\
            .equal('/regions/region/@oid', '1')\
            .equal('/regions/region/@assigned-cnt', '0')\
            .equal('/regions/region/@closed-cnt', '0')\

        AssertXML(self.servant.GetModeratorsX(token=''))\
            .equal('/moderators/moderator[@uid="%s"]/regions/region' % mod, 'xxx')

        AssertXML(self.servant.GetModeratorInfoX(mod, ''))\
            .equal('regions/region', 'xxx')

        AssertXML(self.servant.WithdrawRegionFromModeratorX(sup, mod, region_id)).exist('/ok')
        AssertXML(self.servant.GetModeratorsX(token=''))\
            .equal('/moderators/moderator[@uid="%s"]/regions/region' % mod, [])

        AssertXML(self.servant.DeleteModerationRegionX(sup, region_id)).exist('/ok')

        AssertXML(self.servant.GetModerationRegionsListX(1, 10, ''))\
            .equal('/regions/@page', '1')\
            .equal('/regions/@per-page', '10')\
            .equal('/regions/@total-pages', '0')\
            .equal('/regions/@total-count', '0')

        AssertXML(self.servant.GetModeratorsX(token=''))\
            .equal('/moderators/moderator[@uid="%s"]/regions/region' % mod, [])

        AssertXML(self.servant.CreateAndAssignRegionFromObjectX(sup, mod, 2)).exist('/ok')

        AssertXML(self.servant.GetModerationRegionsListX(1, 10, ''))\
            .equal('/regions/@page', '1')\
            .equal('/regions/@per-page', '10')\
            .equal('/regions/@total-pages', '1')\
            .equal('/regions/@total-count', '1')\
            .equal('/regions/region', ['yyy'])

        AssertXML(self.servant.GetModeratorsX(token=''))\
            .equal('/moderators/moderator[@uid="%s"]/regions/region' % mod, 'yyy')

        AssertXML(self.servant.CreateAndAssignRegionFromObjectX(sup, mod2, 2)).exist('/ok')
        AssertXML(self.servant.CreateAndAssignRegionFromObjectX(sup, mod, 2)).exist('/ok')

        AssertXML(self.servant.GetModerationRegionsListX(1, 10, ''))\
            .equal('/regions/@page', '1')\
            .equal('/regions/@per-page', '10')\
            .equal('/regions/@total-pages', '1')\
            .equal('/regions/@total-count', '1')\
            .equal('/regions/region', ['yyy'])

        AssertXML(self.servant.GetModeratorsX(token=''))\
            .equal('/moderators/moderator[@uid="%s"]/regions/region' % mod, 'yyy')





