# -*- coding: utf-8 -*-

# /C=RU/ST=Russian Federation/L=Saint-Petersburg/O=Yandex/OU=SEPE-QA/CN=%s/emailAddress=sepe-qa@yandex-team.ru


class Subject(object):
    __PATTERN = (
        '/C={country}'
        '/ST={state}'
        '/L={location}'
        '/O={organisation}'
        '/OU={organisation_unit}'
        '/CN={common_name}'
        '/emailAddress={email}'
    )

    def __init__(self, country, state, location, organisation, organisation_unit, common_name, email):
        super(Subject, self).__init__()
        self.__country = country
        self.__state = state
        self.__location = location
        self.__organisation = organisation
        self.__organisation_unit = organisation_unit
        self.__common_name = common_name
        self.__email = email

    @property
    def country(self):
        return self.__country

    @property
    def state(self):
        return self.__state

    @property
    def location(self):
        return self.__location

    @property
    def organisation(self):
        return self.__organisation

    @property
    def organisation_unit(self):
        return self.__organisation_unit

    @property
    def common_name(self):
        return self.__common_name

    @property
    def email(self):
        return self.__email

    def __str__(self):
        return self.__PATTERN.format(
            country=self.country,
            state=self.state,
            location=self.location,
            organisation=self.organisation,
            organisation_unit=self.organisation_unit,
            common_name=self.common_name,
            email=self.email,
        )


def _choose(first, second):
    if first is not None:
        return first
    else:
        return second


class SubjectGenerator(object):
    def __init__(self, subj):
        super(SubjectGenerator, self).__init__()
        self.__subj = subj

    def generate(
        self,
        country=None,
        state=None,
        location=None,
        organisation=None,
        organisation_unit=None,
        common_name=None,
        email=None,
    ):
        return Subject(
            country=_choose(country, self.__subj.country),
            state=_choose(state, self.__subj.state),
            location=_choose(location, self.__subj.location),
            organisation=_choose(organisation, self.__subj.organisation),
            organisation_unit=_choose(organisation_unit, self.__subj.organisation_unit),
            common_name=_choose(common_name, self.__subj.common_name),
            email=_choose(email, self.__subj.email),
        )
