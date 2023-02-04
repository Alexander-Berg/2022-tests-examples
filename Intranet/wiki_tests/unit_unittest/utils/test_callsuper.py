
from wiki.utils.callsuper import callsuper
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class CallsuperTest(BaseApiTestCase):
    def test_basic(self):
        """
        @callsuper invokes super method automatically
        """

        class A(object):
            def f(self):
                self.x += 'A'

        class B(A):
            @callsuper  # Call super(B, self).f() before f()
            def f(self):
                self.x += 'B'

        b = B()
        b.x = ''
        b.f()
        self.assertEqual(b.x, 'AB')

    def test_long_inheritance_chain(self):
        class A(object):
            def f(self):
                self.x += 'A'

        class B(A):
            pass

        class C(B):
            pass

        class D(C):
            @callsuper
            def f(self):
                self.x += 'D'

        class E(D):
            pass

        class F(E):
            pass

        f = F()
        f.x = ''
        f.f()
        self.assertEqual(f.x, 'AD')

    def test_method_args_are_passed(self):
        """
        Super method will be invoked with the same arguments as the method
        """

        class A(object):
            def f(self, y, z):
                self.x += y + z + '/'

        class B(A):
            @callsuper
            def f(self, y, z):
                self.x += y + z + '.'

        b = B()
        b.x = ''
        b.f('Y', z='Z')
        self.assertEqual(b.x, 'YZ/YZ.')

    def test_result(self):
        """
        It's possible to get result of super method invokation
        """

        class A(object):
            def f(self):
                return 'A'

        class B(A):
            @callsuper(result=True)
            def f(self, result):
                return result.lower() + 'B'

        b = B()
        x = b.f()
        self.assertEqual(x, 'aB')

    def test_aliased_result(self):
        """
        One can alias the result of super method invokation
        """

        class A(object):
            def f(self):
                return 'A'

        class B(A):
            @callsuper(result='r')
            def f(self, r):
                return r.lower() + 'B'

        b = B()
        x = b.f()
        self.assertEqual(x, 'aB')

    def test_at_the_end(self):
        """
        Super method can be invoked AFTER the method is run
        """

        class A(object):
            def f(self):
                self.x += 'A'

        class B(A):
            @callsuper(at_the_end=True)
            def f(self):
                self.x += 'B'

        b = B()
        b.x = ''
        b.f()
        self.assertEqual(b.x, 'BA')

    def test_superargs(self):
        """
        Number and content of parameters can be modified before passed
        to super method.
        """

        class A(object):
            def f(self, y):
                self.x += y

        class B(A):
            @callsuper
            def f(self, y, z):
                self.x += y + z

            @f.superargs
            def f(y, z):
                """
                * This method should return args and kwargs for super method
                * Name this method whatever you want
                * It's given the same params as method, but without self
                """
                args = (y.lower(),)
                kwargs = {}
                return args, kwargs

        b = B()
        b.x = ''
        b.f('Y', z='Z')
        self.assertEqual(b.x, 'yYZ')

    def test_chained_callsupers(self):
        """
        It must be okay if super method automatically invokes ITS super method
        """

        class A(object):
            def f(self):
                self.x += 'A'

        class B(A):
            @callsuper
            def f(self):
                self.x += 'B'

        class C(B):
            @callsuper
            def f(self):
                self.x += 'C'

        c = C()
        c.x = ''
        c.f()
        self.assertEqual(c.x, 'ABC')

    def test_no_super_method(self):
        """
        No error if there is no super method
        """

        class A(object):
            # def f(self):
            #    self.x += 'A'
            pass

        class B(A):
            @callsuper
            def f(self):
                self.x += 'B'

        b = B()
        b.x = ''
        b.f()
        self.assertEqual(b.x, 'B')

    def test_multiple_inheritance(self):
        class A(object):
            @callsuper
            def f(self):
                self.x += 'A'

        class B(object):
            @callsuper
            def f(self):
                self.x += 'B'

        class C(A, B):
            @callsuper
            def f(self):
                self.x += 'C'

        c = C()
        c.x = ''
        c.f()
        self.assertEqual(c.x, 'BAC')  # This is how Python's MRO works

    def test_diamond_inheritance(self):
        class A(object):
            @callsuper
            def f(self):
                self.x += 'A'

        class A1(A):
            @callsuper
            def f(self):
                self.x += '1'

        class A2(A):
            @callsuper
            def f(self):
                self.x += '2'

        class B(A1, A2):
            @callsuper
            def f(self):
                self.x += 'B'

        b = B()
        b.x = ''
        b.f()
        self.assertEqual(b.x, 'A21B')

    def test_init_method_is_fine_too(self):
        class A(object):
            x = ''

            @callsuper
            def __init__(self):
                self.x += 'A'

        class A1(A):
            @callsuper
            def __init__(self):
                self.x += '1'

        class A2(A):
            @callsuper
            def __init__(self):
                self.x += '2'

        class B(A1, A2):
            @callsuper
            def __init__(self):
                self.x += 'B'

        b = B()
        self.assertEqual(b.x, 'A21B')
