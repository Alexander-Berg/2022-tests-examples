import os
from datetime import datetime


class Report(object):
    def __init__(self, main_context, report_dir):
        self.main_context = main_context
        self.report_dir = report_dir
        self.output = None

    def __enter__(self):
        report_dir = os.path.join(os.path.expanduser("~"), self.report_dir)
        try:
            os.mkdir(report_dir)
        except OSError as e:
            if e.errno == 17:
                # file exists
                pass
        self.output = open(
            os.path.join(
                report_dir,
                u"{}-{}-report.txt".format(
                    self.main_context.name, datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
                ),
            ).encode("utf8"),
            "wb",
        )
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.output:
            self.output.close()
            self.output = None

    def write(self, *lines):
        if not self.output:
            raise RuntimeWarning("Output file is not opened")
        self.output.write(u"\n".join(lines).encode("utf8"))

    def write_log(self, *entities):
        lines = []
        for entity in entities:
            lines.append(
                u" ".join(
                    [u"{}:".format(entity[0])] + [u"{}".format(c) for c in entity[1:]]
                )
            )
        self.write(*lines)
