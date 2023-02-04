package methods

import (
	"errors"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"
)

type DeleteIdentity struct {
	Tag      string              `json:"tag"`
	Identity *models.CompositeID `json:"identity"`
}

func GenerateDeleteIdentityAmmo(count int, faker *faker.Faker) ([]interface{}, error) {
	if len(faker.Identities) == 0 {
		return nil, errors.New("fake identities source required")
	}

	data := make([]interface{}, count)
	for i := 0; i < count; i++ {
		data[i] = &DeleteIdentity{
			Tag:      "DeleteIdentity",
			Identity: GenerateRandomIdentityID(faker),
		}
	}
	return data, nil
}
