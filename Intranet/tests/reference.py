# coding: utf-8


ENQUIRIES_ALL = {
    'count': 5,
    'page': 1,
    'limit': 40,
    'num_pages': 1,
    'results': [
        {
            'id': 10,
            'subject': 'Мониторы',
            'author': {
                'id': 3,
                'username': 'robot-procu-test',
                'email': 'robot-procu-test@yandex-team.ru',
                'is_staff': True,
                'is_deleted': False,
                'full_name': 'Тестовый Робот Закупок',
            },
            'manager': {
                'id': 3,
                'username': 'robot-procu-test',
                'email': 'robot-procu-test@yandex-team.ru',
                'is_staff': True,
                'is_deleted': False,
                'full_name': 'Тестовый Робот Закупок',
            },
            'updated_at': '2018-06-16T10:17:55.522000Z',
            'status': {'key': 'closed', 'name': 'Закрыт'},
            'reason': {'key': 'cancelled', 'name': 'Отменён'},
            'priority': {'key': 'normal', 'name': 'Средний'},
            'note': None,
            'deadline_at': None,
            'delivery_at': None,
        },
        {
            'id': 4,
            'subject': 'Холодильники',
            'author': {
                'id': 3,
                'username': 'robot-procu-test',
                'email': 'robot-procu-test@yandex-team.ru',
                'is_staff': True,
                'is_deleted': False,
                'full_name': 'Тестовый Робот Закупок',
            },
            'manager': {
                'id': 3,
                'username': 'robot-procu-test',
                'email': 'robot-procu-test@yandex-team.ru',
                'is_staff': True,
                'is_deleted': False,
                'full_name': 'Тестовый Робот Закупок',
            },
            'updated_at': '2018-05-16T10:17:55.522000Z',
            'status': {'key': 'closed', 'name': 'Закрыт'},
            'reason': {'key': 'cancelled', 'name': 'Отменён'},
            'priority': {'key': 'normal', 'name': 'Средний'},
            'note': None,
            'deadline_at': None,
            'delivery_at': None,
        },
        {
            'id': 3,
            'subject': 'Дрели',
            'author': {
                'id': 1,
                'username': 'robot-procu',
                'email': 'robot-procu@yandex-team.ru',
                'is_staff': True,
                'is_deleted': False,
                'full_name': 'Робот Закупок',
            },
            'manager': None,
            'updated_at': '2018-04-16T10:17:55.522000Z',
            'status': {'key': 'draft', 'name': 'Черновик'},
            'reason': {'key': 'none', 'name': ''},
            'priority': {'key': 'normal', 'name': 'Средний'},
            'note': None,
            'deadline_at': None,
            'delivery_at': None,
        },
        {
            'id': 2,
            'subject': 'Автомобили',
            'author': {
                'id': 3,
                'username': 'robot-procu-test',
                'email': 'robot-procu-test@yandex-team.ru',
                'is_staff': True,
                'is_deleted': False,
                'full_name': 'Тестовый Робот Закупок',
            },
            'manager': {
                'id': 1,
                'username': 'robot-procu',
                'email': 'robot-procu@yandex-team.ru',
                'is_staff': True,
                'is_deleted': False,
                'full_name': 'Робот Закупок',
            },
            'updated_at': '2018-03-16T10:17:55.522000Z',
            'status': {'key': 'draft', 'name': 'Черновик'},
            'reason': {'key': 'none', 'name': ''},
            'priority': {'key': 'normal', 'name': 'Средний'},
            'note': None,
            'deadline_at': None,
            'delivery_at': None,
        },
        {
            'id': 1,
            'subject': 'Мультиварки',
            'author': {
                'id': 1,
                'username': 'robot-procu',
                'email': 'robot-procu@yandex-team.ru',
                'is_staff': True,
                'is_deleted': False,
                'full_name': 'Робот Закупок',
            },
            'manager': {
                'id': 1,
                'username': 'robot-procu',
                'email': 'robot-procu@yandex-team.ru',
                'is_staff': True,
                'is_deleted': False,
                'full_name': 'Робот Закупок',
            },
            'updated_at': '2018-02-16T10:17:55.522000Z',
            'status': {'key': 'review', 'name': 'Выбор поставщика'},
            'reason': {'key': 'none', 'name': ''},
            'priority': {'key': 'critical', 'name': 'Критичный'},
            'note': 'no-no-no-note',
            'deadline_at': '2018-02-20T15:33:00Z',
            'delivery_at': '2018-12-30',
        },
    ],
}


