import datetime
import logging
import time
from source.config import st_client

logger = logging.getLogger(__name__)


class QueueHDRFSRobot():
    def _get_last_comment_text_and_date_by_issue(self, issue):
        comments = list(issue.comments)

        if comments:
            comment = comments[-1]
            text = comment.text
            last_change_date = datetime.datetime.strptime(
                comment.updatedAt[0:19], '%Y-%m-%dT%H:%M:%S')
            diff = abs((datetime.datetime.now() - last_change_date).days)
            return (True, diff, text)
        else:
            return (False , 0, '')

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

class PreparedForIssueRobot(QueueHDRFSRobot):
    def __init__(self):

        self.FIRST_TEXT = "кто:{username}, привет, ждем тебя для получения и" \
                          " донастройки профиля оборудования."
        self.SECOND_TEXT = "кто:{username}, когда сможешь прийти в HelpDesk?"
        self.THIRD_TEXT = "2help: этот тикет находится в статусе 'Подготовлено " \
                          "к выдаче' более 3 недель, нужно проверить " \
                          "актуальность задачи."

        self.status_id = '507'
        self.config = {
            "first":{
                "text":self.FIRST_TEXT,
                "spend_days": 14,
                "comment_days": 7,
                "process_func": self._first_hop_function,
                "old_texts": [self.FIRST_TEXT, self.SECOND_TEXT, self.THIRD_TEXT]
            },
            "second": {
                "text": self.SECOND_TEXT,
                "spend_days": 20,
                "comment_days": 7,
                "process_func": self._second_hop_function,
                "old_texts": [self.SECOND_TEXT, self.THIRD_TEXT]
            },
            "third": {
                "text": self.THIRD_TEXT,
                "spend_days": 27,
                "comment_days": 7,
                "process_func": self._third_hop_function,
                "old_texts": [self.THIRD_TEXT]
            }
        }

    def _first_hop_function(self, **kwargs):
        issue = kwargs.get("issue")
        author = kwargs.get("author")

        logger.info('Apply first hop to issue {}'.format(issue.key))
        issue.comments.create(text=self.FIRST_TEXT.format(username=author),
                              summonees=author)
        time.sleep(1)

    def _second_hop_function(self, **kwargs):
        issue = kwargs.get("issue")
        author = kwargs.get("author")

        logger.info('Apply second hop to issue {}'.format(issue.key))
        issue.comments.create(text=self.SECOND_TEXT.format(username=author),
                              summonees=author)
        time.sleep(1)

    def _third_hop_function(self, **kwargs):
        issue = kwargs.get("issue")
        author = kwargs.get("author")
        pre_assignee = kwargs.get("assignee")
        assignee = pre_assignee if pre_assignee else 'agrebenyuk'

        logger.info('Apply third hop to issue {}'.format(issue.key))

        issue.comments.create(text=self.THIRD_TEXT.format(username=author),
                              summonees=assignee)
        time.sleep(1)


    def _process_issue(self, issue):
            logger.info("Start processing issue : {}".format(issue.key))
            author = issue.createdBy.id
            assignee = issue.assignee.id if issue.assignee else None

            spend_days = self._get_change_days_by_id(self.status_id, issue)
            result, comment_days, comment_text = self._get_last_comment_text_and_date_by_issue(
                issue)

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
                    target_function(issue=issue,
                                    author=author,
                                    assignee=assignee)
                    break

    def process(self):
        issues = st_client.issues.find('Queue: HDRFS AND Status: "Подготовлено к выдаче"')
        for issue in issues:
            logger.info('preparedForIssue script started {}'.format(issue.key))
            self._process_issue(issue)


class WaitingForEquipmentRobot(QueueHDRFSRobot):
    def __init__(self):

        self.FIRST_TEXT = "кто:{username}, привет, напоминаем, что тебе нужно " \
                          "сдать оборудование в HelpDesk."
        self.SECOND_TEXT = "кто:{username}, привет, повторно напоминаем, что тебе нужно " \
                          "сдать оборудование в HelpDesk."
        self.THIRD_TEXT = "2help: этот тикет находится в статусе " \
                          "'Ждем оборудование' более 3 недель, нужно " \
                          "проверить актуальность задачи."


        self.status_id = '493'
        self.config = {
            "first":{
                "text":self.FIRST_TEXT,
                "spend_days": 3,
                "comment_days": 0,
                "process_func": self._first_hop_function,
                "old_texts": [self.FIRST_TEXT, self.SECOND_TEXT, self.THIRD_TEXT]
            },
            "second": {
                "text": self.SECOND_TEXT,
                "spend_days": 0,
                "comment_days": 7,
                "process_func": self._second_hop_function,
                "old_texts": [self.SECOND_TEXT, self.THIRD_TEXT]
            },
            "third": {
                "text": self.THIRD_TEXT,
                "spend_days": 0,
                "comment_days": 7,
                "process_func": self._third_hop_function,
                "old_texts": [self.THIRD_TEXT]
            }
        }

    def _first_hop_function(self, **kwargs):
        issue = kwargs.get("issue")
        author = kwargs.get("author")

        logger.info('Apply first hop to issue {}'.format(issue.key))
        issue.update(
            replyBefore=(
            datetime.datetime.now() + datetime.timedelta(days=7)).strftime(
                '%Y-%m-%dT%H:%M:%S')
        )
        issue.comments.create(text=self.FIRST_TEXT.format(username=author),
                              summonees=author)
        time.sleep(1)

    def _second_hop_function(self, **kwargs):
        issue = kwargs.get("issue")
        author = kwargs.get("author")

        logger.info('Apply second hop to issue {}'.format(issue.key))
        issue.update(
            replyBefore=(
                datetime.datetime.now() + datetime.timedelta(days=7)).strftime(
                '%Y-%m-%dT%H:%M:%S')
        )
        issue.comments.create(text=self.SECOND_TEXT.format(username=author),
                              summonees=author)
        time.sleep(1)

    def _third_hop_function(self, **kwargs):
        issue = kwargs.get("issue")
        summonees = kwargs.get("summonees")


        logger.info('Apply third hop to issue {}'.format(issue.key))

        issue.comments.create(text=self.THIRD_TEXT,
                              summonees=summonees)
        time.sleep(1)

    def _generate_summon_list(self, issue):
        summon_list = []
        assignee = issue.assignee.id if hasattr(issue,"assignee") and issue.assignee else None
        prepared = issue.prepared[0].id if hasattr(issue,"prepared") and issue.prepared else None
        gave = issue.gave[0].id if hasattr(issue,"gave") and issue.gave else None

        for person in [assignee, prepared, gave]:
            if person and person not in summon_list: summon_list.append(person)

        return summon_list if summon_list else ["agrebenyuk"]

    def _process_issue(self, issue):
            logger.info("Start processing issue : {}".format(issue.key))
            author = issue.createdBy.id

            spend_days = self._get_change_days_by_id(self.status_id, issue)
            result, comment_days, comment_text = self._get_last_comment_text_and_date_by_issue(
                issue)

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
                    target_function(issue=issue,
                                    author=author,
                                    summonees=self._generate_summon_list(issue))
                    break

    def process(self):
        issues = st_client.issues.find('Queue: HDRFS AND Status: "Ждем оборудование" AND ("Postponed Till": empty()  OR "Postponed Till": <= now())')
        for issue in issues:
            logger.info('WaitingForEquipment script started {}'.format(issue.key))
            self._process_issue(issue)

def main_for_equipment_pinging():
    logger.info("Start Equipment robots")
    PreparedForIssueRobot().process()
    WaitingForEquipmentRobot().process()