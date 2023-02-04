from yb_darkspirit.application.plugins.dbhelper import Session


def insert_into_oebs_data(session, inventary_number, serial_number):
    # type: (Session, int, str) -> None
    query_template = (
        "insert into V_OEBS_DATA " 
        "(instance_id, INSTANCE_NUMBER, SERIAL_NUMBER, ITEM_CONCATENATED_SEGMENTS)" 
        " values (1, '{inventary_number}', {serial_number}, "
        "'BU>CASH_REGISTER>STARRUS>KKT RP Sistema 1FA')"
    )
    session.execute(query_template.format(serial_number=serial_number, inventary_number=inventary_number))
