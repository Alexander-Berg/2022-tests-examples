TRANSLATE_FIELDS = ["description", "summary"]


def check_russian_translations(schema, path=""):
    if isinstance(schema, dict):
        for field in TRANSLATE_FIELDS:
            if field in schema and isinstance(schema[field], str):
                assert f"x-{field}-ru" in schema, f"x-{field}-ru is not provided for {path}"
        for k, v in schema.items():
            check_russian_translations(v, path + "/" + k)
    elif isinstance(schema, list):
        for idx, item in enumerate(schema):
            check_russian_translations(item, path + f"[{idx}]")
