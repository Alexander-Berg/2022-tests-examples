from bcl.toolbox.widgets import WidgetDef, get_widgets_dict_from_data, Checkbox


def test_select():

    args = {
        'number': 'someval',
    }

    form = get_widgets_dict_from_data(args, [
        WidgetDef('select', 'number', 'numa', {'variants': {'one': 'two'}}),
    ])

    rendered = str(form['number'])
    assert 'someval' not in rendered  # У select нет value.


def test_checkbox():
    checkbox = Checkbox('my', 'Some')
    rendered = str(checkbox)
    assert rendered == (
        '<input id="my" name="my" value="1" class="" type="checkbox" placeholder="">&nbsp;'
        '<label for="my">Some</label>')
