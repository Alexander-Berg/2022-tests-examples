package board

import (
	"testing"

	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pkg/grafana/dashboard/panel"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	annt = annotations{
		List: []interface{}{
			annotation{
				Name:       "annotationName",
				Datasource: "datasource",
				Enable:     true,
				IconColor:  "color",
				Hide:       true,
				Tags:       []string{"tag1", "tag2"},
				Type:       "type",
			},
		}}

	temps = templating{
		List: []interface{}{
			TemplateVar{
				Name:       panel.DcTmp,
				Query:      "sas,vla",
				Type:       "custom",
				IncludeAll: true,
				AllValue:   "sas*|vla*|myt*|man*",
				Options: []Option{
					{
						Selected: true,
						Text:     "All",
						Value:    "$__all",
					},
					{
						Selected: false,
						Text:     "sas",
						Value:    "sas",
					},
					{
						Selected: false,
						Text:     "vla",
						Value:    "vla",
					},
					{
						Selected: false,
						Text:     "myt",
						Value:    "myt",
					},
					{
						Selected: false,
						Text:     "man",
						Value:    "man",
					},
				},
				Current: Current{
					Selected: true,
					Text:     "All",
					Value:    "$__all",
				},
			},
		},
	}

	resp = map[string]interface{}{
		uidField:           "uid",
		titleField:         "title",
		editableField:      true,
		annotationsField:   annt,
		templatesField:     temps,
		schemaVersionField: "30",
		gnetIdField:        12,
		iterationField:     "12345",
		versionField:       "3",
	}
)

func TestAddPanels(t *testing.T) {
	sCtx := panel.NewServiceContext(&proto.ServiceMap{
		Name: t.Name(),
	}, nil)

	board := MakeSimpleDashboard(sCtx)
	board.AddPanels([]*panel.Panel{
		panel.MakeFromPreferences(panel.Preferences{Title: "p1", GridPos: panel.GridPos{H: 4, W: 10, X: 7, Y: 3}}),
		panel.MakeFromPreferences(panel.Preferences{Title: "p2", GridPos: panel.GridPos{Y: 1}}),
		panel.MakeFromPreferences(panel.Preferences{Title: "p3", GridPos: panel.GridPos{H: 8, Y: 2}}),
	})

	boardPanels := board.GetPanels()

	require.Equal(t, 3, len(boardPanels))
	require.Equal(t, uint(0), boardPanels[0].GetGridPos().Y)
	require.Equal(t, uint(5), boardPanels[1].GetGridPos().Y)
	require.Equal(t, uint(6), boardPanels[2].GetGridPos().Y)
}

func TestMakeFromRawResponse(t *testing.T) {
	b, err := MakeFromRawResponse(resp)

	require.NoError(t, err)
	assert.NotNil(t, b)
	assert.Equal(t, resp[uidField], b.GetUID())
	assert.Equal(t, resp[titleField], b.GetTitle())
	assert.Equal(t, resp[editableField], b.GetRawData()[editableField])
	assert.Equal(t, len(annt.List), len(b.GetAnnotations()))
	assert.Equal(t, annt.List[0], b.GetAnnotations()[0].RawData)
	assert.Equal(t, len(temps.List), len(b.GetTemplates()))
	assert.Equal(t, temps.List[0], b.GetTemplates()[0].RawData)
	assert.Nil(t, b.rawResponse[schemaVersionField])
	assert.Nil(t, b.rawResponse[gnetIdField])
	assert.Nil(t, b.rawResponse[iterationField])
	assert.Nil(t, b.rawResponse[versionField])
}

func TestAddAnnotations(t *testing.T) {
	b, err := MakeFromRawResponse(resp)
	require.NoError(t, err)

	a := annotation{
		Name:       "someName",
		Datasource: "someDatasource",
		Enable:     false,
		IconColor:  "red",
		Hide:       true,
		Tags:       []string{"tag10"},
		Type:       "type1",
	}

	b.AddAnnotations([]Annotation{
		{
			Name:    a.Name,
			RawData: a,
		},
	})

	assert.Equal(t, 2, len(b.annotations))
	assert.Equal(t, annt.List[0], b.annotations[0].RawData)
	assert.Equal(t, a, b.annotations[1].RawData)
}

func TestAddTemplates(t *testing.T) {
	b, err := MakeFromRawResponse(resp)
	require.NoError(t, err)

	newTemp := TemplateVar{
		Name:       "temp",
		Query:      "query",
		Type:       "custom",
		IncludeAll: true,
	}

	b.AddTemplates([]Template{
		{Name: newTemp.Name, RawData: newTemp},
	})

	assert.Equal(t, 2, len(b.templates))
	assert.Equal(t, temps.List[0], b.templates[0].RawData)
	assert.Equal(t, newTemp, b.templates[1].RawData)
}

func TestClearPanels(t *testing.T) {
	b, err := MakeFromRawResponse(resp)
	require.NoError(t, err)

	b.ClearPanels()

	assert.Equal(t, 0, len(b.GetPanels()))
	assert.Nil(t, b.rawResponse[panelsField])
}

func TestClearAnnotations(t *testing.T) {
	b, err := MakeFromRawResponse(resp)
	require.NoError(t, err)

	b.ClearAnnotations()

	assert.Equal(t, 0, len(b.annotations))
	assert.Equal(t, annotations{}, b.rawResponse[annotationsField])
}

func TestClearTemplates(t *testing.T) {
	b, err := MakeFromRawResponse(resp)
	require.NoError(t, err)

	b.ClearTemplates()

	assert.Equal(t, 0, len(b.templates))
	assert.Equal(t, templating{}, b.rawResponse[templatesField])
}
