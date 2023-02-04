# coding=utf-8

import datetime

import json
import pytest
from mock import mock, Mock, ANY
import tests.object_builder as ob
from dateutil.relativedelta import relativedelta

from balance import reverse_partners, constants, mapper
from balance.exc import COMMON_CONFIGURATION_ERROR
import balance.reverse_partners as rvp

import cluster_tools.generate_partner_acts as gpa
from balance.reverse_partners import ReversePartnersDefaultProcessor, CallableConfig


class TestGetNdsFlag:
    """Проверка функции get_nds_flag"""

    @pytest.mark.parametrize("service_id, is_resident, expected", [
        pytest.param(100, constants.NON_RESIDENT, 0),  # from contract config
        pytest.param(100, constants.RESIDENT, 1),  # from contract config
        pytest.param(200, constants.NON_RESIDENT, 1),  # from contract config
        pytest.param(200, constants.RESIDENT, 1),  # from contract config
        pytest.param(300, constants.NON_RESIDENT, 0),  # from get_nds_flag code
        pytest.param(300, constants.RESIDENT, 1),  # from get_nds_flag code
        pytest.param(constants.ServiceId.ADFOX, constants.NON_RESIDENT, 1),  # from get_nds_flag code
    ])
    def test_get_nds_flag_from_configuration(self, session, service_id, is_resident, expected):
        """Для сервисов, указанных в конфиге, должны возвращаться настройки из конфига.
        Для прочих сервисав должны возвращаться настройки из констант из кода функции get_nds_flag
        """

        scheme = {
            'nds': {
                '100': {
                    'non_resident': 0,
                    'resident': 1,
                },
                '200': 1,
            }
        }
        contract = create_contract(session, is_resident=is_resident, partner_scheme=scheme)
        assert reverse_partners.get_nds_flag(contract, service_id=service_id) == expected

    @pytest.mark.parametrize("service_id, is_resident, expected", [
        pytest.param(300, constants.NON_RESIDENT, 0),  # from get_nds_flag code
        pytest.param(300, constants.RESIDENT, 1),  # from get_nds_flag code
        pytest.param(constants.ServiceId.ADFOX, constants.NON_RESIDENT, 1),  # from get_nds_flag code
    ])
    def test_get_nds_flag_wo_configuration(self, session, service_id, is_resident, expected):
        """Если для контракта НЕ задана конфигурация,
        то должны возвращаться настройки из констант из кода функции get_nds_flag"""

        contract = create_contract(session, is_resident=is_resident)
        assert reverse_partners.get_nds_flag(contract, service_id=service_id) == expected


def test_call_completions_func(session):
    """Проверяем, что откруточная функция, указанная в конфигурации,
    вызываются с нужными параметрами при расчете completions"""

    scheme = {
        'contracts': [
            {
                'ctype': 'GENERAL',
                'tag': 'my_tag',
            }
        ],
        'close_month': [
            {
                'contract_tag': 'my_tag',
                'completions_funcs': {
                    'default': {
                        'name': 'my_func',
                        'params': {
                            'parameter_a': 'aaa',
                            'parameter_b': 123,
                        }
                    }
                },
            }
        ],
    }
    contract = create_contract(session, person_type='ph', partner_scheme=scheme)

    my_completions_func = Mock(return_value=[])
    rvp.register_completions_func(name='my_func')(my_completions_func)

    # делаем расчеты
    gpa.RevPartnerGenerator(contract).generate(month=mapper.ActMonth())

    # убеждаемся, что функция вызвана и ей передавались параметры из конфигурации
    assert my_completions_func.called
    assert my_completions_func.call_args.kwargs == dict(parameter_a='aaa', parameter_b=123)


def test_call_reverse_partners_processor(session):
    """Проверяем, что откруточный процессор вызван с нужными параметрами при расчете completions"""

    scheme = {
        'contracts': [
            {
                'ctype': 'GENERAL',
                'tag': 'my_tag',
            }
        ],
        'close_month': [
            {
                'contract_tag': 'my_tag',
                'month_close_generator': {
                    'reverse_partners_processor': {
                        'name': 'TestProcessor3',
                        'params': {
                            'parameter_a': 'ccc',
                            'parameter_b': 456,
                        }
                    }
                }
            }
        ],
    }
    contract = create_contract(session, person_type='ph', partner_scheme=scheme)

    class TestProcessor3(ReversePartnersDefaultProcessor):
        def __init__(self, contract, service_id, on_dt, parameter_a, parameter_b):
            pass

        def generate_invoice(self):
            pass

    with mock.patch.object(TestProcessor3, '__init__', return_value=None) as mock_init:
        gpa.RevPartnerGenerator(contract).generate(month=mapper.ActMonth())

        assert mock_init.called
        assert mock_init.call_args.assert_called_with(contract, ANY, ANY, parameter_a='ccc', parameter_b=456)


