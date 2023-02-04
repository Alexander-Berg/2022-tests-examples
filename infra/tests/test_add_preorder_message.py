from infra.rtc.janitor.common import render_template


def test_description():
    message = render_template(
        'multi_dc_subticket.jinja',
        DC='vla',
        dc_destonation_project='rtc-vla-test',
        hosts=['101283388', '101287675', '101283408', '101283432', '101283448'],
        responsible='tester@',
        text_case='BODY'
        )
    assert message == '''
Связанный тикет на  ввод хостов в датацентр vla

Тип работ:
ввод/перемещение хостов в wall-e проект

Wall-e проект:
rtc-vla-test

<{Список ID серверов:
  101283388
  101287675
  101283408
  101283432
  101283448

}>

<{for_janitor

Тип работ:
ввод/перемещение хостов в wall-e проект

Wall-e проект:
rtc-vla-test

Демонтаж:
Нет

Список ID серверов:
101283388
101287675
101283408
101283432
101283448

Кто сдает хосты:
--- (tester@)
}>

'''
