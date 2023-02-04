import unittest
import time

import wtforms
import yaml
import mock
from werkzeug.datastructures import MultiDict

import genisys.web.forms
from genisys.web.sandbox import SandboxError

from .base import GenisysWebTestCase


class UserListFieldTestCase(GenisysWebTestCase):
    CONFIG = {
        'BYPASS_STAFF_USERNAMES_CHECK': False,
        'STAFF_HEADERS': {'Authentication': 'OAuth oauthtestingtoken'},
        'EMAIL_MAX_RECIPIENTS': 49,
    }

    class Form(wtforms.Form):
        users = genisys.web.forms.UserListField('users')

    class Response(object):
        def __init__(self, status_code, json):
            self.status_code = status_code
            self.json = lambda: json

    def setUp(self):
        super(UserListFieldTestCase, self).setUp()
        self.ctx = self.app.app_context()
        self.ctx.push()
        self.request_mock = mock.patch('requests.get')
        self.request_mocked = self.request_mock.start()

    def tearDown(self):
        super(UserListFieldTestCase, self).tearDown()
        self.request_mock.stop()
        self.ctx.pop()

    def _make_form(self, data):
        return self.Form(MultiDict(data))

    def test_valid(self):
        self.request_mocked.return_value = self.Response(200, {
            "links": {}, "page": 1, "limit": 4, "total": 4, "pages": 1,
            "result": [
                {"login": "user1"}, {"login": "foo"},
                {"login": "bar"}, {"login": "user3"},
            ],
        })
        form = self._make_form({'users': ['  user3 foo bar    user1 user1  ']})
        self.assertTrue(form.validate())
        self.assertEquals(form.data,
                          {'users': ['bar', 'foo', 'user1', 'user3']})
        self.request_mocked.assert_called_once_with(
            'https://staff-api.yandex-team.ru/v3/persons',
            headers={'Authentication': 'OAuth oauthtestingtoken'},
            params={'official.is_dismissed': 'false', '_limit': 4,
                    '_fields': 'login', 'login': 'bar,foo,user1,user3'},
            timeout=3, allow_redirects=False
        )

    def test_some_missing(self):
        self.request_mocked.return_value = self.Response(200, {
            "links": {}, "page": 1, "limit": 4, "total": 2, "pages": 1,
            "result": [{"login": "user4"}, {"login": "user3"}],
        })
        form = self._make_form({'users': ['user1 user2 user3 user4']})
        self.assertFalse(form.validate())
        self.assertEquals(form.errors,
                          {'users': ['Invalid usernames: user1, user2']})

    def test_one_missing(self):
        self.request_mocked.return_value = self.Response(200, {
            "links": {}, "page": 1, "limit": 1, "total": 0, "pages": 1,
            "result": [],
        })
        form = self._make_form({'users': ['uuser']})
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'users': ['Invalid username: uuser']})

    def test_too_many_users_users_only(self):
        usernames = ['u%d' % i for i in range(50)]
        self.request_mocked.return_value = self.Response(200, {
            "links": {}, "page": 1, "limit": 50, "total": 50, "pages": 1,
            "result": [{"login": login} for login in usernames],
        })
        form = self._make_form({'users': usernames})
        self.assertFalse(form.validate())
        self.assertEquals(
            form.errors, {'users': ['Too many users in a list (50 > 49)']}
        )

    def test_staff_fails_to_respond(self):
        self.request_mocked.return_value = self.Response(500, None)
        form = self._make_form({'users': ['uuser']})
        self.assertFalse(form.validate())
        self.assertEquals(form.errors,
                          {'users': ['Could not validate usernames']})

    def test_value(self):
        form = self._make_form({'users': [' uuser1 user2   user2 group:foo ']})
        self.assertEquals(form.users._value(), 'uuser1 user2 user2 group:foo')

    def test_groups(self):
        self.request_mocked.return_value = self.Response(200, {
            "links": {}, "page": 1, "limit": 2,
            "result": [{"url": "svc_skynet", "affiliation_counters": {}},
                       {"url": "spb-searchstaff", "affiliation_counters": {}}],
            "total": 2, "pages": 1
        })
        form = self._make_form({'users': ['group:svc_skynet  '
                                          'group:spb-searchstaff']})
        self.assertTrue(form.validate())
        self.assertEquals(
            form.data, {'users': ['group:spb-searchstaff', 'group:svc_skynet']}
        )
        self.request_mocked.assert_called_once_with(
            'https://staff-api.yandex-team.ru/v3/groups',
            headers={'Authentication': 'OAuth oauthtestingtoken'},
            params={'is_deleted': 'false', '_limit': 2, '_fields': 'url',
                    '_query': "url in ['spb-searchstaff','svc_skynet']"},
            timeout=3, allow_redirects=False
        )

    def test_some_groups_missing(self):
        self.request_mocked.return_value = self.Response(200, {
            "links": {}, "page": 1, "limit": 2,
            "result": [], "total": 0, "pages": 1
        })
        form = self._make_form({'users': ['group:svc_skynet  '
                                          'group:spb-searchstaff']})
        self.assertFalse(form.validate())
        self.assertEquals(
            form.errors,
            {'users': ['Invalid group names: spb-searchstaff, svc_skynet']}
        )

    def test_one_group_missing(self):
        self.request_mocked.return_value = self.Response(200, {
            "links": {}, "page": 1, "limit": 2,
            "result": [{"url": "spb-searchstaff", "affiliation_counters": {}}],
            "total": 2, "pages": 1
        })
        form = self._make_form({'users': ['group:svc_skynet  '
                                          'group:spb-searchstaff']})
        self.assertFalse(form.validate())
        self.assertEquals(form.errors,
                          {'users': ['Invalid group name: svc_skynet']})

    def test_invalid_symbols(self):
        testvals = ['group:some"thing', 'user1,user2']
        for users in testvals:
            form = self._make_form({'users': users})
            self.assertFalse(form.validate())
            self.assertEquals(form.errors, {'users': ['Invalid characters']})


