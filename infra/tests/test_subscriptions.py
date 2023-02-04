import random
# import time

from unittest import main
from sqlalchemy import select

from lib.subscriptions import SubscriptionsCache
from common import FlaskTest


def count(iter):
    n = 0
    for _ in iter:
        n += 1
    return n


class TestSubscriptions(FlaskTest):
    def setUp(self):
        super().setUp()

        db = self.app.database

        for tag in range(1001):
            db.session.execute(
                self.tags_table.insert().values(tag_id=tag, name="tag" + str(tag))
            )

        for sub in range(10000):
            tags = random.sample(range(0, 1001), random.randint(1, 5))
            rule = '&'.join(str(tag) for tag in tags)
            positive = count(filter(lambda n: n < 500, tags))
            negative = count(tags) - positive

            rowid = db.session.execute(
                self.subscriptions_table.insert().values(
                    rule=rule,
                    positive_clauses=positive,
                    negative_clauses=negative,
                    is_group=False,
                    name="user" + str(sub),
                )
            ).inserted_primary_key[0]

            for tag in tags:
                db.session.execute(
                    self.subscriptions_tags_table.insert().values(
                        subscription_id=rowid, tag_id=tag, invert=int(tag >= 500)
                    )
                )
        db.session.flush()
        db.session.commit()

    def tearDown(self):
        db = self.app.database

        db.session.execute(
            self.tags_table.delete(),
        )
        db.session.execute(
            self.subscriptions_table.delete(),
        )
        db.session.execute(
            self.subscriptions_tags_table.delete(),
        )
        db.session.flush()
        db.session.commit()
        super().tearDown()

    def test_subscriptions(self):
        subscriptions = SubscriptionsCache(self.app.database_pool)

        for i in range(10):
            tags = random.sample(range(0, 1001), random.randint(1, 5))
            # t = time.time()
            users, groups = subscriptions.subscribers_for(["tag" + str(tag) for tag in tags])
            # print("select %.3f users(%d) and groups(%d) for %d tags" %
            #       (time.time() - t, len(users), len(groups), len(tags)))
            self.assertFalse(groups)

            db = self.app.database
            # I'm not using prepared statements here because there're too many variables for sqlite to process
            cursor = db.session.execute(
                select(
                    [self.subscriptions_table.c.rule, self.subscriptions_table.c.name]
                ).where(
                    self.subscriptions_table.c.name.in_(list(users))
                )
            )
            for record in cursor.fetchall():
                chunks = [int(chunk) for chunk in record.rule.split('&')]
                for chunk in chunks:
                    if chunk >= 500:
                        self.assertNotIn(chunk, tags)
                    else:
                        self.assertIn(chunk, tags)
                users.pop(record.name)
            self.assertFalse(users)


class TestFindSubscriptions(FlaskTest):
    def tearDown(self):
        db = self.app.database

        db.session.execute(
            self.tags_table.delete(),
        )
        db.session.execute(
            self.subscriptions_table.delete(),
        )
        db.session.execute(
            self.subscriptions_tags_table.delete(),
        )
        db.session.flush()
        db.session.commit()
        super().tearDown()

    def test_find_subscriptions_by_tags(self):
        subscriptions = SubscriptionsCache(self.app.database_pool)

        subscriptions.add_subscription(name='torkve', is_group=False, tags=['a', '!b'], settings={})
        subscriptions.add_subscription(name='torkve', is_group=False, tags=['!b'], settings={})
        subscriptions.add_subscription(name='torkve2', is_group=False, tags=['c', '!b'], settings={})
        subscriptions.add_subscription(name='torkve3', is_group=True, tags=['a', 'c', '!b'], settings={})
        subscriptions.add_subscription(name='torkve4', is_group=False, tags=['a', '!c'], settings={})
        subscriptions.add_subscription(name='torkve5', is_group=True, tags=['a', '!c', '!b'], settings={})
        subscriptions.add_subscription(name='torkve6', is_group=False, tags=['a', '!b'], settings={})

        users, groups = subscriptions.find_subscriptions_by_tags(['a', '!b'], exact=False)
        assert len(users) == 2 and 'torkve' in users and 'torkve6' in users
        assert len(users['torkve']) == 2, users
        assert {'options': {}, 'tags': ['a', '!b']} in users['torkve'].values()
        assert {'options': {}, 'tags': ['!b']} in users['torkve'].values()

        assert len(users['torkve6']) == 1
        assert {'options': {}, 'tags': ['a', '!b']} in users['torkve6'].values()

        assert len(groups) == 1 and 'torkve5' in groups
        assert len(groups['torkve5']) == 1
        assert {'options': {}, 'tags': ['a', '!c', '!b']} in groups['torkve5'].values()

        users, groups = subscriptions.find_subscriptions_by_tags(['a', '!b'], exact=True)
        assert len(users) == 2 and 'torkve' in users and 'torkve6' in users
        assert len(users['torkve']) == 1, users
        assert {'options': {}, 'tags': ['a', '!b']} in users['torkve'].values()

        assert len(users['torkve6']) == 1
        assert {'options': {}, 'tags': ['a', '!b']} in users['torkve6'].values()

        assert len(groups) == 0

    def test_find_subscriptions_with_options(self):
        subscriptions = SubscriptionsCache(self.app.database_pool)

        login = 'torkve'
        tags = ['ya.deploy', 'stage:id:torkve-test2-stage']
        options = {'telegram': {'enabled': 'true'}}

        subscriptions.add_subscription(
            name=login,
            is_group=False,
            tags=tags,
            settings=options,
        )

        for exact in (True, False):
            users, groups = subscriptions.find_subscriptions_by_tags(tags, exact=exact)

            assert not groups
            assert len(users) == 1
            assert login in users
            subs = users[login]

            assert len(subs) == 1
            sub = next(iter(subs.values()))

            assert set(sub['tags']) == set(tags)
            assert sub['options'] == options


if __name__ == '__main__':
    main()
