from typing import Dict


class StartrekIssue:
    def __init__(self, fields: Dict[str, str]) -> None:
        self.fields = fields

    def __getattr__(self, key):
        if key not in self.fields:
            raise AttributeError(key)

        return self.fields[key]

    def __getitem__(self, item):
        return self.fields[item]
