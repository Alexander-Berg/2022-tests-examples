from uuid import uuid4

from django.db import models


def build_model_class_name():
    return 'TempModel' + uuid4().hex


def get_model_class(attrs=None, mixins=None, class_name=None):
    if class_name is None:
        class_name = build_model_class_name()
    if attrs is None:
        attrs = {}
    if mixins is None:
        mixins = ()
    attrs['__module__'] = '.'.join(__name__.split('.')[0:2])
    return type(class_name, (models.Model,) + mixins, attrs)
