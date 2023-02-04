from mdh.core.toolbox.startrek import StartrekIssue


def test_issue_methods(init_schema):
    schema = init_schema('my_schema')
    issue = StartrekIssue(linked=schema)

    # не пытаемся взимодействовать с трекером, если
    # задача не привязана к объекту.
    assert issue.sync() is False
    assert issue.change_status(status='new', comment='some') == []
    assert issue.update() is None
