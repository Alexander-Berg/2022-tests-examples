package main

import (
	api "a.yandex-team.ru/intranet/ims/imscore/backend/service/proto"
	"a.yandex-team.ru/intranet/ims/load-test/gun/shoots"
	"crypto/tls"
	"github.com/yandex/pandora/cli"
	"github.com/yandex/pandora/core"
	coreimport "github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

type GunConfig struct {
	Target string `validate:"required"`
}

type Gun struct {
	identityServiceClient      api.IdentityServiceClient
	identityGroupServiceClient api.IdentityGroupServiceClient
	conf                       GunConfig
	aggr                       core.Aggregator
	core.GunDeps
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	var tlsConf tls.Config
	tlsConf.InsecureSkipVerify = true
	connection, err := grpc.Dial(g.conf.Target, grpc.WithTransportCredentials(credentials.NewTLS(&tlsConf)))
	//connection, err := grpc.Dial(address, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return err
	}

	g.identityServiceClient = api.NewIdentityServiceClient(connection)
	g.identityGroupServiceClient = api.NewIdentityGroupServiceClient(connection)
	g.aggr = aggr
	g.GunDeps = deps
	return nil
}

func (g *Gun) Shoot(ammo core.Ammo) {
	switch a := ammo.(type) {
	case *shoots.CreateIdentityAmmo:
		shoots.ShootCreateIdentityAmmo(g.identityServiceClient, g.aggr, a)
	case *shoots.GetIdentityAmmo:
		shoots.ShootGetIdentityAmmo(g.identityServiceClient, g.aggr, a)
	case *shoots.DeleteIdentityAmmo:
		shoots.ShootDeleteIdentityAmmo(g.identityServiceClient, g.aggr, a)
	case *shoots.MoveIdentityAmmo:
		shoots.ShootMoveIdentityAmmo(g.identityServiceClient, g.aggr, a)
	case *shoots.ListIdentitiesAmmo:
		shoots.ShootListIdentitiesAmmo(g.identityServiceClient, g.aggr, a)
	case *shoots.ListIdentityGroupsAmmo:
		shoots.ShootListIdentityGroupsAmmo(g.identityGroupServiceClient, g.aggr, a)
	case *shoots.ExistsInGroupAmmo:
		shoots.ShootExistsInGroupAmmo(g.identityGroupServiceClient, g.aggr, a)
	case *shoots.AddToGroupAmmo:
		shoots.ShootAddToGroupAmmo(g.identityGroupServiceClient, g.aggr, a)
	case *shoots.RemoveFromGroupAmmo:
		shoots.ShootRemoveFromGroupAmmo(g.identityGroupServiceClient, g.aggr, a)
	case *shoots.RandomAmmo:
		g.Shoot(a.Value)
	default:
	}
}

func NewGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

func main() {
	fs := coreimport.GetFs()
	coreimport.Import(fs)

	coreimport.RegisterCustomJSONProvider("intranet_ims_random", func() core.Ammo {
		return &shoots.RandomAmmo{}
	})
	coreimport.RegisterCustomJSONProvider("intranet_ims_create_identity", func() core.Ammo {
		return &shoots.CreateIdentityAmmo{}
	})
	coreimport.RegisterCustomJSONProvider("intranet_ims_get_identity", func() core.Ammo {
		return &shoots.GetIdentityAmmo{}
	})
	coreimport.RegisterCustomJSONProvider("intranet_ims_delete_identity", func() core.Ammo {
		return &shoots.DeleteIdentityAmmo{}
	})
	coreimport.RegisterCustomJSONProvider("intranet_ims_list_identities", func() core.Ammo {
		return &shoots.ListIdentitiesAmmo{}
	})
	coreimport.RegisterCustomJSONProvider("intranet_ims_move_identity", func() core.Ammo {
		return &shoots.MoveIdentityAmmo{}
	})
	coreimport.RegisterCustomJSONProvider("intranet_ims_list_identity_groups", func() core.Ammo {
		return &shoots.ListIdentityGroupsAmmo{}
	})
	coreimport.RegisterCustomJSONProvider("intranet_ims_exists_in_group", func() core.Ammo {
		return &shoots.ExistsInGroupAmmo{}
	})
	coreimport.RegisterCustomJSONProvider("intranet_ims_add_to_group", func() core.Ammo {
		return &shoots.AddToGroupAmmo{}
	})
	coreimport.RegisterCustomJSONProvider("intranet_ims_remove_from_group", func() core.Ammo {
		return &shoots.RemoveFromGroupAmmo{}
	})

	register.Gun("intranet_ims", NewGun, func() GunConfig {
		return GunConfig{
			Target: "default target",
		}
	})
	cli.Run()
}
