PY2TEST()

OWNER(okats)

PEERDIR(
    infra/callisto/controllers
)

TEST_SRCS(
    test1.py
    test_builder_planner.py
    test_contour_calc_target_state.py
    test_core.py
    test_loop.py
    test_notify.py
    test_prod_ruler.py
    test_release_controller.py
    test_resource.py
    test_tier.py
    test_utils.py
    test_yt_observers.py
)

REQUIREMENTS(network:full)

END()
