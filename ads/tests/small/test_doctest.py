import doctest
import io

import clemmer


def test():
    stream = io.StringIO()
    finder = doctest.DocTestFinder()
    runner = doctest.DocTestRunner()
    for test in finder.find(clemmer, clemmer.__name__):
        runner.run(test, out=stream.write)
    runner.summarize()
    if runner.failures:
        raise AssertionError(stream.getvalue())
