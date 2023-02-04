from datetime import datetime, timedelta
import dateutil
from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_post,
    create_order,
    create_photo_post_dict,
)

from ya_courier_backend.models import db, Photo
from ya_courier_backend.tasks.delete_expired_photos import DeleteExpiredPhotosTask


TEST_DATETIME = datetime(2022, 2, 28, 12, 10, 0, tzinfo=dateutil.tz.gettz("Europe/Moscow"))


@skip_if_remote
def test_delete_expired_photos_task(env: Environment):
    with freeze_time(TEST_DATETIME) as freezed_time:
        new_order = create_order(env, route_id=env.default_route.id)
        path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
        post_data = [
            create_photo_post_dict(env, created_at=(TEST_DATETIME+timedelta(seconds=1)).isoformat()),
            create_photo_post_dict(env, created_at=TEST_DATETIME.isoformat()),
            create_photo_post_dict(env, created_at=TEST_DATETIME.isoformat()),
        ]
        post_response = local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=post_data)

        freezed_time.tick(delta=timedelta(weeks=2))

        with env.flask_app.app_context():
            task = DeleteExpiredPhotosTask(env.flask_app)
            task.run()

        with env.flask_app.app_context():
            db_photos = db.session.query(Photo).all()

        assert len(db_photos) == 1
        assert db_photos[0].id == int(post_response[0]['id'])
