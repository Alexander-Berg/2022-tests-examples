from intranet.crt.utils.constance import get_values_set_from_str

one_line_var = '''value1, value3,value2,  value4'''
few_line_var = '''
# group one
value1
value2,

 # group two, three
     value4,     value3
#value5
'''


def test_get_values_set_from_str():
    assert get_values_set_from_str(one_line_var) == {'value1', 'value2', 'value3', 'value4'}
    assert get_values_set_from_str(few_line_var) == {'value1', 'value2', 'value3', 'value4'}
