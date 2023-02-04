from smarttv.plant.plant.integrations.quasar.api import QuasarUploader, QuasarDeviceTable


class TestQuasarUploader:
    def test_test_is_working(self):
        quasar_yt = QuasarDeviceTable()
        uploader = QuasarUploader(quasar_yt)
        assert uploader