class BlinovCalcSelectorFieldTestCase(unittest.TestCase):
    class Form(wtforms.Form):
        selector = genisys.web.forms.BlinovCalcSelectorField()

    def _make_form(self, selector=None):
        data = dict()
        if selector is not None:
            data['selector'] = selector
        return self.Form(MultiDict(data))

    def test_valid(self):
        self.assertTrue(self._make_form("""\
I@itag_skynet_experiment
group@[IMGHEAD BRASEROS]\r\n
I@ALL_SEARCH
K@search_robot_web-primus
itag@[ALL_WEB_R1_ALIAS ALL_WEB_C1_ALIAS ALL_IMGS_C1_ALIAS
      ALL_VIDEO_R1_ALIAS MSK_WEB_MMETA_R1]
K@[search_fml-hydralisk search_fml-pools search_fml-ultralisk]
h@hydralisk-dev.search.yandex.net
K@search_instrum-acms""").validate())

    def test_brackets_error(self):
        form = self._make_form("foo[bar")
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'selector': [
            'Brackets syntax is broken'
        ]})

        form = self._make_form("foo[bar}]")
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'selector': [
            'Brackets syntax is broken'
        ]})

        form = self._make_form("foo]bar")
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'selector': [
            'Brackets syntax is broken'
        ]})

    def test_unicode(self):
        form = self._make_form("С@bzz")
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'selector': [
            'Only ASCII symbols are allowed'
        ]})


