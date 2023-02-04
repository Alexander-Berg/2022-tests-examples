from walle.authorization import iam


PROJECT_FOLDER_ID = "some-project-folder-id"
AUTOMATION_PLOT_FOLDER_ID = "some-ap-folder-id"

MOCKED_USER_ID = "mocked_user_id"
MOCKED_SA_ID = "mocked_sa_id"
MOCKED_USER_LOGIN = "mocked_user_login@"
MOCKED_SA_NAME = "mocked_sa_name"
MOCKED_IAM_TOKEN = "mocked_iam_token"
IAM_TOKEN_HEADERS = {
    "Authorization": f"{iam.BEARER_HEADER_PREFIX}{MOCKED_IAM_TOKEN}",
}


def get_calls_args(actual_calls):
    return {
        (c.kwargs["permission"], c.kwargs["resource_path"].id, c.kwargs["resource_path"].type) for c in actual_calls
    }
