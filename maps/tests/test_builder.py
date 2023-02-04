import typing as tp
from contextlib import nullcontext
from dataclasses import dataclass
from pathlib import PurePath

import pytest

from maps.infra.sedem.lib.config.builder import LocalBuilder
from maps.infra.sedem.lib.config.reader import LocalReader, ConfigContent, CONFIG_DIR_NAME


class MockReader(LocalReader):
    def __init__(self, configs_by_service_paths: dict[str, ConfigContent]) -> None:
        self._configs_by_service_paths = {
            PurePath(path): config_content
            for path, config_content in configs_by_service_paths.items()
        }

    def iter_all_service_paths(self, *, path_prefix: tp.Union[PurePath, str, None] = None) -> tp.Iterator[PurePath]:
        for service_path in self._configs_by_service_paths:
            if not path_prefix or str(service_path).startswith(str(path_prefix)):
                yield service_path

    def iter_affected_service_paths(self,
                                    changed_paths: list[tp.Union[PurePath, str]], *,
                                    path_prefix: tp.Union[PurePath, str, None] = None) -> tp.Iterator[PurePath]:
        for path in changed_paths:
            path_parts = PurePath(path).parts
            if CONFIG_DIR_NAME not in path_parts:
                return None

            config_dir_index = path_parts.index(CONFIG_DIR_NAME)
            service_path = PurePath(*path_parts[:config_dir_index])
            if path_prefix and not str(service_path).startswith(path_prefix):
                continue
            if service_path not in self._configs_by_service_paths:
                return None

            yield service_path

    def config_content(self, path: PurePath) -> ConfigContent:
        return self._configs_by_service_paths[path]


DEFAULT_BUILTIN_CONTENT = {
    'common': {'duty_abc': 'maps-fake'},
    'deploy': {
        'type': 'rtc',
        'deploy_profile': 'default',
    },
    'signal_functions': {},
    'panels': {},
}


@dataclass
class BuilderTestCase:
    name: str
    configs_by_service_paths: dict[str, ConfigContent]
    service_path_to_load: str
    expected_result: tp.Optional[dict[str, tp.Any]] = None
    expected_joined_result: tp.Optional[dict[str, tp.Any]] = None  # merged with dependencies
    expected_error: tp.Optional[str] = None

    def __str__(self) -> str:
        return self.name


