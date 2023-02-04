import logging
import re

from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import transaction
from django.db.models import Q

from wiki.api_core.waffle_switches import E2E_TESTS
from wiki.cloudsearch.cloudsearch_client import only_switch
from wiki.files.models import MDS_STORAGE, File, make_storage_id
from wiki.notifications.logic import create_page_event_add_comment
from wiki.pages.logic.backlinks import track_links
from wiki.pages.models import Access, Comment, Page, PageWatch, Revision
from wiki.utils import timezone
from wiki.api_core.errors.rest_api_error import RestApiError

logger = logging.getLogger(__name__)


E2E_RESERVED = 'e2e/fixture'


class MDSException(RestApiError):
    status_code = 503
    error_code = 'MDS_STORAGE_IS_UNAVAILABLE'


@only_switch(E2E_TESTS)
def purge(page: Page):
    """
    FULL METAL DELETION
    """
    # У Access, PageWatch, Comment нет mds
    for file in page.file_set.all():
        if MDS_STORAGE.exists(file.mds_storage_id.name):
            MDS_STORAGE.delete(file.mds_storage_id.name)

    for revision in page.revision_set.all():
        if MDS_STORAGE.exists(revision.mds_storage_id.name):
            MDS_STORAGE.delete(revision.mds_storage_id.name)

    # MDS_STORAGE.delete(page.mds_storage_id.name) todo убрать, когда отлажу
    try:
        page.delete()
    except AssertionError as e:
        logger.warning(f'{page.supertag}: {e}')


def copy_access(eva_page, wiki_page):
    access_list = Access.objects.select_related('group', 'page', 'staff').filter(page_id=eva_page.id)
    for access in access_list:
        Access(
            page=wiki_page,
            staff=access.staff,
            group=access.group,
            is_common=access.is_common,
            is_owner=access.is_owner,
            is_anonymous=access.is_anonymous,
        ).save()


def copy_subscriptions(eva_page, wiki_page):
    subscriptions = PageWatch.objects.filter(page_id=eva_page.id)
    for subscription in subscriptions:
        PageWatch(
            page=wiki_page,
            is_cluster=subscription.is_cluster,
            user=subscription.user,
        ).save()


def copy_comments_and_actualitymark(eva_page, wiki_page):
    comments = Comment.objects.select_related('user', 'page').filter(page_id=eva_page.id).order_by('id')

    eva_wiki_comments_map = {}
    for eva_comment in comments:
        wiki_user = eva_comment.user
        comment = Comment.objects.create(
            user=wiki_user,
            page=wiki_page,
            page_at=eva_comment.page_at,
            body=eva_comment.body,
            parent_id=eva_wiki_comments_map.get(eva_comment.parent_id),
            status=eva_comment.status,
        )
        eva_wiki_comments_map[eva_comment.id] = comment.id

        create_page_event_add_comment(
            page=wiki_page,
            user=wiki_user,
            comment=comment,
            notify=False,
        )


def copy_files(eva_page, wiki_page):
    file_set = File.objects.filter(page_id=eva_page.id).select_related('page', 'user')

    for eva_file in file_set:
        wiki_user = eva_file.user
        storage_key = eva_file.mds_storage_id.storage.save(
            make_storage_id(eva_file.name, eva_file.size), SimpleUploadedFile('file', eva_file.mds_storage_id.read())
        )

        now = timezone.now()
        attached_file = File(
            page=wiki_page,
            user=wiki_user,
            name=eva_file.name,
            url=eva_file.url,
            size=eva_file.size,
            description=eva_file.description,
            created_at=now,
            modified_at=now,
            mds_storage_id=storage_key,
            image_width=eva_file.image_width,
            image_height=eva_file.image_height,
        )

        attached_file.save()


def rewrite_body(body, rules):
    new_body = body
    for old, new, flags in rules:
        new_body = re.sub(old, new, new_body, flags=flags)
    return new_body


@transaction.atomic
def copy_page(page: Page, src_tag_prefix, tgt_tag_prefix, rewrite=None):
    new_supertag = page.supertag.lower().replace(src_tag_prefix, tgt_tag_prefix)
    # logger.info(f'{page.supertag} -> {new_supertag} [{page.id}]')

    if Page.active.filter(supertag=new_supertag).exists():
        logger.warning(f"Page with supertag '{new_supertag}' already exists")
        return

    logger.info('original: ' + page.mds_storage_id.name)
    clone = Page.objects.get(pk=page.pk)  # todo
    clone.tag = new_supertag
    clone.supertag = new_supertag
    clone.pk = None

    # logger.info('clone before save: ' + clone.mds_storage_id.name)
    clone.save()
    # logger.info('clone after save: ' + clone.mds_storage_id.name)

    new_body = clone.body

    if rewrite:
        new_body = rewrite_body(new_body, rewrite)

    clone.body = new_body
    clone.authors.set(page.authors.all())
    clone.save()

    Revision.objects.create_from_page(clone)

    copy_access(page, clone)
    copy_subscriptions(page, clone)
    copy_files(page, clone)
    copy_comments_and_actualitymark(page, clone)

    logger.info('clone after all savings: ' + clone.mds_storage_id.name)

    logger.info('[OK]')
    return clone


def copy_cluster(src_tag, tgt_tag, rewrite_rules=None):  # TODO rewrite
    """
    src_tag='eda/partnersupport/test-help-vendor'
    tgt_tag='eda/partnersupport/by/test-help-vendor'
    """
    pages = Page.objects.filter(Q(supertag__startswith=src_tag + '/') | Q(supertag=src_tag))
    new_pages = []
    for old_page in pages:
        new_page = copy_page(old_page, src_tag, tgt_tag, rewrite_rules)

        if new_page:
            new_pages.append((old_page, new_page))

    logger.info('Start of processing track links...')

    for old, new in new_pages:
        if new.page_type == Page.TYPES.PAGE:
            track_links(new, False)

    logger.info('End of processing.')
