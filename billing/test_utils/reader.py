import json
from json import JSONDecodeError
from typing import Iterator


def from_file(filename: str, raw_messages: bool = False) -> Iterator[tuple[int, dict]]:
    # noinspection Mypy
    try:
        import yatest.common  # type: ignore
        filename = yatest.common.source_path(filename)
    except ModuleNotFoundError:
        # тест запущен локально, корректировать имя файла не нужно
        pass

    with open(filename) as f:
        lines = f.readlines()
    for lino_no, line in enumerate(lines, start=1):
        try:
            data = json.loads(line)
            if raw_messages:
                data = json.loads(data['data'])
            assert isinstance(data, dict), type(data)
            yield lino_no, data
        except JSONDecodeError:
            pass
