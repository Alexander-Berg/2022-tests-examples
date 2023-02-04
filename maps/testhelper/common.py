from dataclasses import dataclass, field
from typing import List, Tuple


# common2.Point
@dataclass
class Point:
    lon: float
    lat: float


def set_point(msg, pt):
    msg.point.lon = pt.lon
    msg.point.lat = pt.lat


# common2.Metadata
def add_metadata(msg, ext):
    return msg.metadata.add().Extensions[ext]


# common2.KeyValuePair
def add_property(msg, key, value):
    p = msg.property.add()
    p.key = key
    p.value = value


# common2.i18n.Money
@dataclass
class Money:
    value: float
    text: str
    currency: str


def set_price(msg, price):
    msg.price.value = price.value
    msg.price.text = price.text
    msg.price.currency = price.currency


def RUR(n):
    return Money(float(n), f'{n} ₽', 'RUR')


# common2.Image
@dataclass
class Image:
    url_template: str
    tags: List[str] = field(default_factory=list)


def set_image(msg, image):
    msg.url_template = image.url_template
    msg.tag.extend(image.tags)


def add_image(msg, image):
    set_image(msg.image.add(), image)


@dataclass
class Icon:
    image: Image
    anchor: Tuple[float, float] = (0.5, 0.5)


def set_icon(msg, icon):
    set_image(msg.image, icon.image)
    msg.anchor.x = icon.anchor[0]
    msg.anchor.y = icon.anchor[1]


def add_icon(msg, icon):
    set_icon(msg.icon.add(), icon)
