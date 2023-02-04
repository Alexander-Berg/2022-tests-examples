from builtins import object, str

import pytest
from mock import patch

from kelvin.courses.models import (
    CourseLessonEdge, CourseLessonGraph, CourseLessonGraphFactory, CourseStudent, UserCLessonState,
)
from kelvin.courses.models.criterion.action import CLessonCompletionAction

from .lib import (
    create_assignment_rule_for_course, create_clesson, create_clesson_criterion, create_lesson, create_random_course,
    create_random_user,
)


class TestCourseLessonGraphBase(object):
    def init(self):
        # init
        self.course = create_random_course()
        self.assignment_rule = create_assignment_rule_for_course(self.course)

        self.lesson1 = create_lesson()
        self.lesson2 = create_lesson()
        self.lesson3 = create_lesson()
        self.lesson4 = create_lesson()

        self.lesson5 = create_lesson()
        self.lesson6 = create_lesson()

        self.clesson1 = create_clesson(self.course, self.lesson1)
        self.clesson2 = create_clesson(self.course, self.lesson2)
        self.clesson3 = create_clesson(self.course, self.lesson3)
        self.clesson4 = create_clesson(self.course, self.lesson4)

        self.clesson5 = create_clesson(self.course, self.lesson5)
        self.clesson6 = create_clesson(self.course, self.lesson6)

        self.criterion1 = create_clesson_criterion(self.clesson1, self.assignment_rule)
        self.criterion2 = create_clesson_criterion(self.clesson1, self.assignment_rule)
        self.criterion3 = create_clesson_criterion(self.clesson2, self.assignment_rule)
        self.criterion4 = create_clesson_criterion(self.clesson3, self.assignment_rule)

        self.criterion5 = create_clesson_criterion(self.clesson4, self.assignment_rule)
        self.criterion6 = create_clesson_criterion(self.clesson4, self.assignment_rule)

        """
          c1
         /  \
        c2  c3
         \  /
          c4
        """
        self.edge1 = CourseLessonEdge.objects.create(
            parent_clesson=self.clesson1,
            child_clesson=self.clesson2,
            criterion=self.criterion1,
            assignment_rule=self.assignment_rule,
        )
        self.edge2 = CourseLessonEdge.objects.create(
            parent_clesson=self.clesson1,
            child_clesson=self.clesson3,
            criterion=self.criterion2,
            assignment_rule=self.assignment_rule,
        )
        self.edge3 = CourseLessonEdge.objects.create(
            parent_clesson=self.clesson2,
            child_clesson=self.clesson4,
            criterion=self.criterion3,
            assignment_rule=self.assignment_rule,
        )
        self.edge4 = CourseLessonEdge.objects.create(
            parent_clesson=self.clesson3,
            child_clesson=self.clesson4,
            criterion=self.criterion4,
            assignment_rule=self.assignment_rule,
        )
        self.edges = [
            self.edge1.get_tuple(),
            self.edge2.get_tuple(),
            self.edge3.get_tuple(),
            self.edge4.get_tuple(),
        ]


