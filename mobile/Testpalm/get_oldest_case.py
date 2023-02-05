from tp_api_client.tp_api_client import *
from datetime import datetime

'''
Этот скрипт выводит тексткейс, который редактировали наибольшее время назад
Для запуска  необходимо добавить токен Тестпалма и указать проект, в рамках которого ищем кейс
'''

TP_TOKEN = 'AQAD-ХХХХХХХХХХХХХХХХХХХ'  # сюда добавить токен TestPalm
tp_project = 'adisk'  # adisk | idisk | mobtela | mobtel


class MyTestPalm:
    def __init__(self, ):
        self.testpalm_project = tp_project
        self.host = 'https://testpalm-api.yandex-team.ru'
        self.headers_testpalm = {'Authorization': 'OAuth {}'.format(TP_TOKEN),
                                 'Content-Type': 'application/json'}

    def getTickets(self):
        r = requests.get(
            'https://testpalm-api.yandex-team.ru/testcases/{}'.format(tp_project),
            headers=self.headers_testpalm,
            verify=True,
            timeout=9999,
            # params={'limit': 10} # количество тикетов, которые нужно вернуть (выключена, чтобы возвразать все)
        )
        return r.json()

    def ticketsSorting(self, tickets):
        lastEdited = 2000000000000
        for ticket in tickets:
            if ticket['lastModifiedTime'] < lastEdited:
                lastEdited = ticket['lastModifiedTime']
                oldestTicket = ticket
        return oldestTicket

    def ticketOutput(self, ticket):
        lastModifiedTime = datetime.utcfromtimestamp(round(ticket['lastModifiedTime']/1000))
        print('\n\x1b[1;42;30mСамый старый тесткейс:\x1b[0m')
        print('Дата последнего редактирование: ' + lastModifiedTime.strftime('%d.%m.%Y'))
        print('Правки вносил(а): ' + 'https://staff.yandex-team.ru/' + ticket['modifiedBy'])
        print('Название: ' + '\x1b[1m' + ticket['name'] + '\x1b[0m')
        print('Статус: ' + ticket['status'])
        print('Количество прогонов тесткейса: ' + str(ticket['stats']['totalRunCount']))
        print('Ссылка:  https://testpalm.yandex-team.ru/testcase/' + tp_project + '-' + str(ticket['id']))


mtp = MyTestPalm()
tickets = mtp.getTickets()
data = mtp.ticketsSorting(tickets)
mtp.ticketOutput(data)
