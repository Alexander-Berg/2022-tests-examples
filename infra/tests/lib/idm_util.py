from walle.clients import staff


def make_role_dict(role_id, state, member, role="owner"):
    path = "/scopes/project/project/project-id/role/{}/".format(role)

    role_dict = {
        "id": role_id,
        "node": {"slug_path": path},
        "user": None,
        "group": None,
        "state": state,
    }
    if member.startswith("@"):
        role_dict["group"] = {"slug": staff.group_name(member), "id": 111}
    else:
        role_dict["user"] = {"username": member}

    return role_dict
