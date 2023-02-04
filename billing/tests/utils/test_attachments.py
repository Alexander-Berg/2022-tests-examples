import json
import pathlib
import functools
import contextlib

import pytest
import openpyxl

from . import project_utils as pu


@pytest.mark.usefixtures('multiprocessing_pool_mock')
class TestAttachmentsManager:
    FILES_COUNT = 10

    @pytest.fixture
    def working_directory_with_files(self, tmp_path: pathlib.Path) -> pathlib.Path:
        root = tmp_path / 'TestAttachmentsManager'
        root.mkdir()

        default_object = {'a': 1, 'b': 2, 'c': 3}
        for index in range(1, self.FILES_COUNT + 1):
            input_path = root / f'input{index}.json'
            with open(input_path, 'w') as f:
                json.dump([default_object] * index, f, indent=4)

        yield root

    def test_processing(self, working_directory_with_files: pathlib.Path):
        root = working_directory_with_files

        expected: dict[[int, str], int] = dict()
        with pu.AttachmentsManager() as am:
            for index in range(1, self.FILES_COUNT + 1):
                input_path = root / f'input{index}.json'
                output_path = root / f'output{index}.xlsx'

                am.append_to_queue(input_path, output_path)
                expected[index - 1] = expected[str(output_path)] = index
            result = am.join()
        assert result == expected


class TestJson2Excel:
    @pytest.fixture(autouse=True)
    def setup(self, tmp_path: pathlib.Path):
        root = tmp_path / 'TestJson2Excel'
        root.mkdir()

        self.input_path = root / 'input.json'
        self.output_path = root / 'output.xlsx'

    def test_convert(self):
        expected = [{'a': 1, 'b': 2, 'c': 3}, {'a': 4, 'b': 5, 'c': 6}]
        self._prepare_input(expected)

        pu.Json2Excel.convert(self.input_path, self.output_path)

        result = self._read_output()
        assert result == expected

    def _prepare_input(self, values: list[dict[str, int]]):
        with open(self.input_path, 'w') as f:
            json.dump(values, f)

    def _read_output(self) -> list[dict[str, int]]:
        with contextlib.closing(openpyxl.open(self.output_path, read_only=True, data_only=True)) as workbook:
            return self._read_workbook(workbook)

    @staticmethod
    def _read_workbook(workbook: openpyxl.Workbook) -> list[dict[str, int]]:
        rows = iter(workbook.active.values)
        header = next(rows)
        output = map(dict, map(functools.partial(zip, header), rows))
        return list(output)