def test_generator_config(session):
    """Проверяем, что используется генератор, указанный в конфигурации"""

    scheme = {
        'contracts': [
            {
                'ctype': 'GENERAL',
                'tag': 'my_tag',
            }
        ],
        'close_month': [
            {
                'contract_tag': 'my_tag',
                'month_close_generator': 'RevPartnerGenerator'
            }
        ],
    }
    contract = create_contract(session, person_type='ph', partner_scheme=scheme)

    generator = gpa.get_generator(contract=contract, act_month=mapper.ActMonth())
    assert isinstance(generator, gpa.RevPartnerGenerator)


def create_contract(session, person_type=None, is_resident=None, partner_scheme=None):
    if person_type is None:
        person_type = ob.PersonCategoryBuilder(resident=is_resident).build(session).obj.category
    person = ob.PersonBuilder(type=person_type).build(session).obj

    additional_params = {}
    if partner_scheme is not None:
        integration, _, _ = create_configuration_for_client(session, client=person.client,
                                                            partner_scheme=partner_scheme)
        additional_params['integration'] = integration.cc

    now_dt = datetime.datetime.now()
    first_day_prev_month = datetime.datetime(now_dt.year, now_dt.month, 1) + relativedelta(months=-1)
    return ob.ContractBuilder(
        client=person.client,
        person=person,
        commission=0,
        payment_type=constants.PREPAY_PAYMENT_TYPE,
        services={constants.ServiceId.ANNOUNCEMENT},  # для теста можно использовать любой сервис
        currency=810,
        dt=first_day_prev_month,
        **additional_params).build(session).obj


def create_configuration_for_client(session, client, partner_scheme):
    """Создает новую конфигурацию со схемой `partner_scheme` и связывает ее с клиентом `client`"""

    uniq_suffix = datetime.datetime.now().isoformat()
    integration = mapper.Integration(
        cc='test integration ' + uniq_suffix,
        display_name='test integration ' + uniq_suffix,
    )
    configuration = mapper.IntegrationsConfiguration(
        cc='test configuration ' + uniq_suffix,
        display_name='test configuration ' + uniq_suffix,
        integration=integration,
        scheme=json.dumps(partner_scheme)
    )
    clients_integration = mapper.ClientsIntegration(
        client=client,
        integration=integration,
        configuration=configuration,
        start_dt=datetime.datetime.now().date() - datetime.timedelta(days=100))

    session.add(integration)
    session.add(configuration)
    session.add(clients_integration)
    session.flush()

    return integration, configuration, clients_integration


class TestCompletionsFuncRegistry:
    """Тестирование декоратора откруточной функции @register_completions_func"""

    def test_register_func_without_name(self):
        """Декоратор записывает функцию открутки в реестр"""

        @rvp.register_completions_func()
        def myfunc(a, b):
            return a + b

        fun = rvp.completions_funcs_registry.get('myfunc')
        assert fun(5, 8) == 13

    def test_register_named_func(self):
        """Если задан параметр name при вызове декоратора,
        то функция сохраняется в реестре под именем, указанным в декораторе"""

        @rvp.register_completions_func(name='othername')
        def myfunc2(a, b):
            return a + b

        fun = rvp.completions_funcs_registry.get('othername')
        assert fun(5, 8) == 13

    def test_register_duplicate_func_name(self):
        """Декоратор бросает исключение, если функция с таким именем уже зарегистрирована"""

        @rvp.register_completions_func(name='fun_name')
        def myfunc3(a, b):
            return a + b

        with pytest.raises(ValueError):
            # регистрируем функцию под тем же именем
            @rvp.register_completions_func(name='fun_name')
            def myfunc4(a, b):
                return a + b