class TestCourseLessonGraphFactory(TestCourseLessonGraphBase):
    @pytest.mark.django_db
    def test_serialize_clesson_graph(self):
        self.init()

        graph = CourseLessonGraph(assignment_rule_id=self.assignment_rule.id, edges=self.edges)

        # проверяем, что изначально 4 вершины в графе и 2 - нет
        assert (graph.has_node(self.clesson1.id) is True)
        assert (graph.has_node(self.clesson2.id) is True)
        assert (graph.has_node(self.clesson3.id) is True)
        assert (graph.has_node(self.clesson4.id) is True)
        assert (graph.has_node(self.clesson5.id) is False)
        assert (graph.has_node(self.clesson6.id) is False)

        # проверяем, что в графе появилось еще две вершины и старые никуда не делись
        graph.add_edge(self.clesson4.id, self.clesson5.id, self.criterion5.id)
        graph.add_edge(self.clesson4.id, self.clesson6.id, self.criterion6.id)
        assert (graph.has_node(self.clesson1.id) is True)
        assert (graph.has_node(self.clesson2.id) is True)
        assert (graph.has_node(self.clesson3.id) is True)
        assert (graph.has_node(self.clesson4.id) is True)
        assert (graph.has_node(self.clesson5.id) is True)
        assert (graph.has_node(self.clesson6.id) is True)

        # проверяем, что после сериализации графа в БД появились недавно добавленные в памяти рёбра
        """
          c1
         /  \
        c2  c3
         \  /
          c4
         /  \
        c5  c6
        """
        CourseLessonGraphFactory.serialize(graph)
        self.edge5 = CourseLessonEdge.objects.filter(
            assignment_rule=self.assignment_rule,
            parent_clesson=self.clesson4,
            child_clesson=self.clesson5,
        ).first()
        self.edge6 = CourseLessonEdge.objects.filter(
            assignment_rule=self.assignment_rule,
            parent_clesson=self.clesson4,
            child_clesson=self.clesson5,
        ).first()
        assert (self.edge5 is not None)
        assert (self.edge6 is not None)

        # проверяем десериализацию - выгружаем ранее сохраненный граф из базы и проверяем, что в нем те же вершины
        deser_graph = CourseLessonGraphFactory.deserialize(assignment_rule_id=self.assignment_rule.id)
        assert (deser_graph.get_graph_id() == self.assignment_rule.id)
        assert (deser_graph.has_node(self.clesson1.id) is True)
        assert (deser_graph.has_node(self.clesson2.id) is True)
        assert (deser_graph.has_node(self.clesson3.id) is True)
        assert (deser_graph.has_node(self.clesson4.id) is True)
        assert (deser_graph.has_node(self.clesson5.id) is True)
        assert (deser_graph.has_node(self.clesson6.id) is True)

        # проверяем возможность узнать о доступности модуля
        # по-умолчанию ноды создаются доступными
        assert deser_graph.get_clesson_available(self.clesson1.id) is True
        assert deser_graph.get_clesson_available(self.clesson2.id) is True
        assert deser_graph.get_clesson_available(self.clesson3.id) is True
        assert deser_graph.get_clesson_available(self.clesson4.id) is True
        assert deser_graph.get_clesson_available(self.clesson5.id) is True
        assert deser_graph.get_clesson_available(self.clesson6.id) is True

        # помечаем часть нод недоступными и проверяем заново
        deser_graph.set_clesson_available(self.clesson2.id, False)
        deser_graph.set_clesson_available(self.clesson4.id, False)
        assert deser_graph.get_clesson_available(self.clesson1.id) is True
        assert deser_graph.get_clesson_available(self.clesson2.id) is False
        assert deser_graph.get_clesson_available(self.clesson3.id) is True
        assert deser_graph.get_clesson_available(self.clesson4.id) is False
        assert deser_graph.get_clesson_available(self.clesson5.id) is True
        assert deser_graph.get_clesson_available(self.clesson6.id) is True

        # сериализуем - десериализуем и проверяем что не просыпали факт доступности/недоступности
        CourseLessonGraphFactory.serialize(deser_graph)
        deser_graph = CourseLessonGraphFactory.deserialize(assignment_rule_id=self.assignment_rule.id)
        assert deser_graph.get_clesson_available(self.clesson1.id) is True
        assert deser_graph.get_clesson_available(self.clesson2.id) is False
        assert deser_graph.get_clesson_available(self.clesson3.id) is True
        assert deser_graph.get_clesson_available(self.clesson4.id) is False
        assert deser_graph.get_clesson_available(self.clesson5.id) is True
        assert deser_graph.get_clesson_available(self.clesson6.id) is True

        # проверяем возможность получения дочерних модулей
        assert set(deser_graph.get_sub_clessons(self.clesson1.id)) == set([self.clesson2.id, self.clesson3.id])
        assert set(deser_graph.get_sub_clessons(self.clesson2.id)) == set([self.clesson4.id])
        assert set(deser_graph.get_sub_clessons(self.clesson3.id)) == set([self.clesson4.id])
        assert set(deser_graph.get_sub_clessons(self.clesson4.id)) == set([self.clesson5.id, self.clesson6.id])
        assert set(deser_graph.get_sub_clessons(self.clesson5.id)) == set([])
        assert set(deser_graph.get_sub_clessons(self.clesson6.id)) == set([])

        # проверяем возможность получения родительских модулей
        assert set(deser_graph.get_parent_clessons(self.clesson1.id)) == set([])
        assert set(deser_graph.get_parent_clessons(self.clesson2.id)) == set([self.clesson1.id])
        assert set(deser_graph.get_parent_clessons(self.clesson3.id)) == set([self.clesson1.id])
        assert set(deser_graph.get_parent_clessons(self.clesson4.id)) == set([self.clesson2.id, self.clesson3.id])
        assert set(deser_graph.get_parent_clessons(self.clesson5.id)) == set([self.clesson4.id])
        assert set(deser_graph.get_parent_clessons(self.clesson6.id)) == set([self.clesson4.id])


