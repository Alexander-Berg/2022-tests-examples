package methods

import (
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"
	"errors"
	"math"
)

type RemoveFromGroup struct {
	Tag        string                `json:"tag"`
	Group      *models.CompositeID   `json:"group"`
	Identities []*models.CompositeID `json:"identities"`
}

func GenerateRemoveFromGroupAmmo(count int, faker *faker.Faker) ([]interface{}, error) {
	if len(faker.Identities) == 0 {
		return nil, errors.New("fake identities source required")
	}
	if len(faker.Groups) == 0 {
		return nil, errors.New("fake groups source required")
	}

	data := make([]interface{}, count)
	for i := 0; i < count; i++ {
		data[i] = &RemoveFromGroup{
			Tag:   "RemoveFromGroup",
			Group: GenerateRandomGroupID(faker),
			Identities: GenerateRandomIdentityIDs(
				faker,
				faker.IntRange(1, int(math.Min(float64(count), 100))),
			),
		}
	}
	return data, nil
}
