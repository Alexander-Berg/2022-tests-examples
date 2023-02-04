# coding: utf-8

import abc
import logging
import os
import time


from six import iteritems, with_metaclass, string_types

from .uriutils import Matcher


logger = logging.getLogger(__name__)


def injected_property(func):
    func._injected_property = True
    return func


def injected_method(func):
    func._injected_method = True
    return func


def match_collection(path):
    result = CollectionMeta.matcher.match(path)
    if result is not None:
        return result

    logger.warning('Unrecognized collection: %s', path)
    return Unknown


class CollectionMeta(abc.ABCMeta):
    matcher = Matcher()

    def __new__(mcs, name, bases, members):
        injected_properties = {}
        injected_methods = {}
        for base in bases:
            injected_properties.update(
                getattr(base, '_injected_properties', {})
            )
            injected_methods.update(
                getattr(base, '_injected_methods', {})
            )
        for member, value in iteritems(members):
            if getattr(value, '_injected_property', None):
                injected_properties[member] = value
            elif getattr(value, '_injected_method', None):
                injected_methods[member] = value
        members['_injected_properties'] = injected_properties
        members['_injected_methods'] = injected_methods

        cls = super(CollectionMeta, mcs).__new__(mcs, name, bases, members)

        if isinstance(cls.path, str):
            mcs.matcher.add(cls.path, cls, cls._priority)
        return cls


class Collection(with_metaclass(CollectionMeta, object)):
    _injected_properties = {}
    _injected_methods = {}
    _priority = 0
    _fields = {}
    _connection = None
    _vars = None

    def __init__(self, connection, **kws):
        self._connection = connection
        self._vars = kws

    def __getitem__(self, key):
        return self.get(key)

    def __iter__(self):
        return iter(self.get_all())

    def __reduce__(self):
        return (self.__class__, (self._connection,),
                {'_fields': self._fields, '_vars': self._vars},
                )

    @abc.abstractproperty
    def path(self):
        pass

    @abc.abstractproperty
    def fields(self):
        pass

    def get_all(self, **params):
        _vars = self._vars.copy()
        _vars.update(params)

        return self._connection.get(
            self.path,
            _vars,
        )

    def get(self, key, **params):
        _vars = self._vars.copy()
        _vars.update(params)
        _vars['id'] = key

        return self._connection.get(
            self.path,
            _vars,
        )

    def create(self, params=None, **kws):
        if not params:
            params = {}

        _vars = self._vars.copy()
        _vars.update(params)

        return self._connection.post(
            self.path,
            params=_vars,
            data=kws,
        )

    def _associated(self, collection, **kws):
        return collection(self._connection, **kws)

    @injected_method
    def update(self, obj, **kws):
        if kws:
            result = self._connection.patch(
                obj._path,
                data=kws,
                version=obj._version
            )
        else:
            result = self._connection.get(obj._path)
        obj.__dict__ = result.__dict__
        return obj

    @injected_method
    def delete(self, obj):
        return self._connection.delete(obj._path)


class Unknown(Collection):
    path = None
    fields = {}
    _priority = -1


class Users(Collection):
    path = '/v2/users/{id}{?expand,localized}'
    fields = {
        'self': None,
        'uid': None,
        'login': None,
        'firstName': None,
        'lastName': None,
        'display': None,
        'email': None,
        'groups': [],
        'office': None,
    }

    @injected_property
    def settings(self, user):
        return self._connection.get(user._path + '/settings')


class IssueTypes(Collection):
    path = '/v2/issuetypes/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'key': None,
        'name': None,
        'description': None,
    }


class Priorities(Collection):
    path = '/v2/priorities/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'key': None,
        'name': None,
        'description': None,
        'order': None,
    }


class Groups(Collection):
    path = '/v2/groups/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'parent': None,
        'type': None,
        'code': None,
        'name': None,
        'url': None,
        'description': None,
    }


class Statuses(Collection):
    path = '/v2/statuses/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'key': None,
        'name': None,
        'description': None,
        'order': None,
    }


class Resolutions(Collection):
    path = '/v2/resolutions/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'key': None,
        'name': None,
        'description': None,
        'order': None,
    }


class Versions(Collection):
    path = '/v2/versions/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'queue': None,
        'name': None,
        'description': None,
        'startDate': None,
        'dueDate': None,
        'releasedAt': None,
        'released': None,
        'archived': None,
        'next': None,
    }


