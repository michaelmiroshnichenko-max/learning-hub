-- Test profile only (quarkus.flyway.locations); dev mode uses db/dev-seed. Never enabled in production.
INSERT INTO account (id, login) VALUES
    ('00000000-0000-0000-0000-00000000000a', 'learner-a'),
    ('00000000-0000-0000-0000-00000000000b', 'learner-b')
ON CONFLICT (id) DO NOTHING;
