from staff.headcounts.headcounts_summary.hierarchy_summary_counter import HierarchySummaryCounter
from staff.headcounts.headcounts_summary.query_builder import Result, SummaryResults


def test_group_by_departments_with_hierarchy():
    # given
    root_dep_id = 1
    child_dep_id = 2
    grand_child_dep_id = 3

    result = Result.default(
        summary=SummaryResults.default(headcount=0),
        next_level_grouping={
            root_dep_id: Result.default(
                result_id=root_dep_id,
                summary=SummaryResults.default(headcount=1),
                children={
                    child_dep_id: Result.default(
                        result_id=child_dep_id,
                        summary=SummaryResults.default(headcount=2),
                        children={
                            grand_child_dep_id: Result.default(
                                result_id=grand_child_dep_id,
                                summary=SummaryResults.default(headcount=3),
                            ),
                        },
                    ),
                },
            ),
        },
    )

    # when
    HierarchySummaryCounter().summarize(result)

    # then
    assert result.summary.headcount == 6
    assert result.next_level_grouping[root_dep_id].summary.headcount == 6
    assert result.next_level_grouping[root_dep_id].children[child_dep_id].summary.headcount == 5
    assert (
        result.next_level_grouping[root_dep_id]
        .children[child_dep_id]
        .children[grand_child_dep_id]
        .summary.headcount == 3
    )


def test_group_by_departments_and_vs_with_hierarchy():
    # given
    # Dp -> Vs1
    #  |
    #   -> Dc -> Vs1
    #         |
    #         -> Vs2
    vs1_id = 4
    vs2_id = 5

    root_dep_id = 1
    vs1_for_root_dep_summary = SummaryResults.default(headcount=1)

    child_dep_id = 2
    vs1_for_child_dep_summary = SummaryResults.default(headcount=1)
    vs2_for_child_dep_summary = SummaryResults.default(headcount=1)

    result = Result.default(
        summary=SummaryResults.default(headcount=0),
        next_level_grouping={
            root_dep_id: Result.default(
                result_id=root_dep_id,
                summary=SummaryResults.default(headcount=0),
                next_level_grouping={
                    vs1_id: Result.default(result_id=vs1_id, summary=vs1_for_root_dep_summary),
                },
                children={
                    child_dep_id: Result.default(
                        result_id=child_dep_id,
                        summary=SummaryResults.default(headcount=0),
                        next_level_grouping={
                            vs1_id: Result.default(result_id=vs1_id, summary=vs1_for_child_dep_summary),
                            vs2_id: Result.default(result_id=vs2_id, summary=vs2_for_child_dep_summary),
                        }
                    ),
                },
            ),
        }
    )

    # when
    HierarchySummaryCounter().summarize(result)

    # then
    expected = Result.default(
        summary=SummaryResults.default(headcount=3),
        next_level_grouping={
            root_dep_id: Result.default(
                result_id=root_dep_id,
                summary=SummaryResults.default(headcount=3),
                next_level_grouping={
                    vs1_id: Result.default(result_id=vs1_id, summary=SummaryResults.default(headcount=2)),
                    vs2_id: Result.default(result_id=vs2_id, summary=vs2_for_child_dep_summary),
                },
                children={
                    child_dep_id: Result.default(
                        result_id=child_dep_id,
                        summary=SummaryResults.default(headcount=2),
                        next_level_grouping={
                            vs1_id: Result.default(result_id=vs1_id, summary=vs1_for_child_dep_summary),
                            vs2_id: Result.default(result_id=vs2_id, summary=vs2_for_child_dep_summary),
                        }
                    ),
                },
            ),
        }
    )
    assert result == expected


