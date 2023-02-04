from maps.automotive.libs.large_tests.lib.http import http_request, http_request_json
import maps.automotive.libs.large_tests.lib.docker as docker

import json

SCHEDULER = 'http://127.0.0.1:9004'
HOST = 'auto-remote-tasks-scheduler.maps.yandex.net'
HEADERS = {'Host': HOST}
ST_SECRET = 'dummy_st_secret'


def set_url(url):
    global SCHEDULER
    SCHEDULER = url


def get_url():
    return SCHEDULER


def get_host():
    return HOST


def get_result(task_id, user):
    return http_request(
        'GET', SCHEDULER + '/tasks/1.x/get_result',
        params={
            'task_id': task_id,
        },
        headers=HEADERS,
        cookies={'Session_id': user.session_id})


def add_task(head_id, task_spec, user):
    return http_request(
        'POST', SCHEDULER + '/tasks/1.x/add',
        params={
            'head_id': head_id,
        },
        headers={
            'Host': HOST,
            'Content-Type': 'application/protobuf',
        },
        cookies={'Session_id': user.session_id},
        data=task_spec)


def cancel_task(task_id, user):
    return http_request(
        'POST', SCHEDULER + '/tasks/1.x/cancel',
        params={
            'task_id': task_id,
        },
        headers=HEADERS,
        cookies={'Session_id': user.session_id})


def startrek_add_task(startrek_form, st_secret=ST_SECRET):
    return http_request(
        'POST', SCHEDULER + '/tasks/startrek/1.x/add',
        params={
            'head_id': startrek_form.head_id,
        },
        headers={
            'Host': HOST,
            'Content-Type': 'text/protobuf',
            'UserID': startrek_form.user_id,
            'IssueID': startrek_form.issue_id,
            'IssueKey': startrek_form.issue_key,
            'Authorization': f'OAuth {st_secret}',
        },
        data=startrek_form.spec_text)


def startrek_cancel_task(startrek_form, st_secret=ST_SECRET):
    return http_request(
        'POST', SCHEDULER + '/tasks/startrek/1.x/cancel',
        params={
            'ticket_id': startrek_form.issue_key,
        },
        headers={
            'Host': HOST,
            'UserID': startrek_form.user_id,
            'Authorization': f'OAuth {st_secret}',
        })


def get_roles_info():
    return http_request_json(
        'GET', SCHEDULER + '/tasks/1.x/roles/info/',
        headers=HEADERS)


def get_all_roles():
    return http_request_json(
        'GET', SCHEDULER + '/tasks/1.x/roles/get-all-roles/',
        headers=HEADERS)


def add_role(personal_role):
    return http_request_json(
        'POST', SCHEDULER + '/tasks/1.x/roles/add-role/',
        data={
            'login': personal_role.login,
            'role': json.dumps(personal_role.role),
            'path': personal_role.path,
            'fields': json.dumps(personal_role.fields),
        },
        headers=HEADERS)


def remove_role(personal_role):
    return http_request_json(
        'POST', SCHEDULER + '/tasks/1.x/roles/remove-role/',
        data={
            'login': personal_role.login,
            'role': json.dumps(personal_role.role),
            'path': personal_role.path,
            'fields': json.dumps(personal_role.fields),
        },
        headers=HEADERS)


def get_file(file_id, user, head_id=None):
    params = {
        'file_id': file_id
    }
    if head_id is not None:
        params['head_id'] = head_id
    return http_request(
        'GET', SCHEDULER + '/reports/1.x/get_file',
        params=params,
        headers=HEADERS,
        cookies={'Session_id': user.session_id})


def detach(check_exit_code=True, timeout=5):
    docker.exec('auto-remote-tasks-scheduler', ['yacare', 'detach', 'all'],
                check_exit_code=check_exit_code, timeout=timeout)


def attach(check_exit_code=True, timeout=5):
    docker.exec('auto-remote-tasks-scheduler', ['yacare', 'attach', 'all'],
                check_exit_code=check_exit_code, timeout=timeout)
