from os import path
import typing as tp

from billing.hot.tests.lib.date import formatted
from billing.hot.tests.lib.rand import rand
from billing.hot.tests.lib.templates import loader


class OEBSLoader:
    def __init__(self, template_dir: str) -> None:
        self.loader = loader.TemplateLoader(template_dir)

    def load_ard_response(self, name: str = 'basic.json') -> loader.RenderedTemplate:
        return self.loader.load(path.join('ard', name))

    def load_oebs_response(self, name: str = 'basic.json') -> loader.RenderedTemplate:
        return self.loader.load(path.join('oebs', name))


class OEBSRenderer:
    def __init__(self, lder: OEBSLoader) -> None:
        self.loader = lder

    def render_ard_response(
        self, response: loader.RenderedTemplateOrTemplatePath = 'basic.json',
    ) -> loader.RenderedTemplate:
        if isinstance(response, str):
            response = self.loader.load_oebs_response(name=response)
        response.update(
            {
                'status_id': response['status_id'] or rand.int64(),
                'status_dt': response['status_dt'] or formatted.date_iso_seconds(),
                'message_id': response['message_id'] or rand.int64(),
                'payment_batch_id': response['payment_batch_id'] or rand.int64(),
            }
        )
        return response

    def render_oebs_response(self, amount: int, response_path: str = 'basic.json'):
        response = self.loader.load_oebs_response(response_path)
        response['payment_amount'] = amount
        return response

    @staticmethod
    def enrich_oebs_response(response: dict[str, tp.Any]) -> loader.RenderedTemplate:
        """OEBS response should already have nonzero amount"""
        response.update(
            {
                'status_dt': response['status_dt'] or formatted.date_iso_seconds(),
                'payment_id': response['payment_id'] or rand.int64(),
            }
        )
        return response
