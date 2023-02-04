class StringId:
    def __eq__(self, another):
        if not isinstance(another, str):
            return False
        try:
            int(another)
            return True
        except:
            return False
