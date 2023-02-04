import luigi
import pytest

from dwh.grocery.rebuild_mv import RebuildMV, SimpleLogHelper


@pytest.mark.skip(reason="need fixture")
def test_rebuild_single_mv():
    """
        Checks that last_update_dt before mv_update and last_update_dt after mv_update differ
    """
    # Store last_update dt before mv update
    SimpleLogHelper._init(
        tables=["ttt_1day_dayly_t"],
        meta={
            "ttt_1day_dayly_t": {
                "name": "ttt_1day_dayly_t",
                "source_uri": "meta/meta:bo.ttt_1day_dayly_t",
                "hint": " NOAPPEND PARALLEL(16) OPTIMIZER_FEATURES_ENABLE(''12.1.0.2'') ",
                "mv_type": "tabular"
            },
            "tables": ["ttt_1day_dayly_t"]
        })
    # run mv_update
    rebuild_mv_task = RebuildMV(
        uri="meta/meta:bo.ttt_1day_dayly_t",
        hint=" NOAPPEND PARALLEL(16) OPTIMIZER_FEATURES_ENABLE(''12.1.0.2'') ",
    )
    is_success = luigi.build([rebuild_mv_task],
                             workers=1)
    assert is_success
    # check that update_dt changed
    assert rebuild_mv_task.complete() is True, \
        "MV must have been updated. Check your db credentials (DB must be meta test)"


@pytest.mark.skip(reason="need fixture")
def test_mv_updates_in_correct_order():
    """
        Checks that mv_f_sales_dayly updates BEFORE group_order_act_div.
        Also checks that RebuildMV for group_order_act_div_t triggers RebuildMV for mv_f_sales_dayly_t
    """
    # Store last_update dt before mv update
    SimpleLogHelper._init(
        tables=["mv_f_sales_dayly_t", "group_order_act_div_t"],
        meta={
            "group_order_act_div_t": {
                "name": "group_order_act_div_t",
                "source_uri": "meta/meta:bo.group_order_act_div_t",
                "hint": " NOAPPEND PARALLEL(16) OPTIMIZER_FEATURES_ENABLE(''12.1.0.2'') ",
                "mv_type": "tabular"
            },
            "mv_f_sales_dayly_t": {
                "name": "mv_f_sales_dayly_t",
                "source_uri": "meta/meta:biee.mv_f_sales_dayly_t",
                "hint": " NOAPPEND PARALLEL(16) OPTIMIZER_FEATURES_ENABLE(''12.1.0.2'') ",
                "mv_type": "tabular"
            },
            "tables": ["mv_f_sales_dayly_t", "group_order_act_div_t"]
        })
    # run mv_updates
    for mv in ('biee.mv_f_sales_dayly_t', 'bo.group_order_act_div_t'):
        rebuild_mv_task = RebuildMV(
            uri="meta/meta:{}".format(mv),
            hint=" NOAPPEND PARALLEL(16) OPTIMIZER_FEATURES_ENABLE(''12.1.0.2'') ",
        )
        is_success = luigi.build([rebuild_mv_task],
                                 workers=2)
        assert is_success
        # check that update_dt changed
        assert rebuild_mv_task.complete() is True, \
            "MV must have been updated. Check your db credentials (DB must be meta test)"

    # check that mv_f_sales_dayly_t was updated
    sales_dayly_upd_dt = SimpleLogHelper.get_last_refresh_dt("mv_f_sales_dayly_t")
    sales_dayly_prev_upd_dt = SimpleLogHelper.refresh_dt_before_upd["BIEE.MV_F_SALES_DAYLY_T"]
    assert sales_dayly_upd_dt > sales_dayly_prev_upd_dt

    # check that mv_f_sales_dayly_t was updated before group_order_act_div_t
    act_div_last_upd_dt = SimpleLogHelper.get_last_refresh_dt("group_order_act_div_t")
    assert sales_dayly_upd_dt < act_div_last_upd_dt, "Wrong mv update order"
