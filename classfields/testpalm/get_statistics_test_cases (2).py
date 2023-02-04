import requests
import operator
import argparse
import sys

headers = {'Authorization': 'OAuth <свой токен>'}
cases_list = []
parser = argparse.ArgumentParser()
parser.add_argument('project', type=str, help='Проект realty / auto')
parser.add_argument('platform', type=str, help=' Платформа ios / android')
parser.add_argument('-m', '--min_count', type=int, default=5,
                    help='Минимальное количество проходов кейса любое число >=0 (default: 5)')
args = parser.parse_args()

# Проверка что проект и платформа указаны верно
if args.project != "realty" and args.project != "auto":
    print("Введён неверный проект, укажите realty или auto")
    sys.exit()

if args.platform != "ios" and args.platform != "android":
    print("Введена неверная платформа, укажите ios или android")
    sys.exit()


# Получить список всех тест-кейсов для проекта
def getTestCasesForProject(project):
    url = 'http://testpalm-api.yandex-team.ru/testcases/{}'.format(project) + '?include=status%2Cattributes%2Cname%2Cid'
    return requests.get(url, headers=headers).json()


# Вывод отсортированного списка тест-кейсов с табличной разметкой для вики
def printSortedAndFormattedStat(cases_list):
    # Сортировка списка тест-кейсов по убыванию
    cases_list.sort(key=operator.itemgetter('count'), reverse=True)

    # Вывод списка тест-кейсов с табличной разметкой для wiki
    print("#|")
    print("|| Ссылка на пальму  | Название кейса| Количество проходов ||")
    for item in cases_list:
        print("|| " + item["link"] + " | " + item["name"] + " | {}".format(item["count"]) + " ||")
    print("|#")


# Получить статистику прохождения тест-кейсов в смоуках/регрессах
def getStatisticsTestCases(project, platform, count_min):
    platform = platform.lower()

    if project == "realty":
        attribute_platform = '5c5afa93708298745a1cceb5'
        attribute_version = '5f327d0ac7b640ebdfd776b9'
        attributes_automation = '5ece1af1e1ff184aeb4cbdfd'
        attributes_without_automation = '60701a30862c720011ac1f7b'
        project = "vsapp"

    if project == "auto":
        attribute_platform = '551c1b2fe4b08f96c185fea7'
        attribute_version = '5e78c55b9fcbfa71d321f7fa'
        attributes_automation = '5ebd3d3be8063d1b8f553994'
        # Поле добавления ключа "without automation" в авто нужно присвоить соответствующий хеш
        attributes_without_automation = '609c0f8de4e8c70085e9ccc9'
        project = "appsautoru"

    testcases_info = getTestCasesForProject(project)

    for item in testcases_info:
        if 'status' not in item: continue
        if 'attributes' not in item: continue
        if attribute_platform not in item['attributes']: continue
        if attribute_version not in item['attributes']: continue
        count = (" ".join(item.get("attributes").get(attribute_version))).lower().count(platform)
        if count < count_min: continue

        # Проверяем есть ли автотесты для данного кейса
        if attributes_automation in item['attributes']:
            # Проверяем есть ли автотесты для данной платформы
            if platform in " ".join(item['attributes'][attributes_automation]).lower(): continue

        # Проверяем есть ли ключ "without automation" для данного кейса
        if attributes_without_automation in item['attributes']:
            # Проверяем есть ли ключ "without automation" для данной платформы
            if platform in " ".join(item['attributes'][attributes_without_automation]).lower(): continue

        # Проверяем что кейс не архивный и он для данной платформы
        if item['status'] != "ARCHIVED" and platform in " ".join(item['attributes'][attribute_platform]).lower():
            cases_list.append({"link": "https://testpalm.yandex-team.ru/testcase/" + project + "-{}".format(item['id']),
                               "name": item.get("name"), "count": count})

    printSortedAndFormattedStat(cases_list)


getStatisticsTestCases(args.project, args.platform, args.min_count)
