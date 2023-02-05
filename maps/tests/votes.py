from pdb import set_trace
import stubout
from tests.common import TransactionalTestCase, MockPumper, Comments
from WikimapPy.impl import Wikimap
import yandex.maps.wikimap.pumper_http
from ytools.xml import AssertXML
import Yandex


spam = Yandex.Maps.Wiki.VotingType.spam
appeal = Yandex.Maps.Wiki.VotingType.appeal

class Votes(TransactionalTestCase):
    def __init__(self, *args, **kwargs):
        self.servant = Wikimap()
        self.comments = Comments()
        self.servant.servant_factory.replace('bind/comments', self.comments)
        self.stubs = stubout.StubOutForTesting()
        self.stubs.Set(yandex.maps.wikimap.pumper_http, 'EventFirer', MockPumper)

        self.uid = 546
        self.uid2 = 547
        super(Votes, self).__init__(*args, **kwargs)


    def testVote(self):
        AssertXML(self.servant.RegisterUserVoteX(self.uid, 100, 1, spam, "test"))\
            .exist('/ok')
        AssertXML(self.servant.GetUserVoteX(self.uid, 100, 1))\
            .equal('/vote/vote', str(spam))

        AssertXML(self.servant.RegisterUserVoteX(self.uid, 101, 1, spam, ""))\
            .exist('/ok')


        AssertXML(self.servant.RegisterUserVoteX(self.uid2, 100, 1, spam, "test"))\
            .exist('/ok')

        AssertXML(self.servant.GetAllObjectVotesX(100, 10, 1))\
            .equal('/votes/@page', '1')\
            .equal('/votes/@per-page', '10')\
            .equal('/votes/@total-pages', '1')\
            .equal('/votes/@total-count', '2')

        AssertXML(self.servant.GetObjectVotesX(100, 1, 10, 1))\
            .equal('/votes/@page', '1')\
            .equal('/votes/@per-page', '10')\
            .equal('/votes/@total-pages', '1')\
            .equal('/votes/@total-count', '2')

    def testUnauthorized(self):
        AssertXML(self.servant.RegisterUserVoteX(0, 1, 1, spam, "test")).exist('/error')


    def testGoodVote(self):
        AssertXML(self.servant.RegisterUserVoteX(self.uid, 100, 1, appeal, "test"))\
            .exist('/ok')
        AssertXML(self.servant.GetUserVoteX(self.uid, 100, 1))\
            .equal('/vote/vote', str(appeal))

        AssertXML(self.servant.RegisterUserVoteX(self.uid, 100, 1, spam, "test2"))\
            .exist('/ok')
        AssertXML(self.servant.GetUserVoteX(self.uid, 100, 1))\
            .equal('/vote/vote', str(spam))\
            .exist('/vote/comment')
        

        AssertXML(self.servant.RegisterUserVoteX(self.uid, 101, 2, spam, ""))\
            .exist('/ok')
        AssertXML(self.servant.GetUserVoteX(self.uid, 101, 2))\
            .equal('/vote/vote', str(spam))\
            .not_exist('/vote/comment')
        
