from collections import defaultdict
from io import BytesIO
from typing import Dict

from PIL import Image

from staff.celery_app import app
from staff.lib.tasks import LockedTask

from staff.map.forms.forms import FloorMapForm, get_file_name
from staff.map.models import FloorMap
from staff.map.tiles import slice_map


saved_instances = {}


def save_to_mock_storage(key, file_):
    global saved_instances
    file_.open(mode='rb')
    saved_instances[key] = file_.read()
    file_.close()


def fetch_from_mock_storage(key):
    return saved_instances[key]


class MockFloorMapForm(FloorMapForm):
    def save(self, **kwargs):
        obj = super(FloorMapForm, self).save(**kwargs)

        obj.file_name = get_file_name(obj)
        obj.save()

        img = self.files.get('image')
        save_to_mock_storage(obj.file_name, img)
        return obj


@app.task(ignore_result=True)
class MockTileCutter(LockedTask):
    counter: Dict[tuple, int] = defaultdict(int)

    def save(self, zoom, x, y, tile):
        self.counter[(zoom, x, y, tile.tobytes())] += 1

    def locked_run(self, map_id, *args, **kwargs):
        self.floor_map = FloorMap.objects.get(id=map_id)
        self.cut()

        self.floor_map.is_ready = True
        self.floor_map.save()

    def cut(self):
        src_file = fetch_from_mock_storage(self.floor_map.file_name)
        stream = BytesIO(src_file)
        map_image = Image.open(stream)

        tiles = slice_map(
            map_image,
            min_zoom=self.floor_map.min_zoom,
            max_zoom=self.floor_map.max_zoom,
            zero_zoom=self.floor_map.zero_zoom,
            tile_size=self.floor_map.tile_size,
        )

        for zoom, x, y, tile in tiles:
            self.save(zoom, x, y, tile)
