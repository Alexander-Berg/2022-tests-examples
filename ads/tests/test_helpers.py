from ads.watchman.experiments.lib.models import models_refactored


class MockLearnedTask(models_refactored.LearnedTask):
    def get_scores(self):
        return {"ll_p": {"learn": 0.1, "test":  0.2}}
