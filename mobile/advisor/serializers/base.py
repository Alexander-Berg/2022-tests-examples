from django.core.validators import MaxLengthValidator
from rest_framework.exceptions import ValidationError
from rest_framework.serializers import Serializer, Field


class MaxElementsValidator(MaxLengthValidator):
    code = 'max_elements'
    message = 'Ensure this list has at most %(limit_value)d items (it has %(show_value)d).'


class BaseSerializer(Serializer):
    OMIT_FIELDS_IF_EMPTY = None

    def to_representation(self, instance):
        res = super(BaseSerializer, self).to_representation(instance)
        if self.OMIT_FIELDS_IF_EMPTY is None:
            return res

        def del_if_empty_from(result):
            if key in result and not result[key]:
                del result[key]

        for key in self.OMIT_FIELDS_IF_EMPTY:
            del_if_empty_from(res)

        return res


class MultipleParametersField(Field):
    """
    Multiple parameters in one GET field like:
    block=format:Single_app;place:tab1
    """

    def to_internal_value(self, data):
        try:
            option_tuples = (option.split(':') for option in data.split(';'))
            return {k: v for k, v in option_tuples}
        except ValueError:
            raise ValidationError("Incorrect multiple parameters string")

    def to_representation(self, obj):
        return u';'.join(u'%s:%s' % item for item in obj.items())


class CommaSeparatedListField(Field):
    default_error_messages = {
        'invalid_choice': '"{input}" is not a valid choice.'
    }

    def __init__(self, *args, **kwargs):
        self.choices = kwargs.pop('choices', None)
        max_count = kwargs.pop('max_count', None)
        super(CommaSeparatedListField, self).__init__(*args, **kwargs)
        if max_count is not None:
            self.validators.append(MaxElementsValidator(max_count))

    def to_representation(self, obj):
        return u','.join(obj)

    def to_internal_value(self, data):
        separated_list = filter(bool, data.split(','))
        if self.choices is not None:
            for item in separated_list:
                if item not in self.choices:
                    self.fail('invalid_choice', input=item)
        return separated_list
