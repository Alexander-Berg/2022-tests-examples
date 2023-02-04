import pytest
from source.dismissal_pinger import *


def get_yatest_user():
    user = User('yatest')
    user.fix_version = {'fixVersions': [{'id': 53070}]}
    user.head = 'testhead'
    user.table = 9999999
    user.business_unit = 'Яндекс'
    user.dismissal_date = '1999-01-01'
    user.current_office = 1
    user.hw_dictionary = {
        'may_take_by_loaders_template': [],
        'mobile_template': [],
        'self_delivery_template': [],
        'buying_template': [],
        'zmb_and_plasma_template': []
    }
    user.hardware_count = 0

    return user


def get_notyatest_user():
    user = User('notyatest')
    user.fix_version = {'fixVersions': [{'id': 53054}]}
    user.head = 'testhead'
    user.table = 9999999
    user.business_unit = 'Команда поддержки бизнеса'
    user.dismissal_date = '1999-01-01'
    user.current_office = 2
    user.hw_dictionary = {
        'may_take_by_loaders_template': [],
        'mobile_template': [],
        'self_delivery_template': [],
        'buying_template': [],
        'zmb_and_plasma_template': []
    }
    user.hardware_count = 0

    return user


def need_review_test_not_exist_user():
    user = User('yatestuser')
    user.get_info_from_staff()


def need_review_bad_test_get_dismissal_date():
    issue_body1 = '<#<html><head></head><body><p><span style="color:#1f497d"></span></p><p>Добрый день!</p>' \
                 '<p>В системе ORACLE зарегистрирована информация о планируемом увольнении сотрудника:</p>' \
                 '<p>ФИО сотрудника: <b>Лесонен Полина Александровна</b><br>Логин: <b>kaktus</b>' \
                 '<br>Подразделение: <b>Яндекс / Outstaff / Яндекс.Такси (Outstaff) / ' \
                 'Операционное управление Яндекс.Такси (Поддержка бизнеса) / Отдел поддержки Яндекс.Такси и Убер ' \
                 '(Поддержка бизнеса) / Международная поддержка (Outstaff) / ' \
                 'Служба операционного управления международной поддержкой (Поддержка бизнеса) / ' \
                 'Сектор международной поддержки пользователей (Поддержка бизнеса) / ' \
                 'Группа поддержки пользователей в Финляндии (Поддержка бизнеса)</b><br>Должность: ' \
                 '<b>Младший специалист международной поддержки.</b><br>Офис: <b>Дистанционный работник</b>' \
                 '<br>Юридическое лицо: <b>ООО "Яндекс.Такси"</b><br>Дата планируемого увольнения: ' \
                 '<b>03.06.2019</b></p><p></p><table cellpadding="5" style="border-collapse:collapse;color:#1f497d">' \
                 '<tbody><tr style="background-color:#6590c4;color:#ffffff"><td align="center" style="border:1px solid ' \
                 '#1f497d">Инвентарный номер</td><td align="center" style="border:1px solid #1f497d">НП</td></tr><tr>' \
                 '<td style="border:1px solid #1f497d">L10007567</td>' \
                 '<td style="border:1px solid #1f497d">SOFT&gt;SOFT&gt;CISCO&gt;L-MIGE-UCM-UWL-PRO</td></tr></tbody>' \
                 '</table><p></p><p>С уважением,<br>Ваш ORACLE OEBS</p> <br> <br> <p></p> </body></html>#>'
    issue_body2 = ''
    user = get_notyatest_user()
    user.get_dismissal_date(issue_body1)
    assert user.dismissal_date == '2019-06-03'
    user.dismissal_date = ''
    user.get_dismissal_date(issue_body2)
    assert user.dismissal_date == ''


def need_review_test_get_hw_for_send():
    user = get_notyatest_user()
    user.hw_dictionary['test1'] = [['inv1', 'vendor1', 'model1'], ['inv2', 'vendor2', 'model2']]
    answer = user.get_hw_for_send('test1')
    assert answer == 'inv1 vendor1 model1\r\ninv2 vendor2 model2\r\n'
    user.hw_dictionary['test2'] = ''
    answer = user.get_hw_for_send('test2')
    assert answer == ''


def need_review_test_generate_mess():
    yauser = get_yatest_user()
    notyauser = get_notyatest_user()
    homework_template = "Привет, сотрудник может сдать свой ноутбук самостоятельно в любом из офисов указаных " \
                        "под катом \"График работы\" ((https://wiki.yandex-team.ru/diy/#grafikraboty здесь)), " \
                        "если пользователь этого сделать не может, пожалуйста, заполни " \
                        "((https://forms.yandex-team.ru/surveys/1308/ форму на отправку оборудования)) " \
                        "в московский офис и привяжи тикет к текущему." \
                        "\r\nЕсли у сотрудника был токен, его так же необходимо отправить.\r\n" \
                        "Как заполнять поля:\r\n" \
                        "ЦФО: указать бизнес-юнит запрашивающего отправку.\r\n" \
                        "ФИО получателя, название организации: HelpDesk.\r\n" \
                        "Адрес получателя: г. Москва, ул. Льва Толстого, 16.\r\n" \
                        "Телефон получателя: +79154476993.\r\n\r\nЕсли у вас остались вопросы, " \
                        "пожалуйста позвоните на номер 444 и скажите номер тикета HDRFS-2"
    issue = st_client.issues['HDRFS-2']
    assert generate_and_send_mess(issue, notyauser, True) == homework_template
    assert generate_and_send_mess(issue, yauser, True) == 'Ничего не числится за {}'.format(yauser.login)
    yauser.hw_dictionary['self_delivery_template'] = [['inv1', 'vendor1', 'model1'], ['inv2', 'vendor2', 'model2']]
    yauser.hardware_count = 2
    yauser_template = "Привет, напоминаем что в последний день до 18:00 нужно сдать рабочее " \
                      "оборудование в HelpDesk, в том числе зарядные устройства и переходники.\r\n" \
                      "Напоминаем, что перед сдачей нужно снять все наклейки c оборудования. На тебе " \
                      "числится следующее оборудование:\r\n" \
                      "<[inv1 vendor1 model1\r\n" \
                      "inv2 vendor2 model2\r\n" \
                      "]>\r\n" \
                      "Все оборудование нужно принести в HelpDesk, корзинку для этого можно взять в " \
                      "HelpDesk'е своего офиса." \
                      "\r\n\r\nЕсли у вас остались вопросы, пожалуйста позвоните на номер " \
                      "444 и скажите номер тикета HDRFS-2"
    assert generate_and_send_mess(issue, yauser, True) == yauser_template
