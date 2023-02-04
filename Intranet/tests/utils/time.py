from datetime import timedelta

from django.utils import timezone


class StopWatch:

    def __init__(self, start_time=None, step=1):
        self.start_time = start_time or timezone.now()
        self.current_time = self.start_time
        self.step = step

    def __iter__(self):
        return self

    def __next__(self):
        self.current_time += timedelta(seconds=self.step)
        return self.current_time
