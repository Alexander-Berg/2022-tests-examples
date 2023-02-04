import pytest

from intranet.search.core.query import parse_query
from intranet.search.abovemeta.decider.triggers import ContentTrigger


@pytest.mark.parametrize('trigger_words,query', [
    (['задача'], 'задача поиска по интранету'),
    (['задача'], 'Задача поиска по интранету'),
    (['задача'], 'ЗАДАЧА поиска по интранету'),
    (['задача'], 'поиска по интранету задачи'),
    (['задача'], 'поиска задачи по интранету'),
    (['задача'], 'задачи поиска по интранету'),
    (['тикет', 'задача', 'таска'], 'поисковый тикет про формулу'),
    (['тикет', 'задача', 'таска'], 'поисковая задача про формулу'),
    (['тикет', 'задача', 'таска'], 'поисковая таска про формулу'),
])
def test_content_trigger_match_by_words(trigger_words, query):
    trigger = ContentTrigger(trigger_words)
    context = {'qtree': parse_query(query)}

    assert trigger(context).result()


@pytest.mark.parametrize('trigger_words,query', [
    (['задача'], 'поисковый запрос'),
    (['тикет', 'задача', 'таска'], 'поисковый запрос'),
])
def test_content_trigger_not_match_by_another_words(trigger_words, query):
    trigger = ContentTrigger(trigger_words)
    context = {'qtree': parse_query(query)}

    assert not trigger(context).result()


@pytest.mark.parametrize('query,expected_replaced_query', [
    ('задачи isearch', 'isearch'),
    ('Задачи isearch', 'isearch'),
    ('ЗАДАЧА isearch', 'isearch'),
    ('isearch задачи', 'isearch'),
    ('isearch задачи в трекере', 'isearch  в трекере'),

    # если замена приводит к пустому запросу, то она не делается
    ('задачи', 'задачи'),
    ('задачи задачи', 'задачи задачи'),
])
def test_content_trigger_replace_query_enabled(query, expected_replaced_query):
    context = {'qtree': parse_query(query)}
    trigger = ContentTrigger(['задача'], replace_query=True)

    assert trigger(context).result()
    assert context['qtree'].to_string() == expected_replaced_query


@pytest.mark.parametrize('query', [
    'задачи isearch',
    'Задачи isearch',
    'ЗАДАЧИ isearch',
    'isearch задачи',
    'isearch задачи в трекере',
    'задача',
])
def test_content_trigger_replace_query_disabled(query):
    context = {'qtree': parse_query(query)}
    trigger = ContentTrigger(['задача'], replace_query=False)

    assert trigger(context).result()
    assert context['qtree'].to_string() == query
