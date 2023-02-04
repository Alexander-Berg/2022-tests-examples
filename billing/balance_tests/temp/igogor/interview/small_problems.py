# coding: utf-8


def parse_dict():
    a = {
        u'b': 4,
        u'c': {
            u'd': 3,
            u'e': 5
        }
    }

    def get_depth_keys(dict_):
        for key, value in dict_.iteritems():
            if isinstance(value, dict):
                subkeys = get_depth_keys(value)
                for subkey, subvalue in subkeys:
                    yield (u'{}.{}'.format(key, subkey), subvalue)
            else:
                yield (key, value)
        return

    depth_keys = get_depth_keys(a)
    depth_keys_ppp = tuple(depth_keys)
    pass


def sort_two_lists():
    a = ["a", "b", "c"]
    i = [2, 1, 30]

    def sort_lists(list_, by):
        if len(list_) != len(by):
            raise ValueError('Lists must have same size')
        return zip(*sorted(zip(i, a), key=lambda x: x[0]))[1]

    srt = sort_lists(a, by=i)
    pass


def intervals():
    def intervals_str(list_):
        if not list_:
            # raise ValueError('List must have values')
            return ''

        l = sorted(set(list_))

        intervals = []
        start = l[0]
        for i in range(1, len(l)):
            if l[i] - l[i - 1] != 1:
                intervals.append((start, l[i - 1]))
                start = l[i]
        intervals.append((start, l[-1]))

        return ','.join([str(a) if a == b else '{}-{}'.format(a, b)
                         for a, b in intervals])

    def intervals_oneline(list_):
        # if not list_:
        #     # raise ValueError('List must have values')
        #     return ''
        # l = sorted(set(list_))
        # intervals = zip([l[0]] + [l[i] for i in range(1, len(l)) if l[i] - l[i - 1] != 1],
        #                 [l[i - 1] for i in range(1, len(l)) if l[i] - l[i - 1] != 1] + [l[-1]])
        #
        # return ','.join([str(a) if a == b else '{}-{}'.format(a, b)
        #                  for a, b in intervals])
        l = sorted(set(list_ or ['']))
        return ','.join([str(a) if a == b else '{}-{}'.format(a, b)
                         for a, b in zip([l[0]] + [l[i] for i in range(1, len(l)) if l[i] - l[i - 1] != 1],
                                         [l[i - 1] for i in range(1, len(l)) if l[i] - l[i - 1] != 1] + [l[-1]])])

    def intervals_julia(l):
        l = sorted(l)
        left = -1
        right = -1
        flag = False  # флаг, найдена ли левая граница текущего интервала
        res = ""
        if (len(l)) == 0:
            return res
        if (len(l)) == 1:
            res += str(l[0])
        for i in range(1, len(l)):
            if l[i] - l[i - 1] == 1:
                if flag == False:
                    left = i - 1
                    flag = True
                right = i
                if i == len(l) - 1:
                    res += str(l[left]) + '-' + str(l[right])
            else:
                if flag == False:
                    res += str(l[i - 1])
                else:
                    res += str(l[left]) + '-' + str(l[right])

                if i != len(l) - 1:
                    res += ','
                else:
                    res += ',' + str(l[i])
                flag = False

        return res

    # print intervals_str([])
    print intervals_oneline([])
    # print intervals_julia([])
    # print intervals_str([1])
    print intervals_oneline([1])
    # print intervals_julia([1])
    # print intervals_str([1, 1, 2, 2])
    print intervals_oneline([1, 1, 2, 2])
    # print intervals_julia([1, 2])
    # print intervals_str([1, 3, 5])
    print intervals_oneline([1, 3, 5])
    # print intervals_julia([1, 3, 5])
    example = [1, 5, 2, 4, 3, 9, 8, 11, 0]
    # print intervals_str(example)  # u'0-5, 8-9, 11'
    print intervals_oneline(example)  # u'0-5, 8-9, 11'
    # print intervals_julia(example)  # u'0-5, 8-9, 11'
    # print intervals_str(example + [-2, -3])  # '-3--2, 0-5, 8-9, 11'
    # print intervals_oneline(example + [-2, -3])  # '-3--2, 0-5, 8-9, 11'

    pass


