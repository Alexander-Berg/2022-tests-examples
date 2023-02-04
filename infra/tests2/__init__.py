CLEAN_AFTER_EACH_RUN = True

pytest_plugins = [
    'tests2._bricks.copier',
    'tests2._bricks.filegenerator',
    'tests2._bricks.supervisors'
]
