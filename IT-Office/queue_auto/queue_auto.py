import datetime
import json
import logging
import re
import time

import requests
import winrm
from source.config import (OAUTH_TOOLS,
                    VM_ZOO_OFR_USER,
                    VM_ZOO_OFR_PWD,
                    st_client)

from requests.packages.urllib3.exceptions import InsecureRequestWarning
from startrek_client import Startrek, exceptions
from source.utils import EmailLogger, abc_help_logins, exc_thread_wrapper
from source.prn_task_marking import *
from source.dismissal_miracle_folder import *

requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

userag = 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.12785 YaBrowser/13.12.1599.12785 Safari/537.36'

client = Startrek(useragent=userag,
                  base_url='https://st-api.yandex-team.ru',
                  token=OAUTH_TOOLS)
AUTH_HEADERS_TOOLS = {
            "Authorization": 'OAuth {}'.format(OAUTH_TOOLS)
        }

logger = logging.getLogger(__name__)

def bu_and_office_setter():
    helpLogins = abc_help_logins()
    if helpLogins == False:
        logger.warning("Failed to get help's logins")
        raise TypeError("Failed to get help's logins")

    issues = client.issues.find('Queue: HDRFS AND "Current Office": empty() AND Created: today()')
    for issue in issues:
        login = issue.createdBy.id

        url = "https://staff-api.yandex-team.ru/v3/persons?_pretty=1&_one=1&login={login}&_fields=official.organization.name,location.office.id".format(
            login=login)
        r = requests.get(url, headers={
            "Authorization": 'OAuth {}'.format(OAUTH_TOOLS)
        })
        creator = [component.updatedBy.id for component in issue.changelog.get_all(type='IssueCreated,IssueCloned')][0]
        if not issue.channel and issue.emailCreatedBy:
            channel = 'Separator'
        elif len(issue.channel) == 0 and creator not in helpLogins:
            channel = 'ST Front'
        else:
            channel = issue.channel

        issue.update(
            currentOffice={'id': r.json()['location']['office']['id']},
            channel=channel
        )
        
        if ':PRINTER_DOWN' in issue.summary and issue.createdBy.id == 'robot-juggling':
            set_attributes_for_printer_mon(issue)


class VirtualZooOFRCreator():
    def __init__(self):
        self.abc_group_text = "Пользователь {login} добавлен в ABC-группу"
        self.success_text_zoo4 = 'Доступ к зоопарку виртуальных браузеров выдан, инструкцию по подключению: ((https://wiki.yandex-team.ru/dljaadminov/winadmin/zoovirtuals/ можно прочитать здесь)).\n' \
                                 'Пожалуйста, подтверди решение этого тикета, нажав кнопку "Подтверждаю"\n' \
                                 'Если у тебя есть какие-то вопросы, напиши их в комментариях и нажми "Не подтверждаю" (мы вернемся к решению).'

        self.success_text_zoo3 = 'Доступ к зоопарку виртуальных браузеров выдан, инструкцию по подключению: ((https://wiki.yandex-team.ru/dljaadminov/winadmin/zoo3virtuals/ можно прочитать здесь)).\n' \
                                 'Пожалуйста, подтверди решение этого тикета, нажав кнопку "Подтверждаю"\n' \
                                 'Если у тебя есть какие-то вопросы, напиши их в комментариях и нажми "Не подтверждаю" (мы вернемся к решению).'
        self.zoo_user = VM_ZOO_OFR_USER
        self.zoo_pwd = VM_ZOO_OFR_PWD
        self.st_delay = 1

    def __create_local_zoo(self, user, issue):
        VM_LIST = [
            "zoo3-vm3.yandex.ru",
            "zoo3-vm4.yandex.ru",
            "zoo3-vm5.yandex.ru",
            "zoo3-vm6.yandex.ru",
            "zoo3-vm7.yandex.ru",
            "zoo3-vm8.yandex.ru",
            "zoo3-vm9.yandex.ru",
            "zoo1-3-vm10.yandex-team.ru"
        ]
        errors_string = ''
        is_failed = False

        for vm in VM_LIST:
            try:
                s = winrm.Session(vm, auth=(self.zoo_user, self.zoo_pwd))
                r = s.run_cmd(
                    'net user {user} 7777777 /logonpasswordchg:yes /ADD && net localgroup "Remote Desktop Users" {user} /ADD && net localgroup "Users" {user} /DELETE'.format(
                        user=user
                    ))
            except Exception as error:
                log_string = ('Problem with creating user {} on vm {} with error {}\n'.format(
                    user, vm , error.__str__()
                ))
                logger.error(log_string)

            code = r.status_code
            message = r.std_out
            if int(code) == 0:
                log_string = 'User: {user} on computer: {vm} created succesfuly\n'.format(
                    user=user,
                    vm=vm
                )
                logger.info(log_string)
            elif int(code) == 2:
                log_string = 'User: {user} on computer:{vm} already exist\n'.format(
                    user=user,
                    vm=vm
                )
                logger.info(log_string)
            else:
                log_string = 'Error on computer: {vm} / message: {message} code: {code}'.format(
                        vm=vm,
                        message=message,
                        code=code
                    )
                errors_string += log_string
                logger.warning(log_string)
                is_failed = True

            if is_failed:
                issue.comments.create(text = errors_string,
                                      summonees = ['waltz'])
                time.sleep(self.st_delay)

        return is_failed

    def __create_abc_group(self, user, issue):
        url = "https://idm-api.yandex-team.ru/api/v1/rolerequests/"
        data = {"user": user,
                "system": "staff",
                "path": "/groups/97598/member/"}
        response_data = requests.post(url,
                                      data=json.dumps(data),
                                      headers=AUTH_HEADERS_TOOLS)
        error_code = response_data.json().get('error_code')
        for comment in issue.comments:
            if re.match('Пользователь .* успешно добавлен в IDM группу',comment.text): return True

        if not error_code or error_code == 'CONFLICT':
            issue.comments.create(
                text="Пользователь {} успешно добавлен в IDM группу".format(user)
            )
            logger.info('Success added {} to IDM group'.format(user))
            time.sleep(self.st_delay)
            return True
        else:
            issue.comments.create(
                text="При добавлении пользователя {} в IDM группу возникли ошибки {}".format(
                    user, response_data.json().get('errors'))
            )
            time.sleep(self.st_delay)
            logger.error('Errors, when added IDM group {}'.format(response_data.json().get('message')))
            return False

    def ofr_zoo_created(self):
        issues = client.issues.find('Queue: HDRFS AND Tags: virtualZooOFR AND Resolution: empty() AND Status:! "В работе"')
        for issue in issues:
            author = issue.createdBy.id
            issue.transitions['inProgress'].execute()
            time.sleep(self.st_delay)
            if re.findall('\*\*Zoo type\:\*\* не доменный', issue.description):
                idm_status = self.__create_abc_group(author, issue)
                vms_status = self.__create_local_zoo(author, issue)
                if idm_status and not vms_status:
                    issue.transitions['resolved'].execute(comment=self.success_text_zoo3, resolution='fixed')
                else:
                    issue.update(
                        assignee=None
                    )
            else:
                idm_status = self.__create_abc_group(author, issue)
                if idm_status:
                    issue.transitions['resolved'].execute(comment=self.success_text_zoo4, resolution='fixed')
                else:
                    issue.update(
                        assignee=None
                    )

