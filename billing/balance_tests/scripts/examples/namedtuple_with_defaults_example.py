import btestlib.reporter as reporter
from btestlib.utils import namedtuple_with_defaults

'''
>>> Node = namedtuple_with_defaults('Node', 'val left right')
>>> Node()
Node(val=None, left=None, right=None)
>>> Node = namedtuple_with_defaults('Node', 'val left right', [1, 2, 3])
>>> Node()
Node(val=1, left=2, right=3)
>>> Node = namedtuple_with_defaults('Node', 'val left right', {'right':7})
>>> Node()
Node(val=None, left=None, right=7)
>>> Node(4)
Node(val=4, left=None, right=7)
'''


def cases():
    Node = namedtuple_with_defaults('Node', 'val left right')
    reporter.log(Node())

    Node = namedtuple_with_defaults('Node', 'val left right', [1, 2, 3])
    reporter.log(Node())

    Node = namedtuple_with_defaults('Node', 'val left right', {'right': 7})
    reporter.log(Node())
    reporter.log(Node(val=None, left=None, right=7))
    reporter.log(Node(4))
    reporter.log(Node(val=4, left=None, right=7))


if __name__ == '__main__':
    cases()
