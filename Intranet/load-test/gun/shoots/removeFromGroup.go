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

const RemoveFromGroupTag = "RemoveFromGroup"

type RemoveFromGroupAmmo struct {
	Tag        string        `json:"tag"`
	Identities []CompositeID `json:"identities"`
	Group      CompositeID   `json:"group"`
}

func ShootRemoveFromGroupAmmo(client api.IdentityGroupServiceClient, aggr core.Aggregator, ammo *RemoveFromGroupAmmo) {
	code := codes.Unknown
	req := &api.RemoveFromGroupRequest{
		Group:      &api.IdentityCompositeId{},
		Identities: make([]*api.IdentityCompositeId, len(ammo.Identities)),
	}
	for i, identity := range ammo.Identities {
		req.Identities[i] = &api.IdentityCompositeId{}
		if identity.ID != "" {
			req.Identities[i].IdentityIdOneof = &api.IdentityCompositeId_Id{
				Id: identity.ID,
			}
		} else {
			req.Identities[i].IdentityIdOneof = &api.IdentityCompositeId_ExternalIdentity{
				ExternalIdentity: &api.ExternalIdentity{
					ExternalId: identity.ExternalIdentity.ExternalID,
					TypeId:     identity.ExternalIdentity.TypeID,
				},
			}
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
	_, err := client.RemoveFromGroup(ctx, req)
	if err == nil {
		code = codes.OK
	} else {
		if e, ok := status.FromError(err); ok {
			code = e.Code()
		}
	}
}
