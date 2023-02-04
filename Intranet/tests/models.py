from django.contrib.postgres.fields import JSONField
from django.db import models
from model_utils.models import TimeStampedModel

from intranet.femida.src.wf.models import WFModelMixin


class Table(TimeStampedModel):

    char_field = models.CharField(max_length=32, default='')
    int_field = models.IntegerField(default=0)
    bool_field = models.BooleanField(default=False)
    json_field = JSONField(default=dict)


class WFTable(WFModelMixin, TimeStampedModel):

    WIKI_FIELDS_MAP = {
        'wf_field': 'formatted_wf_field',
    }

    wf_field = models.TextField()
    formatted_wf_field = models.TextField()
