from pdb import set_trace
import stubout
from tests.common import TransactionalTestCase, MockPumper
from WikimapPy.impl import Wikimap
from WikimapPy.models import UserLayerStats
import yandex.maps.wikimap.pumper_http
from ytools.xml import AssertXML, ET
import Yandex

class UserStats(TransactionalTestCase):
    def __init__(self, *args, **kwargs):
        self.servant = Wikimap()
        self.stubs = stubout.StubOutForTesting()
        self.stubs.Set(yandex.maps.wikimap.pumper_http, 'EventFirer', MockPumper)

        self.uid = 546
        super(UserStats, self).__init__(*args, **kwargs)


    def testStat(self):
        AssertXML(self.servant.GetUserStatisticsX(self.uid, ''))\
            .equal('/stats/@uid', str(self.uid))\
            .equal('/stats/counter[@action="object-new"]', '0')\
            .equal('/stats/counter[@action="object-changed"]', '0')


    def testGetUserStats(self):
        AssertXML(self.servant.GetUsersStatsForStatbox('2010-01-01'))\
            .equal('/user-stats/@date', '2010-01-01')\
            .equal('/user-stats/active', '0')\
            .equal('/user-stats/active-day', '0')\
            .equal('/user-stats/active-week', '0')\
            .equal('/user-stats/active-month', '0')


    def testGetObjectsStats(self):
        AssertXML(self.servant.GetObjectsStatsForStatbox('2010-01-01'))\
            .equal('/object-stats/@date', '2010-01-01')\
            .exist('/object-stats')\
            .exist('/object-stats/layer')



    def testGetModerationStats(self):
        AssertXML(self.servant.GetModerationStatsForStatbox('2010-01-01'))\
            .equal('/moderation-stats/@date', '2010-01-01')\
            .equal('/moderation-stats/complaints', '0')\
            .equal('/moderation-stats/appeals', '0')\
            .equal('/moderation-stats/auto', '0')\
            .equal('/moderation-stats/processed', '0')\
