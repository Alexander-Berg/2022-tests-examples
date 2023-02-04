import infra.reconf_juggler


class TaggedCheck(infra.reconf_juggler.Check):
    opt_handlers = ('_tags',)  # tags only, for shortage
    validate_class = False  # disable docstrings check etc
    level_autotags = False


def test_category():
    class MaintainedCheck(TaggedCheck):
        category = 'my-shiny-category'

    expected = {'tags': ['category_my-shiny-category']}
    assert expected == MaintainedCheck().build()


def test_level_tags():
    class LevelCheck(TaggedCheck):
        level_autotags = True

    check = LevelCheck({
        'children': {
            '00': {
                'children': {
                    '01': {}
                }
            }
        }
    })

    expected = {
        'children': {
            '00:LevelCheck': {
                'children': {
                    '01:LevelCheck': {
                        'tags': ['level_leaf']
                    }
                },
                'tags': []
            }
        },
        'tags': ['level_root']
    }
    assert expected == check.build()


def test_maintainers():
    class MaintainedCheck(TaggedCheck):
        maintainers = ('first', 'second')

    expected = {'tags': ['maintainer_first', 'maintainer_second']}
    assert expected == MaintainedCheck().build()
