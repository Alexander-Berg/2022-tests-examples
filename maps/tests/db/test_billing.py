import pytest
from asyncpg.exceptions import CheckViolationError, NotNullViolationError

from maps_adv.adv_store.v2.lib.db.tables import billing, cpa, cpm, fix

pytestmark = [pytest.mark.asyncio]


async def test_create_fix(db, fix_data):
    await db.rw.execute(fix.insert().values(fix_data))
    row = await db.rw.fetch_one(fix.select())
    assert isinstance(row["id"], int)


async def test_create_cpm(db, cpm_data):
    await db.rw.execute(cpm.insert().values(cpm_data))
    row = await db.rw.fetch_one(cpm.select())
    assert isinstance(row["id"], int)


async def test_create_cpa(db, cpa_data):
    await db.rw.execute(cpa.insert().values(cpa_data))
    row = await db.rw.fetch_one(cpa.select())
    assert isinstance(row["id"], int)


@pytest.mark.parametrize("column_name", ["time_interval", "cost"])
async def test_fix_column_not_nullable(db, fix_data, column_name):
    fix_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(fix.insert().values(fix_data))


async def test_cpm_cost_not_nullable(db, cpm_data):
    cpm_data["cost"] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(cpm.insert().values(cpm_data))


async def test_cpa_cost_not_nullable(db, cpa_data):
    cpa_data["cost"] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(cpa.insert().values(cpa_data))


@pytest.mark.parametrize("column_name", ["cost", "budget", "daily_budget"])
async def test_cpm_field_positive(db, cpm_data, column_name):
    cpm_data[column_name] = -1

    with pytest.raises(CheckViolationError):
        await db.rw.execute(cpm.insert().values(cpm_data))


@pytest.mark.parametrize("column_name", ["cost", "budget", "daily_budget"])
async def test_cpa_field_positive(db, cpa_data, column_name):
    cpa_data[column_name] = -1

    with pytest.raises(CheckViolationError):
        await db.rw.execute(cpa.insert().values(cpa_data))


@pytest.mark.parametrize("billing_type", ["fix", "cpm", "cpa"])
async def test_create_billing(db, fix_id, cpm_id, cpa_id, billing_type):
    billing_data = {
        "fix_id": fix_id if billing_type == "fix" else None,
        "cpm_id": cpm_id if billing_type == "cpm" else None,
        "cpa_id": cpa_id if billing_type == "cpa" else None,
    }
    await db.rw.execute(billing.insert().values(billing_data))
    row = await db.rw.fetch_one(billing.select())
    assert isinstance(row["id"], int)


async def test_fix_cost_positive(db, fix_data):
    fix_data["cost"] = 0

    with pytest.raises(CheckViolationError):
        await db.rw.execute(fix.insert().values(fix_data))


@pytest.mark.parametrize("column_name", ["cost", "budget", "daily_budget"])
async def test_cpm_column_positive(db, cpm_data, column_name):
    cpm_data[column_name] = 0

    with pytest.raises(CheckViolationError):
        await db.rw.execute(cpm.insert().values(cpm_data))


@pytest.mark.parametrize("column_name", ["cost", "budget", "daily_budget"])
async def test_cpa_column_positive(db, cpa_data, column_name):
    cpa_data[column_name] = 0

    with pytest.raises(CheckViolationError):
        await db.rw.execute(cpa.insert().values(cpa_data))


@pytest.mark.parametrize(
    "present_columns",
    [
        ("fix_id", "cpm_id", "cpa_id"),
        ("fix_id", "cpm_id"),
        ("cpm_id", "cpa_id"),
        ("fix_id", "cpa_id"),
    ],
)
async def test_billing_oneof_type(db, fix_id, cpm_id, cpa_id, present_columns):
    billing_data = {
        "fix_id": fix_id if "fix_id" in present_columns else None,
        "cpm_id": cpm_id if "cpm_id" in present_columns else None,
        "cpa_id": cpa_id if "cpa_id" in present_columns else None,
    }

    with pytest.raises(CheckViolationError):
        await db.rw.execute(billing.insert().values(billing_data))
