package methods

import (
	"errors"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"
)

type CreateIdentity struct {
	Tag              string                   `json:"tag"`
	ExternalIdentity *models.ExternalIdentity `json:"external_identity"`
	ParentID         *models.CompositeID      `json:"parent_id,omitempty"`
	Data             *models.IdentityData     `json:"data"`
}

func GenerateCreateIdentityAmmo(count int, faker *faker.Faker) ([]interface{}, error) {
	if len(faker.IdentityTypes) == 0 {
		return nil, errors.New("fake identity types source required")
	}

	data := make([]interface{}, count)
	for i := 0; i < count; i++ {
		var parentID *models.CompositeID
		if len(faker.Groups) > 0 && faker.Bool() {
			parentID = &models.CompositeID{
				ID: faker.GetRandomGroup().ID,
			}
		}
		data[i] = &CreateIdentity{
			Tag: "CreateIdentity",
			ExternalIdentity: &models.ExternalIdentity{
				ExternalID: faker.UUID(),
				TypeID:     faker.GetRandomIdentityType().ID,
			},
			ParentID: parentID,
			Data: &models.IdentityData{
				Name:     faker.FirstName(),
				LastName: faker.LastName(),
				Phone:    faker.Phone(),
				Email:    faker.Email(),
				Slug:     faker.Gamertag(),
			},
		}
	}
	return data, nil
}
