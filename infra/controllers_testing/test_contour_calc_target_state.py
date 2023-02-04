import infra.callisto.controllers.plain_contour.controller as plain


def test_plain_calc_slot_target_state():
    assert plain.calc_slot_target_state(
        deployer_observed={1},
        searcher_observed=1,
        last_generations=[2, 1],
    ) == ({1, 2}, 1)

    assert plain.calc_slot_target_state(
        deployer_observed={1, 2},
        searcher_observed=2,
        last_generations=[2, 1],
    ) == ({1, 2}, 2)

    assert plain.calc_slot_target_state(
        deployer_observed={1},
        searcher_observed=1,
        last_generations=[3, 2],
    ) == ({1, 3}, 1), 'skip not ready state'

    assert plain.calc_slot_target_state(
        deployer_observed=set(),
        searcher_observed=None,
        last_generations=[3, 2],
    ) == ({2, 3}, None), 'get the newest ones'

    assert plain.calc_slot_target_state(
        deployer_observed=set(),
        searcher_observed=1,
        last_generations=[3, 2],
    ) == ({1, 3}, 1), 'keep current search observed and get the newest one'
