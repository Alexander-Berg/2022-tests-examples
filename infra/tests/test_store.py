import pytest

from infra.rtc.walle_validator.lib.store import ConfigStore, CONFIG_SUFFIX
from infra.rtc.walle_validator.dto.project import Project


def test_management(tmp_path):
    configs_dir = tmp_path / "configs"
    configs_dir.mkdir()
    assert not list(configs_dir.iterdir())

    config_store = ConfigStore(str(configs_dir), str(tmp_path / "auxiliaries"))
    assert not list(config_store.iter_project_ids())

    project_alpha = Project("alpha", "Alpha", tags=["rtc"])
    project_beta = Project("beta", "Beta", tags=["rtc"])

    config_store.save_project(project_alpha)
    config_store.save_project(project_beta)
    assert len(list(configs_dir.iterdir())) == 2

    projects = {project.id: project for project in config_store.iter_projects()}
    assert len(projects) == 2
    assert projects[project_alpha.id].to_dict() == project_alpha.to_dict()
    assert projects[project_beta.id].to_dict() == project_beta.to_dict()

    config_store.remove_project(project_alpha.id)
    assert len(list(configs_dir.iterdir())) == 1

    projects = {project.id: project for project in config_store.iter_projects()}
    assert len(projects) == 1 and project_beta.id in projects


def test_bad_project_config(tmp_path):
    configs_dir = tmp_path / "configs"
    configs_dir.mkdir()

    config_path = configs_dir / "a{}".format(CONFIG_SUFFIX)
    with config_path.open("w") as stream:
        stream.write(u"""{"id": "a", "name": "A", "tags": [], "wrong": null}""")

    config_store = ConfigStore(str(configs_dir), str(tmp_path / "auxiliaries"))
    assert list(config_store.iter_project_ids()) == ["a"]

    with pytest.raises(TypeError):
        list(config_store.iter_projects())
