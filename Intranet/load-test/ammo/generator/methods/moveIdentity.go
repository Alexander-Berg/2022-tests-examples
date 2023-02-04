package methods

import (
	"errors"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"
)

type MoveIdentity struct {
	Tag      string              `json:"tag"`
	Identity *models.CompositeID `json:"identity"`
	ToGroup  *models.CompositeID `json:"to_group"`
}

func GenerateMoveIdentityAmmo(count int, faker *faker.Faker) ([]interface{}, error) {
	if len(faker.Identities) == 0 {
		return nil, errors.New("fake identities source required")
	}
	if len(faker.Groups) == 0 {
		return nil, errors.New("fake groups source required")
	}

	data := make([]interface{}, count)
	for i := 0; i < count; i++ {
		data[i] = &MoveIdentity{
			Tag:      "MoveIdentity",
			Identity: GenerateRandomIdentityID(faker),
			ToGroup:  GenerateRandomGroupID(faker),
		}
	}
	return data, nil
}
