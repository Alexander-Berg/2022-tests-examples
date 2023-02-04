package arc

import (
	"context"
	"strings"
	"testing"

	arc "aptly/pb/arc/api/public"
	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
)

const (
	aptlyPath = "classifieds/infra/aptly"
)

func TestClient_CreateBranch(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	svc := New(c, logger)

	branch, err := svc.CreateBranch()
	require.NoError(t, err)
	require.True(t, strings.HasPrefix(branch, c.Str("BRANCH_PREFIX")))

	t.Cleanup(func() {
		_, err := svc.branch.DeleteRef(context.Background(), &arc.DeleteRefRequest{
			Branch: branch,
		})
		require.NoError(t, err)
	})

	resp, err := svc.branch.ListRefs(context.Background(), &arc.ListRefsRequest{
		PrefixFilter: branch,
		Lightweight:  true,
	})
	require.NoError(t, err)

	_, err = resp.Recv()
	require.NoError(t, err)
}

func TestClient_CommitFile(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	svc := New(c, logger)

	branch, err := svc.CreateBranch()
	require.NoError(t, err)

	t.Cleanup(func() {
		_, err := svc.branch.DeleteRef(context.Background(), &arc.DeleteRefRequest{
			Branch: branch,
		})
		require.NoError(t, err)
	})

	err = svc.CommitFile(branch, aptlyPath+"/tests", []byte("test"))
	require.NoError(t, err)
}

func TestClient_GetFile(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	svc := New(c, logger)

	data, err := svc.GetFile("a.yaml")
	require.NoError(t, err)
	require.Contains(t, string(data), "service: ")
}
