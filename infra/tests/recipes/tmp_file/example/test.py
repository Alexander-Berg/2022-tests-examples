import os


def test_cgroup_leakage():
    assert os.path.isfile("/tmp/nvme.img")
    # assert os.path.isfile("/tmp/tmp_file.recipe")  # default