class YamlFieldTestCase(unittest.TestCase):
    class Form(wtforms.Form):
        yaml = genisys.web.forms.YamlField()

    def _make_form(self, yaml=None):
        data = dict()
        if yaml is not None:
            data['yaml'] = yaml
        return self.Form(MultiDict(data))

    def test_valid(self):
        src = """
invoice: 34843
bill-to: &id001
    given  : Chris
    family : Dumars
    address:
        lines: |
            458 Walkman Dr.
            Suite #292
        city    : Royal Oak
        state   : MI
        postal  : 48046
ship-to: *id001
product:
    - sku         : BL394D
      quantity    : 4
      description : Basketball
      price       : 450.00
    - sku         : BL4438H
      quantity    : 1
      description : Super Hoop
      price       : 2392.00
tax  : [251, 12]
total: [4443, null]
comments:
    Late afternoon is best.
    Backup contact is Nancy
    Billsmer @ 338-4338.
"""
        form = self._make_form(src)
        self.assertTrue(form.validate())
        self.assertEquals(form.data, {'yaml': {
            'dict': {
                'bill-to': {'address': {'city': 'Royal Oak',
                                        'lines': '458 Walkman Dr.\n'
                                                 'Suite #292\n',
                                        'postal': 48046,
                                        'state': 'MI'},
                            'family': 'Dumars',
                            'given': 'Chris'},
                'comments': 'Late afternoon is best. Backup contact is '
                            'Nancy Billsmer @ 338-4338.',
                'invoice': 34843,
                'product': [{'description': 'Basketball',
                             'price': 450.0,
                             'quantity': 4,
                             'sku': 'BL394D'},
                            {'description': 'Super Hoop',
                             'price': 2392.0,
                             'quantity': 1,
                             'sku': 'BL4438H'}],
                'ship-to': {'address': {'city': 'Royal Oak',
                                        'lines': '458 Walkman Dr.\n'
                                                 'Suite #292\n',
                                        'postal': 48046,
                                        'state': 'MI'},
                            'family': 'Dumars',
                            'given': 'Chris'},
                'tax': [251, 12],
                'total': [4443, None],
            },
            'source': src
        }})
        self.assertEquals(form.yaml._value(), src)

    def test_parse_error(self):
        src = """
canonical: 12345
decimal: +12345
octal: 0o14
hexadecimal: 0xC
erroneous: [
"""
        form = self._make_form(src)
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'yaml': [
            "Line 6: expected the node content, but found '<stream end>'"
        ]})
        self.assertEquals(form.data, {'yaml': {'dict': None, 'source': src}})
        self.assertEquals(form.yaml._value(), src)

    def test_yaml_error(self):
        src = "src"
        with mock.patch('yaml.load') as mock_load:
            mock_load.side_effect = yaml.error.YAMLError("error text")
            form = self._make_form(src)
        self.assertFalse(form.validate())
        self.assertEquals(form.errors,
                          {'yaml': ["Yaml parser error: error text"]})
        self.assertEquals(form.data, {'yaml': {'dict': None, 'source': 'src'}})
        self.assertEquals(form.yaml._value(), src)

    def test_exception(self):
        src = "src"
        with mock.patch('yaml.load') as mock_load:
            mock_load.side_effect = ZeroDivisionError("foo bar")
            form = self._make_form(src)
        self.assertFalse(form.validate())
        self.assertEquals(form.errors,
                          {'yaml': ["Yaml parser exception: foo bar"]})
        self.assertEquals(form.data, {'yaml': {'dict': None, 'source': 'src'}})
        self.assertEquals(form.yaml._value(), src)

    def test_required(self):
        class Form(wtforms.Form):
            yaml = genisys.web.forms.YamlField('name',
                                               [wtforms.validators.required()])
        form = Form(MultiDict({}))
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'yaml': ["This field is required."]})
        self.assertEquals(form.data, {'yaml': None})
        self.assertEquals(form.yaml._value(), '')

    def test_unsafe(self):
        src = """
foo: !!python/object/apply:time.sleep
    - 1
"""
        form = self._make_form(src)
        started = time.time()
        self.assertFalse(form.validate())
        spent = time.time() - started
        self.assertLess(spent, 0.1)
        self.assertEquals(['yaml'], list(form.errors.keys()))
        self.assertTrue(form.errors['yaml'][0].startswith(
            "Line 1: could not determine a constructor for the tag"
        ))


