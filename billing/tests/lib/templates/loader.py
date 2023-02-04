import json
import typing as tp
from os import path

from library.python import resource

RenderedTemplate = dict[str, tp.Any]
RenderedTemplateOrTemplatePath = tp.Union[str, RenderedTemplate]


class TemplateLoader:
    def __init__(self, tpl_dir: str) -> None:
        self.tpl_dir = tpl_dir

    def load(self, tpl_name: str) -> RenderedTemplate:
        tpl_path = path.join(self.tpl_dir, tpl_name)
        data = resource.find(tpl_path)
        if not data:
            raise ValueError(f"template {tpl_path} wasn't found. Make sure you added it to RESOURCE section.")
        return json.loads(data)
