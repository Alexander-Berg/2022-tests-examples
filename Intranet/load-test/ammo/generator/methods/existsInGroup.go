package methods

import (
	"errors"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"
)

type ExistsInGroup struct {
	Tag          string              `json:"tag"`
	Identity     *models.CompositeID `json:"identity"`
	Group        *models.CompositeID `json:"group"`
	OnlyDirectly bool                `json:"only_directly"`
}

func GenerateExistsInGroupAmmo(count int, faker *faker.Faker) ([]interface{}, error) {
	if len(faker.Identities) == 0 {
		return nil, errors.New("fake identities source required")
	}
	if len(faker.Groups) == 0 {
		return nil, errors.New("fake groups source required")
	}

	data := make([]interface{}, count)
	for i := 0; i < count; i++ {
		data[i] = &ExistsInGroup{
			Tag:          "ExistsInGroup",
			Identity:     GenerateRandomIdentityID(faker),
			Group:        GenerateRandomGroupID(faker),
			OnlyDirectly: faker.Bool(),
		}
	}
	return data, nil
}
