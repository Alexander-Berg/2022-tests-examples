def geometry_to_bbox_mock(fake_hex_encoded_mercator_wkb):
    '''
    Converts a comma separated byte-string of 4 float numbers to bounding box.
    Format: `lat_min, lon_min, lat_max, lon_max`.
    '''
    if fake_hex_encoded_mercator_wkb is not None:
        coords = [float(value) for value in fake_hex_encoded_mercator_wkb.split(b',')]
    else:
        coords = [0, 0, 0, 0]

    return {
        'lat_min': coords[0], 'lon_min': coords[1], 'lat_max': coords[2], 'lon_max': coords[3]
    }
