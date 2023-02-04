from .common import get_multiple_tpts_data as base_get_multiple_tpts_data, SERVICES_INFO


class TicketToEventOverrides(object):
    service_ids, _ = SERVICES_INFO["TicketsToEvents"]

    @staticmethod
    def fill_service_id(tpts):
        for tpt in tpts:
            tpt.update({"service_id": TicketToEventOverrides.service_ids[0]})
        return tpts

    @staticmethod
    def get_multiple_tpts_data(begin_dt, end_dt, act_dt):
        tpts = base_get_multiple_tpts_data(begin_dt, end_dt, act_dt)
        for tpt in tpts:
            tpt.update({"service_code": None})
        return TicketToEventOverrides.fill_service_id(tpts)
