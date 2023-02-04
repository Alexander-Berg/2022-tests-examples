# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Report
from awacs.wrappers.errors import ValidationError


def test_report():
    pb = modules_pb2.ReportModule()

    report = Report(pb)

    with pytest.raises(ValidationError) as e:
        report.validate(chained_modules=True)
    e.match('either "uuid" or "refers" must be specified')

    pb.uuid = "a.a.a"
    report.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        report.validate(chained_modules=True)
    e.match('must match ')

    pb.uuid = "test"
    report.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        report.validate(chained_modules=True)
    e.match('either "ranges" or "refers" must be specified')

    pb.ranges = "test"
    report.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        report.validate(chained_modules=True)
    e.match('ranges.*is not a valid timedeltas string')

    pb.ranges = "1s,2s"
    report.update_pb(pb)

    report.validate(chained_modules=True)
