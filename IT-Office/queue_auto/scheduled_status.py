import datetime
import logging
import re
from source.utils import exc_thread_wrapper

from startrek_client import exceptions

from source.config import (st_client)

logger = logging.getLogger(__name__)

def _modify_logic_tasks(issue):
    for link in issue.links:
        link_key = link.object.key
        if re.match('LOGIC.*',link_key):
            logic_ticket = st_client.issues[link.object.key]
            for cargo_link in logic_ticket.links:
                if re.match('CARGO.*', cargo_link.object.key):
                    issue.links.create(
                        relationship="depends on",
                        issue=cargo_link.object.key
                    )

def _get_transition(issue):
    transitions_dict = {'newComputer': 'confirmed'}
    exclTags = {"COMP_NewLaptopYa", "COMP_NewLaptopOutstaff", "COMP_NewLaptopExt"}
    if set(issue.tags).intersection(exclTags) and "52286" in [x.id for x in issue.components]:
        return "treated"
    return transitions_dict.get(issue.type.key, 'treated')

def scheduled_to_onwork():
    if datetime.datetime.now().hour not in range(9,22): return
    queue = 'Queue: HDRFS AND (Status: Запланирован OR Status: "Ждем выкупа"' \
            ' OR Status: "Отправлено" OR Status: "Ожидание поставки") ' \
            'AND ("Postponed Till": <= now() OR "Postponed Till": empty())'

    queue_test = queue + " AND Key: HDRFS-367014"
    issues = st_client.issues.find(queue_test)

    for issue in issues:
        try:
            _modify_logic_tasks(issue)
        except exceptions.UnprocessableEntity:
            pass

    for issue in issues:
        check = True
        try:
            for link in issue.links:
                if link.type.id == 'subtask' and link.status.id not in ['2','3']:
                    check = False
                if link.type.id == 'depends' and link.status.id not in ['2','3']:
                    check = False
        except exceptions.Forbidden:
            logger.warning('Problem with ticket : {}'.format(issue.key))
            pass

        if check:
            try:
                transition = issue.transitions[_get_transition(issue)]
                transition.execute(
                    assignee=issue.assignee.id if issue.assignee else None)

            except exceptions.NotFound:
                logger.warning('Cannot make transition in {}'.format(issue.key))

@exc_thread_wrapper
def scheduled():
    logger.info('Start Queue Auto')
    scheduled_to_onwork()