class TestCourseLessonAction(TestCourseLessonGraphBase):
    @pytest.mark.django_db
    def test_open_submodules(self):
        self.init()
        # проверяем открытие модуля Action-ом
        user = create_random_user()
        graph = CourseLessonGraph(
            assignment_rule_id=self.assignment_rule.id,
            edges=self.edges,
            nodes=[
                (self.clesson1.id, True),
                (self.clesson2.id, False),
                (self.clesson3.id, False),
                (self.clesson4.id, False),
                (self.clesson5.id, False),
                (self.clesson6.id, False),
            ]
        )
        graph.add_edge(self.clesson4.id, self.clesson5.id, self.criterion5.id)
        graph.add_edge(self.clesson4.id, self.clesson6.id, self.criterion6.id)

        CourseLessonGraphFactory.serialize(graph)

        user_graph = CourseLessonGraphFactory.actualize_user_graph(
            user_id=user.id,
            assignment_rule_id=graph.get_graph_id()
        )

        action = CLessonCompletionAction()
        action.do(user=user, criterion=self.criterion1)
        state2 = UserCLessonState.objects.get(user=user, clesson=self.clesson2)
        state3 = UserCLessonState.objects.get(user=user, clesson=self.clesson3)
        assert state2.available is True
        assert state3.available is False

        action.do(user=user, criterion=self.criterion2)
        state2 = UserCLessonState.objects.get(user=user, clesson=self.clesson2)
        state3 = UserCLessonState.objects.get(user=user, clesson=self.clesson3)
        assert state2.available is True
        assert state3.available is True

        # проверяем попытку открыть модули (5,6), дочерние от еще закрытого модуля (4)
        # для чистоты эксперимента явно помечаем вершины 4,5,6 закрытыми
        user_graph.set_clesson_available(self.clesson4.id, False)
        user_graph.set_clesson_available(self.clesson5.id, False)
        user_graph.set_clesson_available(self.clesson6.id, False)
        CourseLessonGraphFactory.serialize_for_user(user_graph)
        action.do(user=user, criterion=self.criterion5)
        state5 = UserCLessonState.objects.get(user=user, clesson=self.clesson5)
        state6 = UserCLessonState.objects.get(user=user, clesson=self.clesson6)
        assert state5.available is False
        assert state6.available is False

        # проверяем, что и по второму критерию из закрытой вершины 4 нельзя открыть 5 и 6
        action.do(user=user, criterion=self.criterion6)
        state5 = UserCLessonState.objects.get(user=user, clesson=self.clesson5)
        state6 = UserCLessonState.objects.get(user=user, clesson=self.clesson6)
        assert state5.available is False
        assert state6.available is False

        # проверяем попытку открыть модуль (4), у которого один родитель открыт (2), а другой закрыт (3)
        user_graph.set_clesson_available(clesson_id=self.clesson2.id, available=False)
        user_graph.set_clesson_available(clesson_id=self.clesson3.id, available=True)
        CourseLessonGraphFactory.serialize_for_user(user_graph)
        action.do(user=user, criterion=self.criterion3)
        state4 = UserCLessonState.objects.get(user=user, clesson=self.clesson4)
        assert state4.available is False

        # проверяем попытку открыть модуль (4), у которого один родитель открыт (3), а другой закрыт (2)
        user_graph.set_clesson_available(clesson_id=self.clesson2.id, available=True)
        user_graph.set_clesson_available(clesson_id=self.clesson3.id, available=False)
        CourseLessonGraphFactory.serialize_for_user(user_graph)
        action.do(user=user, criterion=self.criterion3)
        state4 = UserCLessonState.objects.get(user=user, clesson=self.clesson4)
        assert state4.available is False

        # проверяем попытку открыть модуль (4), у которого оба родителя открыты
        user_graph.set_clesson_available(clesson_id=self.clesson2.id, available=True)
        user_graph.set_clesson_available(clesson_id=self.clesson3.id, available=True)
        CourseLessonGraphFactory.serialize_for_user(user_graph)
        action.do(user=user, criterion=self.criterion3)
        state4 = UserCLessonState.objects.get(user=user, clesson=self.clesson4)
        assert state4.available is True


