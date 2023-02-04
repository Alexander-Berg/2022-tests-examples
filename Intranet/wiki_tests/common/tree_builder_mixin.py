
from datetime import datetime

from wiki.files.models import File
from wiki.org import get_org
from wiki.pages.models import Access, Page
from wiki.utils.timezone import make_aware_current


class PagesTreeBuilderTestMixin(object):
    """
    Mixin для тестов деревьев страниц.

    Позволяет по данным из JSON создать в базе соответствующие страницы и доступы.

    Структура JSON дерева такова:

    {
        'page': {
            'cluster': <page_cluster>,
            'type': <page_type>,
            'title': '<page_title>,
            'files': [
                {
                    'name': <file_name>,
                    'url': <file_url>,
                },
                ...
            ],
            'created_at': <page_created_at>,
            'modified_at': <page_modified_at>,
        },
        'subpages': [
            'page': {...},
            'subpages': [...],
        ],
    }

    Каждая страница описывается двумя полями
    1) 'page' - данные о странице
    2) 'subpages' - данные о подстраницах

    Данные о странице:

    'cluster' - суффикс супертэга страницы (то, что находится за самым правым '/')

    'type' - тип страницы из следующего списка:
        P - обычная страница
        G - грид
        R - редирект
        C - закрытая страница
        L - страница с ограниченным доступом
        N - несуществующая страница

    'title' - заголовок страницы

    'files' - список файлов, прикрепленных к странице

    'created_at' - дата создания страницы
    'modified_at' - дата модификации страницы

    """

    def build_pages_tree(self, tree, current_user, other_user):
        """
        @type tree: dict
        @type current_user: Staff
        @param other_user: Staff
        @rtype: dict
        """
        return self._build_node(tree, tuple(), current_user, other_user)

    def _build_node(self, node, clusters, current_user, other_user):
        new_clusters = clusters + (node['page']['cluster'],)

        node_type = node['page']['type']
        if node_type != 'N':
            self._create_page_from_node(node, new_clusters, current_user, other_user)

        for subpage in node['subpages']:
            self._build_node(subpage, new_clusters, current_user, other_user)

    @staticmethod
    def _create_page_from_node(node, clusters, current_user, other_user):
        supertag = '/'.join(clusters)

        node_type = node['page']['type']
        if node_type == 'G':
            page_type = Page.TYPES.GRID
        else:
            page_type = Page.TYPES.PAGE

        owner = other_user if node_type == 'C' else current_user
        title = node['page']['title']

        now = make_aware_current(datetime.now())
        created_at = now
        modified_at = now

        if 'created_at' in node['page']:
            created_at = make_aware_current(datetime.fromtimestamp(node['page']['created_at']))
            modified_at = created_at

        if 'modified_at' in node['page']:
            modified_at = make_aware_current(datetime.fromtimestamp(node['page']['modified_at']))

        page = Page(
            supertag=supertag,
            title=title,
            page_type=page_type,
            created_at=created_at,
            modified_at=modified_at,
            org=get_org(),
        )
        page.save()
        page.authors.add(owner.user)

        if node_type == 'R':
            page.redirects_to = page
            page.save()

        if node_type == 'C':
            access = Access(
                page=page,
                staff=other_user,
                is_owner=True,
            )
        elif node_type == 'L':
            access = Access(page=page, staff=current_user)
        else:
            access = Access(page=page, is_common=True)
        access.save()

        if 'files' in node['page']:
            files = node['page']['files']
            for file in files:
                File(
                    page=page,
                    user=current_user.user,
                    name=file['name'],
                    url=file['url'],
                    created_at=created_at,
                    modified_at=modified_at,
                ).save()
