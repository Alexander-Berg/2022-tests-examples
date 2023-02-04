import pytest

from django.template import Template, Context


@pytest.mark.parametrize('test_case', (
    # Проверяем, что работает без ничего
    '{% load utils %}'
    '{% onlyspaces %}'
    '\n\n\n\ntest\n\nok\n\n'
    '{% endonlyspaces %}',

    # Проверяем, что не ломает фильтры
    '{% load utils %}'
    '{% onlyspaces %}'
    '\n\n\n\ntest\n\n{{ True|yesno:"ok,not ok" }}\n\n'
    '{% endonlyspaces %}',

    # Проверяем, что не ломает теги
    '{% load utils %}'
    '{% onlyspaces %}'
    '\n\n\n\ntest\n\n{% if True %}\n\n\nok{% endif %}\n\n'
    '{% endonlyspaces %}',

    # Проверяем, что не ломает теги перед собой и после
    '{% load utils %}'
    '{% if True %}test{% endif %}{% onlyspaces %}'
    '\n\n\n\n'
    '{% endonlyspaces %} {% if True %}ok{% endif %}',
))
def test_onlyspaces(test_case):
    template = Template(test_case)
    context = Context()
    assert template.render(context) == 'test ok'