class TestCourseLessonEdge(TestCourseLessonGraphBase):
    @pytest.mark.django_db
    def test_edge_get_tuple(self):
        self.init()
        """ проверяем корректность tuple-представления ребра """
        edge = CourseLessonEdge.objects.create(
            parent_clesson=self.clesson1,
            child_clesson=self.clesson2,
            assignment_rule=self.assignment_rule,
            criterion=self.criterion1
        )

        assert edge.get_tuple() == (edge.parent_clesson_id, edge.child_clesson_id, edge.criterion_id)

    @pytest.mark.django_db
    def test_edge_unicode_repr(self):
        self.init()
        """ проверяем корректность строкового представления ребра """
        edge = CourseLessonEdge.objects.create(
            parent_clesson=self.clesson1,
            child_clesson=self.clesson2,
            assignment_rule=self.assignment_rule,
            criterion=self.criterion1
        )

        assert str(edge) == u"{} -> {} ({})".format(
            edge.parent_clesson_id,
            edge.child_clesson_id,
            edge.criterion
        )


class TestCourseLessonGraphInit(TestCourseLessonGraphBase):
    @pytest.mark.django_db
    def test_get_graph_id(self):
        """ проверяем, что в идентификатор графа мы кладем именно идентификатор курса """
        # init
        self.init()

        # action
        g = CourseLessonGraph(
            assignment_rule_id=self.assignment_rule.id,
        )

        # test
        assert g.get_graph_id() == self.assignment_rule.id

        # deinit

    @pytest.mark.django_db
    def test_empty_graph_init(self):
        """ проверяем создание пустого графа """
        # init
        self.init()
        # action
        g = CourseLessonGraph(
            assignment_rule_id=self.assignment_rule.id,
            edges=[],
            nodes=[]
        )
        # test
        assert dict(g.graph) == {}
        # deinit

    @pytest.mark.django_db
    def test_empty_edges_init(self):
        """ проверяем создание графа без рёбер но с изолированными вершинами """
        # init
        self.init()
        # action
        g = CourseLessonGraph(
            assignment_rule_id=self.assignment_rule.id,
            edges=[],
            nodes=[
                (self.clesson2.id, True),
                (self.clesson1.id, False)
            ]
        )
        # test
        assert dict(g.graph.edges) == {}
        assert dict(g.graph.nodes) == {
            self.clesson1.id: {'available': False},
            self.clesson2.id: {'available': True},
        }
        # deinit

    @pytest.mark.django_db
    def test_empty_nodes_init(self):
        """
        проверяем создание графа с непустыми рёбрами и без явного указания вершин.
        на выходе должны получить набор рёбер в соответствии со входными паметрами и
        набор вершин, которые были инициализированы из вершин переданных рёбер
        дефолтное состояние вершин при этом True
        """

        # init
        self.init()
        # action
        g = CourseLessonGraph(
            assignment_rule_id=self.assignment_rule.id,
            edges=[(self.clesson2.id, self.clesson1.id, self.criterion1.id), ],
            nodes=[]
        )
        # test
        assert dict(g.graph.edges) == {
            (self.clesson2.id, self.clesson1.id, 0): {'criterion': self.criterion1.id}
        }
        assert dict(g.graph.nodes) == {
            self.clesson1.id: {'available': True},
            self.clesson2.id: {'available': True},
        }
        # deinit

    @pytest.mark.django_db
    def test_default_available_init(self):
        """
        проверяем возможность указать дефолтное значение для доступности вершин графа.
        проверяем, что при создании вершин из рёбер графа дефолтное значение для состояния вершин
        берётся из параметра default_node_is_available
        """
        # init
        self.init()
        # action
        g = CourseLessonGraph(
            assignment_rule_id=self.assignment_rule.id,
            edges=[(self.clesson3.id, self.clesson2.id, self.criterion1.id), ],
            nodes=[(self.clesson1.id, True)],
            default_node_is_available=False
        )
        # test
        assert dict(g.graph.edges) == {
            (self.clesson3.id, self.clesson2.id, 0): {'criterion': self.criterion1.id},
        }
        assert dict(g.graph.nodes) == {
            self.clesson1.id: {'available': True},
            self.clesson2.id: {'available': False},
            self.clesson3.id: {'available': False},
        }
        # deinit

    @pytest.mark.django_db
    def test_add_edge(self):
        """ проверяем интерфейс добавления ребра """
        # init
        self.init()
        # action
        g = CourseLessonGraph(
            assignment_rule_id=self.assignment_rule.id,
            edges=[],
            nodes=[]
        )
        # test
        g.add_edge(self.clesson1.id, self.clesson5.id, self.criterion3.id)
        assert dict(g.graph.edges) == {
            (self.clesson1.id, self.clesson5.id, 0): {'criterion': self.criterion3.id},
        }
        assert dict(g.graph.nodes) == {
            self.clesson1.id: {'available': True},
            self.clesson5.id: {'available': True},
        }

        g.add_edge(self.clesson5.id, self.clesson2.id, self.criterion2.id)
        assert dict(g.graph.edges) == {
            (self.clesson1.id, self.clesson5.id, 0): {'criterion': self.criterion3.id},
            (self.clesson5.id, self.clesson2.id, 0): {'criterion': self.criterion2.id},
        }
        assert dict(g.graph.nodes) == {
            self.clesson1.id: {'available': True},
            self.clesson5.id: {'available': True},
            self.clesson2.id: {'available': True},
        }

        # deinit

    @pytest.mark.django_db
    def test_add_node_has_node(self):
        """ проверяем интерфейс добавления вершины """
        # init
        self.init()
        # action
        g = CourseLessonGraph(
            assignment_rule_id=self.assignment_rule.id,
            edges=[],
            nodes=[]
        )
        g.add_node(self.clesson1.id, True)

        # test
        assert dict(g.graph.nodes) == {
            self.clesson1.id: {'available': True},
        }
        assert g.has_node(self.clesson1.id) is True
        assert g.has_node(self.clesson2.id) is False

        # action
        g.add_node(self.clesson2.id, False)
        assert dict(g.graph.nodes) == {
            self.clesson1.id: {'available': True},
            self.clesson2.id: {'available': False},
        }
        # test
        assert g.has_node(self.clesson1.id) is True
        assert g.has_node(self.clesson2.id) is True

    @pytest.mark.django_db
    def test_get_set_clesson_available_unknown_node(self):
        """ проверяем интерфейс получения/редактирования достуности вершины """
        # init
        self.init()
        # action
        g = CourseLessonGraph(
            assignment_rule_id=self.assignment_rule.id,
            edges=[],
            nodes=[]
        )
        assert g.get_clesson_available(self.clesson3.id) is False

        g.set_clesson_available(self.clesson3.id, True)

        # проверяем, что при попытке сделать доступной неизвестную вершину, таковая не появляется в графе
        assert g.has_node(self.clesson3) is False


