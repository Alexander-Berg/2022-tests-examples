# coding: utf-8

"""Модуль на замену magic tester.
magic tester работает по волшебству и как любое настоящее чудо плохо поддается контролю и поддержке
поэтому модуль стоило назвать mage tester - т.к. он делает многое то же самое,
но понятно что и все можно контролировать - маг управляет магией.
Но мне не дали. =("""

import copy
from collections import defaultdict
from urlparse import urlparse, urljoin

import attr
from lxml import etree

import btestlib.passport_steps as passport
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib import config
from btestlib import diffutils, treediff
from btestlib.constants import Users
from btestlib.treediff import TreeDiff, LxmlElementWrapper, Filter, Types

# todo-igogor подумать какие еще тэги и атрибуты несут важные данные
# https://www.w3.org/TR/REC-html40/index/elements.html
# https://www.w3.org/TR/REC-html40/index/attributes.html
# http://stackoverflow.com/questions/2725156/complete-list-of-html-tag-attributes-which-have-a-url-value
DISPLAYED_TAGS = ['input', 'a', 'img', 'form', 'table', 'select', 'button', 'textarea']
HIDDEN_TAGS = ['option', 'title', 'frame', 'iframe', 'caption']
IGNORED_TAGS = ['script', 'style']
TAGS_TO_FIND = DISPLAYED_TAGS + HIDDEN_TAGS
ATTRIBUTES_TO_FIND = ['value', 'title', 'selected', 'checked']
ATTRIBUTES_TO_CHECK = ATTRIBUTES_TO_FIND + ['name', 'id', 'disabled', 'type', 'href', 'method', 'action',
                                            'src', 'for', 'readonly', 'style']


def pagediff(unique_name, page_html=None, page_xml=None, filters=None):
    if (page_xml is None and page_html is None) or (page_xml is not None and page_html is not None):
        raise ValueError('Only one parameter: page_html or page_xml must be set')

    page_source = page_html or page_xml  # type: str
    is_html = bool(page_html)

    stored_etalon = get_etalon_from_storage(unique_name=unique_name)
    if is_force_rebuild(unique_name) or not stored_etalon:
        reason = u'команда на перестройку эталона' if is_force_rebuild(unique_name) else u'эталон отсутствует'
        with reporter.step(u'Пересохраняем эталон по причине: {}'.format(reason)):
            put_etalon_in_storage(etalon=page_source, unique_name=unique_name)
        return None

    if is_html:
        diff = htmldiff(page_source, stored_etalon, filters=filters)
    else:
        diff = xmldiff(page_source, stored_etalon, filters=filters)

    Reporter(expected_page=stored_etalon, actual_page=page_source, diff=diff,
             is_html=is_html, unique_name=unique_name).report()


def htmldiff(page_html, etalon_html, filters=None):
    # type: (str, str, List) -> TreeDiff

    filters = filters or []
    with reporter.step(u'Производим сравнение html'):
        page_root = LxmlElementWrapper.build(etree.HTML(page_html))
        etalon_root = LxmlElementWrapper.build(etree.HTML(etalon_html))
        diff = TreeDiff.diff(etalon_root, page_root)

        # фильтруем все ненужное
        # todo-igogor хотел сделать это фильтрами, но они не достаточно гибкие.
        def is_element_important(element):
            has_text = bool(element.text)
            has_important_attribute = any([attribute in element.data
                                           for attribute in ['value', 'title', 'selected', 'checked', 'onclick']])
            has_important_tag = element.tag in ['input', 'a', 'img', 'form', 'table', 'tr', 'td', 'th', 'select',
                                                'button', 'textarea', 'option', 'title', 'frame', 'iframe', 'caption']
            is_hidden = any([parent.data.get('style', '') == 'display: none'
                             # or parent.data.get('type', '') == 'hidden'
                             for parent in element.parents()])
            always_ignore = element.tag is etree.Comment or element.tag in ['script', 'style'] or \
                            (element.tag == 'option' and 'CreatedByScript' in element.text)
            return (has_text or has_important_tag or has_important_attribute) and not is_hidden and not always_ignore

        def is_subtree_important(root):
            stack = [root]
            while stack:
                node = stack.pop()
                if is_element_important(node):
                    return True
                stack.extend(node.children)
            return False

        def is_change_important(change):
            # print change
            if change.type is Types.EDIT:
                result = is_subtree_important(change.elem) or is_subtree_important(change.to_elem)
            elif change.type is Types.DELETE:
                result = is_subtree_important(change.elem)
            elif change.type is Types.INSERT:
                result = is_subtree_important(change.to_elem)
            elif change.type is Types.MOVE:
                result = is_subtree_important(change.elem) or is_subtree_important(change.to_elem)
            else:
                raise ValueError('Change type is not supported: {}'.format(change.type.name))

            # print result
            return result

        diff.filter(Filter.Not(Filter.Attributes(['text_', 'value', 'title', 'selected', 'checked', 'name', 'id',
                                                  'disabled', 'type', 'href', 'method', 'action', 'src', 'for',
                                                  'readonly', 'style'])))
        important_changes = [change for change in diff.changes if is_change_important(change)]
        diff = attr.evolve(diff, changes=important_changes, filters=[])

        with reporter.step(u'Применяем фильтры'):
            diff.filter(*filters)

        return diff


