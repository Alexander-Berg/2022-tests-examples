import json
from intranet.femida.tests.mock.contest import ContestAnswers


class FakeContestAPI:
    START_TIME = '2018-01-01T10:00:00Z'
    FINISH_TIME = '2018-01-01T12:00:00Z'
    PROBLEM_A_NAME = 'Problem A'
    PROBLEM_A_TITLE = 'A'
    PROBLEM_B_NAME = 'Problem B'
    PROBLEM_B_TITLE = 'B'

    @classmethod
    def get_contest_standings(cls, contest_id):
        return {
            'rows': [{
                'participantInfo': {
                    'id': 1,
                    'login': 'login1',
                    'name': 'name1',
                },
                'problemResults': [{
                    'status': 'ACCEPTED',
                    'submissionCount': '1',
                    'submitDelay': 10,
                }, {
                    'status': 'NOT_ACCEPTED',
                    'submissionCount': '5',
                    'submitDelay': 100,
                }],
                'score': 1,
            }, {
                'participantInfo': {
                    'id': 2,
                    'login': 'login2',
                    'name': 'name2',
                },
                'problemResults': [{
                    'status': 'ACCEPTED',
                    'submissionCount': '3',
                    'submitDelay': 200,
                }, {
                    'status': 'NOT_SUBMITTED',
                    'submissionCount': '',
                    'submitDelay': 0
                }],
                'score': 1,
            }, {
                'participantInfo': {
                    'id': 3,
                    'login': 'login3',
                    'name': 'name3',
                },
                'problemResults': [{
                    'status': 'NOT_ACCEPTED',
                    'submissionCount': '3',
                    'submitDelay': 150,
                }, {
                    'status': 'NOT_SUBMITTED',
                    'submissionCount': '2',
                    'submitDelay': 30,
                }],
                'score': 0,
            }],
            'titles': [{
                'name': cls.PROBLEM_A_NAME,
                'title': cls.PROBLEM_A_TITLE,
            }, {
                'name': cls.PROBLEM_B_NAME,
                'title': cls.PROBLEM_B_TITLE,
            }],
        }

    @classmethod
    def get_participation(cls, contest_id, participant_id):
        data = {}
        if participant_id == 1:
            data = {
                'contestState': 'FINISHED',
                'participantStartTime': cls.START_TIME,
                'participantFinishTime': cls.FINISH_TIME,
            }
        elif participant_id == 2:
            data = {
                'contestState': 'FINISHED',
                'participantStartTime': cls.START_TIME,
                'participantFinishTime': cls.FINISH_TIME,
            }
        elif participant_id == 3:
            data = {
                'contestState': 'IN_PROGRESS',
                'participantStartTime': cls.START_TIME,
                'participantFinishTime': '',
            }
        return data

    @classmethod
    def get_contest(cls, contest_id):
        if contest_id == 1:
            return {
                "duration": 180,
                "freezeTime": 0,
                "name": "finite",
                "startTime": "2022-01-01T12:00:00.000Z",
                "type": "USUAL"
            }
        if contest_id == 2:
            return {
                "duration": '0',
                "freezeTime": 0,
                "name": "infinite",
                "startTime": "2022-01-01T12:00:00.000Z",
                "type": "USUAL"
            }

    @classmethod
    def get_problems(cls, contest_id):
        if contest_id == 1:
            return json.loads(ContestAnswers.problem_1)

    @classmethod
    def get_statement(cls, problem_id, path):
        if 'md' in path:
            return ContestAnswers.md_statement_html
        if 'tex' in path:
            return ContestAnswers.tex_statement_html


class FakeContestPrivateAPI:

    @classmethod
    def get_participant_ids(cls, answer_ids):
        return {
            'result': {
                'searchResults': [
                    {
                        'answerId': answer_ids[0],
                        'status': 'ok',
                        'participantId': 123,
                    },
                    {
                        'answerId': 111111,
                        'status': 'not-found',
                        'error': 'Answer id 111111 not found',
                    },
                ],
            },
        }


class FakeContestTVMApi:

    @classmethod
    def get_contest_info(cls, contest_id):
        if contest_id == 1:
            return json.loads(ContestAnswers.contest_info_1)
        if contest_id == 2:
            return json.loads(ContestAnswers.contest_info_2)

    @classmethod
    def get_problem_file(cls, problem_id, path):
        if 'md' in path:
            return ContestAnswers.md_statement_html
        if 'tex' in path:
            return ContestAnswers.tex_statement_html
