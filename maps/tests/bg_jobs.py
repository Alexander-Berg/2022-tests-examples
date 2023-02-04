def test_quorums_calculation(coordinator, mongo):
    coordinator.enable_bg_jobs()
    mongo.reconfigure(wait_for_quorums_calculation=True)
    # To kill background jobs in other tests
    coordinator.restart()
