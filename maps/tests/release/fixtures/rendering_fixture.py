import typing as tp


class RenderingFixture:
    def __init__(self):
        self.tables = []
        self.flags = {}

    def render_tables(self, tables: tp.List[tp.Any], **kwargs) -> None:
        self.tables = tables
        self.flags = kwargs
