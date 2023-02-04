import mock

from genisys.web import model

from .base import GenisysWebTestCase


class SectionTestCase(GenisysWebTestCase):
    def test_get(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5', 'gr2'],
                                    stype="yaml", stype_options=None)
        self.storage.create_section("user5", ['gr2'], "sec1", 2, "s11", "sec1.1desc",
                                    owners=['user3'],
                                    stype="yaml", stype_options=None)
        html = self.get_html('/sections/')
        self.assertTrue(html.find(id='createsubsection'))
        self.assertTrue(html.find(id='createrule'))

        self.config['BYPASS_AUTH_AS'] = 'anotheruser'
        html = self.get_html('/sections/')
        self.assertFalse(html.find(id='createsubsection'))

        html = self.get_html('/sections/sec1.s11')
        self.assertEquals(str(html.select_one('div.lead > p')),
                          '<p>sec1.1desc</p>')

        res = self.client.get('/sections/sec2')
        self.assertEquals(res.status_code, 404)

        html = self.get_html('/sections/?revision=1')
        self.assertEquals(
            html.select_one('div.alert-danger > p.lead').text,
            "You are viewing an outdated revision of the section!"
        )
        self.get_html('/sections/?revision=2')
        self.get_html('/sections/?revision=3')
        self.get_html('/sections/')

        resp = self.client.get('/sections/?revision=100500')
        self.assertEquals(resp.status_code, 404)
        resp = self.client.get('/sections/?revision=xasdaf')
        self.assertEquals(resp.status_code, 404)

    def test_new_section(self):
        html = self.get_html('/sections', follow_redirects=True)
        inp = html.select_one('#createsubsection input[name=csrf_token]')
        csrf_token = inp.attrs['value']
        html = self.post_and_redirect(
            data={'name': 'newsubsectionname', 'owners': 'owner1  owner2',
                  'desc': 'new subsection desc', 'csrf_token': csrf_token,
                  'action': 'new-section'},
            url='/sections/',
            expected_redirect='/sections/newsubsectionname'
        )
        self.assertEquals(html.select_one('.ownerslist').text,
                          'owners: owner1, owner2, inherited owners: user1, user2')
        self.assert_nav(html, [
            ('newsubsectionname', '/sections/newsubsectionname?revision=2')
        ])

        self.post_and_form_errors(
            data={'name': ' ', 'owners': '', 'desc': '',
                  'csrf_token': csrf_token, 'action': 'new-section'},
            url='/sections/',
            expected_errors={'Subsection name': ['This field is required.']}
        )

        self.post_and_form_errors(
            data={'name': 'newsubsectionname', 'owners': '', 'desc': '',
                  'csrf_token': csrf_token, 'action': 'new-section'},
            url='/sections/',
            expected_errors={
                'Subsection name': ['Subsection name is not unique.']
            }
        )

        self.post_and_csrf_error('/sections/')

    def test_new_section_outdated(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'],
                                    stype="yaml", stype_options=None)
        html = self.get_html('/sections', follow_redirects=True)
        inp = html.select_one('#createsubsection input[name=csrf_token]')
        csrf_token = inp.attrs['value']
        html = self.post_and_conflict(
            data={'name': 'newsubsectionname', 'owners': 'owner1  owner2',
                  'desc': 'new subsection desc', 'csrf_token': csrf_token,
                  'action': 'new-section', 'parent_revision': 1},
            url='/sections/',
        )

    def test_new_section_stype_sandbox_resource(self):
        html = self.get_html('/sections/')
        inp = html.select_one('#createsubsection input[name=csrf_token]')
        csrf_token = inp.attrs['value']
        with mock.patch('genisys.web.sandbox.get_resource_type_description'):
            html = self.post_and_redirect(
                data={'name': 'sname',
                      'csrf_token': csrf_token,
                      'stype': 'sandbox_resource',
                      'sandbox_resource_type': 'srt',
                      'action': 'new-section'},
                url='/sections/',
                expected_redirect='/sections/sname'
            )
        self.assertTrue(html.select_one('#sandbox-logo'))

    def test_change_desc(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'],
                                    stype="yaml", stype_options=None)

        html = self.get_html('/sections/sec1')
        inp = html.select_one('#descform input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data={'desc': 'brand new<br> *desc*', 'csrf_token': csrf_token,
                  'action': 'change-desc'},
            url='/sections/sec1',
            expected_redirect='/sections/sec1'
        )
        self.assertEquals(str(html.select_one('div.lead > p')),
                          '<p>brand new&lt;br&gt; <em>desc</em></p>')

        self.config['BYPASS_AUTH_AS'] = 'user4'
        self.post_and_unauthorized(
            data={'desc': '*desc3*', 'csrf_token': csrf_token,
                  'action': 'change-desc'},
            url='/sections/sec1'
        )

    def test_change_desc_outdated(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'],
                                    stype="yaml", stype_options=None)
        html = self.get_html('/sections/')
        inp = html.select_one('#descform input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_conflict(
            data={'desc': 'test', 'csrf_token': csrf_token,
                  'action': 'change-desc', 'revision': 1},
            url='/sections/'
        )

    def test_change_owners(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'],
                                    stype="yaml", stype_options=None)

        html = self.get_html('/sections/sec1')
        inp = html.select_one('#ownersform input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data={'owners': 'o1 o2', 'csrf_token': csrf_token,
                  'action': 'change-owners'},
            url='/sections/sec1',
            expected_redirect='/sections/sec1'
        )
        self.assertEquals(html.select_one('.ownerslist').text,
                          'owners: o1, o2, inherited owners: user1, user2')

        self.config['BYPASS_AUTH_AS'] = 'user4'
        self.post_and_unauthorized(
            data={'owners': '', 'csrf_token': csrf_token,
                  'action': 'change-owners'},
            url='/sections/sec1'
        )

    def test_new_rule(self):
        self.storage.create_section("user2", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'],
                                    stype="yaml", stype_options=None)

        html = self.get_html('/sections/sec1')
        inp = html.select_one('#createrule input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='new-rule',
                      revision='2', name='rule name', desc='ruledesc',
                      editors=' e1 e2 ', selector='rule selector',
                      config='foo:\n  bar: 1'),
            url='/sections/sec1',
            expected_redirect='/rules/sec1/rule%20name'
        )
        self.assert_nav(html, [
            ('sec1', '/sections/sec1?revision=3'),
            ('rule name', '/rules/sec1/rule%20name?revision=3')
        ])
        self.assertEquals(str(html.select_one('div.lead > p')),
                          '<p>ruledesc</p>')
        self.assertEquals(html.select_one('.editorslist').text,
                          'editors: e1, e2')
        sec1 = self.storage._find_section('sec1')
        self.assertEquals(sec1['rules'][0], {
            'config': {'foo': {'bar': 1}},
            'config_source': 'foo:\n  bar: 1',
            'ctime': sec1['rules'][0]['ctime'],
            'mtime': sec1['rules'][0]['mtime'],
            'desc': 'ruledesc',
            'editors': ['e1', 'e2'],
            'name': 'rule name',
            'selector': 'rule selector',
            'subrules': []
        })

        html = self.get_html('/sections/sec1')
        inp = html.select_one('#createrule input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        self.post_and_form_errors(
            data=dict(csrf_token=csrf_token, action='new-rule',
                      revision='3', name='rule name', desc='',
                      editors='', selector='sele', config='foo:\n bar: 1'),
            url='/sections/sec1',
            expected_errors={'Name': ['Rule name is not unique.']}
        )

        self.post_and_form_errors(
            data=dict(csrf_token=csrf_token, action='new-rule',
                      revision='2', name='rule name', desc='',
                      editors='', selector='sele', config=']'),
            url='/sections/sec1',
            expected_errors={'Config': ["Line 0: expected the node content, "
                                        "but found ']'"]}
        )

    def test_new_rule_all_hosts(self):
        html = self.get_html('/sections/')
        inp = html.select_one('#createrule input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='new-rule',
                      revision='1', name='rule1', desc='ruledesc',
                      editors='', selector='', htype='all',
                      config='foo:\n  bar: 1'),
            url='/sections/',
            expected_redirect='/rules/rule1'
        )
        sec1 = self.storage._find_section('')
        self.assertEquals(sec1['rules'][0], {
            'config': {'foo': {'bar': 1}},
            'config_source': 'foo:\n  bar: 1',
            'ctime': sec1['rules'][0]['ctime'],
            'mtime': sec1['rules'][0]['mtime'],
            'desc': 'ruledesc',
            'editors': [],
            'name': 'rule1',
            'selector': None,
            'subrules': []
        })
        self.get_html('/history/')

    def test_dots(self):
        html = self.get_html('/sections/')
        inp = html.select_one('#createrule input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='new-rule',
                      revision='1', name='ru.le', desc='ruledesc',
                      editors='', selector='selector',
                      config='o.o: 1'),
            url='/sections/',
            expected_redirect='/rules/ru.le'
        )
        sec1 = self.storage._find_section('')
        self.assertEquals(sec1['rules'][0], {
            'config': {'o.o': 1},
            'config_source': 'o.o: 1',
            'ctime': sec1['rules'][0]['ctime'],
            'mtime': sec1['rules'][0]['mtime'],
            'desc': 'ruledesc',
            'editors': [],
            'name': 'ru.le',
            'selector': 'selector',
            'subrules': []
        })
        self.get_html('/history/')

    def test_reorder_rules(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'], stype="sandbox_resource",
                                    stype_options={'resource_type': 'RESTYPE'})
        self.storage.create_rule(
            'user5', [], 'sec1', 2, rulename='rule0', desc='desc',
            editors=[], selector='slctr', config={'resource_id': 10},
            config_source='config source', parent_rule=None
        )
        self.storage.create_rule(
            'user5', [], 'sec1', 3, rulename='rule1', desc='desc',
            editors=[], selector='slctr', config={'resource_id': 20},
            config_source='config source', parent_rule=None
        )
        self.storage.create_rule(
            'user5', [], 'sec1', 4, rulename='rule2', desc='desc',
            editors=[], selector='slctr', config={'resource_id': 30},
            config_source='config source', parent_rule=None
        )
        html = self.get_html('/sections/sec1')
        order = [e.text for e in html.select('#rules a.rulename')]
        self.assertEquals(order, ['rule0', 'rule1', 'rule2'])

        inp = html.select_one('#createrule input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='reorder-rules',
                      revision='5', order='1 2 0'),
            url='/sections/sec1',
            expected_redirect='/sections/sec1'
        )
        order = [e.text for e in html.select('#rules a.rulename')]
        self.assertEquals(order, ['rule1', 'rule2', 'rule0'])

    def test_delete_empty_section(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'], stype="yaml",
                                    stype_options={})
        html = self.get_html('/sections/sec1')
        self.assertTrue(html.select_one('form#delete-section'))

        self.storage.create_section("user1", [], "sec1", 2, "sec11", "secdesc",
                                    owners=['user5'], stype="yaml",
                                    stype_options={})
        html = self.get_html('/sections/sec1')
        self.assertIsNone(html.select_one('form#delete-section'))

        html = self.get_html('/sections/sec1.sec11')
        form = html.select_one('form#delete-section')
        self.assertTrue(form)
        csrf_token = form.select_one('input[name=csrf_token]').attrs['value']

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token),
            url='/delete/sec1.sec11?revision=3',
            expected_redirect='/sections/sec1'
        )
        self.assertTrue(html.select_one('form#delete-section'))

        res = self.client.post('/delete/sec1.sec11',
                               data={'csrf_token': csrf_token})
        self.assertEquals(res.status_code, 400)


