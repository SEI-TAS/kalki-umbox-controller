CREATE OR REPLACE FUNCTION setCurrentState()
    RETURNS VOID AS $$
DECLARE
    normal RECORD;
    suspicious RECORD;
    attack RECORD;
    dtype RECORD;
    d RECORD;
    dSS RECORD;
    uimage RECORD;
BEGIN
    SELECT INTO normal id FROM security_state WHERE name='Normal';
    SELECT INTO suspicious id FROM security_state WHERE name='Suspicious';
    SELECT INTO attack id FROM security_state WHERE name='Attack';
    SELECT INTO dtype id FROM device_type WHERE name='Udoo Neo';
    INSERT INTO device(name, description, type_id, ip_address, status_history_size, sampling_rate, default_sampling_rate) VALUES ('UNTS1', 'Test Udoo', dtype.id, '10.27.151.101', 1, 10000, 10000) RETURNING id INTO d;
    INSERT INTO device_security_state (device_id, state_id, timestamp) VALUES (d.id, normal.id, current_timestamp) RETURNING id INTO dSS;
    INSERT INTO umbox_image(name, file_name) VALUES ('hello-world', '') RETURNING id INTO uimage;
    INSERT INTO umbox_lookup(state_id, device_type_id, umbox_image_id, dag_order) VALUES (normal.id, dtype.id, uimage.id, 1);
END;
$$ LANGUAGE plpgsql;

SELECT setCurrentState();
