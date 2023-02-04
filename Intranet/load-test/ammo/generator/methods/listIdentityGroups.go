package methods

import (
	"errors"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"
)

type ListIdentityGroups struct {
	Tag          string              `json:"tag"`
	Identity     *models.CompositeID `json:"identity"`
	OnlyDirectly bool                `json:"only_directly"`
}

func GenerateListIdentityGroupsAmmo(count int, faker *faker.Faker) ([]interface{}, error) {
	if len(faker.Identities) == 0 {
		return nil, errors.New("fake identities source required")
	}

	data := make([]interface{}, count)
	for i := 0; i < count; i++ {
		data[i] = &ListIdentityGroups{
			Tag:          "ListIdentityGroups",
			Identity:     GenerateRandomIdentityID(faker),
			OnlyDirectly: faker.Bool(),
		}
	}
	return data, nil
}