BUILDER_TEST_CASES = [
    BuilderTestCase(
        name='invalid_config',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={'filters': {'alerts': ['not-a-dict']}},
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Invalid config at maps/fake',
    ),
    BuilderTestCase(
        name='virtual_config',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={'main': {'name': 'fake'}},
            ),
        },
        service_path_to_load='maps/fake',
        expected_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
        },
        expected_joined_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
        },
    ),
    BuilderTestCase(
        name='virtual_config_with_dependencies',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'dependencies': {'services': [
                        {'path': 'maps/dependency'},
                    ]},
                },
            ),
            'maps/dependency': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'dependency'},
                    'alerts': {'dependency-alert': {}},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Service at maps/fake is virtual and has dependencies',
    ),
    BuilderTestCase(
        name='virtual_config_with_datasets',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'dependencies': {'datasets': ['dataset-name']},
                },
            ),
            'maps/infra/ecstatic': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'ecstatic-agent'},
                    'alerts': {'ecstatic-agent-alert': {}},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Service at maps/fake is virtual and has dependencies',
    ),
    BuilderTestCase(
        name='non-virtual_config',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'garden'},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
            'deploy': {
                'type': 'garden',
                'deploy_profile': 'default',
            },
        },
        expected_joined_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
            'deploy': {
                'type': 'garden',
                'deploy_profile': 'default',
            },
        },
    ),
    BuilderTestCase(
        name='non-virtual_config_with_implicit_dependency',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'resources': {'stable': {}},
                },
            ),
            'maps/infra/baseimage': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'baseimage'},
                    'alerts': {'baseimage-alert': {}},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
            'resources': {'stable': {}},
        },
        expected_joined_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
            'alerts': {'baseimage-alert': {}},
            'resources': {'stable': {}},
        },
    ),
    BuilderTestCase(
        name='non-virtual_config_with_explicit_dependency',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'garden'},
                    'dependencies': {'services': [
                        {'path': 'maps/dependency'},
                    ]},
                },
            ),
            'maps/dependency': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'dependency'},
                    'alerts': {'dependency-alert': {}},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
            'deploy': {
                'type': 'garden',
                'deploy_profile': 'default',
            },
            'dependencies': {'services': [
                {'path': 'maps/dependency'},
            ]},
        },
        expected_joined_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
            'deploy': {
                'type': 'garden',
                'deploy_profile': 'default',
            },
            'alerts': {'dependency-alert': {}},
            'dependencies': {'services': [
                {'path': 'maps/dependency'},
            ]},
        },
    ),
    BuilderTestCase(
        name='non-virtual_config_with_non-virtual_dependency',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'garden'},
                    'dependencies': {'services': [
                        {'path': 'maps/dependency'},
                    ]},
                },
            ),
            'maps/dependency': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'dependency'},
                    'deploy': {'type': 'garden'},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Service at maps/fake has non-virtual dependency at maps/dependency',
    ),
    BuilderTestCase(
        name='non-virtual_config_invalid_dependency',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'garden'},
                    'dependencies': {'services': [
                        {'path': 'maps/dependency'},
                    ]},
                },
            ),
            'maps/dependency': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={'unknown-section': {}},
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Invalid config at maps/dependency',
    ),
    BuilderTestCase(
        name='non-virtual_config_duplicate_dependencies',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'garden'},
                    'dependencies': {'services': [
                        {'path': 'maps/dependency'},
                        {'path': 'maps/dependency'},
                    ]},
                },
            ),
            'maps/dependency': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'dependency'},
                    'alerts': {'dependency-alert': {}},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Service at maps/fake has >1 dependency at maps/dependency',
    ),
    BuilderTestCase(
        name='non-virtual_config_with_datasets',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'dependencies': {'datasets': ['dataset-name']},
                    'resources': {'stable': {}},
                },
            ),
            'maps/infra/baseimage': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'baseimage'},
                    'alerts': {'baseimage-alert': {}},
                },
            ),
            'maps/infra/ecstatic': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'ecstatic-agent'},
                    'alerts': {'ecstatic-agent-alert': {}},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
            'dependencies': {'datasets': ['dataset-name']},
            'resources': {'stable': {}},
        },
        expected_joined_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
            'alerts': {
                'baseimage-alert': {},
                'ecstatic-agent-alert': {},
            },
            'dependencies': {'datasets': ['dataset-name']},
            'resources': {'stable': {}},
        },
    ),
    BuilderTestCase(
        name='meta_config_with_subservice',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'meta'},
                    'dependencies': {'services': [
                        {'path': 'maps/subservice'},
                    ]},
                },
            ),
            'maps/subservice': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'subservice'},
                    'deploy': {'type': 'garden'},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_result={
            **DEFAULT_BUILTIN_CONTENT,
            'main': {'name': 'fake'},
            'deploy': {
                'type': 'meta',
                'deploy_profile': 'default',
            },
            'dependencies': {'services': [
                {'path': 'maps/subservice'},
            ]},
        },
    ),
    BuilderTestCase(
        name='meta_config_with_virtual_subservice',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'meta'},
                    'dependencies': {'services': [
                        {'path': 'maps/subservice'},
                    ]},
                },
            ),
            'maps/subservice': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'subservice'},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Meta-service at maps/fake has virtual dependency at maps/subservice',
    ),
    BuilderTestCase(
        name='meta_config_with_meta_subservice',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'meta'},
                    'dependencies': {'services': [
                        {'path': 'maps/subservice'},
                    ]},
                },
            ),
            'maps/subservice': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'subservice'},
                    'deploy': {'type': 'meta'},
                    'dependencies': {'services': [
                        {'path': 'maps/sub-subservice'},
                    ]},
                },
            ),
            'maps/sub-subservice': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'sub-subservice'},
                    'deploy': {'type': 'garden'},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Meta-service at maps/fake has virtual dependency at maps/subservice',
    ),
    BuilderTestCase(
        name='meta_config_without_subservices',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'meta'},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Meta-service at maps/fake has no dependencies',
    ),
    BuilderTestCase(
        name='non-virtual_config_with_self-dependency',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'garden'},
                    'dependencies': {'services': [
                        {'path': 'maps/fake'},
                    ]},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Loop detected in config at maps/fake',
    ),
    BuilderTestCase(
        name='non-virtual_config_with_recursive_dependency',
        configs_by_service_paths={
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'fake'},
                    'deploy': {'type': 'garden'},
                    'dependencies': {'services': [
                        {'path': 'maps/dependency'},
                    ]},
                },
            ),
            'maps/dependency': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={
                    'main': {'name': 'dependency'},
                    'deploy': {'type': 'garden'},
                    'dependencies': {'services': [
                        {'path': 'maps/fake'},
                    ]},
                },
            ),
        },
        service_path_to_load='maps/fake',
        expected_error='Loop detected in config at maps/fake',
    ),
]