class Components(Collection):
    path = '/v2/components/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'queue': None,
        'name': None,
        'description': None,
        'lead': None,
        'archived': None,
        'assignAuto': None,
    }

    @injected_property
    def permissions(self, project):
        return self._connection.get(project._path + '/permissions')

    @injected_property
    def access(self, project):
        return self._connection.get(project._path + '/access')


class Permissions(Collection):
    path = '/v2/{collection}/{id}/permissions'
    fields = {
        'self': None,
        'create': None,
        'read': None,
        'write': None,
        'grant': None,
    }
    _priority = 1


class PermissionsParticipants(Collection):
    path = '/v2/{collection}/{id}/permissions/{type}'
    fields = {
        'self': None,
        'users': [],
        'groups': [],
        'roles': [],
    }
    _priority = 2


class Access(Collection):
    path = '/v2/{collection}/{id}/access'
    fields = {
        'self': None,
        'create': None,
        'read': None,
        'write': None,
        'grant': None,
    }
    _priority = 1


class AccessParticipants(Collection):
    path = '/v2/{collection}/{id}/access/{type}'
    fields = {
        'self': None,
        'users': [],
        'groups': [],
    }
    _priority = 2


class Issues(Collection):
    path = '/v2/issues/{id}{?filter*,filterId,fields,query,keys,queue,orderBy,orderAsc,language,expand,embed,localized,perPage,notifyAuthor}'
    search_path = '/v2/issues/_search{?perPage,page,expand,fields,language,embed,localized,notifyAuthor}'

    _fields = None

    @staticmethod
    def _parse_order_params(order_by, order_asc, order_list):
        if order_by:
            order_list = ['{direction}{field}'.format(direction='+' if order_asc else '-', field=order_by)]
        return ','.join(order_list) or None

    @property
    def fields(self):
        if self._fields is None:
            self._fields = dict((field.id, [] if field.schema['type'] == 'array' else None)
                                for field in Fields(self._connection).get_all())
        return self._fields

    def create(self, notify_author=False, **kws):
        params = {'notifyAuthor': 'true' if notify_author else 'false'}
        collection = self._associated(Attachments)
        kws['attachmentIds'] = list(collection.create(file).id for file in kws.pop('attachments', []))
        return super(Issues, self).create(params=params, **kws)

    @injected_method
    def update(self, obj, **kws):
        collection = self._associated(Attachments)
        kws['attachmentIds'] = list(collection.create(file).id for file in kws.pop('attachments', []))
        return super(Issues, self).update(obj, **kws)

    def find(self, query=None, per_page=None, keys=None, filter=None, filter_id=None, order=None, **kws):
        """
        Parameters 'orderBy' and 'orderAsc' are deprecated in this method.
        Instead use the parameter 'order' in the form of the fields list
        like in Django: ['field1', '-field2', '+field3']
        """
        data = {
            'filter': filter,
            'filterId': filter_id,
            'query': query,
            'keys': keys,
            'queue': kws.pop('queue', None),
            'order': self._parse_order_params(kws.pop('orderBy', None), kws.pop('orderAsc', True), order or [])
        }
        return self._connection.post(
            self.search_path,
            params=dict(kws, perPage=per_page),
            data=data,
        )

    @injected_method
    def add_remotelink(self, issue, relation, target_url):
        return self._connection.link(
            issue._path,
            target_url=target_url,
            rel=relation,
        )

    @injected_method
    def add_link(self, issue, relation, target_issue, notify=None):
        return issue.links.create(
            relationship=relation,
            issue=target_issue,
            notify=notify,
        )

    @injected_method
    def create_subtask(self, issue, inherit=True, **kws):
        _kws = {
            'queue': issue.queue,
            'parent': issue
        }
        if inherit:
            # Inherit tags, fixVersions, components from parent issue
            _kws['tags'] = issue.tags
            _kws['fixVersions'] = issue.fixVersions
            _kws['components'] = issue.components
        _kws.update(kws)
        return self.create(**_kws)

    @injected_method
    def move_to(self, issue, queue):
        return self._connection.post(
            issue._path + '/_move{?queue}',
            params={'queue': queue}
        )

    @injected_method
    def clone_to(self, issue, queues,
                 clone_all_fields=False,
                 link_with_original=False):
        return self._connection.post(
            issue._path + '/_clone{?queues,cloneAllFields,linkWithOriginal}',
            params={
                'queues': queues,
                'cloneAllFields': 'true' if clone_all_fields else 'false',
                'linkWithOriginal': 'true' if link_with_original else 'false',
            }
        )

    @injected_method
    def link(self, issue, resource, relationship):
        return self._connection.link(issue._path, resource, relationship)

    @injected_method
    def unlink(self, issue, resource, relationship):
        return self._connection.unlink(issue._path, resource, relationship)

    @injected_property
    def comments(self, issue):
        return self._associated(IssueComments, issue=issue.key)

    @injected_property
    def transitions(self, issue):
        return self._associated(IssueTransitions, issue=issue.key)

    @injected_property
    def links(self, issue):
        return self._associated(IssueLinks, issue=issue.key)

    @injected_property
    def remotelinks(self, issue):
        return self._associated(IssueRemoteLinks, issue=issue.key)

    @injected_property
    def attachments(self, issue):
        return self._associated(IssueAttachments, issue=issue.key)

    @injected_property
    def changelog(self, issue):
        return self._associated(IssueChangelog, issue=issue.key)

    @injected_property
    def worklog(self, issue):
        return self._associated(IssueWorklog, issue=issue.key)

    @injected_property
    def permissions(self, issue):
        if 'permissions' in issue._value:
            return issue._value['permissions']
        return self._connection.get(issue._path + '/permissions')


