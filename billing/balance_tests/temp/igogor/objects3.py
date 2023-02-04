# coding: utf-8

from datetime import datetime

import btestlib.utils as utils


class Attribute(object):
    def __init__(self, name=None, dbname=None, apiname=None, default=None, linked=None):
        self.name = name
        self.dbname = dbname
        self.apiname = apiname
        self.default = default
        self.linked = linked

    @property
    def default(self):
        value = self.__dict__.get('default', None)
        if callable(value):
            return value()
        else:
            return value

    @default.setter
    def default(self, value):
        self.__dict__['default'] = value

    def __set__(self, instance, value):
        instance.__dict__[self.name] = value

    def __get__(self, instance, owner):
        if instance is None:
            return self
        return instance.__dict__.get(self.name, None)

    def __hash__(self):
        return hash(self.name)

    def __eq__(self, other):
        return self.name == other.name

    def __ne__(self, other):
        return not (self == other)

    def __repr__(self):
        return '{}(name={}, dbname={}, apiname={}, default={})'.format(type(self).__name__,
                                                                       self.name, self.dbname,
                                                                       self.apiname, self.default)

    def validate(self):
        if not self.name or not self.dbname or not self.apiname:
            raise ObjectError('Attribute is not valid: {!r}'.format(self))


class PrimaryAttribute(Attribute):
    def __set__(self, instance, value):
        # type: (ModelBase, Any) -> None
        super(PrimaryAttribute, self).__set__(instance, value)
        if value is not None:
            ModelObjects.put_in_storage(instance)


# todo-igogor это надо серьезно переименовать
class PropertyAttribute(Attribute):
    def __init__(self, getter,
                 name=None, dbname=None, apiname=None, default=None,
                 linked=None):  # todo-igogor не хотелось бы сюда тянуть все параметры Attribute
        super(PropertyAttribute, self).__init__(name, dbname, apiname, default,
                                                linked)  # todo-igogor как-то не нравится мне
        self.getter = getter

    def __get__(self, instance, owner):
        return self.getter(instance)


class ListAttribute(Attribute):
    def __init__(self, contains,
                 name=None, dbname=None, apiname=None, default=None,
                 linked=None):  # todo-igogor не хотелось бы сюда тянуть все параметры Attribute


        super(ListAttribute, self).__init__(name, dbname, apiname, default,
                                            linked)  # todo-igogor как-то не нравится мне
        self.contains = contains  # todo-igogor а это вообще нахуй здесь?

        # todo-igogor переопределить __getitem__ ?


class LinkedObject(object):
    def __init__(self, linked_to, cls=None):
        # type: (str, BaseModel, str) -> None
        self.linked_to = linked_to
        self.cls = cls

    def __set__(self, instance, value):
        # type: (ModelBase, ModelBase) -> None
        self._ensure_csl(instance)

        primary_attribute_value = getattr(value, type(value).get_primary_attribute().name)
        if not primary_attribute_value:
            raise ObjectError(u"Can't assign {} with empty {}".format(LinkedObject.__name__, PrimaryAttribute.__name__))

        setattr(instance, self.linked_to, primary_attribute_value)

    def __get__(self, instance, owner):
        self._ensure_csl(instance)

        linked_to = getattr(instance, self.linked_to)
        if linked_to:
            # todo-igogor все еще возможны ситуации когда linked_to является id которого у нас нет в хранилище
            return ModelObjects.get_from_storage(model_cls=self.cls, key=linked_to)
        else:
            return None

    def _ensure_csl(self, instance):
        if not self.cls:
            self.cls = type(instance)


class ObjectError(Exception):
    pass


class ModelObjects(object):
    _model_objects = {}

    @classmethod
    def put_in_storage(cls, instance):
        # type: (Any, ModelBase) -> None
        category = type(instance).__name__
        if not cls._model_objects.get(category, None):
            cls._model_objects[category] = {}

        key = getattr(instance, type(instance).get_primary_attribute().name)
        cls._model_objects[category][key] = instance

    @classmethod
    def get_from_storage(cls, model_cls, key):
        return cls._model_objects[model_cls.__name__].get(key, None)


