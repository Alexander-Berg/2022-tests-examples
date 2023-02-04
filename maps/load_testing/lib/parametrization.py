import random


class Parametrized:
    def get(self):
        raise NotImplementedError

    def as_dict(self):
        return self.__dict__


class OneOf(Parametrized):
    def __init__(self, values, weights=None):
        self.values = values.copy()
        self.weights = None
        if weights:
            if len(values) != len(weights):
                raise Exception("Length of `values` and `weights` must be equal.")
            self.weights = weights.copy()

    def get(self):
        return random.choices(self.values, self.weights)[0]


class Linspace(Parametrized):
    def __init__(self, min_value, max_value, step, weights=None):
        self.min_value = min_value
        self.max_value = max_value
        self.step = step
        self.max_num_steps = (max_value - min_value) // step
        self.weights = None
        if weights:
            if self.max_num_steps + 1 != len(weights):
                raise Exception("Number of steps from `min_value` to `max_value` must be equal to the length of `weights` list.")
            self.weights = weights.copy()

    def get(self):
        if self.weights:
            return self.min_value + self.step * random.choices([k for k in range(self.max_num_steps + 1)], self.weights)[0]
        return self.min_value + self.step * random.randint(0, self.max_num_steps)


class RoutingPublicApiPoints(Parametrized):
    def __init__(self, linspace_lat, linspace_lon, linspace_amount):
        self.linspace_lat = linspace_lat
        self.linspace_lon = linspace_lon
        self.linspace_amount = linspace_amount

    def get(self):
        points = [
            {
                'lat': self.linspace_lat.get(),
                'lon': self.linspace_lon.get()
            }
            for _ in range(int(self.linspace_amount.get()))
        ]
        return '|'.join([f"{point['lat']:.6f},{point['lon']:.6f}" for point in points])


class Settings(Parametrized):
    def __init__(self, str_template):
        self.str_template = str_template

    def get(self, settings):
        return self.str_template.format(**settings)


ALL_GENERATORS = {
    cls.__name__: cls for cls in Parametrized.__subclasses__()
}


class RequestsProvider:
    def __init__(self, template):
        self.template = template

    @staticmethod
    def load(json_data):
        assert isinstance(json_data, dict)

        def parse(data):
            if isinstance(data, dict):
                if '_type_' not in data:
                    return {k: parse(v) for k, v in data.items()}

                args = data.copy()
                type_ = args.pop('_type_')
                cls = ALL_GENERATORS[type_]
                return cls(**{k: parse(v) for k, v in args.items()})
            elif isinstance(data, list):
                return [parse(obj) for obj in data]
            else:
                return data

        return RequestsProvider(template=parse(json_data))

    def next_request(self, settings):
        return RequestsProvider._get_concrete_data(self.template, settings)

    @staticmethod
    def _get_concrete_data(node, settings):
        if isinstance(node, Settings):
            return node.get(settings)
        elif isinstance(node, Parametrized):
            return RequestsProvider._get_concrete_data(node.get(), settings)
        elif isinstance(node, dict):
            return {k: RequestsProvider._get_concrete_data(v, settings) for k, v in node.items()}
        elif isinstance(node, list):
            return [RequestsProvider._get_concrete_data(obj, settings) for obj in node]
        else:
            return node
