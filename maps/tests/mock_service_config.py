import typing as tp
from pathlib import PurePath

from maps.infra.sedem.lib.config import ServiceConfigProxy
from maps.infra.sedem.lib.config.test_utils import config_factory


def mock_config(path: str,
                virtual: bool = False,
                dependencies: tp.Optional[list[str]] = None) -> ServiceConfigProxy:
    if virtual:
        assert not dependencies
    return config_factory(
        path=path,
        config={
            'main': {'name': PurePath(path).name.replace('_', '-')},
            'deploy': {'type': 'rtc'},
            **(
                {}
                if virtual else
                {'resources': {'stable': {}}}
            ),
        },
        dependencies=[
            mock_config(config_name, virtual=True)
            for config_name in (dependencies or [])
        ],
    )
