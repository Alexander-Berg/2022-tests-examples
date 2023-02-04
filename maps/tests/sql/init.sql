INSERT INTO public.user(login, uid, is_super) VALUES('test_user_super', 'test_uid_super', true);
INSERT INTO public.user_company(user_id, company_id, role) VALUES(lastval(), NULL, 'admin');
