from stackbot.config import settings


def staff_api_result(staff) -> dict:
    return {
        'login': staff.login,
        'uid': staff.uid,
        'id': staff.id,
        'telegram_accounts': [
            {'value': staff.telegram_account},
        ],
        'official': {
            'is_dismissed': staff.is_dismissed,
            'affiliation': 'yandex',
        }
    }


def get_staff_api_auth_url(telegram_usernames: list[str] = None) -> str:
    fields = '_fields=id%2Cuid%2Clogin%2Ctelegram_accounts.value%2Cofficial.is_dismissed%2Cofficial.affiliation'
    if telegram_usernames is None:
        telegram_usernames = ['smth']
    joined_names = ','.join(telegram_usernames)
    return f'{settings.STAFF_API_HOST}/v3/persons/?telegram_accounts.value={joined_names}&{fields}'
