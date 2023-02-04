package runserver

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	"a.yandex-team.ru/{{cookiecutter.project_path}}/pkg/interactions/example"
	"a.yandex-team.ru/{{cookiecutter.project_path}}/pkg/interactions/example/impl"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
)

func TestRunServer(t *testing.T) {
	registry := solomon.NewRegistry(nil)

	port := os.Getenv("{{cookiecutter.PROJECT_NAME}}_RUNSERVER_PORT")

	client, err := impl.NewClient(impl.ExampleClientConfig{Transport: interactions.Config{
		BaseURL: "http://localhost:" + port,
		Name:    "{{cookiecutter.project_name}}",
	}}, nil, registry)
	require.NoError(t, err)

	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	user, err := client.GetUser(ctx, 0)

	require.NoError(t, err)
	require.Equal(t, example.User{}, user)
}
