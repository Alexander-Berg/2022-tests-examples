def test_add_numbers():
    # Test the function with different inputs
    assert add_numbers(1, 2) == 3
    assert add_numbers(-1, 2) == 1
    assert add_numbers(-1, -2) == -3

    # Test the function with zero
    assert add_numbers(0, 0) == 0
    assert add_numbers(1, 0) == 1
    assert add_numbers(0, 1) == 1

def add_numbers(a, b):
    return a + b
