CREATE OR REPLACE FUNCTION setCurrentState()
    RETURNS VOID AS $$
DECLARE
    normal RECORD;
    suspicious RECORD;
    attack RECORD;
    dtype RECORD;
    d RECORD;
    dSS RECORD;
    u6 RECORD;
    u7 RECORD;
    u4 RECORD;
    dnodeid RECORD;
BEGIN
    SELECT INTO normal id FROM security_state WHERE name='Normal';
    SELECT INTO suspicious id FROM security_state WHERE name='Suspicious';
    SELECT INTO attack id FROM security_state WHERE name='Attack';
    SELECT INTO dtype id FROM device_type WHERE name='WeMo Insight';
    INSERT INTO data_node(name, ip_address) VALUES('Hertz', '10.27.153.2') RETURNING id INTO dnodeid;
    INSERT INTO device(name, description, type_id, ip_address, status_history_size, sampling_rate, default_sampling_rate, data_node_id, credentials) VALUES ('WEMO1', 'Test wemo', dtype.id, '10.27.151.121', 1, 10000, 10000, dnodeid.id, '') RETURNING id INTO d;
    INSERT INTO device_security_state (device_id, state_id, timestamp) VALUES (d.id, normal.id, current_timestamp) RETURNING id INTO dSS;
    INSERT INTO umbox_image(name, file_name) VALUES ('u8-fake-replies', 'u8-fake-replies.qcow2') RETURNING id INTO u7;
    INSERT INTO umbox_image(name, file_name) VALUES ('u8-fake-replies', 'u8-fake-replies.qcow2') RETURNING id INTO u6;
    INSERT INTO umbox_image(name, file_name) VALUES ('u8-fake-replies', 'u8-fake-replies.qcow2') RETURNING id INTO u4;
    INSERT INTO umbox_lookup(security_state_id, device_type_id, umbox_image_id, dag_order) VALUES (normal.id, dtype.id, u7.id, 1);
    INSERT INTO umbox_lookup(security_state_id, device_type_id, umbox_image_id, dag_order) VALUES (suspicious.id, dtype.id, u6.id, 1);
    INSERT INTO umbox_lookup(security_state_id, device_type_id, umbox_image_id, dag_order) VALUES (attack.id, dtype.id, u4.id, 1);
END;
$$ LANGUAGE plpgsql;

SELECT setCurrentState();
