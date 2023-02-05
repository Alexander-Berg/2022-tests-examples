# Alexander Artemenko <art@yandex-team.ru>
# 2009, Yandex
from __future__ import with_statement

from pdb import set_trace

import unittest
from WikimapPy import db

def doc_test(value):
    """ This is example doctest
    >>> doc_test(1)
    2
    >>> doc_test(2)
    4
    """

    return value * 2

class Example(unittest.TestCase):

    def testOneSessionPerThread(self):
        with db.get_write_session() as s1:
            with db.get_write_session() as s2:
                self.assert_(s1 is s2)


    def testNestedTransactions(self):

        @db.write_session('core')
        def update_stats(value, session):
            session.execute(
                'INSERT INTO social.user_stats (uid, votes) VALUES(:uid, :votes)',
                {'uid': 123, 'votes':value})
            session.commit()

        @db.read_session('core')
        def check_stats(value, session):
            self.assertEqual(value, session.execute(
                'SELECT votes FROM social.user_stats WHERE uid=:uid ',
                {'uid': 123 }).scalar())

        with db.get_write_session('core') as sess:
            trans = sess.real_begin()
            trans2 = sess.begin()
            self.assert_(trans is trans2)

            check_stats(None)
            update_stats(567)
            check_stats(567)

            trans.real_rollback()
            check_stats(None)


if __name__ == '__main__':
    unittest.main()
