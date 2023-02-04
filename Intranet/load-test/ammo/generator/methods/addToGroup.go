package methods

import (
	"errors"
	"math"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"
)

type AddToGroup struct {
	Tag        string                `json:"tag"`
	Group      *models.CompositeID   `json:"group"`
	Identities []*models.CompositeID `json:"identities"`
}

func GenerateAddToGroupAmmo(count int, faker *faker.Faker) ([]interface{}, error) {
	if len(faker.Identities) == 0 {
		return nil, errors.New("fake identities source required")
	}
	if len(faker.Groups) == 0 {
		return nil, errors.New("fake groups source required")
	}

	data := make([]interface{}, count)
	for i := 0; i < count; i++ {
		data[i] = &AddToGroup{
			Tag:   "AddToGroup",
			Group: GenerateRandomGroupID(faker),
			Identities: GenerateRandomIdentityIDs(
				faker,
				faker.IntRange(1, int(math.Min(float64(count), 100))),
			),
		}
	}
	return data, nil
}