class SandboxResourceTypeFieldTest(unittest.TestCase):
    class Form(wtforms.Form):
        sare = genisys.web.forms.SandboxResourceTypeField()

    def test_value(self):
        with mock.patch('genisys.web.sandbox.get_resource_type_description'):
            form = self.Form(MultiDict({'sare': 'something'}))
            self.assertEquals(form.sare._value(), 'something')
            form = self.Form()
            self.assertEquals(form.sare._value(), '')

    def test_process_formdata_empty(self):
        form = self.Form()
        self.assertTrue(form.validate())
        self.assertEquals(form.data['sare'], None)

    def test_process_formdata_valid(self):
        with mock.patch('genisys.web.sandbox.get_resource_type_description') as grtd:
            grtd.return_value = 'type description'
            form = self.Form(MultiDict({'sare': 'typename'}))
            self.assertTrue(form.validate())
            self.assertEquals(form.data['sare'], 'typename')

    def test_process_formdata_error(self):
        with mock.patch('genisys.web.sandbox.get_resource_type_description') as grtd:
            grtd.side_effect = SandboxError('error text')
            form = self.Form(MultiDict({'sare': 'typename'}))
            self.assertFalse(form.validate())
            self.assertEquals(form.data['sare'], 'typename')
            self.assertEquals(form.errors['sare'], ['error text'])


class NewSubsectionForm(GenisysWebTestCase):
    CONFIG = {'WTF_CSRF_ENABLED': False}

    def setUp(self):
        super(NewSubsectionForm, self).setUp()
        self.ctx = self.app.app_context()
        self.ctx.push()

    def tearDown(self):
        super(NewSubsectionForm, self).tearDown()
        self.ctx.pop()

    def test_stype_yaml_with_resource_type_name(self):
        with mock.patch('genisys.web.sandbox.get_resource_type_description'):
            form = genisys.web.forms.NewSubsectionForm(MultiDict({
                'name': 'secname', 'stype': 'yaml',
                'sandbox_resource_type': 'resource type',
                'parent_revision': '3',
            }))
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'sandbox_resource_type': [
            'This field is irrelevant for configuration type '
            'other than "Sandbox resource"'
        ]})

    def test_stype_sandbox_resource_without_resource_type_name(self):
        form = genisys.web.forms.NewSubsectionForm(MultiDict({
            'name': 'secname', 'stype': 'sandbox_resource',
            'parent_revision': '3',
        }))
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'sandbox_resource_type': [
            'This field is required for "Sandbox resource" configuration type'
        ]})

    def test_stype_sandbox_resource(self):
        with mock.patch('genisys.web.sandbox.get_resource_type_description'):
            form = genisys.web.forms.NewSubsectionForm(MultiDict({
                'name': 'secname', 'stype': 'sandbox_resource',
                'sandbox_resource_type': 'resourcetype',
                'parent_revision': '15'
            }))
        self.assertTrue(form.validate())
        self.assertEquals(form.data['stype'], form.STYPE_SANDBOX_RESOURCE)
        self.assertEquals(form.data['sandbox_resource_type'], 'resourcetype')

    def test_stype_yaml(self):
        form = genisys.web.forms.NewSubsectionForm(MultiDict({
            'name': 'secname', 'stype': 'yaml', 'parent_revision': '3',
        }))
        self.assertTrue(form.validate())
        self.assertEquals(form.data['stype'], form.STYPE_YAML)


