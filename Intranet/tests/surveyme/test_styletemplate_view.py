# -*- coding: utf-8 -*-
from django.test import TestCase

from events.media.factories import ImageFactory
from events.surveyme.factories import SurveyStyleTemplateFactory
from events.surveyme.api_admin.v2.serializers import SurveyStyleTemplateSerializer


class TestSurveyStyleTemplateSerializer(TestCase):
    serializer_class = SurveyStyleTemplateSerializer

    def test_should_create_new_style_template(self):
        image_page = ImageFactory()
        image_form = ImageFactory()
        data = {
            'name': 'default style',
            'image_page': image_page.pk,
            'image_form': image_form.pk,
            'styles': {
                'css': {},
            },
        }
        serializer = self.serializer_class(data=data)
        self.assertTrue(serializer.is_valid(), msg=serializer.errors)
        style_template = serializer.save()

        self.assertEqual(style_template.name, 'default style')
        self.assertEqual(style_template.image_page.pk, image_page.pk)
        self.assertEqual(style_template.image_form.pk, image_form.pk)
        self.assertDictEqual(style_template.styles, {'css': {}})

    def test_should_return_existing_style_template(self):
        image_page = ImageFactory()
        image_form = ImageFactory()
        style_template = SurveyStyleTemplateFactory(
            name='default style',
            image_page=image_page,
            image_form=image_form,
            styles={
                'css': {},
            },
        )
        serializer = self.serializer_class(style_template)
        self.assertEqual(serializer.data['id'], style_template.pk)
        self.assertEqual(serializer.data['name'], style_template.name)
        self.assertEqual(serializer.data['image_page']['id'], style_template.image_page.pk)
        self.assertIn('links', serializer.data['image_page'])
        self.assertEqual(serializer.data['image_form']['id'], style_template.image_form.pk)
        self.assertIn('links', serializer.data['image_form'])
        self.assertDictEqual(serializer.data['styles'], style_template.styles)

    def test_should_update_existing_style_template(self):
        image_page = ImageFactory()
        image_form = ImageFactory()
        new_image_page = ImageFactory()
        style_template = SurveyStyleTemplateFactory(
            name='default style',
            image_page=image_page,
            image_form=image_form,
            styles={
                'css': {},
            },
        )
        serializer = self.serializer_class(style_template)
        data = {
            'image_page': new_image_page.pk,
            'image_form': None,
            'styles': {
                'font': {},
            },
        }
        serializer = self.serializer_class(style_template, data=data)
        self.assertTrue(serializer.is_valid(), msg=serializer.errors)
        new_style_template = serializer.save()
        self.assertEqual(new_style_template.name, 'default style')
        self.assertEqual(new_style_template.image_page.pk, new_image_page.pk)
        self.assertIsNone(new_style_template.image_form)
        self.assertDictEqual(new_style_template.styles, {'font': {}})
