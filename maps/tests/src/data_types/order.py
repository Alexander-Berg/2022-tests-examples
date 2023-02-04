import rstr


class Car:
    def __init__(self, title=None, plate=None):
        self.plate = plate or rstr.xeger("[АВЕКМНОРСТХУ]\\d{3}[АВЕКМНОРСТХУ]{2}\\d{2,3}")
        self.title = title or rstr.letters(5, 20)

    def toJson(self):
        return {
            "title": self.title,
            "plate": self.plate
        }


class Order:
    def __init__(self, phone, car, carwash_id=None):
        self.phone = phone
        self.car = car
        self.carwash_id = carwash_id or rstr.letters(5, 20)

    def toJson(self):
        return {
            "car": self.car.toJson(),
            "user": {
                "phone_id": self.phone.id
            },
            "carwash_id": self.carwash_id
        }
