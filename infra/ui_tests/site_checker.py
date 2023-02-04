# coding: utf-8

from library.python import selenium_ui_test


class SiteChecker(object):
    def __init__(self, driver, results_folder, cluster):
        self.cluster = cluster
        self.navigator = selenium_ui_test.Navigator(
            main_url=cluster.url,
            driver=driver,
            results_folder=results_folder,
            timeout=cluster.timeout,
        )
        self.main_url = cluster.url

    def check_text(self, url_path, text):
        self.navigator.check_text(url_path, text)