class Queues(Collection):
    path = '/v2/queues/{id}{?expand,fields,localized}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'key': None,
        'name': None,
        'description': None,
        'lead': None,
        'assignAuto': None,
        'allowExternals': None,
        'defaultType': None,
        'defaultPriority': None,
        'teamUsers': [],
        'teamGroups': [],
    }

    @injected_property
    def components(self, queue):
        return self._connection.get(queue._path + '/components')

    @injected_property
    def versions(self, queue):
        return self._connection.get(queue._path + '/versions')

    @injected_property
    def projects(self, queue):
        return self._connection.get(queue._path + '/projects')

    @injected_property
    def issuetypes(self, queue):
        return self._connection.get(queue._path + '/issuetypes')

    @injected_property
    def permissions(self, queue):
        # XXX: Collection
        return self._connection.get(queue._path + '/permissions')

    @injected_property
    def access(self, queue):
        # XXX: Collection
        return self._connection.get(queue._path + '/access')


class QueueDefaultValues(Collection):
    path = '/v2/queues/{queue}/defaultValues/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'queue': None,
        'type': None,
        'component': None,
        'values': {},
        'appendFields': [],
        'createdBy': None,
        'createdAt': None,
        'updatedBy': None,
        'updatedAt': None,
    }
    _priority = 1


class IssueTransitions(Collection):
    path = '/v2/issues/{issue}/transitions/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'to': None,
        'screen': None,
    }
    _priority = 1

    @injected_method
    def execute(self, transition, **kws):
        self._connection.post(
            transition._path + '/_execute',
            data=kws,
        )


class IssueComments(Collection):
    path = '/v2/issues/{issue}/comments/{id}{?expand,localized,notifyAuthor}'
    fields = {
        'id': None,
        'self': None,
        'text': None,
        'textHtml': None,
        'attachments': [],
        'createdBy': None,
        'createdAt': None,
        'updatedBy': None,
        'updatedAt': None,
        'summonee': None,
        'email': None,
    }
    _priority = 1

    def create(self, notify_author=False, **kws):
        params = {'notifyAuthor': 'true' if notify_author else 'false'}
        collection = self._associated(Attachments)
        kws['attachmentIds'] = list(collection.create(file).id for file in kws.pop('attachments', []))
        return super(IssueComments, self).create(params=params, **kws)

    @injected_method
    def update(self, obj, **kws):
        collection = self._associated(Attachments)
        kws['attachmentIds'] = list(collection.create(file).id for file in kws.pop('attachments', []))
        return super(IssueComments, self).update(obj, **kws)


class Links(Collection):
    path = '/v2/links/{id}'
    fields = {
        'id': None,
        'self': None,
        'type': None,
        'direction': None,
        'object': None,
        'createdBy': None,
        'createdAt': None,
        'updatedBy': None,
        'updatedAt': None,
    }
    _noindex = True


class RemoteLinks(Collection):
    path = '/v2/remotelinks/{id}'
    fields = {
        'id': None,
        'self': None,
        'type': None,
        'direction': None,
        'object': None,
        'createdBy': None,
        'createdAt': None,
        'updatedBy': None,
        'updatedAt': None,
    }
    _noindex = True


