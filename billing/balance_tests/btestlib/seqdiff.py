# coding: utf-8
from collections import namedtuple

from btestlib import diffutils
from btestlib.diffutils import Types, Change


def seqdiff(old_sequence, new_sequence, add_same=False, full=None):
    full = full or max(len(old_sequence), len(new_sequence)) <= 10
    if full:
        pretty_operations = None
        min_changes_count = None
        for operations in all_levenshtein_operations(old_sequence, new_sequence, add_same=add_same):
            candidate = add_moves(operations)
            changes_count = sum([1 for o in candidate if o.type is not Types.SAME])
            if not pretty_operations or changes_count < min_changes_count:
                min_changes_count = changes_count
                pretty_operations = candidate
    else:
        operations = levenshtein_operations(old_sequence, new_sequence, add_same=add_same)
        pretty_operations = add_moves(operations)
    # todo-igogor удалить дебаг
    # print "assert seqdiff('{}', '{}', add_same={}) == {}".format(str(old_sequence), str(new_sequence),
    #                                                              str(add_same), str(pretty_operations))
    return pretty_operations


def levenshtein_matrix(old, new):
    # type: (Sequence, Sequence) -> Dict[int, int]
    """
        For all i and j, dist[i,j] will contain the Levenshtein
        distance between the first i elements of old and the
        first j characters of new
    """
    rows = len(old) + 1
    cols = len(new) + 1
    dist = [[0 for x in range(cols)] for x in range(rows)]
    # source prefixes can be transformed into empty strings
    # by deletions:
    for i in range(1, rows):
        dist[i][0] = i
    # target prefixes can be created from an empty source string
    # by inserting the characters
    for i in range(1, cols):
        dist[0][i] = i

    for col in range(1, cols):
        for row in range(1, rows):
            if old[row - 1] == new[col - 1]:
                cost = 0
            else:
                cost = 1
            dist[row][col] = min(dist[row - 1][col] + 1,  # deletion
                                 dist[row][col - 1] + 1,  # insertion
                                 dist[row - 1][col - 1] + cost)  # substitution

    # print '\n'.join([' '.join([' ', ' '] + [str(a) for a in t])] +
    #                 [' '.join([str(([' '] + list(s))[i])] + [str(col) for col in row]) for i, row in enumerate(dist)])
    return dist


Cell = namedtuple('Cell', ['row', 'col', 'val'])


def all_levenshtein_operations(old, new, add_same=False):
    matrix = levenshtein_matrix(old, new)
    operations_lists = []

    stack = [(Cell(row=len(matrix) - 1, col=len(matrix[-1]) - 1, val=matrix[-1][-1]), [])]
    while stack:
        curr, operations = stack.pop()
        if curr.row == 0 and curr.col == 0:
            operations_lists.append(operations[::-1])
        else:
            up = Cell(row=curr.row - 1, col=curr.col, val=matrix[curr.row - 1][curr.col])
            diag = Cell(row=curr.row - 1, col=curr.col - 1, val=matrix[curr.row - 1][curr.col - 1])
            left = Cell(row=curr.row, col=curr.col - 1, val=matrix[curr.row][curr.col - 1])

            min_cell_val = min([cell.val for cell in [up, diag, left] if cell.row >= 0 and cell.col >= 0])

            old_id = curr.row - 1
            new_id = curr.col - 1

            if up.row >= 0 and up.col >= 0 and up.val == min_cell_val and up.val == curr.val - 1:
                stack.append((up, operations + [Change(type=Types.DELETE, elem=old[old_id], id=old_id)]))

            if diag.row >= 0 and diag.col >= 0:
                if diag.val == min_cell_val and diag.val == curr.val:
                    if add_same:
                        stack.append((diag, operations + [Change(type=Types.SAME, elem=old[old_id], to_elem=new[new_id],
                                                                 id=old_id, to_id=new_id)]))
                    else:
                        stack.append((diag, operations))  # igogor: элемент не изменился - операцию не добавляем
                elif diag.val == min_cell_val and diag.val == curr.val - 1:
                    stack.append((diag, operations + [Change(type=Types.REPLACE,
                                                             elem=old[old_id], to_elem=new[new_id],
                                                             id=old_id, to_id=new_id)]))

            if left.row >= 0 and left.col >= 0 and left.val == min_cell_val and left.val == curr.val - 1:
                stack.append((left, operations + [Change(type=Types.INSERT, to_elem=new[new_id], to_id=new_id)]))

    #
    # weights = {Same.__name__: 0,
    #            Replace.__name__: 2}
    #
    # sorted_operations_list = sorted(operations_lists,
    #                                key=lambda operations: sum([weights.get(type(o).__name__, 1) for o in operations]))
    return operations_lists


