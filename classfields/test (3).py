from jinja2 import Template
import json

with open("templates/datasource_page.j2", "r") as f:
    template_text = f.read()
with open("example_objects/datasource.json", "r") as f:
    data = json.load(f)
template = Template(template_text, trim_blocks=True, lstrip_blocks=True)
print(template.render(data=data))