# ------------------------------------------------------------------------------


CREATE_GOODS = {
    'id': 11,
    'no_replacement': True,
    'description': 'Септаккорд обретает субъект власти',
    'products': [
        {'name': 'GT', 'qty': 10, 'comment': 'fast'},
        {'name': 'Электробайк', 'qty': 5, 'comment': 'and furious'},
    ],
    'legal_entity': None,
    'address': None,
    'attachments': [
        {
            'id': 1,
            'filename': 'test6.docx',
            'preview': 'https://docviewer.tst.yandex-team.ru/?url=ya-procu://1/test6.docx',
            'created_at': '2017-12-06T19:16:21.527000+03:00',
        },
        {
            'id': 2,
            'filename': 'test2.docx',
            'preview': 'https://docviewer.tst.yandex-team.ru/?url=ya-procu://2/test1.docx',
            'created_at': '2018-04-06T19:16:21.527000+03:00',
        },
    ],
    'due_at': None,
    'cfo': {
        'id': 1,
        'key': 'INDS01',
        'name': 'Системы хранения и обработки данных.',
    },
    'project': {'id': 1, 'key': 'YTUB.Ш.Основной', 'name': 'YTUB.Ш.Основной'},
    'task': {'id': 1, 'key': 'BDCN0060_9', 'name': 'КЦ для Я.Мастера'},
    'mvp': {
        'id': 1,
        'key': 'ENRU',
        'name': 'Департамент строительства, инженерных систем и админ. вопросов_Россия',
    },
    'service': {
        'id': 1,
        'key': 'IN102',
        'name': 'Oracle Enterprise Business Suite',
    },
    'budget_line': {
        'id': 1,
        'key': 'Operations',
        'name': 'Прочие эксплуатационные расходы',
    },
    'system': {'id': 1, 'key': 'DAT', 'name': 'Сети передачи данных: СКС'},
    'subsystem': {
        'id': 1,
        'key': 'GCN',
        'name': 'Подключение к сетям газоснабжения',
    },
}


CREATE_PROCU = {
    'id': 12,
    'no_replacement': True,
    'description': 'Септаккорд обретает субъект власти',
    'products': [
        {'name': 'GT', 'qty': 10, 'comment': 'fast'},
        {'name': 'Электробайк', 'qty': 5, 'comment': 'and furious'},
    ],
    'legal_entity': {'id': 1, 'title': 'ООО Яндекс', 'is_deleted': False},
    'address': {
        'id': 1,
        'label': 'Штаб-квартира колхоза',
        'text': 'клх Химки, ул. Чехова, д. 12, 207458',
        'is_deleted': False,
    },
    'attachments': [
        {
            'id': 1,
            'filename': 'test6.docx',
            'preview': 'https://docviewer.tst.yandex-team.ru/?url=ya-procu://1/test6.docx',
            'created_at': '2017-12-06T19:16:21.527000+03:00',
        },
        {
            'id': 2,
            'filename': 'test2.docx',
            'preview': 'https://docviewer.tst.yandex-team.ru/?url=ya-procu://2/test1.docx',
            'created_at': '2018-04-06T19:16:21.527000+03:00',
        },
    ],
    'due_at': None,
    'cfo': {
        'id': 1,
        'key': 'INDS01',
        'name': 'Системы хранения и обработки данных.',
    },
    'project': {'id': 1, 'key': 'YTUB.Ш.Основной', 'name': 'YTUB.Ш.Основной'},
    'task': {'id': 1, 'key': 'BDCN0060_9', 'name': 'КЦ для Я.Мастера'},
    'mvp': {
        'id': 1,
        'key': 'ENRU',
        'name': 'Департамент строительства, инженерных систем и админ. вопросов_Россия',
    },
    'service': {
        'id': 1,
        'key': 'IN102',
        'name': 'Oracle Enterprise Business Suite',
    },
    'budget_line': {
        'id': 1,
        'key': 'Operations',
        'name': 'Прочие эксплуатационные расходы',
    },
    'system': {'id': 1, 'key': 'DAT', 'name': 'Сети передачи данных: СКС'},
    'subsystem': {
        'id': 1,
        'key': 'GCN',
        'name': 'Подключение к сетям газоснабжения',
    },
}


