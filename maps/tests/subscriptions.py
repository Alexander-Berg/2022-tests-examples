from pdb import set_trace
import stubout
from tests.common import TransactionalTestCase, MockPumper
from WikimapPy.impl import Wikimap
import yandex.maps.wikimap.pumper_http
from ytools.xml import AssertXML, ET

class Subscriptions(TransactionalTestCase):
    def __init__(self, *args, **kwargs):
        self.servant = Wikimap()
        self.stubs = stubout.StubOutForTesting()
        self.stubs.Set(yandex.maps.wikimap.pumper_http, 'EventFirer', MockPumper)

        self.uid = 546
        self.uid2 = 547
        super(Subscriptions, self).__init__(*args, **kwargs)

    def testSubscribe(self):
        AssertXML(self.servant.SubscribeUserToObjectX(self.uid, 123, False)) \
            .equal('/subscription/@with-env', 'false') \
            .equal('/subscription/@oid', '123') \
            .equal('/subscription/@enabled', 'true') \
            .equal('/subscription/@uid',  str(self.uid))

    def testSubscribtionToken(self):
        a = AssertXML(self.servant.SubscribeUserToObjectX(self.uid, 123, False))
        token = a.xml.xpath('wm:token',
                             namespaces = {'wm': "http://maps.yandex.ru/wikimap/1.x"}
                             )[0].text

        AssertXML(self.servant.GetUserObjectSubscriptionX(self.uid, 123, token))\
            .equal('/subscription/@with-env', 'false') \
            .equal('/subscription/@oid', '123') \
            .equal('/subscription/@enabled', 'true') \
            .equal('/subscription/@uid',  str(self.uid))


    def testUpdateSubscription(self):
        AssertXML(self.servant.SubscribeUserToObjectX(self.uid, 123, False)) \
            .equal('/subscription/@with-env', 'false')

        AssertXML(self.servant.SubscribeUserToObjectX(self.uid, 123, True)) \
            .equal('/subscription/@with-env', 'true')

        AssertXML(self.servant.UnsubscribeUserFromObjectX(self.uid, 123))\
            .equal('/subscription/@enabled', 'false')


    def testUnauthorized(self):
        AssertXML(self.servant.SubscribeUserToObjectX(0, 1, True)).exist('/error')

        AssertXML(self.servant.UnsubscribeUserFromObjectX(0, 1, True)).exist('/error')


    def testSubscriptionsList(self):
        self.servant.SubscribeUserToObjectX(self.uid, 1, False)
        self.servant.SubscribeUserToObjectX(self.uid, 2, False)
        self.servant.SubscribeUserToObjectX(self.uid, 3, False)

        AssertXML(self.servant.GetUserSubscriptionsX(self.uid, 1, 2, ''))\
            .equal('/subscriptions/@page', '1')\
            .equal('/subscriptions/@per-page', '2')\
            .equal('/subscriptions/@total-pages', '2')\
            .equal('/subscriptions/@total-count', '3')\
            .count_equal('/subscriptions/subscription', 2)