class TestRevPartnersProcessorRegistry:
    def test_processor_registered(self):
        """Потомки ReversePartnersServiceProcessorBase автоматически регистрируются в реестре"""

        class TestProcessor1(rvp.ReversePartnersServiceProcessorBase):
            pass

        assert rvp.rev_partners_processors_registry.get('TestProcessor1') == TestProcessor1

    def test_duplicate_processor_name_raise_error(self):
        """Бросаем исключение, если процессор с таким именем уже зарегистрирован"""

        class TestProcessor2(rvp.ReversePartnersDefaultProcessor):
            pass

        with pytest.raises(ValueError):
            class TestProcessor2(rvp.ReversePartnersDefaultProcessor):
                pass


class TestComletionsFuncParsing:
    """Тестирование чтения настройек откруточной функции из конфигурации"""

    @pytest.mark.parametrize("month_close_section, expected", [
        pytest.param(None, None, id="is_none"),
        pytest.param({}, None, id="is_empty"),
        pytest.param(
            {
                "completions_funcs": {
                    "default": "abc"
                }
            },
            CallableConfig('abc', {}),
            id="only_name"),
        pytest.param(
            {
                "completions_funcs": {
                    "default": {
                        "name": "abc"
                    }
                }
            },
            CallableConfig('abc', {}),
            id="only_name_in_dict"
        ),
        pytest.param(
            {
                "completions_funcs": {
                    "default": {
                        "name": "abc",
                        "params": {
                            "a": 5,
                            "b": "bbb",
                        }
                    }
                }
            },
            CallableConfig('abc', {'a': 5, 'b': 'bbb'}),
            id="with_parameters"
        ),
    ])
    def test_valid_configuration(self, month_close_section, expected):
        """Проверяем парсинг валидных настроек для функции"""

        assert rvp.extract_completions_func_config(month_close_section, service_id=10) == expected

    @pytest.mark.parametrize("service, expected", [
        pytest.param(10, CallableConfig(name='abc1', params={"a": 5, "b": "bbb"})),
        pytest.param(20, CallableConfig(name='abc2', params={})),
        pytest.param(30, CallableConfig(name='abc2', params={})),
        pytest.param(40, None),
    ])
    def test_multiple_service_configuration(self, service, expected):
        """Определение конфигурации откруточной функции в зависимости от сервиса"""
        month_close_section = {
            "completions_funcs": {
                "10": {
                    "name": "abc1",
                    "params": {
                        "a": 5,
                        "b": "bbb",
                    },
                },
                "20": "abc2",
                "30": "abc2",
            }
        }
        assert rvp.extract_completions_func_config(month_close_section, service) == expected

    @pytest.mark.parametrize("month_close_section", [
        pytest.param(
            {
                "completions_funcs": 5,
            },
            id="compl_func config must be dict"
        ),
        pytest.param(
            {
                "completions_funcs": {
                    "default": {}
                }
            },
            id="compl_fun has not name"
        ),
        pytest.param(
            {
                "completions_funcs": {
                    "default": 5
                }
            },
            id="compl_func must be str or dict"
        ),
    ])
    def test_invalid_configuration(self, month_close_section):
        """Кидаем исключение при невалидной конфигурации"""
        with pytest.raises(COMMON_CONFIGURATION_ERROR):
            rvp.extract_completions_func_config(month_close_section, service_id=10)


class TestRevPartnerProcessorParsing:
    """Тестирование чтения настройек revpartner-процессора"""

    @pytest.mark.parametrize("month_close_section, expected", [
        pytest.param(None, None, id="is_none"),
        pytest.param({}, None, id="is_empty"),
        pytest.param(
            {
                "month_close_generator": "RevPartnerGenerator"
            },
            None,
            id="generator string w/o processor"),
        pytest.param(
            {
                "month_close_generator": {"name": "RevPartnerGenerator"}
            },
            None,
            id="generator dict w/o processor"),
        pytest.param(
            {
                "month_close_generator": {
                    "name": "RevPartnerGenerator",
                    "reverse_partners_processor": "MyProcessor",
                }
            },
            CallableConfig('MyProcessor', {}),
            id="processor w/o params"),
        pytest.param(
            {
                "month_close_generator": {
                    "name": "RevPartnerGenerator",
                    "reverse_partners_processor": {
                        "name": "MyProcessor",
                        "params": {
                            "param1": "value1"
                        }
                    }
                }
            },
            CallableConfig('MyProcessor', {"param1": "value1"}),
            id="processor w params"),
    ])
    def test_valid_configuration(self, month_close_section, expected):
        """Проверяем парсинг валидных настроек для процессора"""

        assert rvp.extract_reverse_partners_processor_config(month_close_section) == expected
