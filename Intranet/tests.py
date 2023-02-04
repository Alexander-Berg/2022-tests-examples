from django.core import mail
from django.test import TestCase
from fb.feedback.models import Feedback, FeedbackGroup, FeedbackRequest
from fb.feedback.tasks import _send_notification
from fb.staff.tasks import _load_persons_from_file, _load_group_membership_from_file, _load_groups_from_file
from fb.feedback.views import DepartmentFeedbackPersonsList
from django.http import HttpRequest

from fb.staff.models import Person
from fb.quizz.models import Quizz, QuizzQuestion, QuizzQuestionAnswer, PersonAnswer


class TestSendMessage(TestCase):
    def setUp(self):
        _load_persons_from_file()
        _load_groups_from_file()
        _load_group_membership_from_file()

    def test_send_notification(self):
        """
        Tests that 1 + 1 always equals 2.
        """
        f = Feedback(
            reporter_id=2,
            reporter_type='head',
            positive_message='Pluses',
            negative_message='Minuses',
        )
        _send_notification(10, [20, 30, 40], f)
        self.assertEqual(len(mail.outbox), 1)


class TestMySubordinatesStats(TestCase):
    def setUp(self):
        _load_persons_from_file()
        _load_groups_from_file()
        _load_group_membership_from_file()

        # Generate feedbacks
        yaroman = Person.objects.get(login='yaroman')
        yaskevich = Person.objects.get(login='yaskevich')
        for mood in [Feedback.FM_NEGATIVE, Feedback.FM_NEUTRAL, Feedback.FM_POSITIVE]:
            feedback = Feedback.objects.create(
                reporter=yaroman,
                reporter_type=Feedback.RT_SUBORDINATE,
                positive_message='Positive message',
                negative_message='Negative message',
                mood=mood,
            )
            FeedbackGroup.objects.create(person=yaskevich, feedback=feedback)

        # Generate feedback requests
        for i in xrange(5):
            request = FeedbackRequest.objects.create(reporter=yaskevich, suggest_to=yaskevich)
            request.suggested_persons.add(yaroman)
        for i in xrange(2):
            request = FeedbackRequest.objects.create(reporter=yaskevich, suggest_to=yaskevich, is_submitted=True)
            request.suggested_persons.add(yaroman)

        # Generate competention questions
        quizz = Quizz.objects.create(owner=yaroman)
        quizz_question = QuizzQuestion.objects.create(
            text="Competention 1",
            question_type=QuizzQuestion.QT_COMPETENCE,
            quizz=quizz,
            position=1,
        )
        QuizzQuestionAnswer.objects.create(question=quizz_question, text="Open", is_open=True)
        QuizzQuestion.objects.create(
            text="Competention 2",
            question_type=QuizzQuestion.QT_COMPETENCE,
            quizz=quizz,
            position=2,
        )

    def test_feedback_moods_count(self):
        view = DepartmentFeedbackPersonsList()

        queryset = Feedback.objects.all()
        moods = view.get_feedback_moods(queryset)
        self.assertEqual(moods['positive_count'], 1)
        self.assertEqual(moods['neutral_count'], 1)
        self.assertEqual(moods['negative_count'], 1)

        queryset = Feedback.objects.filter(mood=Feedback.FM_NEGATIVE)
        moods = view.get_feedback_moods(queryset)
        self.assertEqual(moods['positive_count'], 0)
        self.assertEqual(moods['neutral_count'], 0)
        self.assertEqual(moods['negative_count'], 1)

        queryset = Feedback.objects.filter(mood=Feedback.FM_POSITIVE)
        moods = view.get_feedback_moods(queryset)
        self.assertEqual(moods['positive_count'], 1)
        self.assertEqual(moods['neutral_count'], 0)
        self.assertEqual(moods['negative_count'], 0)

        queryset = Feedback.objects.filter(mood=Feedback.FM_NEUTRAL)
        moods = view.get_feedback_moods(queryset)
        self.assertEqual(moods['positive_count'], 0)
        self.assertEqual(moods['neutral_count'], 1)
        self.assertEqual(moods['negative_count'], 0)

    def test_feedback_requests_count(self):
        view = DepartmentFeedbackPersonsList()
        view.request = HttpRequest()
        yaroman = Person.objects.get(login='yaroman')
        yaskevich = Person.objects.get(login='yaskevich')

        requests = view.get_requests_statuses(yaroman)
        self.assertEqual(requests['count'], 7)
        self.assertEqual(requests['applied_count'], 2)
        requests = view.get_requests_statuses(yaskevich)
        self.assertEqual(requests['count'], 0)
        self.assertEqual(requests['applied_count'], 0)

    def test_competences_average_values(self):
        view = DepartmentFeedbackPersonsList()

        yaroman = Person.objects.get(login='yaroman')
        yaskevich = Person.objects.get(login='yaskevich')

        any_feedback = Feedback.objects.first()
        first_question = QuizzQuestion.objects.all()[0]
        second_question = QuizzQuestion.objects.all()[1]

        feedbacks = Feedback.objects.all()
        competences = view.get_competences_average_values(yaroman, feedbacks)
        self.assertEqual(competences, [])

        PersonAnswer.objects.create(
            feedback=any_feedback,
            person=yaskevich,
            on_whom=yaroman,
            quizz_answer=first_question.quizzquestionanswer_set.get(text="5")
        )
        competences = view.get_competences_average_values(yaroman, feedbacks)
        self.assertEqual(competences[0]['average_value'], 5)

        PersonAnswer.objects.create(
            feedback=any_feedback,
            person=yaskevich,
            on_whom=yaroman,
            quizz_answer=first_question.quizzquestionanswer_set.get(text="4")
        )
        competences = view.get_competences_average_values(yaroman, feedbacks)
        self.assertEqual(competences[0]['average_value'], 4.5)

        PersonAnswer.objects.create(
            feedback=any_feedback,
            person=yaskevich,
            on_whom=yaroman,
            quizz_answer=first_question.quizzquestionanswer_set.get(is_open=True)
        )
        competences = view.get_competences_average_values(yaroman, feedbacks)
        self.assertEqual(competences[0]['average_value'], 4.5)

        PersonAnswer.objects.create(
            feedback=any_feedback,
            person=yaskevich,
            on_whom=yaroman,
            quizz_answer=second_question.quizzquestionanswer_set.get(text="5")
        )
        competences = view.get_competences_average_values(yaroman, feedbacks)
        self.assertEqual(competences[0]['average_value'], 4.5)
        self.assertEqual(competences[1]['average_value'], 5)

        PersonAnswer.objects.create(
            feedback=any_feedback,
            person=yaroman,
            on_whom=yaskevich,
            quizz_answer=first_question.quizzquestionanswer_set.get(text="5")
        )
        competences = view.get_competences_average_values(yaroman, feedbacks)
        self.assertEqual(competences[0]['average_value'], 4.5)
        self.assertEqual(competences[1]['average_value'], 5)
        competences = view.get_competences_average_values(yaskevich, feedbacks)
        self.assertEqual(competences[0]['average_value'], 5)

        competences = view.get_competences_average_values(yaroman, feedbacks.exclude(pk=any_feedback.pk))
        self.assertEqual(competences, [])
