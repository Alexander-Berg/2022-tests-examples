import os
import datetime
import string

from ads.libs.py_yaml_loader.loader.task_expander import load_and_expand_file

production_folders = ['network', 'autobudget', 'lm-production', 'search', 'search-special', 'rtb_cpm']


def generate_ymls(base=None, files_grep=None):
    for root, sub_folders, files in os.walk(base):
        for f in files:
            if f.endswith('.yml'):
                file_name = os.path.join(root, f)
                if files_grep is None or files_grep(file_name):
                    yield file_name


def get_ymls(learn_tasks_path, folders_to_test=None, files_grep=None):
    ymls_to_test = []
    folders = os.listdir(learn_tasks_path)
    for folder in folders:
        folder_path = os.path.join(learn_tasks_path, folder)
        ymls = []
        if folders_to_test is None or folder in folders_to_test:
            ymls = generate_ymls(folder_path, files_grep)
        ymls_to_test.extend(ymls)
    return ymls_to_test


def is_not_graveyard(file_name):
    return 'graveyard' not in string.lower(file_name).split('/')


def list_dumps_mock():
    # return all possible dates of the two weeks
    base = datetime.datetime.today()
    date_list = [base - datetime.timedelta(days=x) for x in xrange(14)]
    date_str_list = [d.strftime('%Y-%m-%d') for d in date_list]
    return date_str_list


def is_testable_task(yml, learn_tasks_path):
    expanded_task = load_and_expand_file(
        yml,
        include_raw_loaded_data=True,
    )
    return not ('expansion' in expanded_task and 'generate' in expanded_task['expansion'])
