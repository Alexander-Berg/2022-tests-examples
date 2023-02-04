from collections import namedtuple
import unittest

from maps.garden.sdk.utils import pickle_utils


def _test_class_decorator(arg):
    def impl(parent_class):
        class Mutator(parent_class):
            field = arg + parent_class.field

            def f(self):
                return arg + super(Mutator, self).f()

        return pickle_utils.reapply_mixin_on_unpickle(
            _test_class_decorator, (arg,), parent_class)(Mutator)
    return impl


StrTuple = namedtuple(
    "StrTuple",
    "unicode_field str_cyrillic_field str_ascii_field bytes_field"
)


class OriginalClass:
    def __init__(self):
        self.str_tuple = StrTuple(
            unicode_field=u"test юникод test",
            str_cyrillic_field="test кириллица test",
            str_ascii_field="ascii",
            bytes_field=b"bytes",
        )

    field = "original"

    def f(self):
        return "original"


# Notice, that class BEING decorated is NOT top level class
# because DECORATED class is visible in module as top level class
@_test_class_decorator("decorated ")
class DecoratedClass(OriginalClass):
    pass


# Class BEING decorated is top level
# DECORATED class inherits name from OriginalClass but is
# not OriginalClass so it is not top level
NonTopLevelDecoratedClass = _test_class_decorator("decorated ")(OriginalClass)


@_test_class_decorator("2nd ")
@_test_class_decorator("decorated ")
class DoubleDecoratedClass(OriginalClass):
    pass


class ChildNonTopLevel(NonTopLevelDecoratedClass):
    field = "child " + NonTopLevelDecoratedClass.field

    def f(self):
        return "child " + super(ChildNonTopLevel, self).f()


class Child(DecoratedClass):
    field = "child " + DecoratedClass.field

    def f(self):
        return "child " + super(Child, self).f()


class DecoratorTest(unittest.TestCase):
    def setUp(self):
        pass

    def _test_original(self):
        """ Tests that original class was not changed """
        data = OriginalClass()
        self.assertEqual(data.f(), "original")
        self.assertEqual(data.field, "original")

    def _test_decorated(self, decorated_class, prefix="decorated "):
        data = decorated_class()
        self.assertEqual(data.f(), prefix + "original")
        self.assertEqual(data.field, prefix + "original")

        unpickled = pickle_utils.loads(pickle_utils.dumps(data))
        self.assertEqual(unpickled.f(), prefix + "original")
        self.assertEqual(unpickled.field, prefix + "original")

        self._test_original()

    def test_class_decorator(self):
        # Class BEING decorated is top level class, but decorated one is not
        self._test_decorated(
            _test_class_decorator("decorated ")(OriginalClass))

    def test_decorated(self):
        self._test_decorated(DecoratedClass)

    def test_non_top_level_decorated(self):
        self._test_decorated(NonTopLevelDecoratedClass)

    # Test different combinations of double decoration

    def test_double_decorator1(self):
        self._test_decorated(
            _test_class_decorator("2nd ")(DecoratedClass),
            "2nd decorated ")

    def test_double_decorator2(self):
        self._test_decorated(
            _test_class_decorator("2nd ")(NonTopLevelDecoratedClass),
            "2nd decorated ")

    def test_double_decorator3(self):
        self._test_decorated(
            DoubleDecoratedClass,
            "2nd decorated ")

    def test_double_decorator4(self):
        self._test_decorated(
            _test_class_decorator("2nd ")(_test_class_decorator("decorated ")(OriginalClass)),
            "2nd decorated ")

    # Test inheritance

    def test_child(self):
        self._test_decorated(Child, "child decorated ")

    def test_child2(self):
        self._test_decorated(ChildNonTopLevel, "child decorated ")

    def test_non_top_level_child(self):
        class Child(NonTopLevelDecoratedClass):
            field = "child " + NonTopLevelDecoratedClass.field

            def f(self):
                return "child " + super(Child, self).f()
        data = Child()
        exception = AttributeError
        self.assertRaises(exception, pickle_utils.dumps, (data,))
