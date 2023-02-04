global c
c=5
def func1(a,b):
    print a,b
    c=a+b
    return c

def func2():
    print c+3
    return c+3

def func3():
    # global c
    c=10
    print c+5
    return c+5


print func1(func2(), func3())