import requests
import json
import datetime

from enum import Enum
from config.config import TOKEN_VAULT, YAV_TESTPALM_TOKEN
from const.const import LOGGER
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from functools import reduce


class TestPalmJsonTemplates:
    # parentIssue - ticket from ST
    # testSuite.groups - case grouping (module, submodule)
    # testSuite.filter.expression - filter for cases
    # testGroups - test in groups
    test_run_body_template = {
            "title": "title",
            "version": "version",
            "parentIssue": {
                "trackerId": "Startrek",
                "groupId": "AUTORUAPPS",
                "url": "https://st.yandex-team.ru/id",
                "idDisplay": "id",
                "id": "id"
            },
            "testSuite": {
                "groups": ["module", "submodule"],
                "filter": {
                    "sorting": {
                        "id": "ASC"
                    },
                    "expression": {
                        "key": "tag_key",
                        "value": "tag_value",
                        "type": "EQ"
                    }
                }
            },
            "testGroups": []
        }

    testpalm_version_body_template = {
        "status": "CREATED",
        "archived": False,
        "title": "title",
        "id": "id"
    }


class SearchCommentTemplate:

    def __init__(self, template, process=(lambda x: x)):
        self.template = template
        self.process = process


class TestPalmProject(Enum):
    AUTO = {
        'project_name': 'appsautoru',
        'os_id': '551c1b2fe4b08f96c185fea7',
        'is_automated_id': '5ebd3d3be8063d1b8f553994',
        'severity_id': '54d23bf1e4b0574ce855566a',
        'module_id': '54d230afe4b0574ce8555647',
        'submodule_id': '558184afe4b0aae967f7d2cf',
        'tag_id': '5e78c55b9fcbfa71d321f7fa',
        'smoke_cases_severity': 'Smoke',
        'build_templates': {
            'android': [SearchCommentTemplate(r'rc: #\d+'), SearchCommentTemplate(r'dev: #\d+')],
            'ios': [SearchCommentTemplate(r'\d{5}')]
        },
        'smoke_cases_severity': 'Smoke',
        # Settings for old case adding:
        # - threshold_of_old - When case become "old"(days)
        # - test_runs_search_deep - For what period we search test runs to parse(days)
        # - old_case_group_size - max numbers of cases for adding to smoke
        # - severities_for_old_cases - list of severities for case filtering
        'threshold_of_old': 60,
        'test_runs_search_deep': 365,
        'severities_for_old_cases': ["blocker", "critical", "major"],
        'old_case_group_size': 4
    }
    REALTY = {
        'project_name': 'vsapp',
        'os_id': '5c5afa93708298745a1cceb5',
        'is_automated_id': '5ece1af1e1ff184aeb4cbdfd',
        'severity_id': '58c69791889550197f2817ca',
        'module_id': '5c5b0c1b6acd3d47166e35f0',
        'submodule_id': '',
        'tag_id': '5f327d0ac7b640ebdfd776b9',
        'smoke_cases_severity': 'Smoke',
        'build_templates': {
            'android': [SearchCommentTemplate(r'\d+ prod'), SearchCommentTemplate(r'\d+ test universal')],
            'ios': [SearchCommentTemplate(r'Firebase:.*\d+', (lambda x: x.replace('*', ''))),
                    SearchCommentTemplate(r'(Testflight:).*( \d+)', (lambda x: x.replace('*', '')))]
        },
        'smoke_cases_severity': 'Smoke',
        'threshold_of_old': 60,
        'test_runs_search_deep': 365,
        'severities_for_old_cases': ["regress"],
        'old_case_group_size': 2
    }
    # project for testing
    PETPROJECT = {
        'project_name': 'mann',
        'os_id': '58975deb8895501f33a5683e',
        'is_automated_id': '6053574ba5c2020011cd2f05',
        'severity_id': '605357414c36ad001142fd2d',
        'module_id': '5c7e3d23bb580fbc93a15250',
        'submodule_id': '',
        'tag_id': '5e78a59642a89ac70bb53763',
        'smoke_cases_severity': 'Smoke core',
        'build_templates': {
            'android': [SearchCommentTemplate(r'rc: #\d+'), SearchCommentTemplate(r'dev: #\d+')],
            'ios': [SearchCommentTemplate(r'^\d+')]
        },
        'smoke_cases_severity': 'Smoke core',
        'threshold_of_old': 60,
        'test_runs_search_deep': 180,
        'severities_for_old_cases': ['Smoke core'],
        'old_case_group_size': 3
    }


