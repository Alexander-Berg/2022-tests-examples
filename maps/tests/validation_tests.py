from maps.garden.modules.carparks.lib import validation


class TestReport:
    @staticmethod
    def test_html():
        assert (
            '<div style="font-family: Arial,sans-serif; font-size: small;">\n'
            '<p>Message</p>\n'
            '<table>\n'
            '<tr><th>name1</th><th>name2</th></tr>\n'
            '<tr><td>123</td><td>456</td></tr>\n'
            '<tr><td>abc</td><td>def</td></tr>\n'
            '</table>\n'
            '</div>'
            == validation._as_html('Message',
                                   ('name1', 'name2'),
                                   [('123', '456'),
                                    ('abc', 'def')]))

    @staticmethod
    def test_csv():
        assert (
            'name;1,"The ""name"""\n'
            '123,"4\n5"\n'
            '"ab,c",de f\n'
            == validation._as_csv(('name;1', 'The "name"'),
                                  [('123', '4\n5'),
                                   ('ab,c', 'de f')]))
