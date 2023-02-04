# coding: utf-8
FORM_METADATA = {
    'non_field_errors': [],
    'fields': {
        'service_resource': {
            'errors': [],
            'is_required': False,
            'widget': 'TextInput',
            'position': 1,
            'label_image': None,
            'page': 1,
            'name': 'service_resource',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None},
            'help_text': '',
            'hints': [],
            'is_hidden': True,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'service_resource',
                    'id': 'id_service_resource',
                    'maxlength': '255',
                    'type': 'hidden'
                }}],
            'group_slug': None,
            'label': 'service_resource'
        },
        'scenario': {
            'errors': [],
            'is_required': True,
            'value': None,
            'widget': 'select',
            'position': 2,
            'label_image': None,
            'suggest_choices': False,
            'is_disabled_init_item': True,
            'page': 1,
            'name': 'scenario',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'data_source': {
                'items': [{
                    'slug': '495399',
                    'text': 'Перераспределение квоты',
                    'label_image': None,
                    'id': '495399'
                },
                    {
                        'slug': '495400',
                        'text': 'Квота в счет заказа',
                        'label_image': None,
                        'id': '495400'
                    },
                    {
                        'slug': '495401',
                        'text': 'Квота в счет сдачи железа',
                        'label_image': None,
                        'id': '495401'
                    },
                    {
                        'slug': '495402',
                        'text': 'Квота в счет проекта Qloud',
                        'label_image': None,
                        'id': '495402'
                    },
                    {
                        'slug': '495403',
                        'text': 'Другое',
                        'label_image': None,
                        'id': '495403'
                    }],
                'content_type': None},
            'type': 'choices',
            'is_allow_multiple_choice': False,
            'is_hidden': False,
            'help_text': '',
            'show_conditions': None,
            'group_slug': None,
            'label': 'Сценарий'
        },
        'scenario_restricted': {
            'errors': [],
            'is_required': True,
            'value': None,
            'widget': 'select',
            'position': 2,
            'label_image': None,
            'suggest_choices': False,
            'is_disabled_init_item': True,
            'page': 1,
            'name': 'scenario',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'data_source': {
                'items': [
                    {
                        'slug': '995400',
                        'text': 'Квота в счет заказа',
                        'label_image': None,
                        'id': '995400'
                    },
                    {
                        'slug': '995401',
                        'text': 'Квота в счет сдачи железа',
                        'label_image': None,
                        'id': '995401'
                    },
                    {
                        'slug': '995402',
                        'text': 'Квота в счет проекта Qloud',
                        'label_image': None,
                        'id': '995402'
                    },
                    {
                        'slug': '995403',
                        'text': 'Другое',
                        'label_image': None,
                        'id': '995403'
                    },
                ],
                'content_type': None},
            'type': 'choices',
            'is_allow_multiple_choice': False,
            'is_hidden': False,
            'help_text': '',
            'show_conditions': None,
            'group_slug': None,
            'label': 'Сценарий'
        },
        'service_donor': {
            'errors': [],
            'is_required': True,
            'widget': 'TextInput',
            'position': 3,
            'label_image': None,
            'page': 1,
            'name': 'service_donor',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'service_resource',
                    'condition': 'eq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'eq',
                        'operator': 'and',
                        'field_value': '495399'
                    }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'service_donor',
                    'id': 'id_service_donor',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'Слаг Сервиса-донора'
        },
        'ticket': {
            'errors': [],
            'is_required': True,
            'widget': 'TextInput',
            'position': 4,
            'label_image': None,
            'page': 1,
            'name': 'ticket',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'eq',
                    'operator': 'and',
                    'field_value': '495400'
                }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'ticket',
                    'id': 'id_ticket',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'Тикет заказа'
        },
        'answer_long_text_345741': {
            'errors': [],
            'is_required': True,
            'widget': 'Textarea',
            'position': 5,
            'label_image': None,
            'page': 1,
            'name': 'answer_long_text_345741',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'eq',
                    'operator': 'and',
                    'field_value': '495401'
                }]]},
            'help_text': 'или FQDN',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'content': '\r\n',
                'tag': 'textarea',
                'attrs': {
                    'max': 'None',
                    'rows': '10',
                    'min': '0',
                    'name': 'answer_long_text_345741',
                    'id': 'id_answer_long_text_345741',
                    'cols': '40'
                }}],
            'group_slug': None,
            'label': 'Инвентарные номера серверов'
        },
        'answer_short_text_345742': {
            'errors': [],
            'is_required': True,
            'widget': 'TextInput',
            'position': 6,
            'label_image': None,
            'page': 1,
            'name': 'answer_short_text_345742',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'eq',
                    'operator': 'and',
                    'field_value': '495402'
                }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'answer_short_text_345742',
                    'id': 'id_answer_short_text_345742',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'Qloud проект'
        },
        'location': {
            'errors': [],
            'is_required': True,
            'value': None,
            'widget': 'select',
            'position': 7,
            'label_image': None,
            'suggest_choices': False,
            'is_disabled_init_item': True,
            'page': 1,
            'name': 'location',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'data_source': {
                'items': [{
                    'slug': '68414',
                    'text': 'VLA',
                    'label_image': None,
                    'id': '68414'
                },
                    {
                        'slug': '68415', 'text': 'SAS', 'label_image': None,
                        'id': '68415'
                    },
                    {
                        'slug': '86640', 'text': 'SAS_TEST',
                        'label_image': None, 'id': '86640'
                    },
                    {
                        'slug': '68416', 'text': 'MAN', 'label_image': None,
                        'id': '68416'
                    },
                    {
                        'slug': '139448',
                        'text': 'MAN-PRE',
                        'label_image': None,
                        'id': '139448'
                    }],
                'content_type': None},
            'type': 'choices',
            'is_allow_multiple_choice': False,
            'is_hidden': False,
            'help_text': '',
            'show_conditions': [[{
                'field': 'scenario',
                'condition': 'neq',
                'operator': 'and',
                'field_value': ''
            },
                {
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': '495401'
                }]],
            'group_slug': None,
            'label': 'Локация'
        },
        'segment': {
            'errors': [],
            'is_required': True,
            'value': None,
            'widget': 'select',
            'position': 8,
            'label_image': None,
            'suggest_choices': False,
            'is_disabled_init_item': True,
            'page': 1,
            'name': 'segment',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'data_source': {
                'items': [{
                    'slug': '70109',
                    'text': 'default',
                    'label_image': None,
                    'id': '70109'
                },
                    {
                        'slug': '70110', 'text': 'dev', 'label_image': None,
                        'id': '70110'
                    },
                    {
                        'slug': '484143',
                        'text': 'gpu-dev',
                        'label_image': None,
                        'id': '484143'
                    }],
                'content_type': None},
            'type': 'choices',
            'is_allow_multiple_choice': False,
            'is_hidden': False,
            'help_text': '',
            'show_conditions': [[{
                'field': 'scenario',
                'condition': 'neq',
                'operator': 'and',
                'field_value': ''
            },
                {
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': '495401'
                }]],
            'group_slug': None,
            'label': 'Сегмент'
        },
        'cpu-float': {
            'errors': [],
            'is_required': True,
            'widget': 'TextInput',
            'position': 9,
            'label_image': None,
            'page': 1,
            'name': 'cpu-float',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'neq',
                        'operator': 'and',
                        'field_value': '495401'
                    }]]},
            'help_text': 'Необходимое количество ядер. Может быть дробным вплоть до тысячных долей.',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '1',
                    'name': 'cpu-float',
                    'id': 'id_cpu-float',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'CPU'
        },
        'memory': {
            'errors': [],
            'is_required': True,
            'widget': 'TextInput',
            'position': 10,
            'label_image': None,
            'page': 1,
            'name': 'memory',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'neq',
                        'operator': 'and',
                        'field_value': '495401'
                    }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'memory',
                    'id': 'id_memory',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'Память (GB)'
        },
        'hdd': {
            'errors': [],
            'is_required': False,
            'widget': 'TextInput',
            'position': 11,
            'label_image': None,
            'page': 1,
            'name': 'hdd',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'neq',
                        'operator': 'and',
                        'field_value': '495401'
                    }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'hdd',
                    'id': 'id_hdd',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'Диск HDD (TB)'
        },
        'ssd': {
            'errors': [],
            'is_required': False,
            'widget': 'TextInput',
            'position': 12,
            'label_image': None,
            'page': 1,
            'name': 'ssd',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'neq',
                        'operator': 'and',
                        'field_value': '495401'
                    }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'ssd',
                    'id': 'id_ssd',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'Диск SSD (TB)'
        },
        'ipv4': {
            'errors': [],
            'is_required': False,
            'widget': 'NumberInput',
            'position': 13,
            'label_image': None,
            'page': 1,
            'name': 'ipv4',
            'error_messages': {
                'invalid': 'Введите целое число.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': 'number',
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'neq',
                        'operator': 'and',
                        'field_value': '495401'
                    }]]},
            'help_text': 'Количество внешних IPv4 адресов',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': '2147483647',
                    'name': 'ipv4',
                    'min': '0',
                    'type': 'number',
                    'id': 'id_ipv4'
                }}],
            'group_slug': None,
            'label': 'IPv4 адреса'
        },
        'io_ssd': {
            'errors': [],
            'is_required': False,
            'widget': 'TextInput',
            'position': 14,
            'label_image': None,
            'page': 1,
            'name': 'io_ssd',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'neq',
                        'operator': 'and',
                        'field_value': '495401'
                    }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'io_ssd',
                    'id': 'id_io_ssd',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'IO для ssd'
        },
        'io_hdd': {
            'errors': [],
            'is_required': False,
            'widget': 'TextInput',
            'position': 15,
            'label_image': None,
            'page': 1,
            'name': 'io_hdd',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'neq',
                        'operator': 'and',
                        'field_value': '495401'
                    }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'io_hdd',
                    'id': 'id_io_hdd',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'IO для hdd'
        },
        'net': {
            'errors': [],
            'is_required': False,
            'widget': 'TextInput',
            'position': 16,
            'label_image': None,
            'page': 1,
            'name': 'net',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'neq',
                        'operator': 'and',
                        'field_value': '495401'
                    }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': 'None',
                    'min': '0',
                    'name': 'net',
                    'id': 'id_net',
                    'maxlength': '255',
                    'type': 'text'
                }}],
            'group_slug': None,
            'label': 'NET (MB/s)'
        },
        'gpu_model': {
            'errors': [],
            'is_required': True,
            'value': None,
            'widget': 'select',
            'position': 17,
            'label_image': None,
            'suggest_choices': True,
            'is_disabled_init_item': True,
            'page': 1,
            'name': 'gpu_model',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'data_source': {
                'items': [{
                    'slug': '484145',
                    'text': 'gpu_geforce_1080ti',
                    'label_image': None,
                    'id': '484145'
                },
                    {
                        'slug': '484146',
                        'text': 'gpu_tesla_k40',
                        'label_image': None,
                        'id': '484146'
                    },
                    {
                        'slug': '484147',
                        'text': 'gpu_tesla_m40',
                        'label_image': None,
                        'id': '484147'
                    },
                    {
                        'slug': '484148',
                        'text': 'gpu_tesla_p40',
                        'label_image': None,
                        'id': '484148'
                    },
                    {
                        'slug': '484149',
                        'text': 'gpu_tesla_v100',
                        'label_image': None,
                        'id': '484149'
                    }],
                'content_type': None},
            'type': 'choices',
            'is_allow_multiple_choice': False,
            'is_hidden': False,
            'help_text': '',
            'show_conditions': [[{
                'field': 'segment',
                'condition': 'eq',
                'operator': 'and',
                'field_value': '484143'
            }]],
            'group_slug': None,
            'label': 'Модель GPU'
        },
        'gpu_qty': {
            'errors': [],
            'is_required': True,
            'widget': 'NumberInput',
            'position': 18,
            'label_image': None,
            'page': 1,
            'name': 'gpu_qty',
            'error_messages': {
                'invalid': 'Введите целое число.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': 'number',
                'show_conditions': [[{
                    'field': 'segment',
                    'condition': 'eq',
                    'operator': 'and',
                    'field_value': '484143'
                }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'tag': 'input',
                'attrs': {
                    'max': '2147483647',
                    'name': 'gpu_qty',
                    'min': '1',
                    'type': 'number',
                    'id': 'id_gpu_qty'
                }}],
            'group_slug': None,
            'label': 'GPU'
        },
        'description': {
            'errors': [],
            'is_required': False,
            'widget': 'Textarea',
            'position': 19,
            'label_image': None,
            'page': 1,
            'name': 'description',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None,
                'show_conditions': [[{
                    'field': 'scenario',
                    'condition': 'neq',
                    'operator': 'and',
                    'field_value': ''
                },
                    {
                        'field': 'scenario',
                        'condition': 'neq',
                        'operator': 'and',
                        'field_value': '495401'
                    }]]},
            'help_text': '',
            'hints': [],
            'is_hidden': False,
            'tags': [{
                'content': '\r\n',
                'tag': 'textarea',
                'attrs': {
                    'max': 'None',
                    'rows': '10',
                    'min': '1',
                    'name': 'description',
                    'id': 'id_description',
                    'cols': '40'
                }}],
            'group_slug': None,
            'label': 'Комментарий'
        },
        'gencfg-groups': {
            'errors': [],
            'is_required': False,
            'widget': 'Textarea',
            'position': 20,
            'label_image': None,
            'page': 1,
            'name': 'gencfg-groups',
            'error_messages': {
                'invalid': 'Введите правильное значение.',
                'required': 'Это поле обязательно.'
            },
            'other_data': {
                'widget': None},
            'help_text': '',
            'hints': [],
            'is_hidden': True,
            'tags': [{
                'content': '\r\n',
                'tag': 'textarea',
                'attrs': {
                    'max': 'None',
                    'rows': '10',
                    'min': '0',
                    'name': 'gencfg-groups',
                    'id': 'id_gencfg-groups',
                    'cols': '40'
                }}],
            'group_slug': None,
            'label': 'Группы gencfg'
        }}}