class UserSideRobot():
    def __init__(self):

        self.FIRST_TEXT = "кто:{username}, привет, нам все еще нужен ответ от тебя\
         в этом тикете, прокомментируй, пожалуйста. Если мы не получим от тебя\
         ответ в течение двух недель, тикет будет закрыт автоматически."
        self.SECOND_TEXT = "кто:{username}, ответь, пожалуйста."
        self.THIRD_TEXT = "В этом тикете нет уже более двух недель нет ответа\
         от автора, поэтому я перевожу его в статус 'Решен'. кто:{username},\
          пожалуйста, подтверди решение твоего вопроса."

        self.status_id = '400'
        self.config = {
            "first":{
                "text":self.FIRST_TEXT,
                "spend_days": 3,
                "comment_days": 3,
                "process_func": self._first_hop_function,
                "old_texts": [self.FIRST_TEXT, self.SECOND_TEXT]
            },
            "second": {
                "text": self.SECOND_TEXT,
                "spend_days": 6,
                "comment_days": 3,
                "process_func": self._second_hop_function,
                "old_texts": [self.SECOND_TEXT]
            },
            "third": {
                "text": self.THIRD_TEXT,
                "spend_days": 7,
                "comment_days": 8,
                "process_func": self._third_hop_function,
                "old_texts": [self.THIRD_TEXT]
            }
        }

    def _first_hop_function(self, issue, author):
        logger.info('Apply first hop to issue {}'.format(issue.key))
        issue.comments.create(text=self.FIRST_TEXT.format(username=author),
                              summonees=author)
        time.sleep(1)
        issue.transitions['onTheSideOfUser'].execute()


    def _second_hop_function(self, issue, author):
        logger.info('Apply second hop to issue {}'.format(issue.key))
        issue.comments.create(text=self.SECOND_TEXT.format(username=author),
                              summonees=author)
        time.sleep(1)
        issue.transitions['onTheSideOfUser'].execute()

    def _third_hop_function(self, issue, author):
        MOSCOW_IDS = ["60938","53060","100000","100240"]
        if not [x for x in issue.fixVersions if x["id"] in MOSCOW_IDS]:
            logger.info('Apply third hop to issue {}'.format(issue.key))
            issue.comments.create(text=self.THIRD_TEXT.format(username=author))
            time.sleep(1)
            issue.transitions['resolved'].execute()
        else:
            issue.comments.create(text=self.FIRST_TEXT.format(username=author),
                                  summonees=[author,'agrebenyuk'])
            time.sleep(1)
            issue.transitions['onTheSideOfUser'].execute()

    def _get_change_days_by_id(self, id, issue):
        log = issue.changelog
        last_change_date = 0
        for item in log:
            status_changes = [x for x in item.fields if
                              x.get('field').id == 'status']

            if status_changes and status_changes[0].get('to').id == id:
                last_change_date = datetime.datetime.strptime(
                    item.updatedAt[0:19], '%Y-%m-%dT%H:%M:%S')

        if last_change_date:
            return abs((datetime.datetime.now() - last_change_date).days)
        return False

    def _get_last_comment_text_date(self, issue):
        comments = list(issue.comments)

        if comments:
            comment = comments[-1]
            text = comment.text
            last_change_date = datetime.datetime.strptime(
                comment.updatedAt[0:19], '%Y-%m-%dT%H:%M:%S')
            diff = abs((datetime.datetime.now() - last_change_date).days)
            return (diff, text)
        else:
            return (10, False)

    def _process_issue(self, issue):
        logger.info("Start processing issue : {}".format(issue.key))
        author = issue.createdBy.id
        spend_days = self._get_change_days_by_id(self.status_id, issue)
        comment_days, comment_text = self._get_last_comment_text_date(issue)

        logger.info("Parse info: author - {author}| spend_days - {spend_days}|\
         comment_days - {comment_days} | comment_text - {comment_text}".format(
            author = author, spend_days = spend_days,
            comment_days = comment_days, comment_text = comment_text
        ))

        if not spend_days: return

        for conf_item in self.config:
            logger.info('Try acceps config : {}'.format(conf_item))
            current_config = self.config[conf_item]
            if spend_days >= current_config["spend_days"] and \
                comment_days >= current_config["comment_days"] and \
                comment_text not in [x.format(username=author) for x in current_config["old_texts"]]:
                target_function = current_config["process_func"]
                target_function(issue, author)
                break

    def process(self):
        issues = st_client.issues.find('Queue: HDRFS AND Status: "На стороне пользователя" AND Updated: 01.01.2001..today()-4d')
        for count, issue in enumerate(issues):
            logger.info('OnTheUserSide start ticket {}'.format(issue.key))
            self._process_issue(issue)

