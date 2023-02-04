import pytest

from walle.idm.role_tree import BaseInnerNode, ChildNotFound
from walle.idm.traversal import iter_subtree_nodes, iter_value_nodes, iter_leaves, get_node, get_root_node

tree_scheme = (
    "level_1_role",
    (
        ("level_2_value_1", ()),
        ("level_2_value_2", (("level_3_role", (("level_4_value_1", ()),)),)),
    ),
)


class RoleTreeBuilder:
    def build_tree(self, tree_scheme):
        return self.build_role_node(name=tree_scheme[0], children_scheme=tree_scheme[1])

    def build_role_node(self, name, children_scheme):
        children = self.collect_children(children_scheme, self.build_value_node)
        node_cls = self.make_node_class(name, children, slug=name)
        return node_cls()

    def build_value_node(self, name, children_scheme):
        children = self.collect_children(children_scheme, self.build_role_node)
        node_cls = self.make_node_class(name, children)
        return node_cls()

    def collect_children(self, children_scheme, node_builder):
        children = []
        for child_scheme in children_scheme:
            child_node = node_builder(name=child_scheme[0], children_scheme=child_scheme[1])
            children.append(child_node)
        return children

    def make_node_class(self, class_name, children, slug=None):
        attrs = {
            "_children": children,
            "_name": class_name,  # not official attr for testing purposes (unified name extraction)
        }

        if slug is not None:  # role node
            attrs["slug"] = slug
            attrs["_idm_props"] = {"slug": slug}
        else:  # value node
            attrs["slug"] = class_name

        cls = type("{}_node".format(class_name), (BaseInnerNode,), attrs)
        return cls


@pytest.fixture()
def role_tree():
    tree = RoleTreeBuilder().build_tree(tree_scheme)
    return tree


def set_root_node(mp, node):
    mp.function(get_root_node, return_value=node)


def test_iter_subtree_nodes_default_root(mp, role_tree):
    set_root_node(mp, role_tree)
    walk_info = [(path, node._name) for path, node in iter_subtree_nodes()]
    assert walk_info == [
        (["level_1_role"], "level_1_role"),
        (["level_1_role", "level_2_value_1"], "level_2_value_1"),
        (["level_1_role", "level_2_value_2"], "level_2_value_2"),
        (["level_1_role", "level_2_value_2", "level_3_role"], "level_3_role"),
        (["level_1_role", "level_2_value_2", "level_3_role", "level_4_value_1"], "level_4_value_1"),
    ]


def test_iter_subtree_nodes_with_path(mp, role_tree):
    set_root_node(mp, role_tree)
    walk_info = [(path, node._name) for path, node in iter_subtree_nodes(["level_1_role", "level_2_value_2"])]
    assert walk_info == [
        (["level_1_role", "level_2_value_2"], "level_2_value_2"),
        (["level_1_role", "level_2_value_2", "level_3_role"], "level_3_role"),
        (["level_1_role", "level_2_value_2", "level_3_role", "level_4_value_1"], "level_4_value_1"),
    ]


def test_iter_value_nodes(role_tree):
    value_paths = [info.value_path for info in iter_value_nodes(role_tree)]
    expected_paths = [
        ("level_1_role", "level_2_value_1"),
        ("level_1_role", "level_2_value_2"),
        ("level_1_role", "level_2_value_2", "level_3_role", "level_4_value_1"),
    ]
    assert value_paths == expected_paths


def test_iter_leaves(role_tree):
    leaf_paths = {l[0] for l in iter_leaves(role_tree)}
    expected_leaf_paths = {
        ("level_1_role", "level_2_value_1"),
        ("level_1_role", "level_2_value_2", "level_3_role", "level_4_value_1"),
    }
    assert leaf_paths == expected_leaf_paths


@pytest.mark.parametrize(
    "path",
    [
        ("level_1_role",),
        ("level_1_role", "level_2_value_2"),
        ("level_1_role", "level_2_value_2", "level_3_role"),
        ("level_1_role", "level_2_value_2", "level_3_role", "level_4_value_1"),
    ],
)
def test_get_node(role_tree, path):
    node = get_node(path, root_node=role_tree)
    assert node._name == path[-1]


@pytest.mark.parametrize(
    "path",
    [
        ("i_obviously_do_not_exist",),
        ("level_1_role", "wrong_door"),
        ("level_1_role", "there_was_a_typo", "level_3_role"),
    ],
)
def test_get_not_existing_node(role_tree, path):
    with pytest.raises(ChildNotFound):
        get_node(path, root_node=role_tree)
