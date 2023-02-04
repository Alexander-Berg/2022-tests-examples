ON_OFF_SKILL = "devices.capabilities.on_off"
TOGGLE_SKILL = "devices.capabilities.toggle"

ENGINE_INSTANCE_SKILL = "on"
LOCK_INSTANCE_SKILL = "central_lock"
TRUNK_INSTANCE_SKILL = "trunk"


class CarDevice:
    @staticmethod
    def from_car(car):
        dev = CarDevice()

        dev.id = car.id
        dev.manufacturer = car.brand,
        dev.model = car.model
        dev.description = car.brand + " " + car.model
        dev.room = "улица"
        dev.type = "devices.types.remote_car"
        dev.skills = {
            ON_OFF_SKILL: {
                ENGINE_INSTANCE_SKILL: False
            },
            TOGGLE_SKILL: {
                LOCK_INSTANCE_SKILL: False
            }
        }
        if "trunk" in car.details["features"]:
            dev.skills[TOGGLE_SKILL][TRUNK_INSTANCE_SKILL] = False
        return dev

    @staticmethod
    def from_json(car):
        dev = CarDevice()

        dev.id = car["id"]
        dev.manufacturer = car["device_info"]["manufacturer"],
        dev.model = car["device_info"]["model"]
        dev.description = car["description"]
        dev.room = car["room"]
        dev.type = car["type"]
        dev.skills = {}
        for cap in car["capabilities"]:
            if not dev.skills.get(cap["type"]):
                dev.skills[cap["type"]] = {}
            dev.skills[cap["type"]][cap["parameters"]["instance"]] = False
        return dev

    def __eq__(self, another):
        return isinstance(another, CarDevice) and self.__dict__ == another.__dict__

    def has_trunk_support(self):
        return self.skills[TOGGLE_SKILL].get(TRUNK_INSTANCE_SKILL) is not None

    def get_lock_state(self):
        return self.skills[TOGGLE_SKILL][LOCK_INSTANCE_SKILL]

    def get_trunk_state(self):
        return self.skills[TOGGLE_SKILL][TRUNK_INSTANCE_SKILL]

    def get_engine_state(self):
        return self.skills[ON_OFF_SKILL][ENGINE_INSTANCE_SKILL]

    def read_state_from_json(self, data):
        for cap in data["capabilities"]:
            skill = self.skills.get(cap["type"])
            assert skill is not None, data
            assert cap["state"]["instance"] in skill
            skill[cap["state"]["instance"]] = cap["state"]["value"]

    def get_lock_action(self, new_state=None):
        return DeviceAction(self.id, skill_type=TOGGLE_SKILL,
                            skill_instance=LOCK_INSTANCE_SKILL,
                            new_state=new_state)

    def get_engine_action(self, new_state=None):
        return DeviceAction(self.id, skill_type=ON_OFF_SKILL,
                            skill_instance=ENGINE_INSTANCE_SKILL,
                            new_state=new_state)

    def get_trunk_action(self, new_state=None):
        return DeviceAction(self.id, skill_type=TOGGLE_SKILL,
                            skill_instance=TRUNK_INSTANCE_SKILL,
                            new_state=new_state)


class DeviceAction:
    def __init__(self, device_id, skill_type, skill_instance, new_state):
        self.device_id = device_id
        self.skill_type = skill_type
        self.skill_instance = skill_instance
        self.new_state = new_state

    def get_query(self):
        return {
            "id": self.device_id,
            "capabilities": [{
                "type": self.skill_type,
                "state": {
                    "instance": self.skill_instance,
                    "value": self.new_state
                }
            }]
        }

    def save_result(self, result):
        for dev in result["payload"]["devices"]:
            if dev["id"] == self.device_id:
                for cap in dev["capabilities"]:
                    if cap["type"] == self.skill_type and cap["state"]["instance"] == self.skill_instance:
                        self.result = cap["state"]["action_result"]
                        return
        assert False, result

    def succeded(self):
        return self.result["status"] == "DONE"

    def error_code(self):
        return self.result["error_code"]
