import pytest

from hamcrest import assert_that, equal_to, has_entries, match_equality

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.mark.asyncio
async def test_returned(partner, role, app):
    r = await app.get('/api/web/v1/partners')

    assert_that(r.status, equal_to(200))
    data = await r.json()
    assert_that(
        data,
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'partners': [
                        match_equality(
                            has_entries(
                                {
                                    'partner_id': str(partner.partner_id),
                                }
                            )
                        )
                    ]
                },
            }
        ),
    )
