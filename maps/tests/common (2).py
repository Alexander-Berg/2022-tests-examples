import datetime
import unittest
import re
import Yandex

from mock import Mock
from operator import itemgetter
from itertools import groupby
from pumper.event import Event
from WikimapPy import db
from WikimapPy.models import new_user_settings
from ytools.config import Config


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


def to_unicode(val):
    if isinstance(val, str):
        return val.decode('utf-8')
    return val


def create_user(auth_info):
    with db.get_write_session() as session:
        config = Config()
        session.add(new_user_settings(config, uid=auth_info.uid,
                                      registration_date=datetime.datetime.now()))
        session.flush()
class MockPumper(object):
    """ Replace for PumperFirer (for unittesting).
    """
    def __init__(self, *args):
        self.events = []

    def fireEvent(self, event_type, **params):
        e = Event(event_type, **params)
        self.events.append(e)
        return e.get_id()

mock_datetime = Mock(wraps = datetime.datetime)


class Comments(object):
    """Stub object for Comments servant"""

    comment_re = re.compile('child-(\d+)-(\d+)')
    comment_template = "child-1-%s"

    class Comment:

        def __init__(self, root, parent, uid, root_owner, title, body, params, props):
            self.root = root
            self.parent = parent
            self.uid = uid
            self.root_owner = root_owner
            self.title = title
            self.body = body
            self.params = params
            self.props = props

    def __init__(self):
        self.comments = {}
        self.counter = 1

    def changeComment(self, root, comment_id, title, body, params):
        comment = self.comments[comment_id]
        comment.commentXml = "<body>%s</body>" % body

    def changeCommentProperties(self, root, comment_id, properties):
        self.comments[comment_id].props = properties

    def addComment(self, root, parent, uid, rootOwner, title, comment, params, props):
        cid = self.comment_template % self.counter
        commentXml = "<body>%s</body>" % comment
        self.comments[cid] = Yandex.Comments.Comment2(root, cid, parent, 0, uid,
                                                      commentXml, False, props)
        self.counter += 1
        return self.comments[cid]

    def deleteComment(self, root, comment_id):
        del self.comments[comment_id]

    def getComment(self, root, comment_id):
        return self.comments[comment_id]
