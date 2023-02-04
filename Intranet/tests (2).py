from django.test import TestCase

from fb.quizz.models import Quizz, QuizzGroup, QuizzQuestion, QuizzQuestionAnswer
from fb.quizz.serializers import QuizzSerializer


class QuizzTest(TestCase):

    def test_create_not_published_quizz(self):
        # Create invalid but not published quizz
        data = {
            'groups': [
                {
                    'group_id': 101,
                    'include_subdepartments': False,
                }
            ],
            'questions': [
            ],
            'is_published': False,
        }
        serializer = QuizzSerializer(data=data)
        serializer.is_valid()
        serializer.object.owner_id = 12636
        serializer.save()
        self.assertEqual(QuizzGroup.objects.count(), 1)
        self.assertEqual(Quizz.objects.count(), 1)

    def test_create_published_not_valid_quizz(self):
        data = {
            'groups': [
                {
                    'group_id': 101,
                    'include_subdepartments': False,
                }
            ],
            'questions': [
            ],
            'is_published': True,
        }
        serializer = QuizzSerializer(data=data)
        self.assertEqual(serializer.is_valid(), False)

    def test_create_published_valid_quizz(self):
        data = {
            'groups': [
                {
                    'group_id': 101,
                    'include_subdepartments': False,
                }
            ],
            'questions': [
                {
                    'question_type': QuizzQuestion.QT_SINGLE_CHOICE,
                    'question_for': 0,
                    'text': 'test',
                    'description': 'test',
                    'position': 1,
                    'answers': [{"text": "test"}]
                }
            ],
            'is_published': True,
        }
        serializer = QuizzSerializer(data=data)
        serializer.is_valid()
        serializer.object.owner_id = 12636
        serializer.save()
        self.assertEqual(QuizzGroup.objects.count(), 1)
        self.assertEqual(Quizz.objects.count(), 1)
        self.assertEqual(QuizzQuestion.objects.count(), 1)
        self.assertEqual(QuizzQuestionAnswer.objects.count(), 1)
        QuizzQuestionAnswer.objects.create(question=QuizzQuestion.objects.first(), text="Create")
        self.assertEqual(QuizzQuestionAnswer.objects.count(), 2)

    def test_competention_question(self):
        data = {
            'groups': [
                {
                    'group_id': 101,
                    'include_subdepartments': False,
                }
            ],
            'questions': [
                {
                    'question_type': QuizzQuestion.QT_COMPETENCE,
                    'question_for': 0,
                    'text': 'test',
                    'description': 'test',
                    'position': 1,
                }
            ],
            'is_published': True,
        }
        serializer = QuizzSerializer(data=data)
        serializer.is_valid()
        serializer.object.owner_id = 12636
        serializer.save()
        self.assertEqual(QuizzQuestionAnswer.objects.count(), 5)
        QuizzQuestionAnswer.objects.create(question=QuizzQuestion.objects.first(), text="Not create")
        self.assertEqual(QuizzQuestionAnswer.objects.count(), 5)
        # Append open answer
        QuizzQuestionAnswer.objects.create(question=QuizzQuestion.objects.first(), text="Create", is_open=True)
        self.assertEqual(QuizzQuestionAnswer.objects.count(), 6)
        QuizzQuestionAnswer.objects.create(question=QuizzQuestion.objects.first(), text="Not create", is_open=True)
        self.assertEqual(QuizzQuestionAnswer.objects.count(), 6)
