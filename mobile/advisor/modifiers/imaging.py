from operator import itemgetter
from urllib import quote_plus

from yaphone.advisor.common.screen_properties import dp_to_px

FEED_FULL_IMAGE_SIZE_DPI = 320, 400
FEED_PREVIEW_IMAGE_SIZE_DPI = 112, 140
FEED_BACKGROUND_IMAGE_SIZE_DPI = 340, 340

xy_getter = itemgetter('x', 'y')


def get_scaled_image_url(host, image_url, dpi=0, w_dpi=0, h_dpi=0, w_px=0, h_px=0):
    return u'https://%s/get_image/?url=%s&dpi=%s&w_dpi=%s&h_dpi=%s&w_pix=%s&h_pix=%s' % \
           (host, quote_plus(image_url), dpi, w_dpi, h_dpi, w_px, h_px)


def get_thumbnail_size(orig_size, bounding_box):
    """
    Calculate new size of image scaled to fit bounding box keeping the aspect ratio. Copied from PIL library:
    https://github.com/python-pillow/Pillow/blob/c9f54c98a5dc18685a9bf8c8822f770492a796d6/PIL/Image.py#L1923
    :param orig_size: -  (width, height) tuple of original size of image
    :param bounding_box: -  (width, height) tuple of bounding_box to fit the image in
    :return: new size of image - (width, height) tuple
    """
    x, y = orig_size
    if x > bounding_box[0]:
        y = int(max(y * bounding_box[0] / x, 1))
        x = int(bounding_box[0])
    if y > bounding_box[1]:
        x = int(max(x * bounding_box[1] / y, 1))
        y = int(bounding_box[1])
    return x, y


def get_scaled_image_info(host, image_url, dpi, size_dpi, orig_size):
    bounding_box_size = [dp_to_px(x, dpi) for x in size_dpi]
    width, height = get_thumbnail_size(orig_size, bounding_box_size)
    return {
        'width': width,
        'height': height,
        'url': get_scaled_image_url(host, image_url, w_px=width, h_px=height)
    }


def make_screenshot_blocks(host, app, dpi):
    for image_url, screenshot_info in zip(app.screenshot_urls, app.screenshots_info):
        if screenshot_info:
            try:
                orig_size = xy_getter(screenshot_info['meta']['orig-size'])
            except KeyError:
                orig_size = 0, 0
        preview = get_scaled_image_info(host, image_url, dpi, size_dpi=FEED_PREVIEW_IMAGE_SIZE_DPI, orig_size=orig_size)
        full = get_scaled_image_info(host, image_url, dpi, size_dpi=FEED_FULL_IMAGE_SIZE_DPI, orig_size=orig_size)
        app.add_screenshot_block(preview=preview, full=full)
