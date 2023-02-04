# coding: utf-8


DIVIDERS_DATA = {2: u'два',
                 3: u'три',
                 5: u'пять'}


def read_number():
    try:
        return int(raw_input('Input an integer: ').strip())
    except ValueError:
        print 'Not an integer'
        return read_number()


def get_dividers_candidates(dividers_data=None):
    dividers_data = dividers_data or DIVIDERS_DATA
    return dividers_data.keys()


def get_number_dividers(number, candidates):
    return [divider for divider in candidates if number % divider == 0]


def format_dividers(dividers, dividers_data=None):
    dividers_data = dividers_data or DIVIDERS_DATA
    if dividers:
        dividers_str = [dividers_data[divider] for divider in dividers]
        return u'Число {} делится на '.format(number) + u' и '.join(dividers_str)
    else:
        return u'Число {} не делится на: '.format(number) + u', '.join(dividers_data.values())


def output_dividers(output_string):
    print output_string


if __name__ == '__main__':
    number = read_number()
    candidates = get_dividers_candidates()
    dividers = get_number_dividers(number=number, candidates=candidates)
    output_dividers(output_string=format_dividers(dividers=dividers))