class TestPalm:
    API_HOST = 'testpalm-api.yandex-team.ru'
    TESTPALM_TOKEN = TOKEN_VAULT[YAV_TESTPALM_TOKEN]
    BASE_HEADER = {'Authorization': 'OAuth {}'.format(TESTPALM_TOKEN),
                   'accept': 'application/json',
                   'Content-Type': 'application/json'
                   }

    VERSION_ENDPOINT = 'version/{project}'
    TESTCASES_ENDPOINT = 'testcases/{project}'
    BULK_CASES_ENDPOINT = 'testcases/{project}/bulk'
    GROUPED_CASES_ENDPOINT = 'testcases/{project}/groups'
    TEST_RUN_CREATE_ENDPOINT = 'testrun/{project}/create'
    TEST_RUN_ENDPOINT = 'testrun/{project}'

    def __init__(self, project):
        def timestamp_for_day_in_past(days):
            try:
                day_in_past = datetime.datetime.today() - datetime.timedelta(days)
                return int(day_in_past.strftime("%s")) * 1000
            except Exception as err:
                LOGGER.error("Error on calculating timestamp: {}".format(err.__repr__()))
                return 0

        self.project = project['project_name']
        self.platform_id = project['os_id']
        self.automation_id = project['is_automated_id']
        self.severity_id = project['severity_id']
        self.tag_id = project['tag_id']
        self.smoke_severity = project['smoke_cases_severity']
        self.module_id = project['module_id']
        self.submodule_id = project['submodule_id']
        self.build_templates = project['build_templates']

        self.threshold_of_old = timestamp_for_day_in_past(project['threshold_of_old'])
        self.test_runs_search_deep = timestamp_for_day_in_past(project['test_runs_search_deep'])
        self.severities_for_old_cases = project['severities_for_old_cases']
        self.old_case_group_size = project['old_case_group_size']

        if TestPalmProject(project) == TestPalmProject.AUTO:
            LOGGER.info("set auto module grouping functions")
            self.case_grouping = self.group_cases_module_submodule
            self.select_oldest_case_in_block = self.select_oldest_case_for_submodule
        elif TestPalmProject(project) == TestPalmProject.REALTY:
            LOGGER.info("set realty module grouping functions")
            self.case_grouping = self.group_case_module
            self.select_oldest_case_in_block = self.select_oldest_case_for_module
        elif TestPalmProject(project) == TestPalmProject.PETPROJECT:
            LOGGER.info("set pet project module grouping functions")
            self.case_grouping = self.group_case_module
            self.select_oldest_case_in_block = self.select_oldest_case_for_module
        else:
            LOGGER.error("Not supported module: {}".format(project))
            self.case_grouping = None
            self.select_oldest_case_in_block = None

        # Disable warning for self-signed cert
        requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

    def post_request(self, endpoint, json_body):
        url = 'https://{host}/{endpoint}'.format(host=self.API_HOST, endpoint=endpoint)
        headers = self.BASE_HEADER
        try:
            body = json.dumps(json_body)
            response = requests.post(url=url, data=body, headers=headers, verify=False)
            return response
        except Exception as e:
            LOGGER.error("Error on post request {}.\nError: {}".format(url, e.__repr__()))
            return None

    def get_request(self, endpoint):
        url = 'https://{host}/{endpoint}'.format(host=self.API_HOST, endpoint=endpoint)
        headers = self.BASE_HEADER
        try:
            response = requests.get(url=url, headers=headers, verify=False)
            return response
        except Exception as e:
            LOGGER.error("Error on get request {}.\nError: {}".format(url, e.__repr__()))
            return None

    def patch_request(self, endpoint, json_body):
        url = 'https://{host}/{endpoint}'.format(host=self.API_HOST, endpoint=endpoint)
        headers = self.BASE_HEADER
        try:
            body = json.dumps(json_body)
            response = requests.patch(url=url, data=body, headers=headers, verify=False)
            return response
        except Exception as e:
            LOGGER.error("Error on patch request {}.\nError: {}".format(url, e.__repr__()))
            return None

    # get versions for testpalm project
    def get_testpalm_versions(self):
        versions = []
        result = self.get_request(
            endpoint=self.VERSION_ENDPOINT.format(project=self.project),
        )
        if result is not None:
            if result.status_code == 200:
                versions = result.json()
            else:
                LOGGER.error("Error on getting versions list in project {}.\nResponse code: {}"
                             "\nwith error: {}".format(self.project, result.status_code, result.text)
                             )
        return versions

    # get existing version for testpalm project or create new one
    def get_testpalm_version_with_name(self, version_name):
        project_versions = self.get_testpalm_versions()
        for version in project_versions:
            if version_name.lower() == version['title'].lower():
                LOGGER.info("Found existing version with name {}".format(version_name))
                return version
        LOGGER.info("Not found version with name {}. Creating new one".format(version_name))
        return self.create_version(version_name)

    # get case list for version with grouping by module, submodule
    def get_grouped_cases_list_for_version(self, version):
        groups = []
        expression_part = {"key": "attributes.{}".format(self.tag_id), "value": version, "type": "EQ"}
        url = "{url}?expression={expression}&groups={module}&groups={submodule}".format(
            url=self.GROUPED_CASES_ENDPOINT.format(project=self.project),
            expression=json.dumps(expression_part),
            module=self.module_id,
            submodule=self.submodule_id
        )
        result = self.get_request(url)
        if result is not None:
            if result.status_code == 200:
                groups = result.json()['groups']
            else:
                LOGGER.error("Error on getting grouped cacse list for version  {}.\nResponse code: {}"
                             "\nwith error: {}".format(version, result.status_code, result.text)
                             )
        return groups

    def make_smoke_run(self, version, st_task, st_queue):
        expression_part = {
            "key": "attributes.{}".format(self.tag_id),
            "value": version,
            "type": "EQ"
        }

        body = TestPalmJsonTemplates.test_run_body_template
        body['title'] = "{} Smoke".format(version)
        body['version'] = version
        body['parentIssue']['groupId'] = st_queue
        body['parentIssue']['url'] = "https://st.yandex-team.ru/{}".format(st_task)
        body['parentIssue']['idDisplay'] = st_task
        body['parentIssue']['id'] = st_task
        body['testSuite']['groups'] = [self.module_id, self.submodule_id]
        body['testSuite']['filter']['expression'] = expression_part
        body['testGroups'] = self.get_grouped_cases_list_for_version(version)

        result = self.post_request(
            self.TEST_RUN_CREATE_ENDPOINT.format(project=self.project),
            body
        )
        if result is not None:
            if result.status_code == 200:
                LOGGER.info("Test run created for version {} and task {}".format(version, st_task))
                return result.json()
            else:
                LOGGER.error("Error on trying to create smoke run for version  {}.\nResponse code: {}"
                             "\nwith error: {}".format(version, result.status_code, result.text)
                             )
                return None
        return result

    # check if ST version is linked for testpalm version
    @staticmethod
    def is_startrack_version_linked(testpalm_version):
        if 'trackerVersion' in testpalm_version:
            if 'versionId' in testpalm_version['trackerVersion']:
                LOGGER.info("Tracker version with id {} already linked".format(
                    testpalm_version['trackerVersion']['versionId'])
                )
                return True
        return False

    # not working in testpalm api now(
    def link_testpalm_version_with_tracker_version(self, testpalm_version, st_version, queue):
        if not self.is_startrack_version_linked(testpalm_version):
            tracker_version = {"isClosed": False,
                               "versionId": st_version.id,
                               "title": st_version.display,
                               "trackerId": "Startrek",
                               "groupId": queue,
                               "url": "https://st.yandex-team.ru/{queue}/filter?fixVersions={fix_version}".format(
                                   queue=queue, fix_version=st_version.id
                               )
                               }
            testpalm_version['trackerVersion'] = tracker_version
            LOGGER.info("new version: {}".format(json.dumps(testpalm_version)))
            result = self.patch_request(
                self.VERSION_ENDPOINT.format(project=self.project),
                testpalm_version
            )
            if result is not None:
                if result.status_code == 200:
                    LOGGER.info("Linked ST version added  TestPalm version {}".format(st_version.display))
                    return result.json()
                else:
                    LOGGER.error("Error on linking ST and TestPalm version {}.\nResponse code: {}"
                                 "\nwith error: {}".format(st_version.display, result.status_code, result.text)
                                 )
            return testpalm_version

    def create_version(self, title):
        request_body = TestPalmJsonTemplates.testpalm_version_body_template
        request_body['title'] = title
        request_body['id'] = title

        result = self.post_request(
            self.VERSION_ENDPOINT.format(project=self.project),
            request_body
        )
        if result is not None:
            if result.status_code == 200:
                LOGGER.info("New version added to testpalm. Project: {}, name: {}".format(self.project, title))
                return result.json()
            else:
                LOGGER.error("Error on creating new version with name {} in testpalm project {}.\nResponse code: {}"
                             "\nwith error: {}".format(title, self.project, result.status_code, result.text)
                             )
                return None
        return result

    def add_version_tag_to_cases(self, version_tag, cases):
        for case in cases:
            if self.tag_id in case['attributes'].keys():
                if version_tag.lower() not in [tag.lower() for tag in case['attributes'][self.tag_id]]:
                    case['attributes'][self.tag_id] = case['attributes'][self.tag_id] + [version_tag]
            else:
                case['attributes'][self.tag_id] = [version_tag]

        result = self.patch_request(
            endpoint=self.BULK_CASES_ENDPOINT.format(project=self.project),
            json_body=cases
        )
        if result is not None:
            if result.status_code == 200:
                LOGGER.info("Tag {} added to {} cases".format(version_tag, len(cases)))
                return result.json()
            else:
                LOGGER.error("Error on adding version tag {} to testcases.\nResponse code: {}"
                             "\nError: {}".format(version_tag, result.status_code, result.text)
                             )
        return []

    def is_version_in_tags(self, case, version):
        if self.tag_id in case['attributes'].keys():
            if version.lower() not in (tag.lower() for tag in case['attributes'][self.tag_id]):
                return True
        return False

    def is_case_automated(self, case, os):
        if self.automation_id in case['attributes']:
            if os.lower() in (platform.lower() for platform in case['attributes'][self.automation_id]):
                return True
        return False

    def is_case_for_platform(self, case, os):
        if self.platform_id in case['attributes']:
            if os.lower() in (platform.lower() for platform in case['attributes'][self.platform_id]):
                return True
        return False

    def get_smoke_cases(self, platform):
        smoke_cases = []
        cases_list = self.select_non_archived_cases_with_severity(self.smoke_severity)
        for case in cases_list:
            if self.is_case_for_platform(case, platform) and not self.is_case_automated(case, platform):
                smoke_cases.append(case)
        return smoke_cases

    def get_old_cases(self, platform):
        old_cases = []
        try:
            unsorted_cases = self.get_actual_cases_with_last_run_stat(
                platform,
                self.test_runs_search_deep,
                self.severities_for_old_cases
            )
            filtered_unsorted_cases = list(
                filter(
                    lambda case: int(case['lastRunTime']) < self.threshold_of_old and int(case['createdTime']) < self.threshold_of_old,
                    unsorted_cases
                )
            )
            old_cases = self.select_oldest_case_in_block(
                self.case_grouping(filtered_unsorted_cases)
            )
        except Exception as err:
            LOGGER.error("Error on collecting old cases: {}".format(err.__repr__()))
        finally:
            return old_cases

    def select_non_archived_cases_with_severity(self, severity):
        expression_part = json.dumps(
            {
                "type": "AND",
                "left": {
                    "type": "EQ",
                    "key": "attributes.{}".format(self.severity_id),
                    "value": severity
                },
                "right": {
                    "type": "NEQ",
                    "key": "status",
                    "value": "archived"
                }
            }
        )
        include_part = "id,name,status,attributes,createdTime"
        url = "{endpoint}?include={include}&expression={expression}".format(
            endpoint=self.TESTCASES_ENDPOINT.format(project=self.project),
            include=include_part,
            expression=expression_part
        )
        result = self.get_request(url)
        if result is not None:
            if result.status_code == 200:
                return result.json()
            else:
                LOGGER.error("Error on requesting case list with code {}.\n {}".format(result.status_code, result.text))
        return []

    # use "id" in updating_data to specify run for update
    def update_test_run(self, updating_data):
        result = self.patch_request(self.TEST_RUN_ENDPOINT.format(project=self.project), updating_data)
        if result is not None:
            if result.status_code == 200:
                return result.json()
            else:
                LOGGER.error("Error on updating test run with code {}.\n {}".format(result.status_code, result.text))

    def set_builds_for_test_run(self, test_run_id, build_list):
        build_tags = []
        for build in build_list:
            build_tags.append({"value": build, "key": "build"})

        if test_run_id and len(build_tags) > 0:
            update_data = {
                "id": test_run_id,
                "properties": build_tags
            }
            return self.update_test_run(update_data)
        else:
            LOGGER.warning("Not enough data for set builds {} to run {}".format(build_list, test_run_id))

    def get_test_runs_cases(self, platform, since_timestamp):
        expression_part = json.dumps(
            {
                "type": "AND",
                "left": {
                    "type": "GT",
                    "key": "finishedTime",
                    "value": since_timestamp
                },
                "right": {
                    "type": "CONTAIN",
                    "key": "title",
                    "value": platform
                }

            }
        )
        include_part = "id," \
                       "title," \
                       "testGroups.testCases.testCase.id," \
                       "testGroups.testCases.finishedTime," \
                       "testGroups.testCases.status"
        url = "{endpoint}?include={include}&expression={expression}&createdTimeSort=desc".format(
            endpoint=self.TEST_RUN_ENDPOINT.format(project=self.project),
            include=include_part,
            expression=expression_part
        )
        result = self.get_request(url)
        if result is not None:
            if result.status_code == 200:
                return result.json()
            else:
                LOGGER.error("Error on requesting testruns with cases list with code {}.\n {}".format(result.status_code, result.text))
        return []

    # Getting all cases from test runs since 'since_timestamp'
    def get_cases_with_last_run_time(self, platform, since_timestamp):
        runs_with_cases = self.get_test_runs_cases(platform, since_timestamp)
        checked_cases = {}
        try:
            for run in runs_with_cases:
                for test_group in run['testGroups']:
                    for test_case in test_group['testCases']:
                        # only not skipped cases
                        if test_case['status'].lower() == "skipped":
                            continue
                        test_case_id = test_case['testCase']['id']
                        test_case_time = test_case['finishedTime']
                        if test_case_id not in checked_cases or checked_cases[test_case_id] < test_case_time:
                            checked_cases[test_case_id] = test_case_time
        except Exception as err:
            LOGGER.error("Error on getting finish time for cases in test runs. Error: {}".format(err.__repr__()))
        finally:
            return checked_cases

    # Getting all non archived cases without automation
    def get_actual_cases_with_severities(self, platform, search_severities):
        actual_cases = []
        try:
            for severity in search_severities:
                cases_with_severity = self.select_non_archived_cases_with_severity(severity)
                for case in cases_with_severity:
                    if self.is_case_for_platform(case, platform) and not self.is_case_automated(case, platform):
                        actual_cases.append(case)
        except Exception as err:
            LOGGER.error("Error on getting actual cases: {}".format(err.__repr__()))
        finally:
            LOGGER.info("actual cases len: {}".format(len(actual_cases)))
            return actual_cases

    # All non archived cases enriched with last run time
    def get_actual_cases_with_last_run_stat(self, platform, since_timestamp, search_severities):
        actual_cases = self.get_actual_cases_with_severities(platform, search_severities)
        finish_time_stat_cases = self.get_cases_with_last_run_time(platform, since_timestamp)
        try:
            for case in actual_cases:
                if case['id'] in finish_time_stat_cases:
                    case['lastRunTime'] = finish_time_stat_cases[case['id']]
                else:
                    case['lastRunTime'] = '0'
        except Exception as err:
            LOGGER.error("Error on enriching cases with last run time from test runs: {}".format(err.__repr__()))
        finally:
            return actual_cases

    # Get grouped cases by module, submodule
    def group_cases_module_submodule(self, case_list):
        grouped_cases = {}
        module_name = None
        submodule_name = None

        for case in case_list:
            try:
                for module_name in case['attributes'][self.module_id]:
                    for submodule_name in case['attributes'][self.submodule_id]:
                        if module_name not in grouped_cases.keys():
                            grouped_cases[module_name] = {}
                        if submodule_name not in grouped_cases[module_name].keys():
                            grouped_cases[module_name][submodule_name] = []

                if module_name is not None and submodule_name is not None:
                    grouped_cases[module_name][submodule_name].append(case)
                else:
                    LOGGER.warning("No module or submodule for case {}. Case skipped".format(case['id']))
            except Exception as err:
                LOGGER.error("Error when grouping cases by module and submodule: {}".format(err.__repr__()))
                continue
        return grouped_cases

    # Get grouped cases only by module
    def group_case_module(self, case_list):
        grouped_cases = {}
        module_name = None

        for case in case_list:
            try:
                for module_name in case['attributes'][self.module_id]:
                    if module_name not in grouped_cases.keys():
                        grouped_cases[module_name] = []

                if module_name is not None:
                    grouped_cases[module_name].append(case)
                else:
                    LOGGER.warning("No module for case {}. Case skipped".format(case['id']))
            except Exception as err:
                LOGGER.error("error on grouping case {}: {}".format(case, err.__repr__()))
                continue
        return grouped_cases

    @staticmethod
    def get_oldest_case_for_block(case_block):
        oldest_case = None
        if len(case_block) > 0:
            oldest_case = reduce(lambda a, b: a if (int(a['lastRunTime']) < int(b['lastRunTime'])) else b, case_block)
        return oldest_case

    def select_oldest_case_for_submodule(self, sorted_case_list):
        oldest_cases = []
        try:
            for module, submodule in sorted_case_list.items():
                for sub, keys in submodule.items():
                    block_oldest_case = self.get_oldest_case_for_block(keys)
                    if block_oldest_case is not None:
                        oldest_cases.append(block_oldest_case)
        except Exception as err:
            LOGGER.error("Error on getting oldest case for submodule: {}".format(err.__repr__()))
        finally:
            return oldest_cases

    def select_oldest_case_for_module(self, sorted_case_list):
        oldest_cases = []
        try:
            for module, keys in sorted_case_list.items():
                block_oldest_case = self.get_oldest_case_for_block(keys)
                if block_oldest_case is not None:
                    oldest_cases.append(block_oldest_case)
        except Exception as err:
            LOGGER.error("Error on getting oldest case for submodule: {}".format(err.__repr__()))
        finally:
            return oldest_cases


tp_auto = TestPalm(TestPalmProject.AUTO.value)
tp_realty = TestPalm(TestPalmProject.REALTY.value)