def levenshtein_operations(old, new, add_same=False):
    matrix = levenshtein_matrix(old, new)
    operations = []
    curr = Cell(row=len(matrix) - 1, col=len(matrix[-1]) - 1, val=matrix[-1][-1])
    while curr.row != 0 or curr.col != 0:
        up = Cell(row=curr.row - 1, col=curr.col, val=matrix[curr.row - 1][curr.col])
        diag = Cell(row=curr.row - 1, col=curr.col - 1, val=matrix[curr.row - 1][curr.col - 1])
        left = Cell(row=curr.row, col=curr.col - 1, val=matrix[curr.row][curr.col - 1])

        min_cell_val = min([cell.val for cell in [up, diag, left] if cell.row >= 0 and cell.col >= 0])

        old_id = curr.row - 1
        new_id = curr.col - 1

        if diag.row >= 0 and diag.col >= 0 and diag.val == min_cell_val and diag.val == curr.val:
            if add_same:
                operations.append(Change(type=Types.SAME, elem=old[old_id], to_elem=new[new_id],
                                         id=old_id, to_id=new_id))
            curr = diag  # igogor: элемент не изменился - операцию не добавляем
        elif left.row >= 0 and left.col >= 0 and left.val == min_cell_val and left.val == curr.val - 1:
            operations.append(Change(type=Types.INSERT, to_elem=new[new_id], to_id=new_id))
            curr = left
        elif up.row >= 0 and up.col >= 0 and up.val == min_cell_val and up.val == curr.val - 1:
            operations.append(Change(type=Types.DELETE, elem=old[old_id], id=old_id))
            curr = up
        elif diag.row >= 0 and diag.col >= 0 and diag.val == min_cell_val and diag.val == curr.val - 1:
            operations.append(Change(type=Types.REPLACE, elem=old[old_id], to_elem=new[new_id],
                                     id=old_id, to_id=new_id))
            curr = diag
        else:
            raise ValueError("Shouldn't be here")

    return operations


def add_moves(operations):
    def _delete_elements(sequence, *indexes):
        for i in sorted(indexes, reverse=True):
            if i != len(sequence) - 1:
                sequence[i] = sequence[-1]
            del sequence[i]

    inserts = []
    deletes = []
    replaces = []
    moves = []
    sames = []
    for operation in operations:
        if operation.type is Types.INSERT:
            inserts.append(operation)
        elif operation.type is Types.DELETE:
            deletes.append(operation)
        elif operation.type is Types.REPLACE:
            replaces.append(operation)
        else:
            sames.append(operation)

    # todo-igogor это выглядит очень уродливо, но работает и быстрее. Надо обобщить
    i, j = 0, 1
    while i < len(replaces) and j < len(replaces):
        a, b = replaces[i], replaces[j]
        if a.elem == b.to_elem and a.to_elem == b.elem:
            moves.append(Change(type=Types.MOVE, elem=a.elem, to_elem=b.to_elem, id=a.id, to_id=b.to_id))
            moves.append(Change(type=Types.MOVE, elem=b.elem, to_elem=a.to_elem, id=b.id, to_id=a.to_id))
            _delete_elements(replaces, i, j)
            j = 0
        elif a.elem == b.to_elem and a.to_elem != b.elem:
            moves.append(Change(type=Types.MOVE, elem=a.elem, to_elem=b.to_elem, id=a.id, to_id=b.to_id))
            inserts.append(Change(type=Types.INSERT, to_elem=a.to_elem, to_id=a.to_id))
            deletes.append(Change(type=Types.DELETE, elem=b.elem, id=b.id))
            _delete_elements(replaces, i, j)
            j = 0
        elif b.elem == a.to_elem and b.to_elem != a.elem:
            moves.append(Change(type=Types.MOVE, elem=b.elem, to_elem=a.to_elem, id=b.id, to_id=a.to_id))
            inserts.append(Change(type=Types.INSERT, to_elem=b.to_elem, to_id=b.to_id))
            deletes.append(Change(type=Types.DELETE, elem=a.elem, id=a.id))
            _delete_elements(replaces, i, j)
            j = 0
        elif j < len(replaces) - 1:
            j += 1
        else:
            i += 1
            j = 0

    i, j = 0, 0
    while i < len(replaces) and j < len(inserts):
        a, b = replaces[i], inserts[j]
        if a.elem == b.to_elem:
            inserts[j] = Change(type=Types.INSERT, to_elem=a.to_elem, to_id=a.to_id)
            moves.append(Change(type=Types.MOVE, elem=a.elem, to_elem=b.to_elem, id=a.id, to_id=b.to_id))
            _delete_elements(replaces, i)
            j = 0
        elif j < len(inserts) - 1:
            j += 1
        else:
            i += 1
            j = 0

    i, j = 0, 0
    while i < len(replaces) and j < len(deletes):
        a, b = replaces[i], deletes[j]
        if a.to_elem == b.elem:
            deletes[j] = Change(type=Types.DELETE, elem=a.elem, id=a.id)
            moves.append(Change(type=Types.MOVE, elem=b.elem, to_elem=a.to_elem, id=b.id, to_id=a.to_id))
            _delete_elements(replaces, i)
            j = 0
        elif j < len(inserts) - 1:
            j += 1
        else:
            i += 1
            j = 0

    i, j = 0, 0
    while i < len(inserts) and j < len(deletes):
        a, b = inserts[i], deletes[j]
        if a.to_elem == b.elem:
            moves.append(Change(type=Types.MOVE, elem=b.elem, to_elem=a.to_elem, id=b.id, to_id=a.to_id))
            _delete_elements(inserts, i)
            _delete_elements(deletes, j)
            j = 0
        elif j < len(deletes) - 1:
            j += 1
        else:
            i += 1
            j = 0

    return diffutils.sort_by_to_id_and_id(inserts + deletes + replaces + moves + sames)


if __name__ == '__main__':
    def profile():
        from random import randint

        first = "".join([chr(ord('a') + randint(0, 20)) for _ in xrange(10000)])
        second = "".join([chr(ord('a') + randint(0, 20)) for _ in xrange(11000)])

        import cProfile

        pr = cProfile.Profile()
        pr.enable()

        first_diff = seqdiff(first, second, add_same=True)

        pr.disable()
        # after your program ends
        pr.print_stats(sort="time")
        # pr.print_stats(sort="call")
        # pr.print_stats(sort="name")

        raise AssertionError()


    # profile()

