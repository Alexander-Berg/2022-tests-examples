#coding: utf-8

from django.test import TestCase
from django.conf import settings

class RSSTest(TestCase):
    fixtures = ['users.json', 'books_01.json']
    
    def test_rss_latest_ya(self):
        u' rss feed of latest yandex books '
        # Получение ленты последних книг, купленных компанией
        
        response = self.client.get("/rss/latest-ya/" )
        self.assertTemplateUsed(response, 'feeds/latest-books-description.html')
        self.assertTemplateUsed(response, 'feeds/latest-books-title.html')
        self.assertEqual(response.status_code, 200)

    def test_rss_latest_all(self):
        u' rss feed of latest all books '
        # Получение ленты всех последних книг, добавленных в сервис
        
        response = self.client.get("/rss/latest-all/" )
        self.assertTemplateUsed(response, 'feeds/latest-books-description.html')
        self.assertTemplateUsed(response, 'feeds/latest-books-title.html')
        self.assertEqual(response.status_code, 200)
