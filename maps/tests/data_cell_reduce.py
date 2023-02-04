TABLE_0 = [
    {'lon': 31.0, 'lat': 41.0},
    {'lon': 50.0, 'lat': 51.0},
]

TABLE_1 = [
    {'lon': 34.2, 'lat': 41.0},
    {'lon': 50.0, 'lat': 51.0},
]

TABLE_2 = [
    {'lon': 37.0, 'lat': 40.0},
    {'lon': 50.0, 'lat': 51.0},
    {'lon': 59.99, 'lat': 69.99},
    {'lon': 59.99, 'lat': 70.01},
    {'lon': 60.01, 'lat': 69.99},
    {'lon': 60.01, 'lat': 70.01},
]

RESULT = [
    # Table 0, single cell
    {'_x_cell': 15, '_y_cell': 20, 'lat': 41.0, 'lon': 31.0, '_table_name': b'0'},
    # Table 1, two cell by x
    {'_x_cell': 16, '_y_cell': 20, 'lat': 41.0, 'lon': 34.2, '_table_name': b'1'},
    {'_x_cell': 17, '_y_cell': 20, 'lat': 41.0, 'lon': 34.2, '_table_name': b'1'},
    # Table 2, two cell by y
    {'_x_cell': 18, '_y_cell': 19, 'lat': 40.0, 'lon': 37.0, '_table_name': b'2'},
    {'_x_cell': 18, '_y_cell': 20, 'lat': 40.0, 'lon': 37.0, '_table_name': b'2'},
    # Tables 0, 1, 2; two cells by x
    {'_x_cell': 24, '_y_cell': 25, 'lat': 51.0, 'lon': 50.0, '_table_name': b'0'},
    {'_x_cell': 24, '_y_cell': 25, 'lat': 51.0, 'lon': 50.0, '_table_name': b'1'},
    {'_x_cell': 24, '_y_cell': 25, 'lat': 51.0, 'lon': 50.0, '_table_name': b'2'},
    {'_x_cell': 25, '_y_cell': 25, 'lat': 51.0, 'lon': 50.0, '_table_name': b'0'},
    {'_x_cell': 25, '_y_cell': 25, 'lat': 51.0, 'lon': 50.0, '_table_name': b'1'},
    {'_x_cell': 25, '_y_cell': 25, 'lat': 51.0, 'lon': 50.0, '_table_name': b'2'},
    # Table 3, diagonal cells
    {'_x_cell': 29, '_y_cell': 34, 'lat': 69.99, 'lon': 59.99, '_table_name': b'2'},
    {'_x_cell': 29, '_y_cell': 35, 'lat': 69.99, 'lon': 59.99, '_table_name': b'2'},
    {'_x_cell': 30, '_y_cell': 34, 'lat': 69.99, 'lon': 59.99, '_table_name': b'2'},
    {'_x_cell': 30, '_y_cell': 35, 'lat': 69.99, 'lon': 59.99, '_table_name': b'2'},
    {'_x_cell': 29, '_y_cell': 34, 'lat': 70.01, 'lon': 59.99, '_table_name': b'2'},
    {'_x_cell': 29, '_y_cell': 35, 'lat': 70.01, 'lon': 59.99, '_table_name': b'2'},
    {'_x_cell': 30, '_y_cell': 34, 'lat': 70.01, 'lon': 59.99, '_table_name': b'2'},
    {'_x_cell': 30, '_y_cell': 35, 'lat': 70.01, 'lon': 59.99, '_table_name': b'2'},
    {'_x_cell': 29, '_y_cell': 34, 'lat': 69.99, 'lon': 60.01, '_table_name': b'2'},
    {'_x_cell': 29, '_y_cell': 35, 'lat': 69.99, 'lon': 60.01, '_table_name': b'2'},
    {'_x_cell': 30, '_y_cell': 34, 'lat': 69.99, 'lon': 60.01, '_table_name': b'2'},
    {'_x_cell': 30, '_y_cell': 35, 'lat': 69.99, 'lon': 60.01, '_table_name': b'2'},
    {'_x_cell': 29, '_y_cell': 34, 'lat': 70.01, 'lon': 60.01, '_table_name': b'2'},
    {'_x_cell': 29, '_y_cell': 35, 'lat': 70.01, 'lon': 60.01, '_table_name': b'2'},
    {'_x_cell': 30, '_y_cell': 34, 'lat': 70.01, 'lon': 60.01, '_table_name': b'2'},
    {'_x_cell': 30, '_y_cell': 35, 'lat': 70.01, 'lon': 60.01, '_table_name': b'2'},
]
