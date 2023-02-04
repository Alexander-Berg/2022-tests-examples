# -*- coding: utf-8 -*-

import click
import datetime
import json


@click.command()
@click.option('--month', required=True, type=str, help='Next to closed month')
@click.option('--date-format', required=True, type=str, help='Date format')
@click.option('--sql-params', required=True, type=str, help='SQL params')
def main(month, date_format, sql_params):
    month = datetime.datetime.strptime(month, '%m.%y')
    first_day_of_next_month = month.replace(day=1).strftime(date_format)
    last_day_of_closed_month = (month.replace(day=1) - datetime.timedelta(days=1)).strftime(date_format)
    first_day_of_closed_month = (month.replace(day=1) - datetime.timedelta(days=1)).replace(day=1).strftime(
        date_format)
    now = datetime.datetime.now().strftime(date_format)

    params = json.dumps(dict(next_month=first_day_of_next_month,
                             first_day_of_next_month=first_day_of_next_month,
                             closed_month=last_day_of_closed_month,
                             first_day_of_closed_month=first_day_of_closed_month,
                             last_day_of_closed_month=last_day_of_closed_month,
                             now=now))

    with open(sql_params, 'w') as f:
        json.dump({'sql_params': params}, f)


if __name__ == '__main__':
    main()
