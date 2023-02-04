import pytest

from intranet.search.core.query import ast_factors


@pytest.mark.parametrize("test_factor,expected", [
    (ast_factors.InSet('#attr', [1, 2, 3]), 'inset(#attr,1,2,3)'),
    (ast_factors.InSetAny('#attr', [1, 2, 3]), 'insetany(#attr,1,2,3)'),
    (ast_factors.No('#attr'), 'no(#attr)'),
    (ast_factors.Lt('#attr', 10), 'lt(#attr,10)'),
    (ast_factors.Gt('#attr', 10), 'gt(#attr,10)'),
    (ast_factors.And('#attr', '#attr2', 10, 5), 'and(#attr,#attr2,10,5)'),
    (ast_factors.Or('#attr', '#attr2', 10, 5), 'or(#attr,#attr2,10,5)'),
    (ast_factors.Min('#attr', '#attr2', 10, 5), 'min(#attr,#attr2,10,5)'),
    (ast_factors.Max('#attr', '#attr2', 10, 5), 'max(#attr,#attr2,10,5)'),
    (ast_factors.Sum('#attr', '#attr2', 10, 5), 'sum(#attr,#attr2,10,5)'),
    (ast_factors.Eq('#attr', 10), 'inset(#attr,10)'),
    (ast_factors.Ln('#attr'), 'ln(#attr)'),
    (ast_factors.Log10('#attr'), 'log10(#attr)'),
])
def test_ast_factor_to_string(test_factor, expected):
    assert test_factor.to_string() == expected


def test_nested():
    value = ast_factors.And(ast_factors.InSet('#attr', [1, 2]),
                            ast_factors.InSet('#attr', [1, ast_factors.Min(2, 3)]))
    assert value.to_string() == 'and(inset(#attr,1,2),inset(#attr,1,min(2,3)))'
