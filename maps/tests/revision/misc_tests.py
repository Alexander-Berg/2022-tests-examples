from maps.wikimap.mapspro.libs.python import revision as rev
from maps.wikimap.mapspro.libs.python.revision import filters as rf


def test_create_attrs():
    rev.Attributes()
    rev.Attributes(dict(a="b"))


def test_create_object_data():
    rev.ObjectData(1)
    rev.ObjectData(1, attrs=rev.Attributes())
    rev.ObjectData(1, attrs=dict(a="a"), geom="POLYGON ()",
                   relation_data=rev.RelationData(1, 2))


def test_create_filters():
    NOTIF_CATEGORIES = ["cat:rd", "cat:aoi"]
    cat_filter = rf.Attr(NOTIF_CATEGORIES[0]).defined()
    for cat in NOTIF_CATEGORIES[1:]:
        cat_filter = rf.or_(rf.Attr(cat).defined(), cat_filter)
    rf.and_(cat_filter, rf.Geom.intersects(0, 0, 10, 10))
    rf.and_(cat_filter, rf.not_(rf.ObjRevAttr.object_id().is_null()))
