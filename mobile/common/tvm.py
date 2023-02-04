from django.conf import settings
from tvm2 import TVM2


class TVMServiceTicketError(ValueError):
    pass


def get_tvm_client():
    # TVM2 object is a singleton
    return TVM2(
        client_id=settings.TVM_CLIENT_ID,
        secret=settings.TVM_CLIENT_SECRET,
        blackbox_client=settings.BLACKBOX_CLIENT,
        destinations=(settings.BLACKBOX_CLIENT_ID, settings.BIGB_CLIENT_ID),
    )


def add_service_ticket_header(client_id, headers=None):
    """
    Method patches headers dict, adding "X-Ya-Service-Ticket" header for proper client_id
    :param client_id: string
    :param headers: dict
    :return: dict
    """
    if headers is None:
        headers = {}

    tvm = get_tvm_client()
    tickets = tvm.get_service_tickets(client_id)
    ticket = tickets.get(client_id)

    if not ticket:
        raise TVMServiceTicketError("Empty TVM service-ticket for client_id: %s", client_id)

    headers['X-Ya-Service-Ticket'] = ticket
    return headers