class IssueLinks(Collection):
    path = '/v2/issues/{issue}/links/{id}{?notify}'
    fields = Links.fields
    _priority = 1

    def create(self, relationship, issue, notify=None):
        assert 'issue' in self._vars
        params = None if notify is None else {'notify': str(notify).lower()}
        return super(IssueLinks, self).create(
            relationship=relationship,
            issue=issue,
            params=params,
        )


class IssueRemoteLinks(Collection):
    path = '/v2/issues/{issue}/remotelinks/{id}'
    fields = RemoteLinks.fields
    _priority = 1

    def create(self, relationship, key, origin):
        assert 'issue' in self._vars
        super(IssueRemoteLinks, self).create(
            relationship=relationship,
            key=key,
            origin=origin
        )


class Attachments(Collection):
    path = '/v2/attachments/{id}'
    fields = {
        'id': None,
        'self': None,
        'name': None,
        'content': None,
        'thumbnail': None,
        'createdBy': None,
        'createdAt': None,
        'mimetype': None,
        'size': None,
        'metadata': {},
    }
    _priority = 1

    def create(self, file):
        """
        @type file: basestring | file
        """
        if isinstance(file, string_types):
            with open(file, 'rb') as file_to_upload:
                return self._connection.post(
                    self.path,
                    params=self._vars,
                    files={
                        'file': (
                            self._get_filename(file_to_upload),
                            file_to_upload
                        )
                    }
                )
        else:
            return self._connection.post(
                self.path,
                params=self._vars,
                files={'file': (self._get_filename(file), file)}
            )

    @staticmethod
    def _get_filename(file):
        DEFAULT_FILENAME = 'file'

        if isinstance(file, string_types):
            filename = file
        else:
            filename = getattr(file, 'name', None)

        if not filename:
            return DEFAULT_FILENAME

        # пока в стартрек нельзя отправлять не-ascii имена файлов
        # с указанием кодировки как это делает requests STARTREK-7403
        try:
            filename.encode('ascii')
        except UnicodeEncodeError:
            return DEFAULT_FILENAME

        return filename.rsplit('/', 1)[-1]

    @injected_method
    def download_to(self, attachment, directory):
        assert attachment.content is not None
        with open(os.path.join(directory, attachment.name), 'wb') as dest:
            for chunk in self._connection.stream(attachment.content):
                dest.write(chunk)

    @injected_method
    def download_thumbnail_to(self, attachment, directory):
        assert attachment.thumbnail is not None
        with open(os.path.join(directory, attachment.name), 'wb') as dest:
            for chunk in self._connection.stream(attachment.thumbnail):
                dest.write(chunk)


class IssueAttachments(Attachments):
    path = '/v2/issues/{issue}/attachments/{id}'
    fields = Attachments.fields


class Screens(Collection):
    path = '/v2/screens/{id}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'type': None,
        'elements': [],
    }


class ScreenElements(Collection):
    path = '/v2/screens/{screen}/elements/{id}'
    fields = {
        'self': None,
        'field': None,
        'required': None,
        'defaultValue': None,
    }
    _priority = 1


class IssueScreens(Collection):
    path = '/v2/issues/{issue}/screens/{id}'
    fields = Screens.fields
    _priority = 1


class IssueChangelog(Collection):
    path = '/v2/issues/{issue}/changelog/{id}{?sort,field*,type*}'
    fields = {
        'id': None,
        'self': None,
        'issue': None,
        'updatedAt': None,
        'updatedBy': None,
        'type': None,
        'transport': None,
        'fields': [],
        'attachments': None,
        'comments': None,
        'worklog': [],
        'messages': [],
        'links': []
    }
    _priority = 1


class Worklog(Collection):
    path = '/v2/worklog/{id}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'issue': None,
        'comment': None,
        'createdBy': None,
        'createdAt': None,
        'updatedBy': None,
        'updatedAt': None,
        'start': None,
        'duration': None,
    }

    def find(self, **kws):
        return self._connection.get(
            self.path,
            params=kws,
        )


class IssueWorklog(Collection):
    path = '/v2/issues/{issue}/worklog/{id}'
    fields = Worklog.fields
    _priority = 1

    def create(self, start, duration, comment=None):
        assert 'issue' in self._vars
        super(IssueWorklog, self).create(
            start=start,
            duration=duration,
            comment=comment
        )


class Applications(Collection):
    path = '/v2/applications/{id}'
    fields = {
        'id': None,
        'type': None,
        'self': None,
        'name': None,
    }