# ------------------------------------------------------------------------------


ENQUIRY = {
    'id': 1,
    'no_replacement': False,
    'description': 'Неравенство Бернулли означает линейно зависимый график функции многих переменных',
    'products': [
        {'name': 'iPhone 8', 'qty': 20, 'comment': 'Старый'},
        {
            'name': 'iPhone 8 Plus',
            'qty': 20,
            'comment': 'Желательно подлиннее.',
        },
        {
            'name': 'Lumia 782',
            'qty': 3,
            'comment': 'Следовательно, астатическая система координат Булгакова опасна.',
        },
    ],
    'legal_entity': {'id': 1, 'title': 'ООО Яндекс', 'is_deleted': False},
    'address': {
        'id': 1,
        'label': 'Штаб-квартира колхоза',
        'text': 'клх Химки, ул. Чехова, д. 12, 207458',
        'is_deleted': False,
    },
    'attachments': [
        {
            'id': 3,
            'filename': 'test22.docx',
            'preview': 'https://docviewer.tst.yandex-team.ru/?url=ya-procu://3/test22.docx',
            'created_at': '2018-04-06T19:16:21.527000+03:00',
        },
        {
            'id': 4,
            'filename': 'test11.docx',
            'preview': 'https://docviewer.tst.yandex-team.ru/?url=ya-procu://4/test11.docx',
            'created_at': '2018-04-06T19:16:21.527000+03:00',
        },
    ],
    'due_at': '2018-05-10',
    'cfo': {
        'id': 1,
        'key': 'INDS01',
        'name': 'Системы хранения и обработки данных.',
    },
    'project': {'id': 1, 'key': 'YTUB.Ш.Основной', 'name': 'YTUB.Ш.Основной'},
    'task': {'id': 1, 'key': 'BDCN0060_9', 'name': 'КЦ для Я.Мастера'},
    'mvp': {
        'id': 1,
        'key': 'ENRU',
        'name': 'Департамент строительства, инженерных систем и админ. вопросов_Россия',
    },
    'service': {
        'id': 1,
        'key': 'IN102',
        'name': 'Oracle Enterprise Business Suite',
    },
    'budget_line': {
        'id': 1,
        'key': 'Operations',
        'name': 'Прочие эксплуатационные расходы',
    },
    'system': {'id': 1, 'key': 'DAT', 'name': 'Сети передачи данных: СКС'},
    'subsystem': {
        'id': 1,
        'key': 'GCN',
        'name': 'Подключение к сетям газоснабжения',
    },
}


ENQUIRY_HEADER = {
    'id': 1,
    'key': 'YP1',
    'subject': 'Мультиварки',
    'note': 'no-no-no-note',
    'status': {'key': 'review', 'name': 'Выбор поставщика'},
    'reason': {'key': 'none', 'name': ''},
    'assignee': {
        'id': 1,
        'username': 'robot-procu',
        'email': 'robot-procu@yandex-team.ru',
        'full_name': 'Робот Закупок',
        'first_name': 'Робот',
        'last_name': 'Закупок',
        'is_staff': True,
        'is_deleted': False,
        'is_clickable': True,
    },
    'author': {
        'id': 1,
        'username': 'robot-procu',
        'email': 'robot-procu@yandex-team.ru',
        'full_name': 'Робот Закупок',
        'first_name': 'Робот',
        'last_name': 'Закупок',
        'is_staff': True,
        'is_deleted': False,
        'is_clickable': True,
    },
    'deadline_at': '2018-02-20T18:33:00+03:00',
    'created_at': '2017-12-06T18:53:29.502000+03:00',
    'category': {
        'id': 1,
        'name': 'Электроника',
        'sourcing_time': '176w 2d',
        'completion_time': '12h',
    },
    'priority': {'key': 'critical', 'name': 'Критичный'},
    'state': {'key': 'active', 'name': 'В работе'},
}


OPTIONS_ADMIN = {
    'publish': False,
    'cancel': True,
    'deadline': True,
    'clone': True,
    'links': True,
    'access': True,
}


OPTIONS_EMPLOYEE = {
    'publish': False,
    'cancel': False,
    'deadline': False,
    'clone': True,
    'links': True,
    'access': False,
}


OPTIONS_PUBLISHABLE = {
    'publish': True,
    'cancel': True,
    'deadline': False,
    'clone': True,
    'links': True,
    'access': True,
}
