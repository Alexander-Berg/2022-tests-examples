from django.core.exceptions import FieldDoesNotExist  # noqa

from wiki.arc_compat import is_arc


def model_has_the_field(model_class, field_name):
    try:
        model_class._meta.get_field(field_name)  # noqa
        return True
    except FieldDoesNotExist:
        return False


if is_arc():
    from model_mommy import mommy

    get = mommy.make
    new = mommy.prepare
else:
    from ddf import G, N  # noqa

    get = G
    new = N
