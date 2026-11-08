CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (id, username, email, password, display_name, provider) VALUES
  ('00000000-0000-0000-0000-000000000001', 'test1', 'test1@test.com', crypt('test', gen_salt('bf', 10)), 'Test User 1', 'LOCAL'),
  ('00000000-0000-0000-0000-000000000002', 'test2', 'test2@test.com', crypt('test', gen_salt('bf', 10)), 'Test User 2', 'LOCAL'),
  ('00000000-0000-0000-0000-000000000003', 'test3', 'test3@test.com', crypt('test', gen_salt('bf', 10)), 'Test User 3', 'LOCAL'),
  ('00000000-0000-0000-0000-000000000004', 'test4', 'test4@test.com', crypt('test', gen_salt('bf', 10)), 'Test User 4', 'LOCAL')
ON CONFLICT DO NOTHING;
