from util.datetime.base cimport TDuration


def seconds_from_duration(duration_str):
    return TDuration.Parse(duration_str).Seconds()