class NewSandboxResourceRuleFormTest(GenisysWebTestCase):
    CONFIG = {'WTF_CSRF_ENABLED': False}

    def setUp(self):
        super(NewSandboxResourceRuleFormTest, self).setUp()
        self.ctx = self.app.app_context()
        self.ctx.push()
        self.gri_mock = mock.patch('genisys.web.sandbox.get_resource_info')
        self.gri_mocked = self.gri_mock.start()

    def tearDown(self):
        super(NewSandboxResourceRuleFormTest, self).tearDown()
        self.ctx.pop()
        self.gri_mock.stop()

    def test_release(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='sanrestyp',
            released_resources=[
                {'description': 'release1', 'resource_id': 1},
                {'description': 'release2', 'resource_id': 2},
                {'description': 'release4', 'resource_id': 4},
                {'description': 'release7', 'resource_id': 7},
            ],
            aliases=[],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'released',
                'released_resource': '4',
                'resource': '1423',
                'selector': 'blinov',
                'revision': '1',
            })
        )
        form.validate()
        self.assertEquals(form.errors, {})
        self.assertTrue(form.validate())
        self.assertEquals(
            form.data,
            {'action': '',
             'desc': '',
             'editors': [],
             'htype': 'some',
             'name': 'rulename',
             'released_resource': '4',
             'resource': None,
             'revision': 1,
             'rtype': 'released',
             'selector': 'blinov'}
        )
        self.assertEquals(form.get_config(), {'resource_id': 4})
        self.assertEquals(form.get_config_source(),
                          {'resource': 4, 'rtype': 'released',
                           'description': 'release4'})
        self.assertEquals(form.resource._value(), '')

    def test_config_source_release(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='retype',
            released_resources=[{'description': 'release1', 'resource_id': 4}],
            aliases=[],
            config_source={'resource': '4', 'rtype': 'released'}
        )
        self.assertEquals(form.resource.data, '4')
        self.assertEquals(form.released_resource.data, '4')
        self.assertEquals(form.rtype.data, 'released')

    def test_release_empty(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='sanrestyp',
            released_resources=[
                {'description': 'release1', 'resource_id': 1},
                {'description': 'release2', 'resource_id': 2},
            ],
            aliases=[],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'released',
                'released_resource': '',
                'selector': 'blinov',
                'revision': '1',
            })
        )
        self.assertFalse(form.validate())
        self.assertEquals(form.errors,
                          {'released_resource': ['This field is required']})

    def test_resource(self):
        self.gri_mocked.return_value = {'type': 'retype', 'description': 'd n'}
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='retype',
            released_resources=[],
            aliases=[],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'by_id',
                'release': '6668',
                'resource': '7755',
                'selector': 'blinov',
                'revision': '1',
            })
        )
        form.validate()
        self.assertEquals(form.errors, {})
        self.assertTrue(form.validate())
        self.assertEquals(
            form.data,
            {'action': '',
             'desc': '',
             'editors': [],
             'htype': 'some',
             'name': 'rulename',
             'resource': 7755,
             'revision': 1,
             'rtype': 'by_id',
             'selector': 'blinov'}
        )
        self.assertEquals(form.get_config(), {'resource_id': 7755})
        self.assertEquals(form.get_config_source(), {'resource': 7755,
                                                     'rtype': 'by_id',
                                                     'description': 'd n'})
        self.gri_mocked.assert_called_once_with(7755)
        self.assertEquals(form.resource._value(), '7755')
        self.assertNotIn('release', form)

    def test_config_source_resource(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='retype',
            released_resources=[{'description': 'release1', 'resource_id': 1}],
            config_source={'resource': 7755, 'rtype': 'by_id'},
            aliases=[]
        )
        self.assertEquals(form.resource.data, 7755)
        self.assertEquals(form.released_resource.data, '7755')
        self.assertEquals(form.rtype.data, 'by_id')

    def test_alias(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='retype',
            released_resources=[],
            aliases=[
                {'id': 'a1',
                 'name': 'a1',
                 'resource_description': 'rd1',
                 'resource_id': 11},
                {'id': 'a2',
                 'name': 'a2',
                 'resource_description': 'rd2',
                 'resource_id': 22},
            ],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'by_alias',
                'alias': 'a2',
                'release': '',
                'resource': '',
                'selector': 'blinov',
                'revision': '1',
            })
        )
        self.assertIn('by_alias', dict(form.rtype.choices))
        form.validate()
        self.assertEquals(form.errors, {})
        self.assertTrue(form.validate())
        self.assertEquals(
            form.data,
            {'action': '',
             'desc': '',
             'editors': [],
             'htype': 'some',
             'alias': 'a2',
             'name': 'rulename',
             'resource': None,
             'revision': 1,
             'rtype': 'by_alias',
             'selector': 'blinov'}
        )
        self.assertEquals(form.get_config(), {'resource_id': 22})
        self.assertEquals(form.get_config_source(), {
            'alias_id': 'a2',
            'alias_name': 'a2',
            'description': 'rd2',
            'resource': 22,
            'rtype': 'by_alias'
        })

    def test_config_source_alias(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='retype',
            released_resources=[{'description': 'release1', 'resource_id': 1}],
            config_source={
                'alias_id': 'a2',
                'alias_name': 'a2',
                'description': 'rd2',
                'resource': 22,
                'rtype': 'by_alias'
            },
            aliases=[
                {'id': 'a1',
                 'name': 'a1',
                 'resource_description': 'rd1',
                 'resource_id': 11},
                {'id': 'a2',
                 'name': 'a2',
                 'resource_description': 'rd2',
                 'resource_id': 22},
            ]
        )
        self.assertEquals(form.resource.data, 22)
        self.assertEquals(form.alias.data, 'a2')
        self.assertEquals(form.rtype.data, 'by_alias')

    def test_alias_empty(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='retype',
            released_resources=[],
            aliases=[
                {'id': 'a1',
                 'name': 'a1',
                 'resource_description': 'rd1',
                 'resource_id': 11},
            ],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'by_alias',
                'alias': '',
                'release': '',
                'resource': '',
                'selector': 'blinov',
                'revision': '1',
            })
        )
        self.assertFalse(form.validate())
        self.assertEquals(form.errors,
                          {'alias': ['This field is required']})

    def test_no_aliases(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='retype',
            released_resources=[],
            aliases=[],
        )
        self.assertNotIn('alias', form)
        self.assertNotIn('by_alias', dict(form.rtype.choices))

    def test_resource_empty(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='sanrestyp',
            released_resources=[],
            aliases=[],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'by_id',
                'resource': '',
                'selector': 'blinov',
                'revision': '1',
            })
        )
        self.assertFalse(form.validate())
        self.assertEquals(form.errors,
                          {'resource': ['This field is required']})

    def test_resource_invalid_int(self):
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='sanrestyp',
            released_resources=[],
            aliases=[],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'by_id',
                'resource': 'zxdad',
                'selector': 'blinov',
                'revision': '1',
            })
        )
        self.assertFalse(form.validate())
        self.assertEquals(form.errors,
                          {'resource': ['Not a valid integer value']})

    def test_resource_of_different_type(self):
        self.gri_mocked.return_value = {'id': 3322, 'type': 'othertype'}
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='sometype',
            released_resources=[],
            aliases=[],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'by_id',
                'resource': '7755',
                'selector': 'blinov',
                'revision': '1',
            })
        )
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'resource': [
            'Specified resource is of type othertype, not sometype'

        ]})

    def test_resource_sandbox_error(self):
        self.gri_mocked.side_effect = SandboxError('some error')
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='sometype',
            released_resources=[],
            aliases=[],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'by_id',
                'resource': '7755',
                'selector': 'blinov',
                'revision': '1',
            })
        )
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'resource': ['some error']})

    def test_all_hosts(self):
        self.gri_mocked.return_value = {'id': 3322, 'type': 'sometype',
                                        'description': 'descr'}
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='sometype',
            released_resources=[],
            aliases=[],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'by_id',
                'htype': 'all',
                'resource': '7755',
                'revision': '1',
            })
        )
        self.assertTrue(form.validate())

    def test_some_hosts_empty_selector(self):
        self.gri_mocked.return_value = {'id': 3322, 'type': 'sometype',
                                        'description': 'descr'}
        form = genisys.web.forms.NewSandboxResourceRuleForm(
            resource_type='sometype',
            released_resources=[],
            aliases=[],
            formdata=MultiDict({
                'name': 'rulename',
                'rtype': 'by_id',
                'htype': 'some',
                'resource': '7755',
                'revision': '1',
                'selector': '',
            })
        )
        self.assertFalse(form.validate())
        self.assertEquals(form.errors, {'selector': [
            'This field is required for chosen host list type'
        ]})
