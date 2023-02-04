# coding: utf-8

from operator import attrgetter

from lxml import etree


def xml_diff(first, second, consider_order=True,
             ignore=None, ignore_attrs=None, ignore_empty=None):
    """
    Проверяем различия между двумя xml объектами.
    Возращается обхект XmlDiff. Если найдется различие в структуре, то
    обработка прерывается и возвращается результат, потому что различия в
    контенте в разных по структуре деревьях искать обычно имеет мало смысла.
    В ignore можно передавать список игнорируемых тегов в виде
    ['entry/meta', 'entry/body/div']
    """
    ignore = ignore or []
    ignore_attrs = ignore_attrs or []
    ignore_empty = ignore_empty or []
    if isinstance(first, str):
        first = etree.fromstring(first.encode('utf-8'))
    if isinstance(second, str):
        second = etree.fromstring(second.encode('utf-8'))

    if isinstance(first, bytes):
        first = etree.fromstring(first)
    if isinstance(second, bytes):
        second = etree.fromstring(second)

    diff = XmlDiff()

    if first.tag != second.tag:
        msg = 'Tags do not match: %s and %s' % (first.tag, second.tag)
        diff.structure_diff = msg
        return diff

    tag = first.tag

    if tag in ignore:
        return diff

    for name, value in list(first.attrib.items()):
        if name in ignore_attrs:
            continue
        attr_value_in_second = second.attrib.get(name)
        if attr_value_in_second != value:
            msg = 'Attributes do not match: %s=%r, %s=%r' % (
                name, value, name, attr_value_in_second)
            diff.structure_diff = msg
            return diff

    for name in list(second.attrib.keys()):
        if name in ignore_attrs:
            continue
        if name not in first.attrib:
            msg = 'second has an attribute first is missing: %s' % name
            diff.structure_diff = msg
            return diff

    text_diff = text_compare(first.text, second.text)
    if text_diff:
        diff.content_diff[tag] = first.text, second.text

    text_diff = text_compare(first.tail, second.tail)
    if text_diff:
        first_diff, second_diff = diff.content_diff.get(tag, ('', ''))
        diff.content_diff[tag] = (
            first_diff + first.tail,
            second_diff + second.tail,
        )

    children_first = first.getchildren()
    children_second = second.getchildren()

    check_node = lambda node: not _should_ignore_node(
        tag, node, ignore, ignore_empty)
    children_first = list(filter(check_node, children_first))
    children_second = list(filter(check_node, children_second))

    if len(children_first) != len(children_second):
        children_first_tags = set(c.tag for c in children_first)
        children_second_tags = set(c.tag for c in children_second)
        child_difference = ''
        if children_first_tags - children_second_tags:
            child_difference += 'In first, but not in second: %s\n' % (
                children_first_tags - children_second_tags
            )
        if children_second_tags - children_first_tags:
            child_difference += 'In second, but not in first: %s\n' % (
                children_second_tags - children_first_tags
            )
        msg = '<%s> children length differs, %i != %i. %s' % (
            tag, len(children_first), len(children_second),
            child_difference
        )
        diff.structure_diff = msg
        return diff

    sorter = attrgetter('tag', 'attrib')
    if not consider_order:
        pairs = list(zip(
            sorted(children_first, key=sorter),
            sorted(children_second, key=sorter),
        ))
    else:
        pairs = list(zip(children_first, children_second))

    for index, (child_first, child_second) in enumerate(pairs):
        ignore_for_children = [
            _get_tag_chain_tail(tag_chain)
            for tag_chain in ignore
            if _get_tag_chain_tail(tag_chain)
        ]
        ignore_empty_for_children = [
            _get_tag_chain_tail(tag_chain)
            for tag_chain in ignore_empty
            if _get_tag_chain_tail(tag_chain)
        ]

        children_diff = xml_diff(
            child_first, child_second,
            consider_order=consider_order,
            ignore=ignore_for_children,
            ignore_empty=ignore_empty_for_children,
        )
        if children_diff.is_structure_different:
            msg = '. '.join([
                'children %i do not match: %s' % (index, child_first.tag),
                children_diff.structure_diff
            ])
            diff.structure_diff = msg
            return diff

        if children_diff.is_content_different:
            for key, value in list(children_diff.content_diff.items()):
                diff.content_diff[tag + '/' + key] = value

    return diff


def text_compare(first_text, second_text):
    if not first_text and not second_text:
        return
    if first_text == '*' or second_text == '*':
        return

    first_text = (first_text or '').strip()
    second_text = (second_text or '').strip()
    if first_text == second_text:
        return
    return first_text, second_text


def _get_tag_chain_tail(tag_chain):
    if '/' not in tag_chain:
        return
    head, tail = tag_chain.split('/', 1)
    return tail


def _should_ignore_node(parent_tag, child, ignore, ignore_empty):
    if isinstance(child, etree._Comment):
        return True

    child_tag = child.tag

    for ignored_tag in ignore:
        if '/' not in ignored_tag:
            continue
        head, tail = ignored_tag.split('/', 1)

        if head == parent_tag and tail == child_tag:
            return True

    for ignored_empty_tag in ignore_empty:
        if '/' not in ignored_empty_tag:
            continue
        head, tail = ignored_empty_tag.split('/', 1)

        if head == parent_tag and tail == child_tag and not child.text:
            return True
    return False


class SimpleReporter(object):

    def __init__(self):
        self.msg = None

    def __call__(self, msg):
        self.msg = msg


class XmlDiff(object):

    def __init__(self):
        # пока храним объекты в простых структурах/типах, если нужно что-то
        # более интеллектуальное для последующей обработки — можно усложнить.
        self.structure_diff = None
        self.content_diff = {}

    def __bool__(self):
        return self.is_different

    def __str__(self):
        return 'structure: %s\ncontent:%s' % (self.structure_diff, self.content_diff)

    # имена с точки зрения английского «ну такие, не очень»
    @property
    def is_structure_different(self):
        return self.structure_diff is not None

    @property
    def is_content_different(self):
        return bool(self.content_diff)

    @property
    def is_different(self):
        return self.is_structure_different or self.is_content_different