class ModelBase(object):
    # метод задаются декоратором класса
    @classmethod
    def _make_attributes(cls):
        # todo-igogor возможно стоит все-таки использовать метакласс чтобы дефолтная реализация наследовалась
        raise ObjectError('Class {} must define name converters with @attribute'.format(cls.__name__))

    @classmethod
    def attributes(cls):
        # разворачиваем, чтобы атрибуты из сабкласса добавлялись последними
        # и переопределяли атрибуты заданные в базовых классах при пересечении по имени
        # todo-igogor как уберу возможность динамически добавлять аттрибуты - сделать, чтобы кэшировалось,
        # а то собирается 100-500 раз
        model_mro_reversed = [model_cls for model_cls in cls.mro() if issubclass(model_cls, ModelBase)][::-1]
        attr_dict = {}
        for model_cls in model_mro_reversed:
            attr_dict.update({name: value for name, value in model_cls.__dict__.iteritems()
                              if isinstance(value, Attribute)})
        # todo может здесь и возвращать словарь а не список
        return [value for name, value in attr_dict.iteritems()]

    @classmethod
    def get_primary_attribute(cls):
        primary_attributes = [attribute for attribute in cls.attributes() if isinstance(attribute, PrimaryAttribute)]

        if len(primary_attributes) != 1:
            raise ObjectError('Class {} must have one {}'.format(cls.__name__, PrimaryAttribute.__name__))

        return primary_attributes[0]

    @classmethod
    def from_db(cls, **kwargs):
        # type: (dict) -> Client
        normalized_args = {attribute.name: kwargs.get(attribute.dbname, None) for attribute in cls.attributes()}
        return cls(**normalized_args)

    @classmethod
    def from_api(cls, **kwargs):
        # type: (dict) -> Client
        normalized_args = {attribute.name: kwargs.get(attribute.apiname, None) for attribute in cls.attributes()}
        return cls(**normalized_args)

    @classmethod
    def default(cls, **kwargs):
        params = {attr.name: attr.default for attr in cls.attributes()}
        params.update(kwargs)
        return cls(**params)

    def __init__(self, **kwargs):
        for name, value in kwargs.iteritems():
            setattr(self, name, value)

    # todo-igogor нужность этого метода сомнительна
    def add_attribute(self, value, name, dbname=None, apiname=None):
        # type: (Any, str, str, str) -> None
        setattr(self.__class__, name, Attribute(name=name, dbname=dbname, apiname=apiname))
        type(self)._make_attributes()
        setattr(self, name, value)

    def to_db(self, remove_empty=True):
        return self.to_dict(remove_empty=remove_empty, key=lambda attr: attr.dbname)

    def to_api(self, remove_empty=True):
        return self.to_dict(remove_empty=remove_empty, key=lambda attr: attr.apiname)

    # todo-igogor возможно надо помечать некоторые параметры в классе как mandatory
    # или передавать сюда список тех что в любом случае оставить
    def to_dict(self, remove_empty=True, key=lambda attr: attr.name):
        with_empty = {key(attr): getattr(self, attr.name) for attr in self.attributes()}
        return utils.remove_empty(with_empty) if remove_empty else with_empty

    def __repr__(self):
        # todo-igogor хочется иметь сортировку атрибутов в том порядке в каком заданы в классе
        meaningful_attrs = {attr.name: getattr(self, attr.name) for attr in self.attributes() if attr.name}
        return "{cls}({attributes})".format(cls=self.__class__.__name__,
                                            attributes=", ".join(['{}={!s}'.format(name, value)
                                                                  for name, value in meaningful_attrs.iteritems()]))


def dynamic_default(callback, **kwargs):
    def _dynamic_default():
        return callback(**kwargs)

    return _dynamic_default()


def as_is(name):
    return name


def to_camel_case(snake_case):
    return ''.join([word.capitalize() if word != 'id' else 'ID' for word in snake_case.split('_')])


def to_upper_underscore(snake_case):
    return '_'.join([word.upper() for word in snake_case.split('_')])


def attributes(apinames=as_is, dbnames=as_is):
    def _make_attributes(cls):
        if hasattr(cls, '_template'):
            for name, default in cls._template.iteritems():
                if isinstance(default, Attribute):
                    # todo-igogor валидация, что имя есть
                    # todo-igogor нужна ли эта возможность или реализовать ее иначе
                    setattr(cls, default.name, default)
                else:
                    setattr(cls, name, Attribute(default=default))

        for name, attribute in cls.__dict__.iteritems():
            if isinstance(attribute, Attribute):
                if not attribute.name:
                    attribute.name = name
                if not attribute.apiname:
                    attribute.apiname = apinames(name)
                if not attribute.dbname:
                    attribute.dbname = dbnames(name)

        # валидация
        cls.get_primary_attribute()
        for attribute in cls.attributes():
            attribute.validate()
            if attribute.linked:
                name, linked_cls = attribute.linked
                setattr(cls, name, LinkedObject(linked_to=attribute.name, cls=linked_cls))

    def attributes_desctiptor(cls):
        # todo-igogor необходимость записывать этот метод в класс возникает только из-за ModelBase.add_attribute()
        # который особо и не нужен вроде. Записывать не хочется
        cls._make_attributes = classmethod(_make_attributes)
        cls._make_attributes()
        return cls

    return attributes_desctiptor


