def test_get_status_presentation():
    from staff.dismissal.models import DISMISSAL_STATUS
    from staff.dismissal.reports.reports import DismissalReport

    for status, _ in DISMISSAL_STATUS:
        res = DismissalReport.get_status_presentation(status)
        assert isinstance(res, str)
