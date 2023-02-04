# coding: utf-8

"""HttpRequests collection for different kind of posts"""

from at.common import utils


comments = {
    'text': {
        'simple_first_level': {'body': 'Text comment first level', 'item_no': '4', 'userpic': '',
                               
                               'access_type': 'public',
                                'parent_id': '0',
                               'host_id': '4611686018427389210', 'OK': 'OK', 'type': 'text'},
        'with_tb_first_level': {'body': 'Text comment first level with tb', 'item_no': '4', 'userpic': '',
                                
                                'access_type': 'public',
                                 'parent_id': '0',
                                'host_id': '4611686018427389210', 'trackback': '1', 'OK': 'OK', 'type': 'text'},
        'simple_second_level': {'body': 'Text comment second level', 'item_no': '4', 'userpic': '',
                                
                                'access_type': '',
                                 'parent_id': '6',
                                'host_id': '4611686018427389210', 'OK': 'OK', 'type': 'text'},
        'with_tb_second_level': {'body': 'With tb second level', 'item_no': '4', 'userpic': '',
                                 
                                 'access_type': '',
                                  'parent_id': '6',
                                 'host_id': '4611686018427389210', 'trackback': '1', 'OK': 'OK', 'type': 'text'},

    },
    'mood': {
        'first_level': {'body': 'change mood via comment', 'item_no': '4', 'userpic': '',
                         'access_type': 'public',
                         'parent_id': '0',
                        'host_id': '4611686018427389210', 'trackback': '1', 'OK': 'OK', 'type': 'status'},
        'second_level': {'body': 'mood second level', 'item_no': '4', 'userpic': '',
                          'access_type': '',
                          'parent_id': '6',
                         'host_id': '4611686018427389210', 'trackback': '1', 'OK': 'OK', 'type': 'status'}
    },

    'link': {
        'first_level': {'body': 'Link via comment', 'userpic': '', 'tags': 'asdasd',
                        'URL': 'https://clubs.msahnov.at.umbriel.yandex-team.ru/4611686018427389210/4/',
                        'access_type': 'public', 'tag': 'tagfromsuggest', 'host_id': '4611686018427389210',
                        'trackback': '1', 'item_no': '4', 'OK': 'OK', 'title': 'New title',
                         'parent_id': '0', 'type': 'link',
                        'shared_post_id': '4611686018427389210.4.0'},
        'second_level': {'body': 'Link second level', 'userpic': '', 'tags': '',
                         'URL': 'https://clubs.msahnov.at.umbriel.yandex-team.ru/4611686018427389210/4/6#reply-4611686018427389210-6',
                         'access_type': '', 'tag': '', 'host_id': '4611686018427389210', 'trackback': '1',
                         'item_no': '4', 'OK': 'OK', 'title': 'New title',
                          'parent_id': '6', 'type': 'link',
                         'shared_post_id': '4611686018427389210.4.6'},
    },
    'summon': {
        'first_level_person': {'body': 'summon person first level ', 'item_no': '4', 'userpic': '',
                               'text': '\u0421\u0435\u0440\u0433\u0435\u0439 \u0427\u0438\u0441\u0442\u043e\u0432\u0438\u0447',
                               'access_type': 'public', 'summon': 'theigel',
                                'parent_id': '0',
                               'host_id': '4611686018427389210', 'trackback': '0', 'OK': 'OK', 'type': 'summon'},
        'first_level_ml': {'body': 'summon ml first level', 'item_no': '4', 'userpic': '',
                           'text': 'ya-logs-digest@yandex-team.ru',
                           'access_type': 'public', 'summon': 'ya-logs-digest@yandex-team.ru',
                            'parent_id': '0',
                           'host_id': '4611686018427389210', 'trackback': '0', 'OK': 'OK', 'type': 'summon'},
        'second_level_person': {'body': 'Summon person second level', 'item_no': '4', 'userpic': '',
                                'text': '\u0421\u0435\u0440\u0433\u0435\u0439 \u0427\u0438\u0441\u0442\u043e\u0432\u0438\u0447',
                                'access_type': '', 'summon': 'theigel',
                                'parent_id': '6', 'host_id': '4611686018427389210', 'trackback': '0', 'OK': 'OK',
                                'type': 'summon'},
        'second_level_ml': {'body': 'Summon ml second level', 'item_no': '4', 'userpic': '',
                            'text': 'ya-logs-digest@yandex-team.ru',
                            'access_type': '', 'summon': 'ya-logs-digest@yandex-team.ru',
                             'parent_id': '6',
                            'host_id': '4611686018427389210', 'trackback': '0', 'OK': 'OK', 'type': 'summon'}
    },
    'ticket': {
        'ticket': {'body': 'ticket body',
                   'userpic': '',
                   'text': '\u041c\u0438\u0445\u0430\u0438\u043b \u0421\u0430\u0445\u043d\u043e\u0432',
                   'access_type': 'public',
                   'assignee': 'msahnov',
                   'host_id': '4611686018427389210',
                   'trackback': '0',
                   'project': 'TEST',
                   'queue_suggest': 'TEST-\u041f\u0435\u0441\u043e\u0447\u043d\u0438\u0446\u0430',
                   'OK': 'OK',
                   'title': 'Ticket title',
                   'parent_id': '0',
                   'type': 'jira'}
    }
}

