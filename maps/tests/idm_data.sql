INSERT INTO idm.project(id, key, name_ru, name_en, slug, slug_name_ru, slug_name_en, parent_id)
VALUES (1, '_root', 'Система', 'System', 'project', 'Проект', 'Project', null),
       (2, 'one', 'Проект 1', 'Project 1', 'subproject', 'Подпроект', 'Subproject', 1),
       (3, 'two', 'Проект 2', 'Project 2', 'role', 'Роль', 'Role', 1),
       (4, 'one-A', 'Подпроект 1A', 'Subproject 1A', 'role', 'Роль', 'Role', 2),
       (5, 'one-B', 'Подпроект 1B', 'Subproject 1B', 'role', 'Роль', 'Role', 2);

INSERT INTO idm.role(id, key, name_ru, name_en, role_set)
VALUES (1, 'developer', 'Разработчик', 'Developer', 'developer'),
       (2, 'manager', 'Управляющий', 'Manager', null),
       (3, 'manager', 'Менеджер', 'Another Manager', null);

INSERT INTO idm.project_role(id, project_id, role_id)
VALUES (10, 3, 1),
       (11, 3, 2),
       (12, 4, 1),
       (13, 4, 3),
       (14, 5, 1);
