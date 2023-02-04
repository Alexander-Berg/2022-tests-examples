package staff

import (
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
)

type ApiMock struct {
	groups  []*staffapi.DepartmentGroup
	persons []*staffapi.Person
}

func NewApiMock() *ApiMock {
	return &ApiMock{
		groups: []*staffapi.DepartmentGroup{
			&groupWithoutLeader,
			&groupCompAuto,
			&groupInfraService,
			&groupInfraMnt,
		},
		persons: []*staffapi.Person{
			&personGoodfella,
			&personSpooner,
			&personCoderoc,
			&personIbiryulin,
		},
	}
}

func (a *ApiMock) GetPersonByTelegram(login string) (*staffapi.Person, error) {
	return nil, nil
}

func (a *ApiMock) GetPersonByGithub(login string) (*staffapi.Person, error) {
	for _, p := range a.persons {
		for _, acc := range p.Accounts {
			if acc.Type == "github" && acc.Value == login {
				return p, nil
			}
		}
	}

	return nil, staffapi.NewErrUserNotFound(login)
}

func (a *ApiMock) GetPersonByLogin(login string) (*staffapi.Person, error) {
	for _, p := range a.persons {
		if p.Login == login {
			return p, nil
		}
	}

	return nil, staffapi.NewErrUserNotFound(login)
}

func (a *ApiMock) GetPersonsByLogins(logins ...string) ([]*staffapi.Person, error) {
	var res []*staffapi.Person
	for _, login := range logins {
		for _, p := range a.persons {
			if login == p.Login {
				res = append(res, p)
			}
		}
	}
	return res, nil
}

func (a *ApiMock) GetPersonsByGithubs(logins ...string) ([]*staffapi.Person, error) {
	result := make([]*staffapi.Person, 0)

	for _, login := range logins {
		for _, p := range a.persons {
			for _, acc := range p.Accounts {
				if acc.Type == "github" && acc.Value == login {
					result = append(result, p)
				}
			}
		}
	}

	return result, nil
}

func (a *ApiMock) GetPersonsByGroup(groupID int) ([]*staffapi.Person, error) {
	result := make([]*staffapi.Person, 0)

	for _, p := range a.persons {
		if p.DepartmentGroup.ID == groupID {
			result = append(result, p)
		}
	}

	return result, nil
}

func (a *ApiMock) GetGroupByUrl(groupUrl string) (*staffapi.DepartmentGroup, error) {
	for _, g := range a.groups {
		if g.URL == groupUrl {
			return g, nil
		}
	}
	return nil, staffapi.NewErrGroupNotFound(groupUrl)
}

func (a *ApiMock) GetGroupsByParent(group string) ([]*staffapi.DepartmentGroup, error) {
	return nil, nil
}
