FIXTURES_ROOT = 'tests/fixture/'


def find_all_fixtures(fixture_type):
    import glob

    return map(
        lambda f: f.replace(FIXTURES_ROOT, '').replace('.xml', ''),
        glob.glob('{}{}/*.xml'.format(FIXTURES_ROOT, fixture_type))
    )


INVOICE = {
    'all': find_all_fixtures('invoice'),
    'fail': {
        'rtf': [
            'invoice/invoice.ph.rur.1.connect',
            'invoice/invoice.ph.rur.111',
            'invoice/invoice.ph.rur.111.new',
        ],
        'template_not_found': []
    },
}

ACT = {
    'all': find_all_fixtures('act'),
}

CONTRACT = {
    'all': find_all_fixtures('contract'),
}