class AliasesTestCase(GenisysWebTestCase):
    def test_get(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5', 'gr2'],
                                    stype="sandbox_resource",
                                    stype_options={'resource_type': 'RTP'})

        self.storage.db.volatile.update_one(
            {'key': model.volatile_key_hash('RTP'),
             'vtype': 'sandbox_releases'},
            {'$set': {
                'value': model._serialize([
                    {'resource_id': '123', 'description': 's123'},
                    {'resource_id': '124', 'description': 's124'},
                    {'resource_id': '125', 'description': 's125'},
                ])
            }}
        )
        html = self.get_html('/aliases/sec1')
        self.assertTrue(html.find('table', id='aliases'))
        releases = [(e['value'], e.text)
                    for e in html.select('#empty-alias-form #resource option')]
        self.assertEquals(releases, [
            ('', ''), ('123', '#123 (s123)'),
            ('124', '#124 (s124)'), ('125', '#125 (s125)')
        ])

        self.config['BYPASS_AUTH_AS'] = 'anotheruser'
        html = self.get_html('/aliases/sec1')
        self.assertFalse(html.find('table', id='aliases'))

    def test_wrong_stype(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5', 'gr2'],
                                    stype="yaml", stype_options={})
        res = self.client.get('/aliases/sec1')
        self.assertEquals(res.status_code, 404)
        res = self.client.get('/aliases/')
        self.assertEquals(res.status_code, 404)

    def test_edit(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5', 'gr2'],
                                    stype="sandbox_resource",
                                    stype_options={'resource_type': 'RTP'})
        self.storage.db.volatile.update_one(
            {'key': model.volatile_key_hash('RTP'),
             'vtype': 'sandbox_releases'},
            {'$set': {
                'value': model._serialize([
                    {'resource_id': 123, 'description': 's123'},
                    {'resource_id': 124, 'description': 's124'},
                    {'resource_id': 125, 'description': 's125'},
                ])
            }}
        )
        html = self.get_html('/aliases/sec1')
        inp = html.select_one('input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        self.post_and_form_errors(
            data={'name': [' '], 'resource': ['125'],
                  'csrf_token': csrf_token},
            url='/aliases/sec1',
            expected_errors={'Alias name': ['This field is required.']}
        )
        self.post_and_form_errors(
            data={'name': ['nm', 'nm2'], 'resource': ['125', ''],
                  'csrf_token': csrf_token},
            url='/aliases/sec1',
            expected_errors={'Released resource': ['This field is required']}
        )

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token,
                      name=['a1', 'a2'], resource=['125', '123']),
            url='/aliases/sec1',
            expected_redirect='/sections/sec1'
        )
        self.assertEquals([e.text for e in html.select('#flashes p span')],
                          ['Aliases successfully saved'])

        html = self.get_html('/aliases/sec1')
        alias_ids = [e['value']
                     for e in html.select('#aliases input[name=id]')]

        self.storage.create_rule(
            'user5', [], 'sec1', 3, rulename='rule1', desc='desc',
            editors=[], selector='slctr', config={'resource_id': 125},
            config_source={'rtype': 'by_alias', 'alias_id': alias_ids[0],
                           'alias_name': 'a1', 'resource_id': 125,
                           'resource_description': 's125'},
            parent_rule=None
        )
        self.storage.create_rule(
            'user5', [], 'sec1', 4, rulename='rule2', desc='desc',
            editors=[], selector='slctr', config={'resource_id': 125},
            config_source={'rtype': 'by_alias', 'alias_id': alias_ids[0],
                           'alias_name': 'a1', 'resource_id': 125,
                           'resource_description': 's125'},
            parent_rule='rule1',
        )
        html = self.get_html('/aliases/sec1')
        aliases_in_use = [
            (e.find_parent('tr')['id'],
             [a.text for a in e.select('a.rulename')])
            for e in html.select('table#aliases span.in-use-by')
        ]
        self.assertEquals(aliases_in_use, [
            (alias_ids[0], ['rule1', 'rule2'])
        ])
        self.assertTrue(html.find('tr', id=alias_ids[1])
                            .find('button', class_='remove-alias'))
        self.assertFalse(html.find('tr', id=alias_ids[0])
                             .find('button', class_='remove-alias'))

        self.post_and_redirect(
            data=dict(csrf_token=csrf_token,
                      name=['a1', 'a3'], id=[alias_ids[0], ''],
                      resource=['123', '124']),
            url='/aliases/sec1',
            expected_redirect='/sections/sec1'
        )

        self.get_html('/history/sec1')

        html = self.get_html('/aliases/sec1?revision=5')
        self.assertFalse(html.find('table', id='aliases'))

        self.config['BYPASS_AUTH_AS'] = 'e2'
        html = self.get_html('/aliases/sec1')
        self.assertFalse(html.find('table', id='aliases'))


