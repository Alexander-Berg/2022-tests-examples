from functools import partial

from parameterized import parameterized

parameterized_expand_doc = partial(
    parameterized.expand,
    doc_func=lambda func, num, param: f"{func.__name__} with {param}"
)
