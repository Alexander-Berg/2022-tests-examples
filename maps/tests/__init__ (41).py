from pathlib import Path


def load_xlsx_fixture_file(filename):
    return Path("tests/fixtures/xlsx", filename).read_bytes()
