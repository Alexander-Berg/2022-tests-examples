import irt.utils


class ASingleton(irt.utils.Singleton):
    pass


class BSingleton(irt.utils.Singleton):
    pass


class CSingleton(BSingleton):
    pass


def test_file_classes():
    singleton1 = ASingleton()
    singleton2 = ASingleton()
    singleton3 = BSingleton()
    singleton4 = CSingleton()
    singleton5 = CSingleton()

    assert singleton1 is singleton2
    assert singleton1 is not singleton3
    assert singleton1 is not singleton4
    assert singleton3 is not singleton4
    assert singleton4 is singleton5
