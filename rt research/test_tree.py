from irt.utils import from_tsv


def test_tree():
    tree = {}

    records = from_tsv('multik_tree')
    for record in records:
        child = int(record['child'])
        parent = int(record['parent'])
        assert child not in tree, 'There\'re multiple records for one child'
        tree[child] = parent

    for category in tree:
        ancestors = set()
        while True:
            assert category not in ancestors, 'Tree has loops'
            ancestors.add(category)

            parent = tree[category]
            if parent != 0:
                assert parent in tree, 'Not all categories have path to root'
                category = parent
            else:
                break
