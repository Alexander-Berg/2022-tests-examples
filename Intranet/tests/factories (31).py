import factory

from staff.person.models import ResponsibleForRobot


class ResponsibleForRobotFactory(factory.DjangoModelFactory):
    class Meta:
        model = ResponsibleForRobot
