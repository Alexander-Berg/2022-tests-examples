from PIL import Image, ImageChops


def change_source(im, R=1, G=1, B=1):
    source = im.split()

    Red = source[0].point(lambda i: i / R)
    Green = source[1].point(lambda i: i / G)
    Blue = source[2].point(lambda i: i / B)
    A = source[3]

    return Image.merge('RGBA', (Red, Green, Blue, A))


if __name__ == "__main__":
    base = Image.open('pillow/base.png')
    new = Image.open('pillow/new.png')

    # --------------------------------------------------------------------------

    # diff = ImageChops.difference(base, new)
    # diff = ImageChops.invert(diff)
    # ratio = 0.75
    # blended = ImageChops.blend(base, diff, ratio)
    # blended.show()

    # --------------------------------------------------------------------------

    left = ImageChops.subtract(base, new)
    right = ImageChops.subtract(new, base)

    left = ImageChops.invert(left)
    right = ImageChops.invert(right)

    left = change_source(left, R=0.25)
    right = change_source(right, B=0.25)
    comb = ImageChops.multiply(left, right)

    ratio = 0.75
    blended = ImageChops.blend(base, comb, ratio)
    blended.show()
    pass
