import itertools
import typing as tp
from dataclasses import dataclass, field
from unittest.mock import MagicMock


@dataclass
class ModulesMock:
    @dataclass
    class Module:
        name: str
        pid: tp.Union[int, list[int], None]

        def __post_init__(self):
            if isinstance(self.pid, list):
                self.pid_iterator = iter(self.pid)
            else:
                self.pid_iterator = itertools.repeat(self.pid)

    modules: dict[str, Module] = field(default_factory=dict)

    def add_module(self, name: str, pid: tp.Union[list[int], int, None] = None) -> None:
        self.modules[name] = self.Module(name=name, pid=pid)

    def flock(self, name: str) -> MagicMock:
        assert name in self.modules
        return MagicMock()

    def exists(self, name: str) -> bool:
        return name in self.modules

    def all_modules(self) -> list[str]:
        return sorted(self.modules)

    def read_pidfile(self, name: str) -> tp.Optional[int]:
        return next(self.modules[name].pid_iterator)


@dataclass
class SupervisorCtlMock:
    @dataclass
    class Command:
        name: str
        output: tp.Union[str, list[str]]

        def __post_init__(self):
            if isinstance(self.output, list):
                self.output_iterator = iter(self.output)
            else:
                self.output_iterator = itertools.repeat(self.output)

    commands: dict[str, Command] = field(default_factory=dict)

    def add_commands(self, *commands: Command) -> None:
        self.commands.update({
            command.name: command
            for command in commands
        })

    def __call__(self, command_name: str, *_, **__) -> str:
        return next(self.commands[command_name].output_iterator)
