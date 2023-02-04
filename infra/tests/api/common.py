from infra.walle.server.tests.lib.util import TestCase, BOT_PROJECT_ID
from walle.constants import ROBOT_WALLE_OWNER
from walle.idm.project_role_managers import ProjectRole
from walle.util.misc import concat_dicts


def toggle_reboot_via_ssh(project, enable):
    role_manager = ProjectRole.get_role_manager(ProjectRole.SSH_REBOOTER, project)
    if enable:
        role_manager.add_member(ROBOT_WALLE_OWNER)
    else:
        if ROBOT_WALLE_OWNER in role_manager.list_members():
            role_manager.remove_member(ROBOT_WALLE_OWNER)


def project_settings(**custom_options):
    project_base = {
        "id": "project-id",
        "name": "Some name",
        "provisioner": TestCase.project_provisioner,
        "deploy_config": TestCase.project_deploy_config,
        "bot_project_id": BOT_PROJECT_ID,
    }

    return concat_dicts(project_base, custom_options)
