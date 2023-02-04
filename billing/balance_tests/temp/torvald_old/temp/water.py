__author__ = 'torvald'


def simple():
    mtrx = [
        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0],
        [0, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0],
        [1, 0, 1, 1, 1, 1, 0, 1, 0, 0, 1],
        [1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1]
    ]

    mtrx = [
        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0],
        [0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0],
        [1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0],
        [1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0],
        [1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0],
        [1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1]
    ]

    ttl = 0
    for line in mtrx:
        str_line = ''
        for item in line:
            str_line += str(item)
        str_line = str_line.rstrip('0').lstrip('0')
        ttl += str_line.count('0')
    print ttl


def hard(city):
    '''
    >>> hard(city = [7,2,0,6,1,0,8])
    26
    >>> hard(city = [8,2,0,6,1,0,5,0,3])
    22
    >>> hard(city = [3,2,0,6,1,0,5,0,3])
    16
    '''
    base = city[0]
    ttl = 0
    while len(city) > 1:
        temp_ttl = 0
        right_border_found = 0
        for pos, item in enumerate(city[1:]):
            if item < base:
                temp_ttl += base - item
            else:
                ttl += temp_ttl
                base = item
                city = city[pos + 1:]
                right_border_found = 1
                break
        if right_border_found == 0:
            city = city[-1::-1]
            base = city[0]

    print ttl


import sys


def reverse(x):
    """
    :type x: int
    :rtype: int
    """

    def checker(reversed_x):
        return reversed_x if abs(reversed_x) <= sys.maxint else 0

    if x < 0:
        return checker(-1 * int(str(x)[-1:0:-1]))
    else:
        return checker(int(str(x)[-1::-1]))


def lists(nums, target):
    minuses = [target - item for item in nums]
    intersection_list = [int(x) for x in set(minuses).intersection(set(nums))]
    if len(intersection_list) > 1:
        answers = [x for x in intersection_list if (target - x != x or nums.count(x) > 1)]
        positions = [nums.index(x) + 1 for x in answers]
    else:
        positions = []
        positions.append(nums.index(intersection_list[0]) + 1)
        nums.pop(positions[0] - 1)
        positions.append(nums.index(intersection_list[0]) + 2)
    return sorted(positions)


if __name__ == '__main__':
    print (lists([3, 1, 4, 3], 6))
