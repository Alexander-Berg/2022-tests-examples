
from unittest import TestCase

from django import forms
from django.conf import settings
from django.utils.translation import activate
from pretend import stub

from wiki.actions.classes.base_action import ParamsWrapper, WikiBaseAction
from wiki.actions.classes.base_widget_action import WikiWidgetAction
from wiki.utils.errors import InputValidationError


class BaseActionTestCase(TestCase):
    def test_it_handles_error(self):
        params_wrapper = stub(dict={}, list=[])
        request = stub(page=stub(), user=stub())

        class MyAction(WikiBaseAction):
            def json_for_formatter_view(self, page, user, params):
                self.error_happened('an error')

        action_instance = MyAction(params_wrapper, request)

        self.assertRaises(
            InputValidationError,
            lambda: action_instance.render(),
        )


class BaseActionValidationTestCase(TestCase):
    def setUp(self):
        self.request = stub(page=stub(), user=stub())

    def _fake_params_wrapper(self, dict=None, list=None):
        return stub(dict=dict or None, list=list or None)

    def _action_class(self, validation_form):
        class MyAction(WikiBaseAction):
            def form_class_to_validate_params(self):
                return validation_form

            def json_for_formatter_view(self, page, user, params):
                return {'success': params}

        return MyAction

    def test_it_validates_params(self):
        params_wrapper = self._fake_params_wrapper({'param': '1'})

        class ValidationFormClass(forms.Form):
            param = forms.IntegerField()

        MyAction = self._action_class(ValidationFormClass)

        action_instance = MyAction(params_wrapper, self.request)
        self.assertEqual(action_instance.render(), {'success': {'param': 1}})

    def test_position_parameter_does_not_interfere(self):
        params_wrapper = self._fake_params_wrapper({'param': '1'}, [(None, 'some param')])

        class ValidationFormClass(forms.Form):
            param = forms.IntegerField()

        MyAction = self._action_class(ValidationFormClass)

        action_instance = MyAction(params_wrapper, self.request)
        self.assertEqual(action_instance.render(), {'success': {'param': 1, 'some param': ''}})

    def test_it_handles_validation_errors(self):
        class ValidationFormClass(forms.Form):
            param = forms.IntegerField()

        MyAction = self._action_class(ValidationFormClass)

        params_wrapper = self._fake_params_wrapper({'param': 'something'}, [(None, 'some param')])
        action_instance = MyAction(params_wrapper, self.request)
        self.assertRaises(InputValidationError, lambda: action_instance.render())
        activate('en')
        try:
            action_instance.render()
        except InputValidationError as exc:
            self.assertEqual(str(exc), 'param: Enter a whole number.')

    def test_default_params_work(self):
        params_wrapper = self._fake_params_wrapper()

        class ValidationFormClass(forms.Form):
            param = forms.IntegerField(required=False)

        class MyAction(WikiBaseAction):
            def form_class_to_validate_params(self):
                return ValidationFormClass

            def json_for_formatter_view(self, page, user, params):
                return {'success': params['param']}

            @property
            def default_params(self):
                return {'param': 1}

        action_instance = MyAction(params_wrapper, self.request)
        self.assertEqual(action_instance.render(), {'success': 1})

    def test_params_are_not_masked_by_validation(self):
        params_wrapper = self._fake_params_wrapper({'param': '1', 'param2': '2'})

        class ValidationFormClass(forms.Form):
            param = forms.IntegerField()

        MyAction = self._action_class(ValidationFormClass)

        action_instance = MyAction(params_wrapper, self.request)
        self.assertEqual(action_instance.render(), {'success': {'param': 1, 'param2': '2'}})


class BaseWidgetActionTestCase(TestCase):
    """
    Экшены сделанные для новой верстки должны быть интегрированы со старой версткой.
    Для того, чтобы всю логику и для старой верстки и для новой держать.
    """

    def setUp(self):
        self.params_wrapper = stub()
        self.request = stub(page=stub(), user=stub())

    def test_instantiated_freely(self):

        WikiWidgetAction(self.params_wrapper, self.request)

    def test_inheritors_instantiated_freely(self):
        class Action(WikiWidgetAction):
            pass

        Action(self.params_wrapper, self.request)

    def test_it_generates_data_for_formatter(self):
        if not settings.IS_INTRANET:
            return

        class Myaction(WikiWidgetAction):
            pass

        page = stub(tag='tag-gat')
        action = Myaction(params=stub(dict={'param1': 'value1'}, list=None), request=stub(page=page, user=stub()))
        self.assertEqual(
            action.render(),
            {
                'action_name': 'myaction',
                'params': {'param1': 'value1'},
                # TODO: WIKI-8571 исправит урл на /_api/frontend/tag-gat/.actions?param1=value1
                'url_with_data': '/_api/frontend/tag-gat/.actions_view?action_name=myaction&param1=value1',
            },
        )

    def test_it_converts_positional_params(self):
        wrapper = ParamsWrapper(
            dict={'param1': 'spider man'},
            list=[
                ('param1', 'spider man'),
                (None, 'batman'),
            ],
        )
        action = WikiWidgetAction(params=None, request=self.request)

        self.assertEqual(
            action.encode_params(wrapper),
            {
                'param1': 'spider man',
                'batman': '',
            },
        )

    def test_it_passes_params_to_front(self):
        if not settings.IS_INTRANET:
            return

        class Myaction(WikiWidgetAction):
            action_params_to_frontend = ['param1']

        page = stub(tag='tag-gat')
        action = Myaction(
            params=stub(dict={'param1': 'value1', 'param2': 'value2'}, list=None), request=stub(page=page, user=stub())
        )
        self.assertIn('param1', action.render()['params'])
        self.assertEqual(action.render()['params']['param1'], 'value1')


class BooleanFromTextFieldTestCase(TestCase):
    def check(self, input, expected_value):
        from wiki.actions.classes.params_validator import BooleanFromTextField

        class Validator(forms.Form):
            field = BooleanFromTextField(required=False)

        form = Validator({'field': input})

        self.assertTrue(form.is_valid(), msg='fails on "{0}", {1}'.format(input, form.errors))

        self.assertEqual(expected_value, form.cleaned_data['field'])

    def test_it_goes_ok(self):
        falses = ['0', 'false', 'False', 'none', False]
        for value in falses:
            self.check(value, False)

        truths = ['1', 'True', 'true', True]
        for value in truths:
            self.check(value, True)
