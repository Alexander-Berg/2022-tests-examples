В данном документе содержится информация для разработчика касаемая аспектов тестирования Java-частей составляющих *систему мониторинга Solomon* (далее, *Solomon*).

# Glossary

* **DAL** (сокр. от *Data Access Layer*) - слой доступа к данным
* **ConfigDB** (сокр. от *Configuration DB*) - база данных используемая Solomon для хранения [объектов конфигурации](https://wiki.yandex-team.ru/users/snoop/Solomon-ConfDB/) (например, project, cluster, service, shard и т.п.)
* **ConfigDB DAL** - слой доступа к данным *ConfDB*
* **SBX** (сокр. от *Sandbox*) - [распределённая система выполнения задач общего назначения](https://wiki.yandex-team.ru/sandbox/)
* **SBX-ресурс** - ресурс *Sandbox*
* **SBX_ID** - идентификатор ресурса *Sandbox*
* **tgz-файл** - файл в виде tar-архива с компрессией gzip
* **YQL** (сокр. от *Yandex Query Language*) - разработанный внутри Яндекс [многоцелевой язык запросов](https://wiki.yandex-team.ru/yql/) с синтаксисом *SQL*
* **YQL UDF** (сокр. от *YQL's User Defined Function*) - определяемая пользователем функция YQL
* **YQL UDF-библиотека** - набор UDF-функций YQL реализуемых в виде [UDF-библиотеки](https://wiki.yandex-team.ru/yql/udf/list/)  

# ConfDB DAL testing

Solomon использует [Kikimr](https://wiki.yandex-team.ru/kikimr/) в качестве ConfDB. Ранее для этого использовалась MongoDB.

В тестах используется инстанс Kikimr разворачиваемый локально с помощью библиотеки [misc-local-kikimr](https://a.yandex-team.ru/arc/trunk/arcadia/iceberg/misc-local-kikimr).
Для активации локального Kikimr используется аннотация *EnableLocalKikimr*.
Пример конфигурирования ConfDB DAL для тестов в рамках фреймуорка *Spring* см. в классе [ru.yandex.solomon.core.db.dao.kikimr.KikimrDaoTestConfig](../solomon-core/ut/src/db/dao/kikimr/KikimrDaoTestConfig.java)

Такой подход (развертывание локального Kikimr) позволяет достичь сразу несколько целей:

 * независимость от доступности какого-либо внешнего к тестам Kikimr
 * автономный запуск тестов на машине разработчика не требующего доступа к сети Яндекс

Под капотом *misc-local-kikimr* развертывание локального Kikimr выполняется из содержимого SBX-ресурсов.
Содержимое SBX-ресурса зависит от типа конфигурирования локального Kikimr.
Существуют следующие варианты конфигурирования Kikimr:
 * ранее конфигурирование (т.н. предконфигурирование) **preconfigured Kikimr** - конфигурирование выполняется заранее с использованием по необходимости уже готовых для этого [скриптов](https://a.yandex-team.ru/arc/trunk/arcadia/iceberg/misc-local-kikimr/toolkit)
 * конфигурирование по месту с тонкой подстройкой Kikimr прямо в тестах (т.н. **tunable Kikimr**) - конфигурирование выполняется по мере запуска тестов

SBX-ресурс для *preconfigured Kikimr* содержит tgz-файл.
Внутри файл есть все необходимое для развертывания локального Kikimr включая (но не ограничиваясь этим):
 * скрипты для управления Kikimr (start/stop), 
 * файл для диска Kikimr,
 * файл драйвера Kikimr,
 * конфигурационные файлы Kikimr,
 * *опционально* UDF-библиотеки YQL.
В таком исполнении развертывание и запуск локального Kikimr является достаточно тривиальной задачей.
Рационале такого типа локального Kikimr - это сократить время на создание диска и т.п. необходимого для работы Kikimr.

SBX-ресурс для *tunable Kikimr* в минимальной версии представляет собой файл с драйвером Kikimr.
В таком исполнении локальный Kikimr конфигурируется в момент инициализации контекста тестирования и, как правило, включает в себя следующие шаги:
 * создание и форматирование диска Kikimr,
 * генерация файлов конфигурации Kikimr,
 * инициализация корневой схемы Kikimr. 
Отсюда видно, что каждый запуск новой версии Kikimr **требует чуть больше времени** нежели для *preconfigured Kikimr*.
Особенно это справедливо для OSX-платформы (запуск тестов на макбуке разработчика), так как, например, создание т.н. [sparse-диска](https://en.wikipedia.org/wiki/Sparse_file) на платформе OSX невозможно, что увеличивает время для инициализации окружения Kikimr.

Несмотря на все сказанное в тестах Solomon **используется**, по умолчанию, **tunable-версия** для развертывания локального Kikimr.
Для оптимизации работы используется подход запуска одного инстанса локального Kikimr на набор тестов (*test suite*).
Такой подход исключает необходимость повторной инициализации и конфигурирования локального Kikimr для каждого следующего теста.
Изолирование тестов по данным выполняется путем создания каждый раз новой таблицы по уникальному пути к таблице относительно корневой схемы Kikimr. 

Если в тестовых сценариях используются специфичные YQL-запросы к Kikimr-таблицам, в которых используются UDF-функции, то необходим специальный SBX-ресурс с включением в него соотв-х UDF-библиотек.
Включение UDF-библиотек наряду с драйвером Kikimr в SBX-ресурс требует упаковки этих файлов в tgz-файл.
В тестах Solomon используются UDF-функции (например, функции из *Json*, *String*, *Re2*), а папка содержащая UDF-библиотеки именуется как **udfs**.
Соотв-но, **требование к SBX-ресурсу для тестов Solomon** - это tgz-файл с с следующей структурой:
 * папка **udfs**
   * файлы с udf-библиотеками YQL
 * файл драйвера **kikimr**

В тестах Solomon аннотация используется tunable-версия Kikimr через аннотацию *EnableLocalKikimr*.
Конфигурирование выполняется в классе [ru.yandex.solomon.core.TunableLocalKikimrConfiguration](../solomon-core/ut/src/TunableLocalKikimrConfiguration.java).
Процедура создания SBX-ресурса для тестов использующих локальный Kikimr приводится [ниже](#creating-sandbox-resource-for-local-kikimr).

Конфигурирование локального Kikimr реализовано таким образом, что позволяет запускать тесты как с помощью стандартного ```ya make -A``` из командной строки, так и с помощью встроенного в *IntelliJ IDEA*  механизма для запуска JUnit-тестов.
Для использования всей мощи ```yatool``` рекомендуется загружать SBX-ресурс через специальный макрос **TEST_DATA** с указанием SBX_ID ресурса - см. его использование в [ya.make](../solomon-core/ut/ya.make).
К сожалению, ```yatool``` плохо интегрирован с IDE разработчика, а в частности с IDEA.
В связи с этим, чтобы SBX-ресурс резольвился при запуске тестов из IDEA необходимо явно при конфигурировании тестового контекста выполнить загрузку и инсталляцию локального Kikimr.
Для этого необходимо указать тот же *SBX_ID* в классе конфигурации, что и в *ya.make-файле*.
См. класс конфигурирования [ru.yandex.solomon.core.TunableLocalKikimrConfiguration](../solomon-core/ut/src/TunableLocalKikimrConfiguration.java).

# Creating Sandbox resource for local Kikimr 

Создание SBX-ресурса выполняется из содержимого deb-пакета с релизом Kikimr.
Идея состоит в том, чтобы исключить проблемы связанные с использованием билда Kikimr по текущей ревизии транка Arcadia.
Концепция "зеленого" транка для Kikimr в стадии реализации.
Более того, использование драйвера Kikimr из продакшена более правильный подход нежели тестировать функциональность Solomon на ревизии текущего транка.

То же самое относится к UDF-библиотекам YQL используемым в сценариях Solomon для работы с Kikimr-таблицами.

Для платформ Linux и OSX (в исходниках кода используется именование **Darwin**) создаются отдельные SBX-ресурсы.

Стоит отметить, что команда Kikimr не выполняет сборку Kikimr-артефактов для OSX-платформы по очевидным причинам (в продакшене используется Linux).
Команде Solomon, тем не менее, крайне удобно иметь драйвер Kikimr и UDF-библиотеки собранных для OSX, чтобы была возможность запускать локальный Kikimr на макбуке разработчика.
Запуск на макбуке существенно упрощает отладку тестов локально (без необходимости удаленной отладки Kikimr развернутого где-то на одной из DEV-машин с Linux-системой).

Потому, создание SBX-ресурса с Kikimr для макбука отличается от [базовой процедуры создания ресурса для Linux-платформы](#preparing-files-for-linux) и описывается в [процедуре создания ресурса для OSX-платформы](#preparing-files-for-osx).
Отличие заключается в подготовке файлов составляющих содержание создаваемого SBX-ресурса.
В целом, после подготовки файлов, должен быть каталог (назовем его **BUILD_DIR**) в котором располагаются следующие файлы:
 * файл **kikimr** - драйвер Kikimr
 * директория **udfs** - внутри файлы с требуемыми для тестов UDF-библиотеками YQL
 
В папке **BUILD_DIR** необходимо выполнить следующие шаги:
 * **создать tgz-файл** - ```tar czf kikimr_driver_udf.tar.gz kikimr udfs```
 * **загрузить** созданный файл **в SBX** - ```ya upload kikimr_driver_udf.tar.gz --type OTHER_RESOURCE --token $YA_TOKEN --description "KiKiMR driver with UDFs for Solomon UTs under $(uname -s)" --do-not-remove```
 * выданный **SBX_ID** для вновь созданного SBX-ресурса использовать в соответствующих местах тестового кода. Например, для ConfDB DAL UTs:
   * [ya.make](../solomon-core/ut/ya.make)
   * [ru.yandex.solomon.core.TunableLocalKikimrConfiguration](../solomon-core/ut/src/TunableLocalKikimrConfiguration.java)

## Preparing files for Linux

Для Linux используются файлы из соотв-го deb-пакета связанного с релизом Kikimr.

А именно пошагово:

1. В ST ищем тикеты в очереди **KIKIMR** с тегом **release** и статусом **CLOSED**, и сортировкой по номеру тикета в обратном порядке https://st.yandex-team.ru/KIKIMR/order:key:false/filter?status=3&tags=release&queue=KIKIMR
2. Переходим к первому тикету из выдачи для которого наименование тикета должно выглядеть примерно как *Выпустить релиз stable-XX-X-X*. 
3. В комментариях к тикету ищем последнее упоминание ссылки на sandbox и переходим по ссылке на страницу с описанием SBX-ресурса.
4. На вкладке **Resources** найти следующие ресурсы с типом **YA_PACKAGE** и скачать их локально с соотв-щим именем для deb-пакета:  
   1. атрибут **resource_name** равен *yandex-search-kikimr-kikimr-bin* - это deb-пакет с драйвером Kikimr
   2. атрибут **resource_name** равен *yandex-search-kikimr-yql-udfs-kikimr* - это deb-пакет с UDF-библиотеками YQL идущими в стандартной поставке Kikimr
5. Из скачанных deb-пакетов скопировать драйвер Kikimr и необходимые UDF-библиотеки в папку **BUILD_DIR**. 

Всю эту процедуру можно автоматизировать и даже собирать SBX-ресурс для тестов Solomon по триггеру связанному с релизом Kikimr.
   
## Preparing files for OSX

1. Скачать исходники Kikimr из Arcadia по тегу релиза Kikimr (см. шаг 2 в [процедуре подготовки файлов для Linux](#preparing-files-for-linux)) 
2. Собрать из исходников (на макбуке или через Sandbox-задачу) через ```ya make -r```:
  * драйвер Kikimr - относительный путь *arcadia/kikimr/driver*
  * необходимые UDF-библиотеки YQL - относительный путь к библиотекам *arcadia/yql/udfs/common*
3. Скопировать результат сборки в папку **BUILD_DIR** по соотв-м путям 
