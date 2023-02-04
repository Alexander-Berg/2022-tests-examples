from collections import namedtuple

import pytest

from walle.clients import bot, staff
from walle.idm.project_staff_id_converter import ProjectStaffIdConverter, MissingItem

ProjectIdConversion = namedtuple("ProjectIdConversion", "id, bot_project_id, planner_id, staff_id")
PROJECT_PROPS1 = ProjectIdConversion("project1", 1111111, 111, 888)
PROJECT_PROPS2 = ProjectIdConversion("project2", 2222222, 222, 999)


@pytest.fixture()
def bot_projects(mp):
    class BotProjectsMapper:
        def __init__(self):
            self.bpi_to_planner_id = {}

        def add_mapping(self, bpi, planner_id):
            self.bpi_to_planner_id[bpi] = planner_id

        def get_oebs_projects(self):
            return {bpi: {"planner_id": planner_id} for bpi, planner_id in self.bpi_to_planner_id.items()}

    mapper = BotProjectsMapper()
    mp.function(bot.get_oebs_projects, side_effect=mapper.get_oebs_projects)
    return mapper


@pytest.fixture()
def staff_ids(mp):
    class StaffIdMapper:
        def __init__(self):
            self.planner_id_to_staff_id = {}

        def add_mapping(self, planner_id, staff_id):
            self.planner_id_to_staff_id[planner_id] = staff_id

        def groups_by_planner_ids(self, _planner_ids, fields=None):
            return [
                {"id": staff_id, "service": {"id": planner_id}}
                for planner_id, staff_id in self.planner_id_to_staff_id.items()
            ]

    mapper = StaffIdMapper()
    mp.function(staff.groups_by_planner_ids, side_effect=mapper.groups_by_planner_ids)
    return mapper


@pytest.fixture()
def projects(walle_test):
    # remove default project
    default_proj = walle_test.default_project
    walle_test.projects.remove(walle_test.default_project)
    default_proj.delete()

    projects_ = {}
    for proj_props in (PROJECT_PROPS1, PROJECT_PROPS2):
        projects_[proj_props] = walle_test.mock_project(
            {
                "id": proj_props.id,
                "name": "Project {}".format(proj_props.id),
                "bot_project_id": proj_props.bot_project_id,
            }
        )
    return projects_


def test_normal_conversion(projects, bot_projects, staff_ids):
    for proj_props in projects.keys():
        bot_projects.add_mapping(proj_props.bot_project_id, proj_props.planner_id)
        staff_ids.add_mapping(proj_props.planner_id, proj_props.staff_id)

    psic = ProjectStaffIdConverter(projects.values())
    for proj_props, project in projects.items():
        assert psic.get_staff_id(project) == proj_props.staff_id

    assert psic.get_bot_project_ids_with_errors() == {}


def test_no_planner_id(projects, bot_projects, staff_ids):
    bot_projects.add_mapping(PROJECT_PROPS1.bot_project_id, PROJECT_PROPS1.planner_id)
    staff_ids.add_mapping(PROJECT_PROPS1.planner_id, PROJECT_PROPS1.staff_id)
    # do not add project2 planner_id

    psic = ProjectStaffIdConverter(projects.values())
    expected_errors = {
        PROJECT_PROPS2.bot_project_id: "Project {}: Bot project id {} was not found in BOT".format(
            PROJECT_PROPS2.id, PROJECT_PROPS2.bot_project_id
        )
    }

    with pytest.raises(MissingItem):
        psic.get_staff_id(projects[PROJECT_PROPS2])
    assert psic.get_bot_project_ids_with_errors() == expected_errors

    assert psic.get_staff_id(projects[PROJECT_PROPS1]) == PROJECT_PROPS1.staff_id


def test_no_staff_id(projects, bot_projects, staff_ids):
    bot_projects.add_mapping(PROJECT_PROPS1.bot_project_id, PROJECT_PROPS1.planner_id)
    bot_projects.add_mapping(PROJECT_PROPS2.bot_project_id, PROJECT_PROPS2.planner_id)
    staff_ids.add_mapping(PROJECT_PROPS1.planner_id, PROJECT_PROPS1.staff_id)
    # do not add project2 staff id

    psic = ProjectStaffIdConverter(projects.values())

    with pytest.raises(MissingItem):
        psic.get_staff_id(projects[PROJECT_PROPS2])
    expected_errors = {
        PROJECT_PROPS2.bot_project_id: "Project {}: Planner id {} (bot project id {}) was not found in Staff".format(
            PROJECT_PROPS2.id, PROJECT_PROPS2.planner_id, PROJECT_PROPS2.bot_project_id
        ),
    }
    assert psic.get_bot_project_ids_with_errors() == expected_errors

    assert psic.get_staff_id(projects[PROJECT_PROPS1]) == PROJECT_PROPS1.staff_id