def xmldiff(page_xml, etalon_xml, filters=None):
    # type: (str, str, List) -> TreeDiff

    filters = filters or []
    with reporter.step(u'Производим сравнение xml'):
        page_root = LxmlElementWrapper.build(etree.XML(page_xml))
        etalon_root = LxmlElementWrapper.build(etree.XML(etalon_xml))
        diff = TreeDiff.diff(etalon_root, page_root)

        with reporter.step(u'Применяем фильтры'):
            diff.filter(*filters)

        return diff


filter_ = treediff.filter_
all_ = treediff.all_
not_ = treediff.not_


def get_page(url, user=Users.YB_ADM, prepare_page=None):
    with reporter.step(u'Получаем данные страницы: ' + url):
        with utils.Web.DriverProvider() as driver:
            with reporter.step(u'Авторизация'):
                passport.auth_post(driver=driver, user=user)

            if not prepare_page:
                def prepare_page(driver_, url_):
                    driver_.get(url_)

            prepare_page(driver, url)

            page_html = normalize_source_urls(page_url=url, page_html=driver.page_source)
            return page_html


def normalize_source_urls(page_url, page_html):
    base_url = '{uri.scheme}://{uri.netloc}/'.format(uri=urlparse(page_url))
    root = etree.HTML(page_html)
    tree = etree.ElementTree(root)

    def make_absolute(base, partial):
        return partial if urlparse(partial).scheme else urljoin(base, partial)

    new_html = page_html  # type: str
    for element in tree.xpath('//*[@src]'):
        # меняем как строку, а не через lxml, т.к. lxml сильно изменяет html
        new_html = new_html.replace('src="{}"'.format(element.attrib['src']),
                                    'src="{}"'.format(make_absolute(base=base_url, partial=element.attrib['src'])))

    for element in tree.xpath('//link[@href]'):
        new_html = new_html.replace('href="{}"'.format(element.attrib['href']),
                                    'href="{}"'.format(make_absolute(base=base_url, partial=element.attrib['href'])))
    return new_html


def is_force_rebuild(unique_name):
    rebuild_var = config.PAGEDIFF_REBUILD
    return bool(rebuild_var) and (rebuild_var == '*' or unique_name in rebuild_var)


def s3_html_key(unique_name):
    return 'pagediff_html_' + unique_name


def put_etalon_in_storage(etalon, unique_name):
    # type: (str, str) -> None
    with reporter.step(u"Сохраняем эталон в хранилище: " + unique_name):
        with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
            utils.s3storage_pagediff().set_string_value(key=s3_html_key(unique_name),
                                                        value=etalon.encode('utf-8'))


def get_etalon_from_storage(unique_name):
    with reporter.step(u'Получаем эталон из хранилища: ' + unique_name):
        with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
            if utils.s3storage_pagediff().is_present(s3_html_key(unique_name)):
                return utils.s3storage_pagediff().get_string_value(key=s3_html_key(unique_name)).decode('utf-8')
            else:
                return None


