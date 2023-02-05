from WikimapPy import layers


def test_load_layers():
    l = layers.load_layers_info(open('../../configs/servant/map/map.xml'),
                                    open('../../configs/common/wikimaps.xml'))
    hl = l[1]
    assert(hl.lname == 'houses')
    assert(hl.tablename == 'objects_1_houses')

def test_load_categories():
    categories = layers.load_categories_info(open('../../configs/common/wikimaps.xml'))
    assert(categories)
