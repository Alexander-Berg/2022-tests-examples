def test_manifest(manifest):
    expected_checks = [
        "walle_meta", "walle_memory", "walle_gpu", "walle_disk", "walle_link",
        "walle_fs_check", "walle_reboots", "walle_tainted_kernel", "walle_cpu_capping", "walle_bmc",
        "walle_cpu", "walle_cpu_caches", "walle_clocksource", "walle_fstab", "walle_infiniband",
    ]

    known_checks = [check["check_name"] for check in manifest.to_dict()["checks"]]
    assert sorted(expected_checks) == sorted(known_checks)
