# coding: utf-8

from django.test import TestCase
from django.conf import settings
from django.core.urlresolvers import reverse
from django_intranet_stuff.models import Staff
from libra.books.models import PurchaseRequest, PurchaseRequestOneBook


class PurchaseOrdersTest(TestCase):
    fixtures = ['users.json', 'books_01.json', 'tagging.json']
    reader_login = settings.DUMMY_TEST_USER
    dummy_user = None
#    book_id = 2666
#    book = None

    dataset = {
        'normal': {  # Правильно заполненная форма, без указания руководителя
            'book-0-url': u'http://www.books.ru/shop/books/352130',
            'book-0-title': u'Приемы объектно-ориентированного проектирования',
            'book-0-count': 1,
            'book-0-cost1': '260',
            'book-0-comment': u'эту книгу нужно купить',
            'book-INITIAL_FORMS': 1,
            'book-TOTAL_FORMS': 1,
            'book-MAX_NUM_FORMS': 25,
            'request-0-approve_man': '',
            'request-0-approve_man__login_ld': '',
            'request-0-delivery_office': 1,
            'request-TOTAL_FORMS': 1,
            'request-INITIAL_FORMS': 1,
            'request-MAX_NUM_FORMS': 1,
        },
        '': {},
    }

    def setUp(self):
        # Для некоторых операций нам нужно знать "текущего" пользователя,
        # которого в тестах мы авторизуем фиктивно
        # (через settings.DUMMY_TEST_USER)
        self.dummy_user = Staff.objects.get(login_ld=self.reader_login)

    def test_view_request_purchase_page(self):
        # Страница с формой заказа книг (get-запрос)
        # Ожидаем: HTTP200, использование правильных шаблонов
        response = self.client.get(reverse('add_request'))
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'orders/add-request-form.html')

    """
    def test_save_unfilled_form(self):
        # Сохранение незаполненной формы
        # Ожидаем: http 200, зафиксированные ошибки в форме,
        # не изменившееся количество заказов и строчек заказов (это важно),
        # отсутствие почтовых отправлений
        pass
    """

    def test_save_normal_one_book_wo_approve(self):
        # Сохранение нормально заполненной формы, без указания руководителя
        # Ожидаем: http302, http200, добавление в БД 1 записи PurchaseRequest,
        # 1 записи PurchaseRequestOneBook
        # поля из формы должны нормально пройти в поля модели
        purchase_requests_before = PurchaseRequest.objects.all().count()
        requested_books_before = PurchaseRequestOneBook.objects.all().count()

        response = self.client.post(
            reverse('add_request'), data=self.dataset['normal'], follow=True
        )

        self.assertRedirects(
            response, reverse('view_request', kwargs={'request_id': 1}),
            status_code=301, target_status_code=200
        )
        purchase_requests_after = PurchaseRequest.objects.all().count()
        # ожидаем увеличение на 1 запись
        self.assertEqual(purchase_requests_after, purchase_requests_before + 1)
        self.assertEqual(
            requested_books_before + 1,
            PurchaseRequestOneBook.objects.all().count()
        )

        added_request = PurchaseRequest.objects.get(id=1)
        books_requested = added_request.requested_books.all()

        self.assertEquals(
            self.dataset['normal']['book-0-url'],
            books_requested[0].url,
            u'Не совпадают url заказанной книги'
        )

    """
    def test_save_normal_one_book_with_approve(self):
        # Сохранение нормально заполненной формы, с указанием руководителя
        # Ожидаем:
        pass
    """
