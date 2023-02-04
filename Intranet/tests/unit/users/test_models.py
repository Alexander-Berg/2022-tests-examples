from intranet.femida.tests.factories import UserFactory


def test_subordinates_rec():
    user1 = UserFactory.create(username='aba')
    user2 = UserFactory.create(username='caba', chief=user1)
    user3 = UserFactory.create(username='daba', chief=user2)
    user4 = UserFactory.create(username='eaba', chief=user3)
    user5 = UserFactory.create(username='faba', chief=user1)
    subordinates = user1.subordinates_rec
    expected = set([user2, user3, user4, user5])
    assert (
        len(subordinates) == len(expected)
        and set(subordinates) == expected
    )
