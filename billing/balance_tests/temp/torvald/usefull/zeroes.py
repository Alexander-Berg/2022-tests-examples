
from collections import namedtuple

Sequence = namedtuple('Sequence', 'start, end, length, is_zero_used, open_left, open_right')

def longest_non_zero(l):
    current = None
    previous = None
    pre_previous = None

    longest = 0

    sequences = []

    def update_length (current_sequence, longest):
        addition = current.open_left or current.open_right
        current_length = current.length if current.is_zero_used else current.length + addition
        result = longest if longest >= current_length else current_length
        return result

    for item in l:
        previous = current if current else None
        current = item

        if current == 1:
            if previous == 0 or previous is None:
                if len(sequences) == 2:
                    current = sequences[0]
                    longest = update_length(current, longest)
                    sequences = sequences[1:]

                open_left = (previous == 0)
                sequences.append(Sequence(current, None, 1, False, open_left, False))

            if previous == 1:
                for seq in sequences:
                    seq.length += 1

        if current == 0:
            if previous == 0:








if __name__ == "__main__":
    l = [0, 1, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 1]
    result = longest_non_zero(l)