@pytest.mark.parametrize('case', BUILDER_TEST_CASES, ids=str)
def test_builder_load(case: BuilderTestCase) -> None:
    builder = LocalBuilder(
        reader=MockReader({
            path: content
            for path, content in case.configs_by_service_paths.items()
        }),
    )

    if case.expected_error:
        error_context = pytest.raises(Exception, match=case.expected_error)
    else:
        error_context = nullcontext()

    with error_context:
        config_proxy = builder.load_config(PurePath(case.service_path_to_load))

    if case.expected_result:
        result = config_proxy.as_dict()
        assert case.expected_result == result

    if case.expected_joined_result:
        joined_result = config_proxy.with_dependencies().as_dict()
        assert case.expected_joined_result == joined_result


def test_builder_iter_all_service_paths() -> None:
    result = sorted(
        LocalBuilder(
            reader=MockReader({
                'maps/common/mock': ConfigContent(
                    builtin=DEFAULT_BUILTIN_CONTENT,
                    mixin={'main': {'name': 'common-mock'}},
                ),
                'maps/infra/mock': ConfigContent(
                    builtin=DEFAULT_BUILTIN_CONTENT,
                    mixin={'main': {'name': 'infra-mock'}},
                ),
                'devtools/mock': ConfigContent(
                    builtin=DEFAULT_BUILTIN_CONTENT,
                    mixin={'main': {'name': 'devtools-mock'}},
                ),
            }),
        ).iter_all_service_paths(
            path_prefix='maps',
        )
    )
    result = [str(path) for path in result]

    assert result == [
        'maps/common/mock',
        'maps/infra/mock',
    ]


def test_builder_iter_affected_service_paths() -> None:
    result = sorted(
        LocalBuilder(
            reader=MockReader({
                'maps/common/mock': ConfigContent(
                    builtin=DEFAULT_BUILTIN_CONTENT,
                    mixin={'main': {'name': 'common-mock'}},
                ),
                'maps/infra/mock': ConfigContent(
                    builtin=DEFAULT_BUILTIN_CONTENT,
                    mixin={'main': {'name': 'infra-mock'}},
                ),
                'devtools/mock': ConfigContent(
                    builtin=DEFAULT_BUILTIN_CONTENT,
                    mixin={'main': {'name': 'devtools-mock'}},
                ),
            }),
        ).iter_affected_service_paths(
            [
                'maps/common/mock/sedem_config/main.yaml',
                'devtools/mock/sedem_config/main.yaml'
            ],
            path_prefix='maps',
        )
    )
    result = [str(path) for path in result]

    assert result == [
        'maps/common/mock',
    ]


def test_config_proxy_convert_to_fancy_tree() -> None:
    builder = LocalBuilder(
        reader=MockReader({
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={'main': {'name': 'fake'}},
            ),
        }),
    )

    fancy_tree = builder.load_config(PurePath('maps/fake')).as_deprecated_fancy_tree()
    assert 'fake' == fancy_tree('main.name')


def test_config_proxy_service_name() -> None:
    builder = LocalBuilder(
        reader=MockReader({
            'maps/fake': ConfigContent(
                builtin=DEFAULT_BUILTIN_CONTENT,
                mixin={'main': {'name': 'fake'}},
            ),
        }),
    )

    config_proxy = builder.load_config(PurePath('maps/fake'))
    assert 'maps-core-fake' == config_proxy.service_name()
