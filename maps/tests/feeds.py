from __future__ import with_statement
import Yandex
import simplejson
from tests.common import TransactionalTestCase
from datetime import datetime, timedelta
from WikimapPy.impl import Wikimap
from WikimapPy.models import Message
from WikimapPy import db, utils
from ytools.xml import AssertXML, ET


class Feeds(TransactionalTestCase):
    def __init__(self, *args, **kwargs):
        self.servant = Wikimap()
        super(Feeds, self).__init__(*args, **kwargs)

    def testPutMessage(self):
        self.assertEqual(True,
                         self.servant.PutMessage([123], Message.NOTIF_FEED,
                                                 '<data by-uid="456" action="test" blah="minor" />'))

    def testGetMessages(self):
        self.assertEqual(True, self.servant.PutMessage([123], Message.NOTIF_FEED,
                                                       '<body by-uid="456" action="test" blah="minor" />'))
        self.assertEqual(True, self.servant.PutMessage([123, 890], Message.NOTIF_FEED,
                                                       '<body by-uid="456" action="test" blah="minor" />'))
        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid=890, feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .count_equal('/messages/message', 1) \
            .exist('/messages/message/@id') \
            .not_exist('/messages/message/env') \
            .not_exist('/messages/message/uid') \
            .not_exist('/messages/message/by-uid') \
            .not_exist('/messages/message/action') \
            .not_exist('/messages/message/data') \
            .equal('/messages/message/body/@action', 'test') \
            .equal('/messages/message/body/@blah', 'minor')

    def testMessagesByType(self):
        self.assertEqual(True, self.servant.PutMessage([123], Message.NOTIF_FEED,
                                                       '<body by-uid="456" action="new" />'))
        self.assertEqual(True, self.servant.PutMessage([123, 400], Message.NOTIF_FEED,
                                                       '<body by-uid="456" action="modified" object="First"/>'))
        self.assertEqual(True, self.servant.PutMessage([123, 300], Message.NOTIF_FEED,
                                                       '<body by-uid="456" action="modified" object="Second"/>'))
        self.assertEqual(True, self.servant.PutMessage([123, 300], Message.NOTIF_FEED,
                                                       '<body by-uid="456" action="deleted" object="First"/>'))
        self.assertEqual(True, self.servant.PutMessage([300], Message.NOTIF_FEED,
                                                       '<body by-uid="456" action="deleted" object="Second"/>'))

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid=123, action = 'modified',
                                                            feed = Message.feed2str[Message.NOTIF_FEED]))))\
            .count_equal('/messages/message', 2)
        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid=123, action = 'deleted',
                                                            feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .count_equal('/messages/message', 1)
        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid=123, action = 'other',
                                                            feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .not_exist('/messages/message')

    def testMessagesByEnv(self):
        self.assertEqual(True, self.servant.PutMessage([123], Message.NOTIF_FEED,
                                                       '<body by-uid="1" action="test" env="false" />'))
        self.assertEqual(True, self.servant.PutMessage([123], Message.NOTIF_FEED,
                                                       '<body by-uid="2" action="test" env="true" />'))
        self.assertEqual(True, self.servant.PutMessage([123], Message.NOTIF_FEED,
                                                       '<body by-uid="3" action="test" env="false" />'))

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid=123, env=False,
                                                            feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .count_equal('/messages/message', 2)
        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid=123, env=True,
                                                            feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .count_equal('/messages/message', 1)

    def testMessagesByTypeAndEnv(self):
        self.assertEqual(True, self.servant.PutMessage([123], Message.NOTIF_FEED, '<body by-uid="1" action="blah"  env="false" />'))
        self.assertEqual(True, self.servant.PutMessage([123], Message.NOTIF_FEED, '<body by-uid="2" action="minor" env="false" />'))
        self.assertEqual(True, self.servant.PutMessage([123], Message.NOTIF_FEED, '<body by-uid="3" action="blah"  env="false" />'))
        self.assertEqual(True, self.servant.PutMessage([123], Message.NOTIF_FEED, '<body by-uid="3" action="blah"  env="true" />'))

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid=123, action = 'blah',
                                                            feed = Message.feed2str[Message.NOTIF_FEED],
                                                            env = False)))) \
            .count_equal('/messages/message', 2)

    def testShowMyMessagesOnly(self):
        self.assertEqual(True, self.servant.PutMessage([1], Message.EDIT_FEED,
                                                       '<body by-uid="1" action="blah" />'))
        self.assertEqual(True, self.servant.PutMessage([2], Message.EDIT_FEED,
                                                       '<body by-uid="2" action="minor"/>'))
        self.assertEqual(True, self.servant.PutMessage([1], Message.EDIT_FEED,
                                                       '<body by-uid="1" action="blah" />'))
        self.assertEqual(True, self.servant.PutMessage([1], Message.NOTIF_FEED,
                                                       '<body by-uid="2" action="blah" />'))

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid = 1,
                                                            feed = Message.feed2str[Message.EDIT_FEED])))) \
            .count_equal('/messages/message', 2)

    def testShowAllExceptMyMessages(self):
        self.assertEqual(True, self.servant.PutMessage([1], Message.EDIT_FEED,
                                                       '<body by-uid="1" action="blah" />'))
        self.assertEqual(True, self.servant.PutMessage([2], Message.EDIT_FEED,
                                                       '<body by-uid="2" action="minor"/>'))
        self.assertEqual(True, self.servant.PutMessage([1], Message.EDIT_FEED,
                                                        '<body by-uid="1" action="blah" />'))
        self.assertEqual(True, self.servant.PutMessage([1], Message.NOTIF_FEED,
                                                       '<body by-uid="2" action="blah" />'))

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid = 1,
                                                            feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .count_equal('/messages/message', 1)

    def testGetMessagesHasDefaultLimit(self):
        for x in xrange(100):
            self.servant.PutMessage([1], Message.EDIT_FEED, '<body by-uid="1" action="blah" />')

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid = 1,
                                                            feed = Message.feed2str[Message.EDIT_FEED])))) \
            .count_equal('/messages/message', 10) \
            .equal('/messages/@page', '1') \
            .equal('/messages/@total-count', '100')

    def testGetMessagesSupportPaging(self):
        for x in xrange(100):
            self.servant.PutMessage([1], Message.EDIT_FEED, '<body by-uid="1" action="blah" num="%d"/>' % x)

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid = 1, page = 3, per_page = 5,
                                                            feed = Message.feed2str[Message.EDIT_FEED])))) \
            .count_equal('/messages/message', 5) \
            .equal('/messages/@page', '3') \
            .equal('/messages/@total-count', '100') \
            .not_exist('/messages/message/body[@num = "90"]') \
            .exist('/messages/message/body[@num = "89"]') \
            .exist('/messages/message/body[@num = "85"]') \
            .not_exist('/messages/message/body[@num = "84"]')

    def testCounters(self):
        self.servant.PutMessage([1], Message.NOTIF_FEED, '<body by-uid="2" action="object-new" env="false" />')
        self.servant.PutMessage([1], Message.NOTIF_FEED, '<body by-uid="2" action="object-new" env="false" />')
        self.servant.PutMessage([1], Message.NOTIF_FEED, '<body by-uid="3" action="object-changed" env="true" />')
        self.servant.PutMessage([1], Message.NOTIF_FEED, '<body by-uid="3" action="object-changed" env="false" />')
        self.servant.PutMessage([1], Message.NOTIF_FEED, '<body by-uid="2" action="object-deleted" env="true" />')

        # messages for another user
        self.servant.PutMessage([2], Message.EDIT_FEED, '<body by-uid="2" action="object-new" env="false" />')
        self.servant.PutMessage([2], Message.EDIT_FEED, '<body by-uid="2" action="object-changed" env="true" />')

        msgs = self.servant.GetMessagesX(simplejson.dumps(dict(uid=1,
                                            feed=Message.feed2str[Message.NOTIF_FEED])))
        AssertXML(msgs) \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "all"]', '5') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "env"]', '2') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "no-env"]', '3') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "all"]', '5') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-new"]', '2') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-changed"]', '2') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-deleted"]', '1')

        AssertXML(self.servant.GetMessagesX(
                simplejson.dumps(dict(uid = 1, feed = Message.feed2str[Message.NOTIF_FEED],
                                                            env = True)))) \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "all"]', '5') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "env"]', '2') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "no-env"]', '3') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "all"]', '2') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-changed"]', '1') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-deleted"]', '1')
       
        AssertXML(self.servant.GetMessagesX(
                simplejson.dumps(dict(uid = 1, feed = Message.feed2str[Message.NOTIF_FEED],
                                      env = False)))) \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "all"]', '5') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "env"]', '2') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "no-env"]', '3') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "all"]', '3') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-new"]', '2') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-changed"]', '1') 
  
        AssertXML(self.servant.GetMessagesX(
                simplejson.dumps(dict(uid = 1, feed = Message.feed2str[Message.NOTIF_FEED],
                                      action = 'object-changed')))) \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "all"]', '2') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "env"]', '1') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "no-env"]', '1') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "all"]', '5') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-new"]', '2') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-changed"]', '2') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-deleted"]', '1') 
        AssertXML(self.servant.GetMessagesX(
                simplejson.dumps(dict(uid = 1, feed = Message.feed2str[Message.NOTIF_FEED],
                                 action = 'object-new')))) \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "all"]', '2') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "env"]', '0') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "no-env"]', '2') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "all"]', '5') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-new"]', '2') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-changed"]', '2') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-deleted"]', '1') 

    def testMarkAsRead(self):
        self.servant.PutMessage([1], Message.NOTIF_FEED, '<body by-uid="2" action="object-new" env="false" />')
        self.servant.PutMessage([1], Message.NOTIF_FEED, '<body by-uid="2" action="object-new" env="false" />')
        self.servant.PutMessage([1], Message.NOTIF_FEED, '<body by-uid="2" action="object-new" env="false" />')

        xml = ET.fromstring(self.servant.GetMessagesX(
            simplejson.dumps(dict(uid = 1,
                            feed = Message.feed2str[Message.NOTIF_FEED]))))
        ids = [int(m.attrib['id']) for m in xml.findall('message')]

        AssertXML(self.servant.GetMessagesX(
            simplejson.dumps(dict(uid = 1, feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .count_equal('/messages/message', 3)

        a = AssertXML(self.servant.MarkMessagesAsReadX(1, '%d,%d' % (ids[0], ids[2])))
        token = a.xml.xpath('wm:token',
                             namespaces = {'wm': "http://maps.yandex.ru/wikimap/1.x"})[0].text
        a.exist('/ok')

        AssertXML(self.servant.GetMessagesX(
            simplejson.dumps(dict(uid = 1, token = token,
                            feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .count_equal('/messages/message', 1)
        AssertXML(self.servant.GetMessagesX(
            simplejson.dumps(dict(uid = 1, token = token, show_read = True,
                            feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .count_equal('/messages/message', 3)

    def testRemoveMessageByObjectId(self):
        uid = 1
        self.servant.PutMessage([uid], Message.NOTIF_FEED, '<body by-uid="2" action="object-new" object-id="123" env="false" />')
        self.servant.PutMessage([uid], Message.NOTIF_FEED, '<body by-uid="2" action="complain" object-id="123" env="false" />')

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid = uid,
                                                            feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-new"]', '1') \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "complain"]', '1')

        AssertXML(self.servant.MarkMessagesAsReadX2(uid, '{"action": "complain", "object_id": 123}')) \
            .exist('/ok')

        AssertXML(self.servant.GetMessagesX(
                simplejson.dumps(dict(uid = uid, feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .equal('/messages/counters/group[@by = "action"]/counter[@by = "object-new"]', '1') 

    def testRemoveMessageByEnv(self):
        uid = 1
        self.servant.PutMessage([uid], Message.NOTIF_FEED,
                                '<body by-uid="2" action="object-new" object-id="123" env="false" />')
        self.servant.PutMessage([uid], Message.NOTIF_FEED,
                                '<body by-uid="2" action="object-new" object-id="124" env="true" />')

        AssertXML(self.servant.MarkMessagesAsReadX2(uid,
                                                    simplejson.dumps(dict(env=False)))).exist('/ok')

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid = uid,
                                                            feed = Message.feed2str[Message.NOTIF_FEED]))))\
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "env"]', '1')\
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "no-env"]', '0')

        AssertXML(self.servant.MarkMessagesAsReadX2(uid, simplejson.dumps(dict(env=True)))).exist('/ok')

        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid = uid,
                                                            feed = Message.feed2str[Message.NOTIF_FEED])))) \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "env"]', '0') \
            .equal('/messages/counters/group[@by = "env"]/counter[@by = "no-env"]', '0')

    def testEmptyUid(self):
        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid = '')))).exist('/error')\
            .equal('/error/@status', "USER_NOT_REGISTERED")

    def testTimeLimitedCounters(self):
        uid = 1
        feed = Message.NOTIF_FEED
        feed_str = Message.feed2str[Message.NOTIF_FEED]
        params = dict(uid=uid, feed=feed, data='<test/>', action='object-new', object_id=1, env=False)
        now = utils.utcnow()
        with db.get_write_session('social') as session:
            for i in range(10):
                session.add(Message(date = now - timedelta(hours=i), **params))
        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid=uid, feed=feed_str)))).count_equal('/messages/message', 10)
        AssertXML(self.servant.GetMessagesX(simplejson.dumps(dict(uid=uid, feed=feed_str, time_limit=60 * 70))))\
            .count_equal('/messages/message', 2)

