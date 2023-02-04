package methods

import (
	"encoding/base64"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"
)

type ListIdentities struct {
	Tag       string              `json:"tag"`
	Group     *models.CompositeID `json:"group,omitempty"`
	PageToken string              `json:"page_token,omitempty"`
}

func GenerateListIdentitiesAmmo(count int, faker *faker.Faker) ([]interface{}, error) {
	data := make([]interface{}, count)
	for i := 0; i < count; i++ {
		req := &ListIdentities{
			Tag: "ListIdentities",
		}
		if len(faker.Groups) > 0 {
			req.Group = GenerateRandomGroupID(faker)
		} else {
			if len(faker.Identities) > 0 {
				identity := GenerateRandomIdentityID(faker)
				if identity.ID != "" {
					req.PageToken = base64.StdEncoding.EncodeToString([]byte(identity.ID))
				}
			}
		}
		data[i] = req
	}
	return data, nil
}
