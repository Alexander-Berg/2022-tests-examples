package generator

import (
	"flag"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/faker"
	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator/methods"
)

const (
	createIdentityMethod     = "createIdentity"
	deleteIdentityMethod     = "deleteIdentity"
	getIdentityMethod        = "getIdentity"
	listIdentitiesMethod     = "listIdentities"
	moveIdentityMethod       = "moveIdentity"
	addToGroupMethod         = "addToGroup"
	removeFromGroupMethod    = "removeFromGroup"
	existsInGroupMethod      = "existsInGroup"
	listIdentityGroupsMethod = "listIdentityGroups"
)

type AmmoGenerator struct {
	Faker      *faker.Faker
	FakeSource map[faker.FakeSource]string
	Method     string
	Count      int
}

func NewAmmoGenerator() (*AmmoGenerator, error) {
	return initAmmoGenerator()
}

func initAmmoGenerator() (*AmmoGenerator, error) {
	g := &AmmoGenerator{
		Faker:      faker.NewFaker(),
		FakeSource: map[faker.FakeSource]string{},
	}

	flag.StringVar(&g.Method, "method", "getIdentity", "name of ammo method")
	flag.IntVar(&g.Count, "count", 1, "number of units")

	var identitySource string
	flag.StringVar(
		&identitySource,
		"identitySource", "", "path to fake identity source")

	var groupSource string
	flag.StringVar(
		&groupSource,
		"groupSource", "", "path to fake group source")

	var identityTypeSource string
	flag.StringVar(
		&identityTypeSource,
		"identityTypeSource", "", "path to fake identity type source")

	flag.Parse()

	if identitySource != "" {
		g.FakeSource[faker.FakeSourceIdentityPath] = identitySource
	}
	if groupSource != "" {
		g.FakeSource[faker.FakeSourceGroupPath] = groupSource
	}
	if identityTypeSource != "" {
		g.FakeSource[faker.FakeSourceIdentityTypePath] = identityTypeSource
	}

	err := g.Faker.Init(g.FakeSource)
	if err != nil {
		return nil, err
	}
	return g, nil
}

func (g *AmmoGenerator) GetAmmo() (ammo []interface{}, err error) {
	switch g.Method {
	case getIdentityMethod:
		ammo, err = methods.GenerateGetIdentityAmmo(g.Count, g.Faker)
	case createIdentityMethod:
		ammo, err = methods.GenerateCreateIdentityAmmo(g.Count, g.Faker)
	case deleteIdentityMethod:
		ammo, err = methods.GenerateDeleteIdentityAmmo(g.Count, g.Faker)
	case listIdentitiesMethod:
		ammo, err = methods.GenerateListIdentitiesAmmo(g.Count, g.Faker)
	case moveIdentityMethod:
		ammo, err = methods.GenerateMoveIdentityAmmo(g.Count, g.Faker)
	case addToGroupMethod:
		ammo, err = methods.GenerateAddToGroupAmmo(g.Count, g.Faker)
	case removeFromGroupMethod:
		ammo, err = methods.GenerateRemoveFromGroupAmmo(g.Count, g.Faker)
	case existsInGroupMethod:
		ammo, err = methods.GenerateExistsInGroupAmmo(g.Count, g.Faker)
	case listIdentityGroupsMethod:
		ammo, err = methods.GenerateListIdentityGroupsAmmo(g.Count, g.Faker)
	}
	return ammo, err
}
