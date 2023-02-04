package shoots

import (
	api "a.yandex-team.ru/intranet/ims/imscore/backend/service/proto"
	"context"
	"github.com/gofrs/uuid"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
)

const ExistsInGroupTag = "ExistsInGroup"

type ExistsInGroupAmmo struct {
	Tag          string      `json:"tag"`
	Identity     CompositeID `json:"identity"`
	Group        CompositeID `json:"group"`
	OnlyDirectly bool        `json:"only_directly"`
}

func ShootExistsInGroupAmmo(client api.IdentityGroupServiceClient, aggr core.Aggregator, ammo *ExistsInGroupAmmo) {
	code := codes.Unknown
	req := &api.ExistsInGroupRequest{
		Identity: &api.IdentityCompositeId{},
		Group:    &api.IdentityCompositeId{},
	}
	if ammo.Identity.ID != "" {
		req.Identity.IdentityIdOneof = &api.IdentityCompositeId_Id{
			Id: ammo.Identity.ID,
		}
	} else {
		req.Identity.IdentityIdOneof = &api.IdentityCompositeId_ExternalIdentity{
			ExternalIdentity: &api.ExternalIdentity{
				ExternalId: ammo.Identity.ExternalIdentity.ExternalID,
				TypeId:     ammo.Identity.ExternalIdentity.TypeID,
			},
		}
	}
	if ammo.Group.ID != "" {
		req.Group.IdentityIdOneof = &api.IdentityCompositeId_Id{
			Id: ammo.Group.ID,
		}
	} else {
		req.Group.IdentityIdOneof = &api.IdentityCompositeId_ExternalIdentity{
			ExternalIdentity: &api.ExternalIdentity{
				ExternalId: ammo.Group.ExternalIdentity.ExternalID,
				TypeId:     ammo.Group.ExternalIdentity.TypeID,
			},
		}
	}

	sample := netsample.Acquire(ammo.Tag)
	defer func() {
		sample.SetProtoCode(int(code))
		aggr.Report(sample)
	}()

	requestID, _ := uuid.NewV4()
	ctx := metadata.AppendToOutgoingContext(context.Background(),
		"X-Request-Id", requestID.String(),
	)
	_, err := client.ExistsInGroup(ctx, req)
	if err == nil {
		code = codes.OK
	} else {
		if e, ok := status.FromError(err); ok {
			code = e.Code()
		}
	}
}