'''posts = {

    'ticket': {
        'body': 'ticket body',
        'userpic': '',
        'text': 'blah test ticket',
        'access_type': u'public',
        'assignee': u'theigel',
        'host_id': u'4611686018427389210',
        'project': u'AT',
        'queue_suggest': 'AT',
        'OK': u'OK',
        'title': u'Ticket title',
        'type': u'jira'
        },
'''

posts = {
    'text': {
        'body': 'Text post body',
        'carbon-copy-address': ', good-ezhik@yandex-team.ru',
        'carbon-copy-address-input': 'good-ezhik-cc@yandex-team.ru',
        'tags': 'entered-tag',
        'tag': 'tagfromsuggest',
        'text': '',
        'access_type': 'public',
        'rubric': 'life',
        'replies': '1',
        'title': 'Text post title',
        'submit_btn': 'btn',
        'type': 'text',
    },
    'link': {
        'body': 'link body',
        'carbon-copy-address': ', msahnov@yandex-team.ru',
        'tags': 'entered-tag',
        'URL': 'http://google.com',
        'text': '',
        'access_type': 'public',
        'tag': 'tagfromsuggest',
        'rubric': 'life',
        'replies': '1',
        'carbon-copy-address-input': 'msahnov2@yandex-team.ru',
        'title': 'Linktitle',
        'submit_btn': 'btn',
        'type': 'link',
    },
    'poll': {
        'body': 'Poll description',
        'carbon-copy-address': '',
        'tags': 'tag',
        'text': '',
        'expires': '2015-12-11',
        'access_type': 'public',
        'tag': 'tag-from-suggest',
        'rubric': 'life',
        'replies': '1',
        'poll_type': 'single',
        'carbon-copy-address-input': '',
        'title': 'Poll title',
        'option2': 'var2',
        'option3': 'var3',
        'option1': 'var1',
        'submit_btn': 'btn',
        'hidden': 'on',
        'type': 'poll',
    },
    'mood': {
        'body': 'mood',
        'carbon-copy-address': '',
        'replies': '1',
        'access_type': 'public',
        'rubric': 'work',
        'submit_btn': 'btn',
        'type': 'status',
        'carbon-copy-address-input': '',
    },
    'news': {
        'body': 'new body',
        'carbon-copy-address': ', msahnov@yandex-team.ru',
        'tags': 'tag',
        'access_type': 'public',
        'tag': 'tag-from-suggest',
        'rubric': 'life',
        'replies': '1',
        'carbon-copy-address-input': 'msahnov2@yandex-team.ru',
        'title': 'New title ',
        'submit_btn': 'btn',
        'type': 'news',
    },
    'rules': {
        'body': 'Rules are simple. ',
        'carbon-copy-address': '',
        'replies': '0',
        'access_type': 'public',
        'rubric': 'life',
        'submit_btn': 'btn',
        'type': 'rules',
        'carbon-copy-address-input': '',
    }
}


CLUB_ONLY_TYPES = ('news', 'rules')
PERSON_ONLY_TYPES = ('mood',)


def set_access(request, access):
    accesses = {
        'public': 'public',
        'friends_only': 'all_friends',
        'private': 'private',
        'moderators': 'moderators'
    }
    request['access'] = accesses[access]


def enable_comments(request, enable):
    if enable:
        request['replies'] = '1'
    else:
        try:
            del request['replies']
        except KeyError:
            pass


def set_feed_id(request, feed_id):
    request['feed_id'] = str(feed_id)


def set_base_url(request, feed_id):
    if utils.is_community_id(feed_id):
        request['base_url'] = 'https://clubs.at.yandex-team.ru/%s' % feed_id


def set_form_id(request):
    form_id = generate_form_id()
    request['form_id'] = form_id


def generate_form_id():
    return generate_digits_string(36) + '.' + generate_digits_string(25)


def generate_digits_string(length):
    import random
    import string

    return ''.join(random.choice(string.digits) for _ in range(length))


def get_post_request(type, feed_id):
    request = posts[type]
    set_feed_id(request, feed_id)
    set_base_url(request, feed_id)
    set_form_id(request)
    return request
