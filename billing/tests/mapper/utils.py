from billing.apikeys.apikeys import mapper


def user_add_role_with_permission_constraint(user, perm_name, perm_action, constraint):
    permission = mapper.Permission(id=perm_name + '-' + perm_action, name=perm_name, action=perm_action).save()
    perm_set = mapper.PermissionSet(name='constrained_perm_set', permissions=[permission.pk, ]).save()
    role = mapper.Role(
        id='role_with_permission_constraint',
        perm_sets=[perm_set.pk, ],
        constraints={perm_name: {perm_action: constraint}}
    ).save()
    user.roles.append(role.pk)
    return user.save()