class ApplicationObjects(Collection):
    path = '/v2/applications/{application}/objects/{id}'
    fields = {
        'id': None,
        'key': None,
        'self': None,
        'application': None,
    }
    _priority = 1
    _noindex = True


class LinkTypes(Collection):
    path = '/v2/linktypes/{id}'
    fields = {
        'id': None,
        'self': None,
        'inward': None,
        'outward': None,
    }


class Fields(Collection):
    path = '/v2/fields/{id}'
    fields = {
        'id': None,
        'self': None,
        'name': None,
        'description': None,
        'schema': None,
        'readonly': None,
        'options': None,
        'suggest': None,
    }


class Sprints(Collection):
    path = '/v2/sprints/{id}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'name': None,
        'board': None,
        'status': None,
        'archived': None,
        'createdBy': None,
        'createdAt': None,
        'startDate': None,
        'endDate': None,
    }

class BoardSprints(Sprints):
    path = '/v2/boards/{board}/sprints/'


class Roles(Collection):
    path = '/v2/roles/{id}'
    fields = {
        'id': None,
        'self': None,
        'name': None,
    }


class WebHooks(Collection):
    path = '/v2/webhooks/{id}'
    fields = {
        'id': None,
        'self': None,
        'endpoint': None,
        'filter': {},
        'enabled': None,
    }


class Offices(Collection):
    path = '/v2/offices/{id}'
    fields = {
        'id': None,
        'self': None,
        'display': None,
    }


class Countries(Collection):
    path = '/v2/countries/{id}'
    fields = {
        'id': None,
        'self': None,
        'name': None,
    }


class Workflows(Collection):
    path = '/v2/workflows/{id}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'xml': None,
    }


class Projects(Collection):
    path = '/v2/projects/{id}{?expand,localized}'
    fields = {
        'id': None,
        'self': None,
        'key': None,
        'name': None,
        'description': None,
        'lead': None,
        'teamUsers': [],
        'teamGroups': [],
        'status': None,
        'startDate': None,
        'endDate': None,
        'queues': [],
    }

    @injected_property
    def permissions(self, project):
        # XXX: Collection
        return self._connection.get(project._path + '/permissions')

    @injected_property
    def access(self, project):
        # XXX: Collection
        return self._connection.get(project._path + '/access')


class MailLists(Collection):
    path = '/v2/maillists/{id}'
    fields = {
        'id': None,
        'self': None,
        'name': None,
        'email': None,
        'open': None,
        'info': None,
    }


class Boards(Collection):
    path = '/v2/boards/{id}{?localized}'
    fields = {
        'id': None,
        'self': None,
        'version': None,
        'name': None,
        'columns': [],
        'filter': {},
        'orderBy': None,
        'orderAsc': None,
        'query': None,
        'selected': None,
        'useRanking': None,
        'country': None,
    }

    @injected_property
    def columns(self, board):
        return self._associated(BoardColumns, board=board.id)

    @injected_property
    def sprints(self, board):
        return self._associated(BoardSprints, board=board.id)


class BoardColumns(Collection):
    path = '/v2/boards/{board}/columns/{id}'
    fields = {
        'id': None,
        'self': None,
        'name': None,
        'limit': None,
        'statuses': [],
    }
    _priority = 1


class BulkChange(Collection):
    path = '/v2/bulkchange/{id}'
    fields = {
        'id': None,
        'self': None,
        'createdAt': None,
        'status': None,
        'statusText': None,
        'executionChunkPercent': None,
        'executionIssuePercent': None,
    }

    def update(self, issues, **values):
        data = {
            'issues': issues,
            'values': values,
        }
        return self._connection.post(self.path + '_update', data=data)

    def transition(self, issues, transition, **values):
        data = {
            'transition': transition,
            'issues': issues,
            'values': values,
        }
        return self._connection.post(self.path + '_transition', data=data)

    def move(self, issues, queue, move_all_fields=False, **values):
        data = {
            'queue': queue,
            'issues': issues,
            'values': values,
            'moveAllFields': move_all_fields,
        }
        return self._connection.post(self.path + '_move', data=data)

    @injected_property
    def issues(self, bulkchange):
        # TODO: implement it when /issues objects had 'self' field.
        # return self._connection.get(bulkchange._path + '/issues')
        raise NotImplementedError()

    @injected_method
    def wait(self, bulkchange, interval=1.0):
        while bulkchange.status not in ('COMPLETE', 'FAILED'):
            time.sleep(interval)
            bulkchange = self[bulkchange.id]
        return bulkchange