class RuleTestCase(GenisysWebTestCase):
    def test_edit(self):
        self.storage.create_section("user2", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'],
                                    stype="yaml", stype_options=None)
        self.storage.create_rule(
            'user2', [], 'sec1', 2, rulename='rule0', desc='desc',
            editors=['e1', 'e2'], selector='slctr', config='config',
            config_source='config source', parent_rule=None
        )

        html = self.get_html('/rules/sec1/rule0')
        inp = html.select_one('#editruleform input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='edit-rule',
                      revision='3', desc='ruledesc new',
                      editors='e3', selector='rule selector new'),
            url='/rules/sec1/rule0',
            expected_redirect='/rules/sec1/rule0'
        )
        self.assertEquals(str(html.select_one('div.lead > p')),
                          '<p>ruledesc new</p>')
        self.assertEquals(html.select_one('.editorslist').text,
                          'editors: e3')
        self.assertEquals(html.select_one('div.selector > p').text,
                          'rule selector new')

        res = self.client.get('/rules/sec1/rule2')
        self.assertEquals(res.status_code, 404)

    def test_edit_config(self):
        self.storage.create_section("user2", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'],
                                    stype="yaml", stype_options=None)
        self.storage.create_rule(
            'user2', [], 'sec1', 2, rulename='rule0', desc='desc',
            editors=['e1', 'e2'], selector='slctr', config='config',
            config_source='config source', parent_rule=None
        )

        self.config['BYPASS_AUTH_AS'] = 'e2'

        html = self.get_html('/rules/sec1/rule0')
        inp = html.select_one('#configform input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='edit-config',
                      revision='3', config='foo:\n  bar: true'),
            url='/rules/sec1/rule0',
            expected_redirect='/rules/sec1/rule0'
        )
        self.assertEquals(html.select_one('#configform textarea').text,
                          'foo:\n  bar: true')
        sec1 = self.storage._find_section('sec1')
        self.assertEquals(sec1['rules'][0]['config'], {'foo': {'bar': True}})

        self.config['BYPASS_AUTH_AS'] = 'e3'

        self.post_and_unauthorized(
            data=dict(csrf_token=csrf_token, action='edit-config',
                      revision='4', config='foo:\n  bar: true'),
            url='/rules/sec1/rule0'
        )

    def test_edit_config_retpath(self):
        self.storage.create_section("user2", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'],
                                    stype="yaml", stype_options=None)
        self.storage.create_rule(
            'user2', [], 'sec1', 2, rulename='rule0', desc='desc',
            editors=['e1', 'e2'], selector='slctr', config='config',
            config_source='config source', parent_rule=None
        )

        self.config['BYPASS_AUTH_AS'] = 'e2'

        html = self.get_html('/rules/sec1/rule0')
        inp = html.select_one('#configform input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='edit-config',
                      revision='3', config='foo:\n  bar: true'),
            url='/rules/sec1/rule0?retpath=/sections/%3fbzz',
            expected_redirect='/sections/?bzz'
        )

        self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='edit-config',
                      revision='4', config='foo:\n  bar: true'),
            url='/rules/sec1/rule0?retpath=http://something',
            expected_redirect='/rules/sec1/rule0'
        )

    def test_edit_config_sandbox_resource(self):
        self.storage.create_section("user1", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'], stype="sandbox_resource",
                                    stype_options={'resource_type': 'RESTYPE'})
        self.storage.db.volatile.update_one(
            {'key': model.volatile_key_hash('RESTYPE'),
             'vtype': 'sandbox_releases'},
            {'$set': {
                'value': model._serialize([
                    {'resource_id': '123', 'description': 's123'},
                    {'resource_id': '124', 'description': 's124'},
                    {'resource_id': '125', 'description': 's125'},
                ])
            }}
        )
        self.storage.create_rule(
            'user1', [], 'sec1', 2, rulename='rule1', desc='desc',
            editors=[], selector=None, config={'resource_id': 12},
            config_source={'rtype': 'by_id', 'description': 'descr'},
            parent_rule=None
        )

        html = self.get_html('/rules/sec1/rule1')
        options = [(e.attrs['value'], e.text)
                   for e in html.select('#config-source-form #released_resource option')]
        self.assertEquals(options, [('', ''),
                                    ('123', '#123 (s123)'),
                                    ('124', '#124 (s124)'),
                                    ('125', '#125 (s125)')])

        inp = html.select_one('#configform input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        with mock.patch('genisys.web.sandbox.get_resource_info') as gri:
            gri.return_value = {'type': 'RESTYPE', 'description': 'redesc'}
            html = self.post_and_redirect(
                data=dict(csrf_token=csrf_token, action='edit-config',
                          revision='3', rtype='by_id', resource=19922),
                url='/rules/sec1/rule1',
                expected_redirect='/rules/sec1/rule1'
            )
        self.assertEquals(html.select_one('#config-source-readonly p a').text,
                          '#19922')
        self.get_html('/history/')

    def test_all_hosts_to_some_and_back(self):
        self.storage.create_rule(
            'user1', [], '', 1, rulename='rule1', desc='desc',
            editors=[], selector=None, config='config',
            config_source='config source', parent_rule=None
        )

        html = self.get_html('/rules/rule1')
        inp = html.select_one('#configform input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='edit-rule',
                      revision='2', desc='', editors='',
                      selector='selecto r', htype='some'),
            url='/rules/rule1',
            expected_redirect='/rules/rule1'
        )
        self.assertEquals(html.select_one('div.selector > p').text, 'selecto r')

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='edit-rule',
                      revision='3', desc='', editors='',
                      selector='selecto r', htype='all'),
            url='/rules/rule1',
            expected_redirect='/rules/rule1'
        )
        self.assertEquals(len(html.select('#applies-to-all')), 1)
        self.assertEquals(len(html.select('#selectorfield')), 0)

        self.get_html('/history/')

    def test_delete_rule(self):
        self.storage.create_rule(
            'user1', [], '', 1, rulename='rule1', desc='desc',
            editors=[], selector=None, config='config',
            config_source='config source', parent_rule=None
        )

        html = self.get_html('/rules/rule1')
        inp = html.select_one('#configform input[name=csrf_token]')
        csrf_token = inp.attrs['value']

        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='delete-rule',
                      revision='2'),
            url='/rules/rule1',
            expected_redirect='/sections/'
        )

    def test_reorder_subrules(self):
        self.storage.create_rule(
            'user1', [], '', 1, rulename='rootrule', desc='desc',
            editors=['group:coolpeople'], selector='slctr',
            config={'resource_id': 10},
            config_source='config source', parent_rule=None
        )
        self.storage.create_rule(
            'user1', [], '', 2, rulename='srule1', desc='desc',
            editors=[], selector='slctr', config={'resource_id': 20},
            config_source='config source', parent_rule='rootrule'
        )
        self.storage.create_rule(
            'user1', [], '', 3, rulename='srule2', desc='desc',
            editors=[], selector='slctr', config={'resource_id': 30},
            config_source='config source', parent_rule='rootrule'
        )
        html = self.get_html('/rules/rootrule')
        order = [e.text for e in html.select('#rules a.rulename')]
        self.assertEquals(order, ['srule1', 'srule2'])

        csrf_token = html.select_one('input[name=csrf_token]').attrs['value']

        self.config['BYPASS_AUTH_AS'] = 'somecoolguy'
        self.config['BYPASS_AUTH_AS_GROUPS'] = ['othergroup', 'coolpeople']
        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, action='reorder-rules',
                      revision='4', order='1 0'),
            url='/rules/rootrule',
            expected_redirect='/rules/rootrule'
        )
        order = [e.text for e in html.select('#rules a.rulename')]
        self.assertEquals(order, ['srule2', 'srule1'])


