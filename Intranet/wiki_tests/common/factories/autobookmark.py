import factory

from django.conf import settings
from django.utils import timezone

from wiki.favorites_v2.models import Folder, AutoBookmark
from wiki.pages.models import Page


class AutoBookmarkFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = AutoBookmark

    page: Page
    folder: Folder

    title = factory.LazyAttribute(lambda a: a.page.title)
    supertag = factory.LazyAttribute(lambda a: a.page.supertag)
    url = factory.LazyAttribute(lambda a: '%s://%s/%s' % (settings.WIKI_PROTOCOL, settings.NGINX_HOST, a.page.supertag))
    page_last_editor = factory.LazyAttribute(lambda a: a.page.last_author.username or '')

    created_at = factory.LazyAttribute(lambda a: timezone.now())
    page_modified_at = factory.LazyAttribute(lambda a: timezone.now())

    @factory.post_generation
    def inc_count_in_folder(self, *args, **kwargs):
        self.folder.favorites_count += 1
        self.folder.save()