class TestUserGraph(TestCourseLessonGraphBase):
    @pytest.mark.django_db
    def test_auto_create_graph_upon_course_add(self, mocker):
        """ проверяем автоматическое создание графа пользователя при добавлении курса в "Мои" """
        # init
        self.init()
        student = create_random_user()

        from django.core.cache import caches
        from django.conf import settings
        user_course_cache = caches['user_course']
        user_course_cache.set(
            settings.USER_RULES_CACHE_KEY_TMPL.format(student.id),
            [(self.assignment_rule.id, self.course.id)]
        )

        # action
        cs = CourseStudent(
            course_id=self.course.id,
            student_id=student.id,
        )
        cs.save()

        # test
        state_items = UserCLessonState.objects.filter(
            user_id=student.id,
            clesson__course_id=self.course.id
        )
        g = CourseLessonGraphFactory.deserialize_for_user(
            user_id=student.id,
            assignment_rule_id=self.assignment_rule.id
        )

        assert state_items

        for item in state_items:
            assert g.has_node(item.clesson_id)
            assert g.get_clesson_available(item.clesson_id) == item.available

    @pytest.mark.django_db
    def test_auto_create_graph_upon_course_add_mandatory(self, mocker):
        """ проверяем автоматическое создание графа пользователя при добавлении обязательного курса в "Мои" """
        # init
        self.init()
        student = create_random_user()

        # action
        cs = CourseStudent(
            course_id=self.course.id,
            student_id=student.id,
            assignment_rule_id=self.assignment_rule.id,
        )
        cs.save()

        # test
        state_items = UserCLessonState.objects.filter(
            user_id=student.id,
            clesson__course_id=self.course.id,
        )
        g = CourseLessonGraphFactory.deserialize_for_user(
            user_id=student.id,
            assignment_rule_id=self.assignment_rule.id
        )

        assert state_items

        for item in state_items:
            assert g.has_node(item.clesson_id)
            assert g.get_clesson_available(item.clesson_id) == item.available

    @pytest.mark.django_db
    def test_auto_create_empty_graph_upon_course_add(self, mocker):
        """ проверяем, что все ок при добавлении курса, у которого нет графа модулей """
        # init
        self.init()
        course = create_random_course()
        user = create_random_user()
        lesson = create_lesson()
        clesson = create_clesson(course, lesson)

        # action
        cs = CourseStudent(
            course_id=course.id,
            student_id=user.id,
        )
        cs.save()

        state_items = UserCLessonState.objects.filter(
            user_id=user.id,
            clesson__course_id=course.id,
        )

        # test
        assert list(state_items) == []


class TestConsitency(object):
    @pytest.mark.django_db
    def test_assignment_rule_deletion(self):
        """
        тест проверяет, что состояние пользователя в модуле автоматически удаляется,
        когда удаляется правило назначения
        """
        # init
        course = create_random_course()
        assignment_rule = create_assignment_rule_for_course(course)
        state = UserCLessonState.objects.create(
            assignment_rule=assignment_rule,
            clesson=create_clesson(
                course=course,
                lesson=create_lesson()
            ),
            available=True,
            user=create_random_user(),
        )

        # action
        assignment_rule.delete()

        # test
        assert UserCLessonState.objects.filter(id=state.id).first() is None