def _is_ticket_appl_for_close(issue):
    log = issue.changelog
    last_change_date = 0
    for item in log:
        status_changes = [x for x in item.fields if
                          x.get('field').id == 'status']

        if status_changes \
            and status_changes[0].get('to').id == '2':

            last_change_date = datetime.datetime.strptime(
                item.updatedAt[0:19], '%Y-%m-%dT%H:%M:%S')

    if last_change_date and abs(
            (datetime.datetime.now() - last_change_date).days) > 14:
        return True
    return False

def check_zoo():
    logger.info('Start ZOO Creation')
    VirtualZooOFRCreator().ofr_zoo_created()

def check_office_and_bu():
    logger.info('StartOfficeAndBU')
    check_count = 3
    while check_count:
        try:
            bu_and_office_setter()
            check_count = False
        except (exceptions.Conflict, TypeError):
            time.sleep(0.5)
            check_count -= 1
            if check_count == 0:
                logger.error('StartOfficeAndBU not completed')

@exc_thread_wrapper
def main_closing_resolve_tickets():
    issues = client.issues.find('Queue: HDRFS AND (type: serviceRequest or type: incident) AND Status: Решен AND Updated: 01.01.2001..today()-14d')
    comment = "Тикет закрываю. Коллеги, если у вас остались какие-либо вопросы, пожалуйста, создайте новый тикет, написав на рассылку help@yandex-team.ru. В этот тикет писать уже не стоит, так как мы не увидим комментарии."
    for issue in issues:
        if _is_ticket_appl_for_close(issue):
            issue.transitions['closed'].execute(comment=comment,
                                               resolution='fixed')
            logger.info('Successfuly closed ticket: {}'.format(issue.key))

@exc_thread_wrapper
def mark_printer_down_tasks():
    issues = client.issues.find('Queue: HDRFS AND Author: "robot-juggling@" AND Created: > today() - "3d" AND components: ! 50062 AND tags: ! "PRN_UsePrinter_MonDown"')
    if issues:
        for issue in issues:
            set_attributes_for_printer_mon(issue)
            logger.info('Try marking printer_down task: {}'.format(issue.key))
    else:
        logger.info("Today not find task about printer_down")

@exc_thread_wrapper
def check_users_folder_for_dismiss():
    issues = client.issues.find('Queue: HDRFS AND Author: "zomb-prj-191@" AND Description: "Что делать с файлами" AND Created: today() AND components: ! 31135 AND tags: ! "Subservice:Dismissal_PersonalFile"')
    if issues:
        for issue in issues:
            processing_task_miracle_folder(issue)
            logger.info('Try check user folder, task: {}'.format(issue.key))
    else:
        logger.info("Today not find task about dismiss user and miracle folder")

@exc_thread_wrapper
def main_userside_robot():
    logger.info('Start usersiderobot')
    UserSideRobot().process()
