from builtins import object

import pytest
from mock import MagicMock, call

from kelvin.common.templatetags.admin_reorder_tag import AppOrderNode, app_order


def test_app_order(mocker):
    """
    Тест тега, упорядочивающего приложения
    """
    parser = MagicMock()
    token = MagicMock()

    result = app_order(parser, token)

    assert isinstance(result, AppOrderNode), (
        u'Должен возвращаться объект `AppOrderNode`')
    # TODO: тут честнее посчитать количество использований как аргументов
    assert parser.mock_calls == [], u'Не должно быть обращений к парсеру'
    assert token.mock_calls == [], u'Не должно быть обращений к токену'


class TestAppOrderNode(object):
    """
    Тесты ноды, упорядочивающей приложения
    """
    render_cases = (
        (
            [], (), [],
        ),
        (
            [], (('app1', [],), ('app2', [],),), [],
        ),
        (
            [
                {
                    'app_label': 'app1',
                    'models': [
                        {
                            'object_name': 'model1',
                        },
                        {
                            'object_name': 'model2',
                        },
                    ],
                },
                {
                    'app_label': 'app2',
                    'models': [
                        {
                            'object_name': 'model3',
                        },
                        {
                            'object_name': 'model4',
                        },
                    ],
                },
            ],
            (),
            [
                {
                    'app_label': 'app1',
                    'models': [
                        {
                            'object_name': 'model1',
                        },
                        {
                            'object_name': 'model2',
                        },
                    ],
                },
                {
                    'app_label': 'app2',
                    'models': [
                        {
                            'object_name': 'model3',
                        },
                        {
                            'object_name': 'model4',
                        },
                    ],
                },
            ],
        ),
        (
            [
                {
                    'app_label': 'app1',
                    'models': [
                        {
                            'object_name': 'model1',
                        },
                        {
                            'object_name': 'model2',
                        },
                    ],
                },
                {
                    'app_label': 'app2',
                    'models': [
                        {
                            'object_name': 'model3',
                        },
                        {
                            'object_name': 'model4',
                        },
                    ],
                },
                {
                    'app_label': 'app3',
                    'models': [
                        {
                            'object_name': 'model5',
                        },
                        {
                            'object_name': 'model6',
                        },
                        {
                            'object_name': 'model7',
                        },
                        {
                            'object_name': 'model8',
                        }
                    ],
                },
            ],
            (
                ('app3', (
                    'model7',
                    'model6',
                )),
            ),
            [
                {
                    'app_label': 'app3',
                    'models': [
                        {
                            'object_name': 'model7',
                        },
                        {
                            'object_name': 'model6',
                        },
                        {
                            'object_name': 'model5',
                        },
                        {
                            'object_name': 'model8',
                        }
                    ],
                },
                {
                    'app_label': 'app1',
                    'models': [
                        {
                            'object_name': 'model1',
                        },
                        {
                            'object_name': 'model2',
                        },
                    ],
                },
                {
                    'app_label': 'app2',
                    'models': [
                        {
                            'object_name': 'model3',
                        },
                        {
                            'object_name': 'model4',
                        },
                    ],
                },
            ],
        ),
    )

    @pytest.mark.parametrize('app_list,admin_app_order,expected', render_cases)
    def test_render(self, mocker, app_list, admin_app_order, expected):
        """
        Тест переупорядочивания приложений
        """
        mocked_settings = mocker.patch(
            'kelvin.common.templatetags.admin_reorder_tag.settings')
        mocked_settings.ADMIN_APP_ORDER = admin_app_order
        context = dict()
        context['app_list'] = app_list

        assert AppOrderNode().render(context) == '', (
            u'Метод должен возвращать пустую строку')
        assert context['app_list'] == expected, (
            u'Неправильный порядок приложений в контексте после работы метода')
