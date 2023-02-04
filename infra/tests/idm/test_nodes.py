import pytest

from walle.idm.node_views import ValueNodeView, RoleNodeView, MissingField, WrongNodeType


def test_value_node_init():
    vn = ValueNodeView(name="test", help="help text", additional="some val")
    assert vn.to_dict() == {"name": "test", "help": "help text", "additional": "some val"}


def test_role_node_init():
    rn = RoleNodeView(slug="role", name="Role node", values={})
    assert rn.to_dict() == {"slug": "role", "name": "Role node", "values": {}}


@pytest.fixture()
def value_node_base():
    return ValueNodeView(name="test")


@pytest.fixture()
def role_node_base():
    return RoleNodeView(slug="role", name="Role node", values={})


class TestValueNode:
    def test_set_name(self, value_node_base):
        value_node_base.set_name("another name")
        assert value_node_base.to_dict() == {"name": "another name"}

    def test_set_help(self, value_node_base):
        value_node_base.set_help("help text")
        assert value_node_base.to_dict() == {"name": "test", "help": "help text"}

    @pytest.mark.parametrize("visible", [True, False])
    def test_set_visibility(self, value_node_base, visible):
        value_node_base.set_visibility(visible)
        assert value_node_base.to_dict() == {"name": "test", "visibility": visible}

    def test_set_set_attribute(self, value_node_base):
        value_node_base.set_set_attribute("project owner")
        assert value_node_base.to_dict() == {"name": "test", "set": "project owner"}

    def test_required_fields(self, value_node_base):
        del value_node_base["name"]
        with pytest.raises(MissingField):
            value_node_base.validate()

    def test_set_role(self, value_node_base, role_node_base):
        value_node_base.set_role(role_node_base)
        assert value_node_base.to_dict() == {
            "name": "test",
            "roles": {"slug": "role", "name": "Role node", "values": {}},
        }

    def test_set_role_raises_on_wrong_node_type(self, value_node_base):
        with pytest.raises(WrongNodeType):
            value_node_base.set_role(ValueNodeView(name="Wrong type node"))


class TestRoleNode:
    def test_add_value(self, role_node_base, value_node_base):
        del role_node_base["values"]  # check that key is created
        role_node_base.add_value("value name", value_node_base)
        assert role_node_base.to_dict() == {
            "slug": "role",
            "name": "Role node",
            "values": {"value name": {"name": "test"}},
        }

    def test_add_value_raises_on_wrong_node_type(self, role_node_base):
        with pytest.raises(WrongNodeType):
            role_node_base.add_value("value name", RoleNodeView())
