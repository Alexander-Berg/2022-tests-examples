from walle.util import template_loader


class TestJinjaTemplateRenderer:
    def test_render_template(self, mp):
        renderer = template_loader.JinjaTemplateRenderer()
        mp.setattr(template_loader, "TEMPLATE_DIR", "infra/walle/server/walle/templates")
        result = renderer.render_template("test.html", name="WALL-E")
        assert result == "HELLO, WALL-E!"
