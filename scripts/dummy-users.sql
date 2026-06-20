CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, email, password, display_name, provider) VALUES
  ('test1',  'test1@test.com',  crypt('test', gen_salt('bf', 10)), 'Test User 1',  'LOCAL'),
  ('test2',  'test2@test.com',  crypt('test', gen_salt('bf', 10)), 'Test User 2',  'LOCAL'),
  ('test3',  'test3@test.com',  crypt('test', gen_salt('bf', 10)), 'Test User 3',  'LOCAL'),
  ('test4',  'test4@test.com',  crypt('test', gen_salt('bf', 10)), 'Test User 4',  'LOCAL'),
  ('test5',  'test5@test.com',  crypt('test', gen_salt('bf', 10)), 'Test User 5',  'LOCAL'),
  ('test6',  'test6@test.com',  crypt('test', gen_salt('bf', 10)), 'Test User 6',  'LOCAL'),
  ('test7',  'test7@test.com',  crypt('test', gen_salt('bf', 10)), 'Test User 7',  'LOCAL'),
  ('test8',  'test8@test.com',  crypt('test', gen_salt('bf', 10)), 'Test User 8',  'LOCAL'),
  ('test9',  'test9@test.com',  crypt('test', gen_salt('bf', 10)), 'Test User 9',  'LOCAL'),
  ('test10', 'test10@test.com', crypt('test', gen_salt('bf', 10)), 'Test User 10', 'LOCAL')
ON CONFLICT DO NOTHING;
