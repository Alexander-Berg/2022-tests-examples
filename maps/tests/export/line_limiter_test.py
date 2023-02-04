import unittest

from yandex.maps.wiki.export.line_limiter import LineLimiter

import six
if six.PY3:
    xrange = range


class Test1(unittest.TestCase):
    def test1(self):
        suffix = "...<TRUNCATED>"
        processed_lines = []

        def sink(s, truncated):
            if truncated:
                s = s + suffix
            processed_lines.append(s)
            print(s)

        message = "very long message longer than max_line"
        sep = "|"

        for max_line in xrange(0, 10):
            processor = LineLimiter(sink, max_line_length=max_line, sep=sep)
            lines = []
            for line_length in xrange(0, 10):
                lines.append(message[:line_length])
            buf = "|".join(lines)
            for n in xrange(0, len(buf), 2):
                processor.process_chunk(buf[n:n+2])
            for l, pl in zip(lines, processed_lines):
                self.assertTrue(
                    len(pl) == len(l)
                    or len(pl) == max_line + len(suffix))
                if len(pl) == len(l) + len(suffix):
                    self.assertEqual(pl, l + suffix)
            processed_lines = []


if __name__ == "__main__":
    unittest.main()
