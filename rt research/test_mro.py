from irt.init_subclass import suppress_unused


class A(object):
    subclasses = []

    def __init_subclass__(cls, **kwargs):
        A.subclasses.append(cls.__name__)


class B(A):
    pass


class C(B):
    pass


class D(A):
    subclasses = []

    def __init_subclass__(cls, **kwargs):
        D.subclasses.append(cls.__name__)


class E(D):
    pass


class F(A):
    pass


class G(D, F):
    pass


class H(F, D):
    pass


class J(D):
    subclasses = []

    @classmethod
    def __init_subclass__(cls, **kwargs):
        J.subclasses.append(cls.__name__)


class K(J):
    pass


class L(K):
    pass


class M(D):
    pass


class N(A):
    subclasses = []

    @classmethod
    def __init_subclass__(cls, **kwargs):
        N.subclasses.append(cls.__name__)
        super(N, cls).__init_subclass__(**kwargs)

    class P(A):
        pass

    class R(P):
        subclasses = []

        @classmethod
        def __init_subclass__(cls, **kwargs):
            N.R.subclasses.append(cls.__name__)


class S(N.R):
    pass


class T(N):
    subclasses = []
    u_subclasses = []

    def __init_subclass__(cls, **kwargs):
        T.subclasses.append(cls.__name__)

        class U(object):
            def __init_subclass__(cls, **kwargs):
                T.u_subclasses.append(cls.__name__)

        class V(U):
            pass


class W(T):
    pass


def test_mro():
    suppress_unused()
    assert A.subclasses == ['B', 'C', 'D', 'F', 'P', 'R', 'N', 'T']
    assert D.subclasses == ['E', 'G', 'H', 'J', 'M']
    assert J.subclasses == ['K', 'L']
    assert N.subclasses == ['T']
    assert N.R.subclasses == ['S']

    assert T.subclasses == ['W']
    assert T.u_subclasses == ['V']
