# flake8: noqa: E501

from wiki.pages.utils.comments import strip_body_from_html
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase

test_comments = [
    (
        """1. ок<br />
2. ок<br />
3. а&nbsp;может тогда сделать &laquo;3 часа заявок&raquo;, объединив близкие часовые пояса &ndash; напр., группа 1 &ndash; ЕКТ+НСК, группа 2 &ndash; РНД+КЗН+СПБ, группа 3 &ndash; Украина. Это&nbsp;поможет оптимизироваться данный процесс и&nbsp;главное, использовать весь доступный ресурс, что&nbsp;очень важно для&nbsp;нас в&nbsp;условиях сильной ограниченности квот.""",
        """1. ок
2. ок
3. а может тогда сделать «3 часа заявок», объединив близкие часовые пояса – напр., группа 1 – ЕКТ+НСК, группа 2 – РНД+КЗН+СПБ, группа 3 – Украина. Это поможет оптимизироваться данный процесс и главное, использовать весь доступный ресурс, что очень важно для нас в условиях сильной ограниченности квот.""",
    ),
    (
        """<div class="b-page-dbclick_oh_no"><!--notypo--><span class="b-page-code g-js" onclick="return {name: 'b-page-code'}"><code class="b-page-code__code b-page-code__code_inline_on no-highlight">((mailto:dns-dhcp@yandex-team.ru?subject=***.yandex.ru:DNS&amp;body=Привет.%20%0DПрошу%20зарегистрировать%20в%20прямой%20и%20обратной%20зоне%20%0D***.yandex.ru%20-%20***%20%0DСпасибо.%20%0D%20%0Dsysadmin запрос на добавление записи в DNS))</code></span><!--/notypo--></div>""",
        """((mailto:dns-dhcp@yandex-team.ru?subject=***.yandex.ru:DNS&body=Привет.%20%0DПрошу%20зарегистрировать%20в%20прямой%20и%20обратной%20зоне%20%0D***.yandex.ru%20-%20***%20%0DСпасибо.%20%0D%20%0Dsysadmin запрос на добавление записи в DNS))""",
    ),
    (
        """<div class="email1 email-odd">&gt; Вся&nbsp;информация о&nbsp;партнерах сведена в&nbsp;один файл на&nbsp;svn</div><br />
Как&nbsp;мне добавить урлы для&nbsp;паспорта? Для&nbsp;РФ&nbsp;&ndash; одни, для&nbsp;турции &ndash; другие?""",
        """> Вся информация о партнерах сведена в один файл на svn
Как мне добавить урлы для паспорта? Для РФ – одни, для турции – другие?""",
    ),
    (
        """Бага.<br />
<br />
<br />
Результат:<br />
Неправильный ответ от&nbsp;Джиры""",
        """Бага.


Результат:
Неправильный ответ от Джиры""",
    ),
    ("""А&nbsp;где же&nbsp;кнопочка &laquo;подписаться&raquo;?""", """А где же кнопочка «подписаться»?"""),
    ("""сделал сам""", """сделал сам"""),
    (
        """$ /etc/init.d/clustermaster start не&nbsp;работает:<br />
<br />
onotole@mfas004:$ sudo /etc/init.d/clustermaster start<br />
Usage: /etc/init.d/clustermaster {start|stop|status|restart|force-reload} {ALL|&lt;instance&gt;} [master|worker]<br />
Examples:<br />
<div class="indent"><div class="indent"><div class="indent"><div class="indent">/etc/init.d/clustermaster start ALL&nbsp;&ndash; start all&nbsp;instances<br />
/etc/init.d/clustermaster restart mascot worker &ndash; restart worker of&nbsp;mascot instance</div></div></div></div>""",
        """$ /etc/init.d/clustermaster start не работает:

onotole@mfas004:$ sudo /etc/init.d/clustermaster start
Usage: /etc/init.d/clustermaster {start|stop|status|restart|force-reload} {ALL|<instance>} [master|worker]
Examples:
/etc/init.d/clustermaster start ALL – start all instances
/etc/init.d/clustermaster restart mascot worker – restart worker of mascot instance""",
    ),
    (
        """(по итогам обновления TB&nbsp;на маке 10.0.0esr -&gt; 10.0.7esr)<br />
а&nbsp;если параметра нет, то&nbsp;создать его&nbsp;и&nbsp;потом установить в&nbsp;false :)""",
        """(по итогам обновления TB на маке 10.0.0esr -> 10.0.7esr)
а если параметра нет, то создать его и потом установить в false :)""",
    ),
    (
        """\x01linklhttp://www.youtube.com/watch?v=pH-a9j0UXfM ==\x01linkr""",
        """linklhttp://www.youtube.com/watch?v=pH-a9j0UXfM ==linkr""",
    ),
    (
        """GET&nbsp;\x01linkl/DljaSapportov/Platons/scripts/userjs/SmartLinks/.files/informers.1162.js ==\x01linkr 404 (NOT FOUND)<br />
:(""",
        """GET\xa0linkl/DljaSapportov/Platons/scripts/userjs/SmartLinks/.files/informers.1162.js ==linkr 404 (NOT FOUND)
:(""",
    ),
    ('Fri&nbsp;Jun 01 16:35:43 MSK&nbsp;2012', 'Fri\xa0Jun 01 16:35:43 MSK\xa02012'),
]


class StripCommentsTest(BaseTestCase):
    """
    Test stripping comments body from html tags
    """

    def test_strip(self):

        for comment, etalon in test_comments:

            stripped = strip_body_from_html(comment)
            self.assertEqual(stripped, etalon)