class Reporter(object):
    def __init__(self, actual_page, expected_page, diff, is_html, unique_name):
        self.actual_page = actual_page
        self.expected_page = expected_page
        self.diff = diff
        self.is_html = is_html
        self.unique_name = unique_name
        parser = etree.HTML if is_html else etree.XML
        self.report_tree = parser(self.actual_page).getroottree()
        self._comments = defaultdict(list)

    def report_type(self, type_):
        step_strings = {Types.INSERT: u'На страницу добавилось',
                        Types.DELETE: u'Со страницы пропало',
                        Types.MOVE: u'На странице переместилось',
                        Types.EDIT: u'На странице изменилось'}
        changes = [c for c in self.diff.changes if c.type is type_]
        with reporter.step(step_strings[type_] + u' {} элементов'.format(len(changes))):
            attach_labels = {Types.INSERT: u'Добавившиеся элементы',
                             Types.DELETE: u'Пропавшие элементы',
                             Types.MOVE: u'Переместившиеся элементы',
                             Types.EDIT: u'Изменившиеся элементы'}
            reporter.attach(attach_labels[type_],
                            u'\n\n'.join([self.change_str(c) + u'\n' + Filter.for_change(c).smart_repr()
                                          for c in changes]))

            for change in changes:
                self.add_comment(change)

    def report(self):
        # type: () -> None
        with reporter.step(u'Строим отчет по {}'.format(u'расхождениям' if self.diff.changes else u'фильтрам')):
            attachment_type = reporter.attachment_type.HTML if self.is_html else reporter.attachment_type.XML
            reporter.attach(u'Ожидаемая страница', self.expected_page, attachment_type, allure_=True)
            reporter.attach(u'Текущая страница', self.actual_page, attachment_type, allure_=True)

            for type_ in [Types.INSERT, Types.DELETE, Types.MOVE, Types.EDIT]:
                self.report_type(type_)

            self.report_filters()

            reporter.attach(u'HTML отчет с {}'.format(u'расхождениями' if self.diff.changes else u'фильтрами'),
                            self.get_report_page(),
                            attachment_type, allure_=True)
            reporter.report_url(u'Инструкция к HTML отчету',
                                'https://github.yandex-team.ru/Billing/balance-tests/blob/master/scripts/examples/pagediff_examples.py#L15')
            reporter.report_url(u'Видео-инструкция к HTML отчету',
                                'https://jing.yandex-team.ru/files/igogor/screencast_2017-01-25_18-15-36.mp4')

            if self.diff.changes:
                raise PagediffError(unique_name=self.unique_name,
                                    types_count=[(type_, sum([1 for c in self.diff.changes if c.type is type_]))
                                                 for type_ in [Types.INSERT, Types.DELETE, Types.MOVE, Types.EDIT]])
                # utils.check_that(True, equal_to(False), step=u'Фейлим тест т.к. есть расхождения')

    def get_report_page(self):
        return etree.tounicode(self.report_tree, method='html' if self.is_html else 'xml')

    def report_filters(self):
        with reporter.step(u'Отфильтрованные элементы'):
            for filter_ in self.diff.filters:
                reporter.attach(filter_.smart_repr(),
                                u'{filter}\n\n{filtered}'.format(
                                    filter=filter_.smart_repr(),
                                    filtered=u'\n'.join([self.change_str(c) for c in filter_.filtered])))

                for change in filter_.filtered:
                    self.add_comment(change, filter_=filter_)

    def add_comment(self, change, filter_=None):
        # type: (Change) -> None

        if change.type in [Types.DELETE, Types.MOVE]:
            report_parent_path = self.diff._other_parent[change.elem.parent.path].path  # todo-igogor костыль
        else:
            report_parent_path = change.to_elem.parent.path

        prefix = u'{}-{}'.format(u'filtered' if filter_ else u'pagediff', change.type.name.lower())
        filter_descr = (filter_ or Filter.for_change(change)).smart_repr()
        diff_descr = u', '.join([u'{}: {}'.format(key, attr_change_str) for
                                 key, attr_change_str in diffutils.report_changes_as_dict(change.diff).iteritems()]) \
            if change.diff else u''
        expected_element_descr = u'' if change.type is Types.INSERT else self.source_str(change.elem)
        text = u'\n'.join(utils.remove_false([u'{} {}'.format(prefix, filter_descr),
                                              diff_descr,
                                              expected_element_descr]))

        if change.type in [Types.EDIT, Types.INSERT]:
            self._add_comment(report_parent_path, text, change, change.to_elem.index, index_from_expected=False)
        elif change.type is Types.DELETE:
            self._add_comment(report_parent_path, text, change, change.elem.index, index_from_expected=True)
        elif change.type is Types.MOVE:
            self._add_comment(report_parent_path, text, change, change.elem.index, index_from_expected=True)
            self._add_comment(report_parent_path, u'pagediff-move-to', change,
                              change.to_elem.index, index_from_expected=False)

    def _add_comment(self, report_parent_path, text, change, index, index_from_expected):
        # igogor: циклы нельзя заменять на компрехеншон, т.к. index не будет на каждой итерации новый
        counted_shifts = []
        for comment_index, comment_change in self._comments[report_parent_path]:
            if index_from_expected \
                    and comment_change.type in [Types.INSERT, Types.MOVE] \
                    and comment_change not in counted_shifts \
                    and comment_change.to_elem.index <= index:
                index += 1
                counted_shifts.append(comment_change)
            if comment_index <= index:
                index += 1

        report_parent = self.report_tree.xpath(report_parent_path)[0]
        report_parent.insert(index, etree.Comment(text))
        self._comments[report_parent_path].append((index, change))

    def source_str(self, element):
        lxml_element = element._element
        element_copy = copy.deepcopy(lxml_element)
        for child in element_copy:
            element_copy.remove(child)
        elem_str = etree.tounicode(element_copy, pretty_print=True, method='html' if self.is_html else 'xml').strip()
        return utils.String.normalize_whitespaces(elem_str)

    def change_str(self, change):
        descr = u'pagediff-{}'.format(change.type.name.lower())
        if change.type is Types.INSERT:
            return descr
        elif change.type is Types.DELETE:
            return u'{}\n{}'.format(descr, self.source_str(change.elem))
        elif change.type is Types.MOVE:
            return u'{}\n{} -> {}\n{}'.format(descr, self.source_str(change.elem),
                                              change.elem.path, change.to_elem.path)
        elif change.type is Types.EDIT:
            return u'{}\n{}\n{}\n{}'.format(descr, self.source_str(change.elem), self.source_str(change.to_elem),
                                            diffutils.report_changes_as_dict(change.diff))
        else:
            raise ValueError('Change type is not supported: {}'.format(change.type))


class PagediffError(AssertionError):
    def __init__(self, unique_name, types_count):
        self.unique_name = unique_name
        self.types_count_str = 'pagediff ' + ', '.join(['{}: {}'.format(type_.name.lower(), count)
                                                        for type_, count in types_count])
        super(PagediffError, self).__init__('{} unique_name = {}'.format(self.types_count_str, self.unique_name))
