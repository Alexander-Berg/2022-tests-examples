from blinovmatcher import parse, Filter, Tag, TagType


def test_main():
    I = lambda name: Tag(TagType.INSTANCE, name)
    rule = Filter(parse('[I@a . I@b] - [I@c I@d]'))
    assert not rule.apply([I('a')])
    assert rule.apply([I('a'), I('b')])
    assert not rule.apply([I('a'), I('b'), I('c')])

    rule = Filter(parse('I@A . I@B'))
    assert not rule.apply([I('A')])
    assert rule.apply([I('A'), I('B')])
