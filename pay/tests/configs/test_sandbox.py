import sandbox.projects.paysys.tasks.monitorings.PaysysMonitoringsApplier
import paysys.sre.tools.monitorings.configs


def test_sandbox_projects():
    assert sandbox.projects.paysys.tasks.monitorings.PaysysMonitoringsApplier.PROJECTS == paysys.sre.tools.monitorings.configs.PROJECTS
