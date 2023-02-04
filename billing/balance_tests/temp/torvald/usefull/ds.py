class Node(object):
    next_ = None
    previous = None

    value = None

    def __init__(self, value):
        self.value = value

    def __repr__(self):
        return str(self.value)


class Llist(object):
    head = None
    tail = None
    length = 0

    def __init__(self, node):
        self.head = node
        self.tail = node
        self.length = 1

    def append(self, node):
        self.tail.next_ = node
        node.previous = self.tail
        self.tail = node
        self.length += 1

    def insert(self, node):
        self.head.previous = node
        node.next_ = self.head
        self.head = node
        self.length += 1

    def pop(self):

        if not self.tail.previous:
            return None

        last = self.tail
        self.tail = self.tail.previous
        self.length -= 1
        return last

    def iter(self):
        current = self.head
        while current.next_:
            value = current.value
            current = current.next_
            yield value
        yield current

    def delete_value(self, value):

        current = self.head
        while current.next_:
            if current.value == value:
                if self.length > 1:
                    if current.previous:
                        current.previous.next_ = current.next_
                    if current.next_:
                        current.next_.previous = current.previous
                else:
                    self.head = None
                    self.tail = None

            else:
                current = current.next_


if __name__ == '__main__':
    a = Node(6)
    l = Llist(a)
    l.append(Node(7))
    l.append(Node(8))
    l.insert(Node(5))
    l.insert(Node(4))
    pass