def max_product_problem():
    def max_product(l, n):
        size = len(l)
        if n <= 0 or n > size:
            raise ValueError('Pff')
        if n == size:
            return l

        abs_sorted = sorted(l, key=lambda x: abs(x))

        limit = len(l) - n
        if abs_sorted[limit] == 0:
            raise ValueError('Pff')

        left_positive = None
        left_negative = None
        right_positive = None
        right_negative = None

        n_product = 1

        for i, val in enumerate(abs_sorted):
            if i < limit:
                left_positive = (i, val) if val > 0 and (left_positive is None or val > left_positive[1]) \
                    else left_positive
                left_negative = (i, val) if val < 0 and (left_negative is None or val < left_negative[1]) \
                    else left_negative
            else:
                right_positive = (i, val) if right_positive is None and val > 0 else right_positive
                right_negative = (i, val) if right_negative is None and val < 0 else right_negative
                n_product *= val

        change_negative = False
        change_positive = False
        # произведение положительно или слева все нули
        if n_product > 0 or left_positive is None and left_negative is None:
            return abs_sorted[limit:]
        elif right_positive is None:
            # все отрицательны
            if left_positive is None:
                return abs_sorted[:n]
            else:
                change_negative = True
        else:
            if left_positive is None:
                change_positive = True
            elif left_negative is None:
                change_negative = True
            elif n_product * left_negative[1] / right_positive[1] < n_product * left_positive[1] / right_negative[1]:
                change_negative = True
            else:
                change_positive = True

        result = abs_sorted[limit:]
        if change_negative:
            result[right_negative[0] - limit] = left_positive[1]
        elif change_positive:
            result[right_positive[0] - limit] = left_negative[1]
        else:
            raise ValueError('Pff')

        return result

    tst = max_product(None, 3)
    tst = max_product([], 3)  #
    tst = max_product([1, 2, 3], None)
    tst = max_product([1, 2, 3], 0)
    tst = max_product([1, 2, 3], 1)
    tst = max_product([0], 1)
    tst = max_product([-1], 1)
    tst = max_product([1], 1)
    tst = max_product([1, -2], 1)
    tst = max_product([1, 2], 1)
    tst = max_product([1, 2], 2)
    tst = max_product([1, 2], 3)
    tst = max_product([0, 0, 0, 0], 3)
    tst = max_product([0, 0, 0, 1], 3)
    tst = max_product([0, 0, -1, 2], 2)
    tst = max_product([0, 1, -2, 3], 2)
    tst = max_product([1, 2, 3, -4, -5, -6], 2)
    tst = max_product([1, 2, 3, -4, -5, -6], 3)
    tst = max_product([1, 2, 3, -4, -5, -6], 4)
    tst = max_product([-1, -2, -3, 4, 5, 6], 2)
    tst = max_product([-1, -2, -3, 4, 5, 6], 3)
    tst = max_product([-1, -2, -3, 4, 5, 6], 4)
    tst = max_product([1, 2, 3, 4], 2)
    tst = max_product([-1, -2, -3, -4], 2)
    txt = max_product([-1, -2, -3, -4, -5, -6], 3)
    pass


def sign_change_problem():
    def sign_change_index(l):
        if not l:
            raise ValueError('Pff')
        not_sorted = any([i for i in range(1, len(l)) if l[i] < l[i - 1]])
        if not_sorted:
            raise ValueError('Pff')

        if l[-1] < 0 or l[0] >= 0:
            return None
        l_size = len(l)
        sci = l_size / 2
        while sci not in [0, l_size] and not (l[sci] >= 0 and l[sci - 1] < 0):
            step = sci / 2
            if step == 0 or sci + step >= l_size:
                step = 1

            sign = 1 if l[sci] == 0 else l[sci] / abs(l[sci])
            sci += -sign * step

        return sci

    # tst0 = sign_change_index(None)
    # tst1 = sign_change_index([])
    tst2 = sign_change_index([1])
    tst3 = sign_change_index([0])
    tst4 = sign_change_index([-1])
    tst5 = sign_change_index([0, 1])
    tst6 = sign_change_index([-1, 0])
    tst7 = sign_change_index([-2, -1])
    tst8 = sign_change_index([-10, 7, 13])
    tst9 = sign_change_index([-33, -10, 18])
    tst10 = sign_change_index([-5, -88, -43])
    tst11 = sign_change_index([12, 34, 56])


# parse_dict()
# sort_two_lists()
intervals()
# max_product_problem()
# sign_change_problem()
