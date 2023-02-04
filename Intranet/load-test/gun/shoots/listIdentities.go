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

const ListIdentitiesTag = "ListIdentities"

type ListIdentitiesAmmo struct {
	Tag   string       `json:"tag"`
	Group *CompositeID `json:"group"`
}

func ShootListIdentitiesAmmo(client api.IdentityServiceClient, aggr core.Aggregator, ammo *ListIdentitiesAmmo) {
	code := codes.Unknown
	req := &api.ListIdentitiesRequest{}
	if ammo.Group != nil {
		req.GroupId = ammo.Group.ID
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
	_, err := client.ListIdentities(ctx, req)
	if err == nil {
		code = codes.OK
	} else {
		if e, ok := status.FromError(err); ok {
			code = e.Code()
		}
	}
}
