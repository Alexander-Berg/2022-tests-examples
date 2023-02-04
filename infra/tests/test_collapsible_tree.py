from infra.reconf_juggler.util.d3js import collapsible_tree


def test_get_html():
    tree = {'name': 'parent-node', 'children': [{'name': 'child-node'}]}
    html = collapsible_tree.get_html(tree, title='Testing trees')

    assert html.startswith('<!DOCTYPE html>')
    assert '<title>Testing trees</title>' in html
    assert 'child-node' in html
    assert 'function draw_chart' in html