class RevertRulesTestCase(GenisysWebTestCase):
    def test(self):
        self.storage.create_section("user2", [], "", 1, "sec1", "secdesc",
                                    owners=['user5'],
                                    stype="yaml", stype_options=None)
        self.storage.create_rule(
            'user2', [], 'sec1', 2, rulename='rule0', desc='desc',
            editors=['e1', 'e2'], selector='slctr', config='config',
            config_source='config source', parent_rule=None
        )
        self.storage.create_rule(
            'user2', [], 'sec1', 3, rulename='rule1', desc='desc',
            editors=['e1', 'e2'], selector='slctr', config='config',
            config_source='config source', parent_rule=None
        )
        html = self.get_html('/sections/sec1?revision=3')
        form = html.find('button', text='revert').find_parent('form')
        inp = form.select_one('input[name=csrf_token]')
        csrf_token = inp.attrs['value']
        action = form.attrs['action']
        html = self.post_and_redirect(
            data=dict(csrf_token=csrf_token, current_rev=4, revert_to_rev=3),
            url=action,
            expected_redirect='/sections/sec1'
        )
        self.assertEquals(len(html.select('#rulestable tr')), 1)


class GroupTestCase(GenisysWebTestCase):
    CONFIG = {
        'STAFF_HEADERS': {'x-staff-header': 'bzz'},
        'STAFF_TIMEOUT': 4,
        'STAFF_URI': 'https://staff'
    }

    def _resp(self, content):
        return Resp()

    def _test(self, groupname, response, expected_redirect):
        class Resp(object):
            def json(self):
                return response
            def raise_for_status(self):
                pass
        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = Resp()
            result = self.get_and_redirect('/group/{}'.format(groupname),
                                           expected_redirect)

    def test_department(self):
        self._test(
            'yandex_search',
            {"type": "department",
             "service": {"id": None},
             "parent": {"service": {"id": None}}},
            'https://staff.yandex-team.ru/departments/yandex_search'
        )

    def test_wiki(self):
        self._test(
            'mlrussia',
            {"type": "wiki", "service": {"id": None}},
            'https://staff.yandex-team.ru/groups/mlrussia'
        )

    def test_service(self):
        self._test(
            'svc_skynet',
            {"type": "service", "service": {"id": 593}},
            'https://abc.yandex-team.ru/services/593/'
        )

    def test_servicerole(self):
        self._test(
            'svc_skynet_development',
            {"type": "servicerole",
             "service": {"id": None},
             "parent": {"service": {"id": 593}}},
            'https://abc.yandex-team.ru/services/593/'
        )

    def test_othertype(self):
        self._test(
            'unknowngroup',
            {"type": "unknown"},
            'https://staff.yandex-team.ru/groups/unknowngroup'
        )
