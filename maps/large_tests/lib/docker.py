import os
import yatest
import subprocess


CONFIG_DIR = None


def get_docker_compose():
    docker_compose = yatest.common.build_path("library/recipes/docker_compose/bin/docker-compose")
    assert os.path.exists(docker_compose), "cannot find docker_compose by build_path '{}'".format(docker_compose)
    return docker_compose


def set_config_dir(config_dir):
    global CONFIG_DIR
    CONFIG_DIR = config_dir


def get_config_dir():
    directory = yatest.common.source_path(CONFIG_DIR)
    assert os.path.isdir(directory), "cannot find compose config dir by source_path '{}'".format(directory)
    return directory


def stop_container(name):
    res = yatest.common.execute(
        [get_docker_compose(), "pause", name], cwd=get_config_dir())
    res.wait()
    assert res.exit_code == 0


def start_container(name):
    res = yatest.common.execute([get_docker_compose(), "unpause", name], cwd=get_config_dir())
    res.wait()
    assert res.exit_code == 0


def check_containers_health():
    compose = get_docker_compose()

    compose_response = yatest.common.execute([compose, "ps", "-q"], cwd=get_config_dir(), stdout=subprocess.PIPE)
    assert compose_response.exit_code == 0, "'docker-compose ps' returned {}'".format(compose_response.exit_code)

    ids = str.splitlines(compose_response.std_out)
    assert len(ids), "empty `docker-compose ps` output"

    for container_id in ids:
        response = yatest.common.execute(["docker", "ps", "-a", "--filter", f"id={container_id}", "--format", "{{.Status}}"], cwd=get_config_dir())
        assert response.exit_code == 0, f"Failed to get container {container_id} status, got: {response.exit_code}"

        status_line = str(response.std_out)
        if not ("Up" in status_line or "Exited (0)" in status_line):
            return False
    return True


def exec(name, args, check_exit_code=True, timeout=5):
    docker_args = [
        get_docker_compose(),
        'exec',
        '-T',
        name,
    ]
    docker_args.extend(args)

    with open("/dev/null", "wb") as stdin:
        # need to pass stdin as docker-compose exec needs it (it runs `docker exec --interactive`)
        resp = yatest.common.execute(
            docker_args, cwd=get_config_dir(), stdin=stdin, stdout=subprocess.PIPE,
            check_exit_code=check_exit_code, timeout=timeout)
        return str(resp.std_out)
