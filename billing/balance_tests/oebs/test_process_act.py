from tests import object_builder as ob
import pytest
import datetime
import mock
from balance.processors.oebs import process_act
from balance import mapper

NOW = datetime.datetime.now()


@pytest.fixture
def act(session):
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Act': {'pct': 100}}
    invoice = ob.InvoiceBuilder.construct(session)
    order = invoice.invoice_orders[0].order
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice.dt)
    order.calculate_consumption(NOW, {order.shipment_type: 10})
    act, = invoice.generate_act(backdate=NOW, force=1)
    return act


def test_not_reexport_when_exporting(session, act):
    act.export()
    session.flush()
    act.exports['OEBS_API'].state = 0
    act_export = (session
                  .query(mapper.Export)
                  .filter_by(object_id=act.id, type='OEBS_API', classname='Act')
                  .first()
                  )
    act_export.export = mock.MagicMock()
    result = process_act(act.exports['OEBS'])
    assert result == 'Act is exporting in OEBS_API'
    assert act_export.export.call_count == 0


def test_not_reexport_when_exported(session, act):
    act.export()
    session.flush()
    act.exports['OEBS_API'].state = 1
    act.exports['OEBS_API'].export_dt = datetime.datetime.now()
    act_export = (session
                  .query(mapper.Export)
                  .filter_by(object_id=act.id, type='OEBS_API', classname='Act')
                  .first()
                  )
    act_export.export = mock.MagicMock()
    result = process_act(act.exports['OEBS'])
    assert result == 'Act has been already exported in OEBS_API'
    assert act_export.export.call_count == 0
