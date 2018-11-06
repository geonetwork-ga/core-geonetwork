INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('system/site/organizationWebsite', 'http://my.organization.net', 0, 140, 'n') ON CONFLICT DO NOTHING;
