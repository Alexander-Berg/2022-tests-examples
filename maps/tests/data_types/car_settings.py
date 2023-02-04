import random


class CarSettings:
    def __init__(self, user, json=None):
        self.value = json if json else CarSettings.get_default_settings(user)

    def to_dict(self):
        return self.value

    @staticmethod
    def random_sorted_weekdays(k):
        WEEKDAYS = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun']
        indexes_range = range(len(WEEKDAYS))
        indexes_sample = sorted(random.sample(indexes_range, k))
        return [WEEKDAYS[i] for i in indexes_sample]

    @staticmethod
    def get_dummy_autostarts_schedule():
        return {
            'isEnabled': random.randint(0, 1) > 0,
            'days': CarSettings.random_sorted_weekdays(2),
            'time': {
                'hours': random.randint(0, 23),
                'minutes': random.randint(0, 59)
            }
        }

    @staticmethod
    def get_default_settings(user):
        return {
            'engine': {
                'autostartConditions': {
                    'engineTemperature': {
                        'isEnabled': False,
                        'max': 0,
                        'min': -40,
                        'step': 1,
                        'value': -20},
                    'schedules': [],
                    'timeInterval': {
                        'isEnabled': False,
                        'maxSeconds': 82800,
                        'minSeconds': 3600,
                        'stepSeconds': 3600,
                        'valueSeconds': 3600},
                    'voltage': {
                        'isEnabled': False,
                        'max': 12.5,
                        'min': 10,
                        'step': 0.1,
                        'value': 11.8}},
                'stopConditions': {
                    'timeout': {
                        'currentSeconds': 1200,
                        'valuesSeconds': [600, 1200, 1800]}}},
            'isOwner': True,
            'maxAdditionalPhones': 2,
            'maxSharedAccesses': 1,
            'notifications': {
                'additionalNumbers': [],
                'additionalPhones': [],
                'myPhone': user.get_masked_phone(),
                'phoneCalls': {
                    'isEnabled': True,
                    'items': [
                        {
                            'id': 'Alarm',
                            'isEnabled': True,
                            'title': 'Тревога: угон или эвакуация'}]},
                'push': {
                    'isEnabled': True,
                    'items': [
                        {
                            'id': 'AlarmMovement',
                            'isEnabled': True,
                            'title': 'Тревога: угон или эвакуация'},
                        {
                            'id': 'EngineState',
                            'isEnabled': True,
                            'title': 'Запуск и остановка двигателя'},
                        {
                            'id': 'LockUnlock',
                            'isEnabled': True,
                            'title': 'Открытие и закрытие автомобиля'},
                        {
                            'id': 'Maintenance',
                            'isEnabled': True,
                            'title': 'Включение режима обслуживания'},
                        {
                            'id': 'BatteryState',
                            'isEnabled': True,
                            'title': 'Низкий заряд аккумулятора'},
                        {
                            'id': 'Balance',
                            'isEnabled': True,
                            'title': 'Низкий баланс на сим-карте в машине'},
                        {
                            'id': 'TankState',
                            'isEnabled': True,
                            'title': 'Низкий уровень топлива'},
                        {
                            'id': 'ConnectionState',
                            'isEnabled': False,
                            'title': 'Нет связи с машиной'}]},
                'sms': {
                    'isEnabled': True,
                    'items': [
                        {
                            'id': 'Alarm',
                            'isEnabled': True,
                            'title': 'Тревога: угон или эвакуация'}]}},
            'sharedAccess': []}

    def notifications_random_change(self):
        method_name = random.choice(['phoneCalls', 'sms', 'push'])
        method = self.value['notifications'][method_name]
        item_num = int(random.random() * (len(method["items"]) + 1))
        if item_num == 0:
            method["isEnabled"] = not method["isEnabled"]
        else:
            item = method["items"][item_num - 1]
            item["isEnabled"] = not item["isEnabled"]
