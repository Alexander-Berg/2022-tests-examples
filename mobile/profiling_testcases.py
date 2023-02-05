import time


class BaseTest:
    def __init__(self, adb):
        self._adb = adb

    @classmethod
    def step(cls, action, sleep=7):
        action()
        time.sleep(sleep)

    def close_context_widget(self):
        self._adb.launch(["shell", "input", "tap", "730", "300"])

    def open_context_widget(self):
        self._adb.launch(["shell", "input", "tap", "1000", "300"])

    def open_uma(self):
        self._adb.launch(["shell", "am", "start", "yandex.auto.uma/.ui.FullMainActivity"])

    def open_navi(self):
        self._adb.launch(["shell", "am", "start", "ru.yandex.yandexnavi.auto/.ui.NaviActivity"])

    def open_applist(self):
        self._adb.launch(["shell", "am", "start", "yandex.auto.homescreen/.MenuActivity"])

    def open_homescreen(self):
        self._adb.launch(["shell", "am", "start", "yandex.auto.homescreen/.MainActivity"])

    def open_dialer(self):
        self._adb.launch(["shell", "am", "start", "yandex.auto.dialer/.ui.DialerActivity"])

    def open_settings(self):
        self._adb.launch(
            ["shell", "am", "start", "yandex.auto.settings/.ui.settings.SettingsActivity"])

    def swipe_left(self):
        self._adb.launch(["shell", "input", "swipe", "600", "300", "300", "300", "100"])

    def swipe_right(self):
        self._adb.launch(["shell", "input", "swipe", "300", "300", "600", "300", "100"])

    def swipe_context_widget_up(self):
        self._adb.launch(["shell", "input", "swipe", "900", "500", "900", "200", "100"])

    def swipe_context_widget_down(self):
        self._adb.launch(["shell", "input", "swipe", "900", "200", "900", "500", "100"])

    def run(self):
        pass


class TestCase1(BaseTest):
    def run(self):
        self.step(self.open_homescreen)
        self.step(self.swipe_left)
        self.step(self.swipe_right)

        self.step(self.open_uma)
        self.step(self.open_context_widget)
        self.step(self.close_context_widget)
        self.step(self.open_context_widget)
        self.step(self.swipe_context_widget_up)
        self.step(self.swipe_context_widget_up)
        self.step(self.swipe_context_widget_down)
        self.step(self.swipe_context_widget_down)

        self.step(self.open_navi, sleep=15)

        self.step(self.open_applist)
        self.step(self.swipe_context_widget_up)
        self.step(self.swipe_context_widget_down)

        self.step(self.open_settings)
        self.step(self.swipe_context_widget_up)
        self.step(self.swipe_context_widget_down)

        self.step(self.open_navi, sleep=10)
        self.step(self.close_context_widget, sleep=10)
        self.step(self.open_context_widget, sleep=10)
        self.step(self.close_context_widget, sleep=10)

        time.sleep(15)
