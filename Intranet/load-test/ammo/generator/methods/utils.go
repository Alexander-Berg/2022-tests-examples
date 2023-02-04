package methods

import (
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"
)

func GenerateRandomIdentityID(faker *faker.Faker) *models.CompositeID {
	var identity = faker.GetRandomIdentity()
	if faker.Bool() {
		return &models.CompositeID{
			ID: identity.ID,
		}
	}
	return &models.CompositeID{
		ExternalIdentity: &models.ExternalIdentity{
			ExternalID: identity.ExternalID,
			TypeID:     identity.TypeID,
		},
	}
}

func GenerateRandomIdentityIDs(faker *faker.Faker, count int) []*models.CompositeID {
	identities := make([]*models.CompositeID, count)
	for j := 0; j < count; j++ {
		identities[j] = GenerateRandomIdentityID(faker)
	}
	return identities
}

func GenerateRandomGroupID(faker *faker.Faker) *models.CompositeID {
	var group = faker.GetRandomGroup()
	if faker.Bool() {
		return &models.CompositeID{
			ID: group.ID,
		}
	}
	return &models.CompositeID{
		ExternalIdentity: &models.ExternalIdentity{
			ExternalID: group.ExternalID,
			TypeID:     group.TypeID,
		},
	}
}
