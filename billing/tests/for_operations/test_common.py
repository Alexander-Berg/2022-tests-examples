import vh3

from . import project_operations as operations


def test_python_cat(vh3_test_env):
    with vh3_test_env.build() as wi:
        hi = vh3.echo('Hi ', vh3.TSV)
        there = vh3.echo('there!', vh3.TSV)
        out = operations.python_cat([hi, there])
    wi.run()

    assert str(out.result) == 'Hi there!'
