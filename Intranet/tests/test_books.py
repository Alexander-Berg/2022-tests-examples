# -*- coding: utf-8 -*-

from django.test import TestCase
from django.conf import settings
from libra.books.models import Book, ReadHistory, Opinion
from django_intranet_stuff.models import Staff

#Тестируем кастомные фильтры для шаблонов


class BooksTest(TestCase):
    fixtures = ['users.json', 'books_01.json', 'tagging.json']
    reader_login = settings.DUMMY_TEST_USER
    dummy_reader = None
    book_id = 2666
    book = None

    def setUp(self):
        self.dummy_reader = Staff.objects.get(login_ld=self.reader_login)
        self.book = Book.objects.get(id=self.book_id)

    def test_mark_book_as_read(self):
        u' Mark book as read (with blank opinion text). '
        # Пометка книги как прочтенной (текст отзыва пустой).
        # Без краевых условий.

        data = {'read': 1, 'opinion_text': ''}
        response = self.client.post("/books/%d/iread/" % (self.book_id,), data)
        self.assertContains(response, 'saved', 1, 200)
        readhistory = ReadHistory.objects.filter(
            book=self.book, reader=self.dummy_reader
        )
        op = Opinion.objects.filter(book=self.book, reader=self.dummy_reader)
        # Должен создаваться только 1 объект истории
        self.assertEqual(len(readhistory), 1)
        # Не должно быть отзывов (потому что текст был пустой)
        self.assertEqual(len(op), 0)

    def test_unmark_book_as_read_with_opinion(self):
        u"""
        Mark book as unread. opinion's text not blank.
        Expect to not create opinion
        """
        # Снятие пометки о прочтении с книги.
        # При этом отправляется также текст отзыва,
        # но отзыв не должен создаваться

        data = {'read': 0, 'opinion_text': 'some text here for test'}
        response = self.client.post("/books/%d/iread/" % (self.book_id,), data)
        self.assertContains(response, 'saved', 1, 200)
        readhistory = ReadHistory.objects.filter(
            book=self.book, reader=self.dummy_reader
        )
        op = Opinion.objects.filter(book=self.book, reader=self.dummy_reader)
        self.assertEqual(len(readhistory), 0)    # история должна удалиться
        # Не должно быть отзывов (потому что текст был пустой)
        self.assertEqual(len(op), 0)

    def test_set_and_unset_read_mark_book(self):
        # Ставим и снимаем пометку о прочтении с книги.
        # Ожидаем что на выходе не будет ни истории чтения, ни отзыва

        # Ставим пометку
        data = {'read': 1}
        response = self.client.post("/books/%d/iread/" % (self.book_id,), data)
        self.assertContains(response, 'saved', 1, 200)
        readhistory = ReadHistory.objects.filter(
            book=self.book, reader=self.dummy_reader
        )
        op = Opinion.objects.filter(book=self.book, reader=self.dummy_reader)
        # Должен создаваться только 1 объект истории
        self.assertEqual(len(readhistory), 1)
        # Не должно быть отзывов (потому что текст был пустой)
        self.assertEqual(len(op), 0)

        # Снимаем пометку
        data = {'read': 0}
        response = self.client.post("/books/%d/iread/" % (self.book_id,), data)
        self.assertContains(response, 'saved', 1, 200)
        readhistory = ReadHistory.objects.filter(
            book=self.book, reader=self.dummy_reader
        )
        op = Opinion.objects.filter(book=self.book, reader=self.dummy_reader)
        # Должен создаваться только 1 объект истории
        self.assertEqual(len(readhistory), 0)
        # Не должно быть отзывов (потому что текст был пустой)
        self.assertEqual(len(op), 0)

    def test_set_opinion(self):
        # Установка отзыва. Без краевых условий.
        # В результате ожидаем HTTP200, 1 объект истории,
        # 1 отзыв и текст отзыва совпадает с заданным
        data = {'read': 1, 'opinion_text':
                u'Две лошади задумали построить пирамиду. Как же так?'}
        response = self.client.post("/books/%d/iread/" % (self.book_id,), data)
        self.assertContains(response, 'saved', 1, 200)
        readhistory = ReadHistory.objects.filter(
            book=self.book, reader=self.dummy_reader
        )
        op = Opinion.objects.filter(book=self.book, reader=self.dummy_reader)
        # Должен создаваться только 1 объект истории
        self.assertEqual(len(readhistory), 1)
        # Не должно быть отзывов (потому что текст был пустой)
        self.assertEqual(len(op), 1)
        self.assertEqual(op[0].text, data['opinion_text'])

    def test_iread_nonexistent_book(self):
        # Если передана книга, которой нет, то ожидаем Http404
        # Ставим пометку для несуществующей книги
        data = {'read': 1, 'opinion_text':
            u'Две лошади задумали построить пирамиду. Как же так?'}
        response = self.client.post("/books/%d/iread/" % (9999999,), data)
        self.assertContains(response, '', None, 404)

        # Снимаем пометку для несуществующей книги
        data = {'read': 0, 'opinion_text':
            u'Две лошади задумали построить пирамиду. Как же так?'}
        response = self.client.post("/books/%d/iread/" % (9999999,), data)
        self.assertContains(response, '', None, 404)

    def test_double_mark_as_read_book(self):
        # Проверка на устойчивость к многократной отметке книги о прочтении.
        #  Ожидаем что в результате двух последовательных отмечаний
        # объект ReadHistory и отзыв будут в 1 экземпляре
        data = {'read': 1, 'opinion_text': 'yohoho'}
        # поставим отметку
        response = self.client.post("/books/%d/iread/" % (self.book_id,), data)
        self.assertContains(response, 'saved', 1, 200)
        readhistory1 = ReadHistory.objects.get(
            book=self.book, reader=self.dummy_reader
        )
        # и еще раз поставим отметку
        response = self.client.post("/books/%d/iread/" % (self.book_id,), data)
        self.assertContains(response, 'saved', 1, 200)

        # проверяем сколько объектов создалось
        readhistory2 = ReadHistory.objects.filter(
            book=self.book, reader=self.dummy_reader
        )
        op = Opinion.objects.filter(book=self.book, reader=self.dummy_reader)
        # Должен создаваться только 1 объект истории
        self.assertEqual(len(readhistory2), 1)
        # Должен создаваться только 1 объект отзыва
        self.assertEqual(len(op), 1)
        # Объект истории не должен пересоздаваться при повторном запросе
        self.assertEqual(readhistory1.id, readhistory2[0].id)

    def test_mark_bookItem_as_read(self):
        # Пометка экземпляра книги как прочитанного.
        pass

    def test_iread_unauthorized_user(self):
        # Проверка что будет если пользователь незалогинен?
        pass

    def test_list_all_books(self):
        'test all books list'
        # Проверяет отдачу полного списка книг (без фильтров, с паджинаций).
        url = '/books/'
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'alphabet-books-list.html')

    def test_view_tags_list(self):
        'test tags list'
        # Проверяет отдачу списка категорий (тегов)
        url = '/books/tags/'
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'view-tags.html')

    def test_list_books_by_tag(self):
        'test list of books with existent tag'
        # Проверяет отдачу списка книг в заданной категории
        url = '/books/tag140/'  # ID=140 взят из /books/fixtures/tagging.json
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'list-books.html')

    def test_list_books_by_non_existent_tag(self):
        'test list of books with non existent tag'
        # Проверяет отдачу списка книг по несуществующей категории
        # (ожидаем http 404)
        # категории с таким ID в наших fixtures не существует
        url = '/books/tag99999/'
        response = self.client.get(url)
        self.assertEqual(response.status_code, 404)
        self.assertTemplateUsed(response, '404.html')

    def test_index_page_authorized(self):
        'test if index page is accessible for authorized user'
        # проверяем доступность морды для авторизованного пользователя
        url = '/'
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'index.html')

    def test_index_page_unauthorized(self):
        'test if index page is accessible for unauthorized user'
        # проверяем доступность морды для неавторизованного пользователя
        url = '/'

        settings.DUMMY_PASSPORT_USER = None

        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'index.html')
        settings.DUMMY_PASSPORT_USER = settings.DUMMY_TEST_USER

    def test_view_book_page(self):
        'view book page test'
        # Отдается ли страница книги
        url = '/books/%d/' % self.book_id
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'view-book-form.html')

    def test_view_lenta(self):
        'view book lenta test'
        # Отдается ли лента новых и ожидаемых книги
        url = '/books/lenta/'
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'books-lenta.html')
