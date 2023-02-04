class UserAgentCreator(object):
    fallback_func = None
    _mapper = {}

    def set_fallback(self, func):
        self.fallback_func = func

    def register(self, name, func):
        self._mapper[name] = func

    def create(self, name, *args, **kwargs):
        if name not in self._mapper:
            if self.fallback_func:
                return self.fallback_func(*args, **kwargs)
            raise ValueError

        return self._mapper[name](*args, **kwargs)


user_agent_creator = UserAgentCreator()
