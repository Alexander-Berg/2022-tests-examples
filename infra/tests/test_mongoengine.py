"""Tests MongoEngine behaviour."""


from mongoengine import Q, Document, StringField


def test_query_object_behaviour():
    """
    MongoEngine's query object has a very convenient but very strange behaviour. We rely on it, but just in case test
    the behaviour here to be sure that any future versions of MongoEngine won't break anything.
    """

    class Doc(Document):
        name = StringField()

    assert Q().to_query(Doc) == {}
    assert Q(name="test").to_query(Doc) == {"name": "test"}

    assert (Q() | Q(name="test")).to_query(Doc) == {"name": "test"}
    assert (Q() & Q(name="test")).to_query(Doc) == {"name": "test"}
