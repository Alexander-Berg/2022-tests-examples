"""
Views only for testing environment!
"""

import yenv

if yenv.type in ('testing', 'development'):
    from functools import partial
    import json

    from django.http import Http404, HttpResponseBadRequest, HttpResponse
    from django.views.decorators.csrf import csrf_exempt
    from django.views.decorators.http import require_http_methods

    from staff.departments.models import DepartmentStaff, DepartmentRoles, Department
    from staff.lib.decorators import responding_json
    from staff.person.controllers import PersonCtl
    from staff.person.models import Staff

    from .forms import HeadChangingForm, CreatePersonInTestingForm

    def delete_head(url, role):
        try:
            current_head = DepartmentStaff.objects.get(
                department__url=url, role_id=role)
            current_head.delete()
        except DepartmentStaff.DoesNotExist:
            pass
        finally:
            return 'ok'

    @responding_json
    def set_heads(request, role, url):
        if request.method != 'POST':
            raise Http404

        result = {}
        data = {
            'url': url,
            'role': role,
            'login': request.POST.get('login'),
        }
        form = HeadChangingForm(data=data)
        if form.is_valid():
            login = form.cleaned_data['login']
            if not login:
                result['result'] = delete_head(url, role)
                return result

            department = form.cleaned_data['department']
            new_head = form.cleaned_data['new_head']
        else:
            result['result'] = 'error'
            result['errors'] = form.errors
            return result

        current_head = DepartmentStaff.objects.filter(
            department=department, role_id=role)
        if current_head.count():
            current_head = current_head[0]
            current_head.staff = new_head
        else:
            current_head = DepartmentStaff.objects.create(
                department=department,
                staff=new_head,
                role_id=role,
            )
        try:
            current_head.save()
        except Exception:
            result['result'] = 'error'
            result['error'] = 'Error while updating DepartmentStaff object'

        result['result'] = 'ok'
        return result

    set_chief = csrf_exempt(partial(set_heads, role=DepartmentRoles.CHIEF.value))
    set_deputy = csrf_exempt(partial(set_heads, role=DepartmentRoles.DEPUTY.value))

    @require_http_methods(['POST'])
    @csrf_exempt
    def create_user_in_testing(request):
        if yenv.type == 'production':
            raise Http404

        try:
            form = CreatePersonInTestingForm(json.loads(request.body))
        except json.JSONDecodeError:
            return HttpResponseBadRequest()

        if not form.is_valid():
            return HttpResponseBadRequest(content=json.dumps(form.errors_as_dict()))

        data = form.cleaned_data
        deps = data.pop('departments')
        data['department'] = Department.objects.get(url=deps[0])
        existing_person_model = Staff.objects.filter(login=data['login']).first()
        if existing_person_model:
            ctl = PersonCtl(existing_person_model)
            ctl.restore().save()
        else:
            ctl = PersonCtl.create(data)
            ctl.save()
        return HttpResponse(status=201)