@attributes(apinames=to_upper_underscore)
class Client(ModelBase):
    id = PrimaryAttribute()
    name = Attribute(default='balance-test-user')
    is_agency = Attribute(default=False)
    agency_id = Attribute()
    agency = LinkedObject(linked_to='agency_id')


# todo-igogor классы которые используются как LinkedObject должны быть определены раньше классов которые их используют,
# что очевидно, но не удобно. можно вынести в отдельный модуль
@attributes()
class Product(ModelBase):
    id = PrimaryAttribute()
    service_id = Attribute()
    name = Attribute()


class Products(object):
    # Для сущностей которые мы не создаем можно просто создавать объекты как переменные класса - удобный доступ
    # Они сохранятся в хранилище и их можно без проблем использовать как LinkedObject
    # От енумов пока пользы не увидел, только гемор в данной ситуации
    # todo-igogor юникод точно ломает __repr__ и бог знает что еще. Возможно нужно сделать дескриптор отдельный.
    DIRECT_1475 = Product(id=1475, service_id=7, name=u'Рекламная кампания что-то там')


@attributes(apinames=to_camel_case)
class Order(ModelBase):
    id = PrimaryAttribute(apiname='OrderID')
    client_id = Attribute()
    client = LinkedObject(linked_to='client_id', cls=Client)
    service_id = Attribute()
    service_order_id = Attribute()
    # todo-igogor при таком способе пучарм не подсказывает это поле =(
    product_id = Attribute(linked=('product', Product))


class Person(object):
    class BasePerson(ModelBase):
        id = PrimaryAttribute(name='id', dbname='id', apiname='person_id')
        pass

    @attributes()
    class Ur(BasePerson):
        _template = {'name': 'PyTest Org',
                     'phone': '+7 905 1234567',
                     'email': 'test-balance-notify@yandex-team.ru',
                     'postcode': '123456',
                     'postaddress': 'Python street, 42',
                     'inn': '7719246912',
                     'longname': dynamic_default(lambda name: '{} {}'.format(name, datetime.now()),
                                                 name='PyTest Organization'),
                     'legaladdress': 'Python street, 42',
                     'kpp': '123456789'
                     }

    @attributes()
    class Ph(BasePerson):
        _template = {'phone': '+7 905 1234567',
                     'email': 'testagpi2@yandex.ru',
                     'kpp': '123456789',
                     'person_id': 0,
                     'lname': None,
                     'mname': None,
                     'fname': None}


# todo-igogor сделать @attributes или уже можно без него. Не помню как я сделал то.
@attributes(apinames=to_upper_underscore)
class RequestOrder(ModelBase):
    # todo-igogor здесь нет primary атрибута. Можно сделать id конечно но он вообще-то не нужен
    id = PrimaryAttribute()

    # todo-igogor заказ надо на самом деле преобразовывать в пару значений. Но это не обязательно делать здесь. Делать в реквесте?
    order = LinkedObject(linked_to='order_id', cls=Order)
    service_id = PropertyAttribute(getter=lambda obj: obj.order.service_id)  # todo-igogor нахуй лямбды
    service_order_id = PropertyAttribute(getter=lambda obj: obj.order.service_order_id)

    # todo-igogor нельзя ссылаться на класс Request здесь и RequestOrder в Request. Придется использовать строковую константу.
    # request = LinkedObject(linked_to='request_id', cls=Request)
    qty = Attribute()


class Request(ModelBase):
    id = PrimaryAttribute()  # todo-igogor как быть в ситуациях когда не задан в апи?
    # todo-igogor как быть со списками и более сложными конструкциями?
    # todo-igogor в данном случае это реально просто список, т.к. это отдельный параметр а не поле запроса.
    # отдельный атрибут правда все-равно может пригодится чтобы переопределить __getitem__ пока только не понял нахуя. =)
    orders = ListAttribute(contains=RequestOrder)
    client = LinkedObject(linked_to='client_id', cls=Client)


class Invoice(ModelBase):
    pass
