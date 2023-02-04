from datetime import date
from typing import Optional, Dict, List

from staff.workspace_management import entities
from staff.workspace_management.entities import RoomSharePie


class RepositoryMock(entities.Repository):
    def __init__(self) -> None:
        self._last_share_pie_for_room: Dict[int, entities.RoomSharePie] = {}
        self.saved_share_pies: List[entities.RoomSharePie] = []

    def save_share_pie(self, share_pie: entities.RoomSharePie) -> None:
        self.saved_share_pies.append(share_pie)

    def set_last_share_pie_for_room(self, room_share_pie: entities.RoomSharePie) -> None:
        self._last_share_pie_for_room[room_share_pie.room_id] = room_share_pie

    def last_share_pie_for_room(self, room_id: int) -> Optional[entities.RoomSharePie]:
        return self._last_share_pie_for_room.get(room_id)

    def share_pie_for_room_and_date(self, room_id: int, current_date: date) -> Optional[RoomSharePie]:
        return None

    def share_pies_for_rooms_and_date(self, room_ids: List[int], current_date: date) -> List[RoomSharePie]:
        return []
