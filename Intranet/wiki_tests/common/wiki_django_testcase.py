
import contextlib

from django.test import TestCase as DjangoTestCase
from django_replicated.utils import routers


class WikiTestCaseMixin(object):
    # при False не сравниваем html с эталонным в тестах, включается при ручном прогоне тестов по необходимости
    compare_html = False

    # bigger diff for crappy unittest tests
    maxDiff = None

    def assertTrue(self, expr, msg=None):
        if msg is not None and not isinstance(msg, str):
            raise Exception('assertion message is not a string')
        if msg is not None and not isinstance(expr, bool):
            raise Exception('specify bool type value')
        super(WikiTestCaseMixin, self).assertTrue(expr, msg)

    def assertFalse(self, expr, msg=None):
        if msg is not None and not isinstance(msg, str):
            raise Exception('assertion message is not a string')
        if msg is not None and not isinstance(expr, bool):
            raise Exception('specify bool type value')
        super(WikiTestCaseMixin, self).assertFalse(expr, msg)

    def assertHtmlEqual(self, expected, actual):
        if self.compare_html:
            self.assertEqual(expected, actual, '\n\nExpected html:\n%s\n\nActual html:\n%s' % (expected, actual))
        else:
            self.assertTrue(isinstance(actual, str) and actual)

    def assertHtmlIn(self, member, container):
        if self.compare_html:
            self.assertIn(member, container)
        else:
            self.assertTrue(isinstance(member, str) and member)


last_setUpClass_caller = None


class WikiDjangoTestCase(WikiTestCaseMixin, DjangoTestCase):
    # Флаг, определяющий необходимость реального создания пользовательского кластера.
    # Установить create_user_clusters = True, если в тесте нужны страницы из пользовательского кластера.
    create_user_clusters = False

    # Флаг, определяющий необходимость установки has_personal_cluster = True.
    # Установить has_personal_cluster = False, если надо протестировать создание пользовательского кластера.
    has_personal_cluster = True

    def setUp(self):
        routers._context.state_stack = []
        super(WikiDjangoTestCase, self).setUp()

    def assertNumQueries(self, num, func=None, *args, **kwargs):
        if func is None:
            return contextlib.nullcontext()
        func(*args, **kwargs)

    @classmethod
    def setUpClass(cls):
        global last_setUpClass_caller
        if last_setUpClass_caller is not None:
            raise Exception('Test case %s did not call tearDownClass' % last_setUpClass_caller)
        last_setUpClass_caller = cls

        super(WikiDjangoTestCase, cls).setUpClass()

    @classmethod
    def tearDownClass(cls):
        global last_setUpClass_caller
        if last_setUpClass_caller is None:
            raise Exception('setUpClass was not called')
        last_setUpClass_caller = None

        super(WikiDjangoTestCase, cls).tearDownClass()

    def assertNotRaises(self, excClass, callableObj=None, *args, **kwargs):
        """
        Проверить, что исключение не райзится.
        Использовать по аналогии с self.assertRaises, т.е.

        with self.assertNotRaises(TypeError):
            function('bla-bla', x=10)

        или

        self.assertNotRaises(TypeError, function, 'bla-bla', x=10)
        """
        context = self._assertNotRaisesContext(excClass)
        if callableObj:
            with context:
                callableObj(*args, **kwargs)
        else:
            return context

    @contextlib.contextmanager
    def _assertNotRaisesContext(self, excClass):
        try:
            yield
        except Exception as exc:
            if isinstance(exc, excClass):
                msg = "%s raised, but shouldn't" % excClass
                raise self.failureException(msg)

    def refresh_objects(self, *objects):
        # == obj.refresh_from_db()
        refreshed = []
        for obj in objects:
            refreshed.append(obj.__class__._default_manager.get(id=obj.id))

        if len(refreshed) == 1:
            return refreshed.pop()
        else:
            return refreshed
