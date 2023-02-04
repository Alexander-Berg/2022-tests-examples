# coding: utf-8

def pytest_runtest_setup(item):
    from events.common_app.disk_admin.mock_client import clear_disk_admin_api_data
    from events.common_app.kinopoisk.mock_client import clear_kinopoisk_data
    clear_disk_admin_api_data()
    clear_kinopoisk_data()
