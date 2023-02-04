import maps.automotive.qa.metrics.time_in_test.lib.eval_report as eval_report
import maps.automotive.qa.metrics.common.lib.abc as abc

from nile.api.v1 import (
    clusters,
    datetime as nd,
    statface as ns
)

import argparse


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        '--from-date',
        help='Begin date, iso format: 2018-12-31',
        required=True
    )
    parser.add_argument(
        '--to-date',
        help='End date, iso format: 2018-12-31',
        required=True
    )
    parser.add_argument(
        '--prod',
        help='Upload to production (Statface and issues tables)',
        action='store_true'
    )
    parser.add_argument(
        '--statface-token',
        help='Statface token',
        required=True
    )
    parser.add_argument(
        '--yt-token',
        help='Yt token'
    )
    parser.add_argument(
        '--abc-token',
        help='ABC token'
    )
    args = parser.parse_args()

    if args.prod:
        statface_client = ns.StatfaceProductionClient(
            token=args.statface_token
        )
    else:
        statface_client = ns.StatfaceBetaClient(
            token=args.statface_token
        )

    start_date = nd.Datetime.from_iso(args.from_date)
    end_date = nd.Datetime.from_iso(args.to_date)
    if start_date > end_date:
        raise RuntimeError('begin date > end date')

    dates = list(nd.date_range(start_date, end_date, step=1, stringify=False))

    hahn_cluster = clusters.yt.Hahn(
        token=args.yt_token if args.yt_token else None
    )

    abc_gw = abc.ABCGateway(
        oauth_token=args.abc_token if args.abc_token else None
    )

    testers_logins = abc_gw.get_testers_list()

    job = hahn_cluster.job("time_in_test_aggregator")
    eval_report.make_job(
        job, dates, statface_client, testers_logins
    )
    job.run()


if __name__ == '__main__':
    main()
