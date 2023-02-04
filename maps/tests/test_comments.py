from maps.geoq.hypotheses.entrance.lib import comments


def test_sensitive_info_filter_positives():
    raw_telephone_comments = [
        '+7(999)999-99-99',
        '+7 (123) 321-21-12',
        '+71233212112',
        '79039039393'
    ]

    raw_doorcode_comments = [
        'пароль  123к123',
        'домофон 31#13',
        'код    31к13',
        '31к13',
    ]

    flat_number_comments = [
        'квартира  123',
        'кв 321',
        'кв231',
    ]

    test_cases = raw_telephone_comments + raw_doorcode_comments + flat_number_comments

    for test_case in test_cases:
        assert comments.sensitive_info_filter(test_case) == comments.FILTERED_TOKEN


def test_sensitive_info_filter_negatives():
    test_cases = [
        'далеко, далеко на лугу пасутся к',
        'Упакуйте мне пожалуйста 999 салфеток и все это доставьте в дом 123',
        'Ситибанк',
        'подойдите к двери'
    ]

    for test_case in test_cases:
        assert comments.sensitive_info_filter(test_case) == test_case


def test_process_comment():
    unrelated_comments = [
        'Доставьте пожалуйста заказ ngng@',
        'Мне кажется, вы всегда меня обкрадываете!',
        'Положите уже нормальное количество лука , я всё измерял!!!!ГОСТ нарушен на 20 граммов ,я подам в суд!',
        'договор'
    ]

    related_comments = [
        'Зайдите во двор, у двери',
        'Комментарий к заказу:\r\nМежду двумя большими красными зданиями.',
        'напротив\n\n\n\n\nмоста'
    ]

    test_cases = unrelated_comments + related_comments

    targets = [
        None
        for i in range(len(unrelated_comments))
    ] + related_comments

    for test_case, target in zip(test_cases, targets):
        assert comments.process_comment(test_case) == target
