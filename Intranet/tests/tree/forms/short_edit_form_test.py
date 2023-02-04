from random import random

from django.conf import settings

from staff.departments.tree.forms import ShortEditForm


def test_short_edit_form_wiki_page_invalid_host():
    target = ShortEditForm(data={
        'wiki_page': 'https://host/test',
    })

    result = target.is_valid()

    assert result is False
    assert 'wiki_page' in target.errors['errors']
    assert any(x.get('code') == 'invalid' for x in target.errors['errors']['wiki_page'])


def test_short_edit_form_wiki_page_with_host():
    page = f'page {random()}'
    target = ShortEditForm(data={
        'wiki_page': f'https://{settings.WIKI_HOST}/{page}',
    })

    result = target.is_valid()

    assert result is True
    assert target.cleaned_data['wiki_page'] == page


def test_short_edit_form_wiki_page_with_leading_slash():
    page = f'page {random()}'
    target = ShortEditForm(data={
        'wiki_page': f'/{page}',
    })

    result = target.is_valid()

    assert result is True
    assert target.cleaned_data['wiki_page'] == page


def test_short_edit_form_wiki_page():
    page = f'page {random()}'
    target = ShortEditForm(data={
        'wiki_page': page,
    })

    result = target.is_valid()

    assert result is True
    assert target.cleaned_data['wiki_page'] == page


def test_short_edit_form_wiki_page_scheme_without_host():
    target = ShortEditForm(data={
        'wiki_page': 'https:///test',
    })

    result = target.is_valid()

    assert result is False
    assert 'wiki_page' in target.errors['errors']
    assert any(x.get('code') == 'invalid' for x in target.errors['errors']['wiki_page'])
