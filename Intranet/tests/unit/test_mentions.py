
import pytest

from at.aux_.models import Person
from at.common import utils
from at.common.postlink import MyUrlParser

pytestmark = pytest.mark.django_db

DEBUG_UIDS = {
    'default_blog_id': 1120000000000227,
    'default_club_id': 4611686018427387905
}


NAMES = ['club', 'yauser', 'post', 'comment', 'blog', 'new_post', 'new_comment']
RES = {}
for name in NAMES:
    globals()[name + '_re'] = RES[name] = \
            getattr(MyUrlParser, name + '_regexp')


def user_or_club(link, pattern, feed_type):
    re = RES[pattern]
    for match in re.finditer(link):
        try:
            feed_id = MyUrlParser(**DEBUG_UIDS).get_feed_id(match)
        except:
            print(match.groups())
            raise
        assert utils.is_community_id(feed_id) == (feed_type=='club'), \
                '%s should be %s %s %s' %\
                (feed_id, feed_type, match.groups(), match.re.pattern)



def only_one_of(link, good_name):
    for name, re in RES.items():
        assert bool(re.search(link)) == (good_name == name), \
                'should%s match %s' % ('' if good_name == name else ' NOT', re.pattern)



post_links = [
        ('https://kukutz.at.yandex-team.ru/replies.xml?item_no=1234567', 'post', (1120000000000227, 1234567, 0)),
        ('https://kukutz.at.yandex-team.ru/1234/', 'new_post', (1120000000000227, 1234, 0)),
        ('https://kukutz.at.yandex-team.ru/1234', 'new_post', (1120000000000227, 1234, 0)),
        ('http://kukutz.at.yandex-team.ru/1234/5678', 'new_comment', (1120000000000227, 1234, 5678)),
        ('https://kukutz.at.yandex-team.ru/replies.xml?parent_id=12345&item_no=1234567', 'comment', (1120000000000227, 1234567, 12345)),
        ]

feed_links = [
        ('http://kukutz.at.yandex-team.ru', 'blog', (1120000000000227, 0, 0)),
        ('http://clubs.at.yandex-team.ru/sex-toys/', 'club', \
                (DEBUG_UIDS['default_club_id'], 0, 0)),
        ]

html_samples = [
        ('<ya user="kukutz"/>', 'yauser', (1120000000000227, 0, 0)),
        ('<a href="http://clubs.at.yandex-team.ru/4611686018427387905/1234/5678">aaaaa</a>', 'new_comment', (4611686018427387905, 1234, 5678)),
        ]

all_samples = post_links + feed_links + html_samples


@pytest.mark.parametrize(
    'link,pattern,_',
    all_samples
)
def test_patterns(link, pattern, _):
    re = RES[pattern]
    assert re.search(link), '%s should match %s' % (link, re.pattern)


mmoo = '<p><a href="http://mmoo.at.yandex-team.ru/4136/4164#reply-mmoo-4164">http://mmoo.at.yandex-tea<ya-wbr/>m.ru/4136/4164#reply-mmoo<ya-wbr/>-4164</a></p>'


@pytest.fixture
def prepare_db():
    Person.objects.create(login='mmoo', person_id=1270000002322323, community_type='NONE')
    Person.objects.create(login='kukutz', person_id=1120000000000227, community_type='NONE')


@pytest.mark.usefixtures('prepare_db')
def test_mmoo():
    matched = []
    for name, re in list(RES.items()):
        try:
            match = re.search(mmoo)
            assert match is not None
            feed_id = MyUrlParser(**DEBUG_UIDS).get_feed_id(match)
            matched.append(name)
        except AssertionError:
            pass
    assert matched == ['new_comment'], 'Matched nothing'


@pytest.mark.usefixtures('prepare_db')
def test_long_iter():
    text = '''
Roman <ya user="kukutz"/> in his <a href="https://kukutz.at.yandex-team.ru">blog</a>
has mentioned <a href="https://kukutz.at.yandex-team.ru/123/456">his post</a>.
'''
    parser = MyUrlParser(**DEBUG_UIDS)
    result = list(parser.finditer(text))
    assert sorted(result) == [
            (1120000000000227, 0, 0),
            (1120000000000227, 0, 0),
            (1120000000000227, 123, 456),
            ], '%s + %s' % (
                    list(parser.finditer_item(text)),
                    list(parser.finditer_blog(text))
                    )


@pytest.mark.usefixtures('prepare_db')
@pytest.mark.parametrize(
    'link, _, post_coords',
    post_links + feed_links,
)
def test_parser_match(link, _, post_coords):
    parser = MyUrlParser(**DEBUG_UIDS)
    res = parser.match(link)
    assert post_coords == res, '%s should match %s, got %s' % (
        link, post_coords, res)


