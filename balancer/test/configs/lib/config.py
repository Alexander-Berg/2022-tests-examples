class Config(object):
    def __init__(self,
                 module_name,  # module name
                 backends,  # list of backends
                 paths,  # list of paths
                 native_location,  # "MAN"/"SAS"/"VLA"
                 has_antirobot=False,
                 has_laas=True,
                 has_uaas=True,
                 ):
        self.module_name = module_name
        self.path = None
        self.paths = paths
        self.backends = backends
        self.native_location = native_location
        self.has_antirobot = has_antirobot
        self.has_laas = has_laas
        self.has_uaas = has_uaas

    def get_options(self):
        return {
            "has_antirobot": self.has_antirobot,
            "has_laas": self.has_laas,
            "has_uaas": self.has_uaas,
        }
