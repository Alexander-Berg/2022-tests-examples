class Node(object):
    left = None
    right = None

    def __init__(self, value):
        self.value = value

    def __repr__(self):
        return str(self.value)


def pathprint(point, path, res):
    path += str(point.value)
    if point.left:
        res = pathprint(point.left, path, res)
    if point.right:
        res = pathprint(point.right, path, res)
    if not (point.left or point.right):
        res.append(path)
        return res
    return res


def path_print(root):
    routes = [[root]]
    final_routes = []
    while routes:
        routes, current_rote = routes[:-1], routes[-1]
        last_point = current_rote[-1]
        if last_point.left:
            routes.append(current_rote + [last_point.left])
        if last_point.right:
            routes.append(current_rote + [last_point.right])
        if not (last_point.left or last_point.right):
            final_routes.append(current_rote)

    return [''.join([str(item.value) for item in route]) for route in final_routes]


def ones(input):
    current_start = None
    current_end = None
    rows = []
    for i, element in enumerate(input):
        if element == 1:
            if current_start is None:
                current_start = i
                current_end = i
            else:
                current_end = i
        if element == 0:
            if current_end is not None:
                rows.append((current_start, current_end))
                current_start = None
                current_end = None
    if current_start or current_end:
        rows.append((current_start, current_end))

    def calc_len(row):
        return row[1] - row[0] + 1

    max = 0
    if len(rows) == 1:
        max = calc_len(rows[0])
    for i, item in enumerate(rows[:-1]):
        if item[1] + 2 == rows[i + 1][0]:
            current = calc_len(item) + calc_len(rows[i + 1])
            max = current if current > max else max

    return max


def guests(timetable):
    from collections import defaultdict

    c = defaultdict(int)
    max = 0

    for item in timetable:
        time_in, time_out = item[0], item[1]
        for day in xrange(time_in, time_out + 1):
            c[day] += 1
            max = c[day] if c[day] > max else max

    return max


def q_sort(x):
    if len(x) < 2:
        return x
    pivot = x[0]
    smaller = [item for item in x if item < pivot]
    greater = [item for item in x if item > pivot]
    return q_sort(smaller) + [pivot] + q_sort(greater)


def binary_search(x, needle):
    if len(x) == 1:
        return x[0] == needle
    middle = len(x) / 2
    if x[middle] < needle:
        binary_search(x[middle:], needle)
    if x[middle] > needle:
        binary_search(x[:middle], needle)
    if x[middle] == needle:
        return True
    return False


if __name__ == "__main__":
    A = Node(1)
    B = Node(2)
    C = Node(3)
    D = Node(4)
    E = Node(5)
    F = Node(6)
    G = Node(7)

    A.left = B
    A.right = C
    B.left = D
    B.right = E
    E.left = F
    C.right = G

    # res = pathprint(A, '', [])
    # res2 = path_print(A)
    # print res
    # print res2

    # assert (ones([0, 0]) == 0)
    # assert (ones([1, 0]) == 1)
    # assert (ones([0, 1]) == 1)
    # assert (ones([1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1]) == 5)
    # assert (ones([1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1]) == 6)

    # print guests([(2,5), (3,6), (4,7), (5,8), (6,9)]) == 4
    # print guests([]) == 0
    # print guests([(1,2), (3,4), (5,8)]) == 1
    # print guests([(1,10), (2,5)]) == 2
    #
    # assert (maxGuests({}) == 0);
    # assert (maxGuests({{1, 2}}) == 1);
    # assert (maxGuests({{1, 2}, {2, 3}}) == 1);
    # assert (maxGuests({{1, 5}, {0, 1}, {4, 5}}) == 2);

    # print (q_sort([1,4,11,8,2,17,123,3]))

    print binary_search([1, 2, 4, 6, 8, 9, 11, 12, 13, 14], 7)
    print binary_search([1, 2, 4, 6, 8, 9, 11, 12, 13, 14], 6)

##      A1
##   B2     C3
## D4   E5      G7
##    F6
