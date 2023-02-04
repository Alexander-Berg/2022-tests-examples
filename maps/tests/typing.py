from collections.abc import Callable

from maps.infra.sedem.cli.lib.service import Service


ServiceFactory = Callable[[...], Service]