def test_collecting_from_next_level_grouping():
    # given
    # Dp -> Vs1
    #  |
    #   -> Dc -> Vs1
    #         |
    #         Dg -> Vs1
    vs1_id = 1

    root_dep_id = 1
    root_summary = SummaryResults.default(headcount=0)
    vs1_for_root_dep_summary = SummaryResults.default(headcount=1)

    child_dep_id = 2
    child_summary = SummaryResults.default(headcount=0)
    vs1_for_child_dep_summary = SummaryResults.default(headcount=1)

    grand_child_dep_id = 3
    grand_child_summary = SummaryResults.default(headcount=0)
    vs1_for_grand_child_dep_summary = SummaryResults.default(headcount=1)

    result = Result.default(
        summary=SummaryResults.default(headcount=0),
        next_level_grouping={
            root_dep_id: Result.default(
                result_id=root_dep_id,
                summary=root_summary,
                next_level_grouping={
                    vs1_id: Result.default(result_id=vs1_id, summary=vs1_for_root_dep_summary),
                },
                children={
                    child_dep_id: Result.default(
                        result_id=child_dep_id,
                        summary=child_summary,
                        next_level_grouping={
                            vs1_id: Result.default(result_id=vs1_id, summary=vs1_for_child_dep_summary),
                        },
                        children={
                            grand_child_dep_id: Result.default(
                                result_id=grand_child_dep_id,
                                summary=grand_child_summary,
                                next_level_grouping={
                                    vs1_id: Result.default(result_id=vs1_id, summary=vs1_for_grand_child_dep_summary)
                                }
                            )
                        }
                    ),
                },
            ),
        }
    )

    # when
    HierarchySummaryCounter().summarize(result)

    # then
    expected = Result.default(
        summary=SummaryResults.default(headcount=3),
        next_level_grouping={
            root_dep_id: Result.default(
                result_id=root_dep_id,
                summary=SummaryResults.default(headcount=3),
                next_level_grouping={
                    vs1_id: Result.default(result_id=vs1_id, summary=SummaryResults.default(headcount=3)),
                },
                children={
                    child_dep_id: Result.default(
                        result_id=child_dep_id,
                        summary=SummaryResults.default(headcount=2),
                        next_level_grouping={
                            vs1_id: Result.default(result_id=vs1_id, summary=SummaryResults.default(headcount=2)),
                        },
                        children={
                            grand_child_dep_id: Result.default(
                                result_id=grand_child_dep_id,
                                summary=SummaryResults.default(headcount=1),
                                next_level_grouping={
                                    vs1_id: Result.default(result_id=vs1_id, summary=vs1_for_grand_child_dep_summary)
                                }
                            )
                        }
                    ),
                },
            ),
        }
    )
    assert result == expected


def test_summarizing_for_deeper_groupings():
    # given
    # Dp -> Vs1 -> Geo1
    #  |
    #   -> Dc -> Vs1 -> Geo1
    #                -> Geo2
    parent_dep_id = 1
    child_dep_id = 2
    vs1_id = 3
    geo1_id = 4
    geo2_id = 5

    result = Result.default(
        summary=SummaryResults.default(headcount=0),
        next_level_grouping={
            parent_dep_id: Result.default(
                result_id=parent_dep_id,
                summary=SummaryResults.default(headcount=0),
                next_level_grouping={
                    vs1_id: Result.default(
                        result_id=vs1_id,
                        summary=SummaryResults.default(headcount=0),
                        next_level_grouping={
                            geo1_id: Result.default(
                                result_id=geo1_id,
                                summary=SummaryResults.default(headcount=1)
                            )
                        }
                    ),
                },
                children={
                    child_dep_id: Result.default(
                        result_id=child_dep_id,
                        summary=SummaryResults.default(headcount=0),
                        next_level_grouping={
                            vs1_id: Result.default(
                                result_id=vs1_id,
                                summary=SummaryResults.default(headcount=0),
                                next_level_grouping={
                                    geo1_id: Result.default(
                                        result_id=geo1_id,
                                        summary=SummaryResults.default(headcount=1)
                                    ),
                                    geo2_id: Result.default(
                                        result_id=geo2_id,
                                        summary=SummaryResults.default(headcount=1)
                                    )
                                }
                            ),
                        },
                    ),
                },
            ),
        }
    )

    # when
    HierarchySummaryCounter().summarize(result)

    # then
    expected = {
        vs1_id: Result.default(
            result_id=vs1_id,
            summary=SummaryResults.default(headcount=3),
            next_level_grouping={
                geo1_id: Result.default(result_id=geo1_id, summary=SummaryResults.default(headcount=2)),
                geo2_id: Result.default(result_id=geo2_id, summary=SummaryResults.default(headcount=1)),
            }
        ),
    }
    assert result.next_level_grouping[parent_dep_id].next_level_grouping == expected
