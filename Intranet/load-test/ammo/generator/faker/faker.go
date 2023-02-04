package faker

import (
	"io/ioutil"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/models"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/utils/csv"
	"github.com/brianvoe/gofakeit/v6"
)

type FakeSource int

const (
	FakeSourceIdentityPath = iota
	FakeSourceGroupPath
	FakeSourceIdentityTypePath
)

type Faker struct {
	gofakeit.Faker
	Identities    []*models.Identity
	Groups        []*models.Identity
	IdentityTypes []*models.IdentityType
}

func NewFaker() *Faker {
	goFaker := gofakeit.NewCrypto()
	return &Faker{
		Faker:         *goFaker,
		Identities:    make([]*models.Identity, 0),
		Groups:        make([]*models.Identity, 0),
		IdentityTypes: make([]*models.IdentityType, 0),
	}
}

func (f *Faker) Init(sources map[FakeSource]string) error {
	for key, path := range sources {
		err := f.loadFakeDataFromSource(key, path)
		if err != nil {
			return err
		}
	}
	return nil
}

func (f *Faker) GetRandomIdentity() *models.Identity {
	if len(f.Identities) == 0 {
		return nil
	}
	return f.Identities[f.IntRange(0, len(f.Identities)-1)]
}

func (f *Faker) GetRandomGroup() *models.Identity {
	if len(f.Groups) == 0 {
		return nil
	}
	return f.Groups[f.IntRange(0, len(f.Groups)-1)]
}

func (f *Faker) GetRandomIdentityType() *models.IdentityType {
	if len(f.IdentityTypes) == 0 {
		return nil
	}
	return f.IdentityTypes[f.IntRange(0, len(f.IdentityTypes)-1)]
}

func (f *Faker) loadFakeDataFromSource(sourceType FakeSource, path string) error {
	rows, err := ioutil.ReadFile(path)
	if err != nil {
		return err
	}

	switch sourceType {
	case FakeSourceIdentityPath:
		err = csv.Unmarshal(rows, &f.Identities)
	case FakeSourceGroupPath:
		err = csv.Unmarshal(rows, &f.Groups)
	case FakeSourceIdentityTypePath:
		err = csv.Unmarshal(rows, &f.IdentityTypes)
	}
	return err
}
