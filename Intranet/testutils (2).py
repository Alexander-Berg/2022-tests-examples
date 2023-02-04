# -*- coding: utf-8 -*-
from django.utils.encoding import force_str
from events.data_sources.models import DataSourceItem


def get_or_create_data_source_item_for_question_choice(choice, identity=None):
    if not identity:
        identity = force_str(choice.id)
    return DataSourceItem.objects.get_and_update(
        data_source='survey_question_choice',
        identity=identity,
        text=choice.get_label(),
    )


def get_or_create_data_source_item_for_question_matrix(row, column, identity=None):
    if not identity:
        identity = '%s_%s' % (force_str(row.id), force_str(column.id))
    return DataSourceItem.objects.get_and_update(
        data_source='survey_question_matrix_choice',
        identity=identity,
        text='"%s": %s' % (row.get_label(), column.get_label()),
    )
