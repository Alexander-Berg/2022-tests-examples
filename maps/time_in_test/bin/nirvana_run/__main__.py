from __future__ import print_function, absolute_import, division

import maps.automotive.qa.metrics.time_in_test.lib.eval_report as eval_report

from nile.api.v1 import (
    cli,
    datetime as nd
)

import json
from datetime import timedelta


@cli.statinfra_job
def make_job(job, options, statface_client):
    current_date = nd.Datetime.from_iso(options.dates[0])
    previous_date = current_date - timedelta(days=1)

    testers_logins = json.load(open('0_files', 'r'))[0]

    eval_report.make_job(
        job, [previous_date], statface_client, testers_logins
    )
    return job


if __name__ == '__main__':
    cli.run()
