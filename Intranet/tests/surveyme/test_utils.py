# -*- coding: utf-8 -*-
from django.test import TestCase

from events.surveyme.factories import SurveyFactory
from events.surveyme.models import SurveyTicket, Survey


class Test__check_survey_tickets_count_consistency(TestCase):
    def setUp(self):
        SurveyTicket.objects.all().delete()
        self.survey = SurveyFactory(maximum_answers_count=4)
        self.survey.save()

    def check_survey_with_4_free_tickets_exists(self):
        msg = 'При создании формы должны были создаться 4 свободных билета'
        self.assertEqual(SurveyTicket.objects.count(), 4, msg=msg)
        self.assertEqual(set(SurveyTicket.objects.values_list('acquired', flat=True)), set([False]), msg=msg)

    def test_must_create_free_tickets_after_form_creating(self):
        SurveyTicket.objects.all().delete()
        Survey.objects.all().delete()
        self.survey = SurveyFactory(maximum_answers_count=4)
        self.survey.save()

        self.check_survey_with_4_free_tickets_exists()

    def test_must_remove_all_if_new_maximum_answers_lte_than_acquired_tickets_and_acquired_tickets_not_exists(self):
        self.check_survey_with_4_free_tickets_exists()

        msg = 'Все свободные билеты должны были удалиться, т.к. установлено максимальное количество билетов: 0'
        self.survey.maximum_answers_count = 0
        self.survey.save()
        msg = ''
        self.assertEqual(SurveyTicket.objects.count(), 0, msg=msg)

    def test_must_remove_all_free_tickets_if_new_maximum_answers_is_none(self):
        msg = 'Все свободные билеты должны были удалиться, т.к. установлено неограниченное количество билетов'
        self.survey.maximum_answers_count = None
        self.survey.save()
        msg = ''
        self.assertEqual(SurveyTicket.objects.count(), 0, msg=msg)

    def test_must_remove_free_tickets_if_new_maximum_answers_lte_than_acquired_tickets(self):
        self.check_survey_with_4_free_tickets_exists()

        ids = list(SurveyTicket.objects.all().values_list('pk', flat=True)[:2])
        SurveyTicket.objects.filter(pk__in=ids).update(acquired=True)

        self.survey.maximum_answers_count = 1
        self.survey.save()
        msg = (
            'Если максимальное количество ответов меньше уже заблокированных '
            '- их нужно оставить, а все свободные удалить'
        )
        self.assertEqual(SurveyTicket.objects.count(), 2, msg=msg)
        self.assertEqual(SurveyTicket.objects.filter(acquired=True).count(), 2, msg=msg)

    def test_must_remove_free_tickets_to_new_maximum_answers(self):
        self.check_survey_with_4_free_tickets_exists()

        ids = list(SurveyTicket.objects.all().values_list('pk', flat=True)[:1])
        SurveyTicket.objects.filter(pk__in=ids).update(acquired=True)

        self.survey.maximum_answers_count = 2
        self.survey.save()
        msg = (
            'Если максимальное количество ответов больше уже заблокированных '
            '- нужно оставить их + оставшиеся свободные билеты, а все остальные удалить'
        )
        self.assertEqual(SurveyTicket.objects.count(), 2, msg=msg)
        self.assertEqual(SurveyTicket.objects.filter(acquired=True).count(), 1, msg=msg)
        self.assertEqual(SurveyTicket.objects.filter(acquired=False).count(), 1, msg=msg)
