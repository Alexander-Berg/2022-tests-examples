# -*- coding: utf-8 -*-


from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('testenv', '0001_initial'),
    ]

    operations = [
        migrations.CreateModel(
            name='AllowedPhone',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('mobile_phone', models.CharField(max_length=15)),
                ('comment', models.TextField(blank=True)),
                ('added_at', models.DateTimeField(auto_now_add=True)),
            ],
        ),
    ]
