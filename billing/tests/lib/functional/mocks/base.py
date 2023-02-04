from typing import Optional

from aioresponses import aioresponses


class BaseMocker:
    def __init__(self, aioresponses_mocker: aioresponses, base_url: str):
        self.mocker = aioresponses_mocker
        self.base_url = base_url

    def endpoint_url(self, relative: str, override_base: Optional[str] = None):
        base = (override_base or self.base_url).rstrip('/')
        relative = relative.lstrip('/')
        return f'{base}/{relative}'
