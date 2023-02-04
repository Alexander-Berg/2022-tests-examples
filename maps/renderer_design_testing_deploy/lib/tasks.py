import jinja2
import json
import library.python.resource
import smtplib
import yenv

from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

from maps.libs.config.py.config import read_config_file

from maps.garden.sdk.core import Task


FROM_MAIL = 'noreply@yandex-team.ru'
SMTP_SERVER = 'outbound-relay.yandex.net'


def _subject(version):
    return 'Release {0} is active in design testing ({1})'.format(version, yenv.type)


class NotifyTask(Task):
    def __init__(self):
        super(NotifyTask, self).__init__()
        notify_settings = json.loads(read_config_file(
            "/renderer_design_testing_deploy.json"))
        self.report_dst_address = notify_settings['report_dst_address']

    def __call__(self, *datasets, **kwargs):
        dataset = next(d for d in datasets if d)
        bundle_releases = dataset.properties.get('bundle_releases')
        if not bundle_releases:
            return

        template = library.python.resource.find('/report_template.html').decode('utf-8')
        html = jinja2.Environment().from_string(template).render(
            bundle_releases=bundle_releases)

        msg = MIMEMultipart()
        msg.attach(MIMEText(html, 'html', 'utf-8'))
        msg.add_header('Content-type', 'text/html')
        msg['Subject'] = _subject(dataset.dataset_version)
        msg['From'] = FROM_MAIL
        msg['To'] = self.report_dst_address

        smtp = smtplib.SMTP(SMTP_SERVER)
        smtp.sendmail(FROM_MAIL, [self.report_dst_address], msg.as_string())
        smtp.quit()
