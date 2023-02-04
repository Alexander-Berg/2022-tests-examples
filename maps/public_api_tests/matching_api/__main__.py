#!/usr/bin/env python

import unittest

from apikey import TestMatchApiKey
from test_generic import TestMatchApiGeneric

__all__ = [
    TestMatchApiKey,
    TestMatchApiGeneric
]


def main():
    unittest.main()


if __name__ == "__main__":
    main()
