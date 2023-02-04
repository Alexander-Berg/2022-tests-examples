package lbint

import (
	"context"
	echo "github.com/YandexClassifieds/admin-utils/grpc-echo/protos"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"log"
	"testing"
	"time"
)

func TestResolver(t *testing.T) {
	cc, err := grpc.Dial("lbint:///grpc-echo-api.vrts-slb.test.vertis.yandex.net:80", grpc.WithInsecure(), grpc.WithBalancerName("round_robin"))
	require.NoError(t, err)
	ec := echo.NewEchoClient(cc)

	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()
	resp, err := ec.Ping(ctx, &echo.PingRequest{})
	require.NoError(t, err)
	assert.NotEmpty(t, resp.GetHostname())

	for i := 0; i <  10; i++ {
		resp, err := ec.Ping(context.TODO(), &echo.PingRequest{})
		assert.NoError(t, err)
		log.Printf("host: %s", resp.GetHostname())
	}
}
