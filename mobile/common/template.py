class InvalidTemplateVarException(Exception):
    pass


class InvalidTemplateString(str):
    """ Hack for django template engine to make it raise exception on unkhown variable
    See Django code here:
    https://github.com/django/django/blob/3dcc3516914f25b58bde9312831f1d94b05cdb53/django/template/base.py#L682

    string_if_invalid = context.template.engine.string_if_invalid
    if string_if_invalid:
        if '%s' in string_if_invalid: <- __contains__ checks for this condition
            return string_if_invalid % self.var <- raise InvalidTemplateVarException here
        else:
            return string_if_invalid
    else:
        obj = string_if_invalid
    """

    def __mod__(self, missing):
        raise InvalidTemplateVarException('Unknown template variable %r' % missing)

    def __contains__(self, search):
        return search == '%s'
