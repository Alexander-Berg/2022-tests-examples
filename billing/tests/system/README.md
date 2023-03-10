## Cистемные e2e тесты

Тесты проверяют работу системы целиком от одного конца до другого. В тестах используются рецепты, чтобы иметь
возможность запускать тесты в CI. Выходные данные каждой системы проверяются с
использованием [канонизации](https://docs.yandex-team.ru/ya-make/manual/tests/canon#kak-kanonizirovat).

Тесты НЕ РАБОТАЮТ на macOS из-за зависимости YQL-рецепта от линуксового бинаря Mongo. Чтобы запустить тесты локально,
можно создать виртуалку в [qyp](https://qyp.yandex-team.ru/).

Для запуска всех системных тестов можно использовать команду:

```bash
# Из корня Аркадии.
ya make -A billing/hot/tests/system
```

Не рекомендуется игнорировать падения этого теста в CI. Большинство случаев его падения связаны с изменением формата
входных или выходных данных одного из сервисов. Если тест упал на этапе сверки с каноническим результатом, то вы можете
переканонизировать тест при условии, что вас устраивает новый результат. Если в выходных данных сервиса изменились
case-sensitive поля (
которые меняются при каждом запуске теста), то нужно изменить обфускацию этих данных
в [obfuscator.py](../lib/schema/obfuscation.py). Не нужно менять вручную [result.json](canondata/result.json) и другие
файлы директории _canondata_, нужно переканонизировать консольной командой. Иначе это может привести к некорректным
тестам.

Для повторной канонизации всех системных тестов можно использовать:

```bash
# Из корня Аркадии.
ya make -A billing/hot/tests/system -AZ
```
