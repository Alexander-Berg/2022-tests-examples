import factory
import faker
from factory.django import DjangoModelFactory

from ..models import ColorTheme

fake = faker.Faker()


def gradient_color():
    return f"linear-gradient(to right, {fake.hex_color()}, {fake.hex_color()})"


class ColorThemeFactory(DjangoModelFactory):
    class Meta:
        model = ColorTheme

    name = factory.Faker("color_name")
    slug = factory.LazyAttributeSequence(lambda c, n: f"{n}-{c.name.lower()}")

    course_card_gradient_color = factory.LazyFunction(gradient_color)
