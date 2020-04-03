CREATE OR REPLACE FUNCTION setCurrentState()
    RETURNS VOID AS $$
DECLARE
    normal RECORD;
    suspicious RECORD;
    attack RECORD;
    dtype RECORD;
    d RECORD;
    dSS RECORD;
    u1 RECORD;
    u2 RECORD;
    u3 RECORD;
    dnodeid RECORD;
BEGIN
    SELECT INTO normal id FROM security_state WHERE name='Normal';
    SELECT INTO suspicious id FROM security_state WHERE name='Suspicious';
    SELECT INTO attack id FROM security_state WHERE name='Attack';
    SELECT INTO dtype id FROM device_type WHERE name='DLink Camera';
    INSERT INTO data_node(name, ip_address) VALUES('Hertz', '10.27.153.2') RETURNING id INTO dnodeid;
    INSERT INTO device(name, description, type_id, ip_address, status_history_size, sampling_rate, default_sampling_rate, data_node_id) VALUES ('CAM1', 'Test cam', dtype.id, '10.27.151.114', 1, 10000, 10000, dnodeid) RETURNING id INTO d;
    INSERT INTO device_security_state (device_id, state_id, timestamp) VALUES (d.id, normal.id, current_timestamp) RETURNING id INTO dSS;
    INSERT INTO umbox_image(name, file_name) VALUES ('u14-dlc-max-login', 'u14-dlc-max-login.qcow2') RETURNING id INTO u1;
    INSERT INTO umbox_image(name, file_name) VALUES ('u3-http-auth-proxy-block', 'u3-http-auth-proxy-block.qcow2') RETURNING id INTO u2;
    INSERT INTO umbox_image(name, file_name) VALUES ('u4-block-all', 'u4-block-all.qcow2') RETURNING id INTO u3;
    INSERT INTO umbox_lookup(security_state_id, device_type_id, umbox_image_id, dag_order) VALUES (normal.id, dtype.id, u1.id, 1);
    INSERT INTO umbox_lookup(security_state_id, device_type_id, umbox_image_id, dag_order) VALUES (suspicious.id, dtype.id, u2.id, 1);
    INSERT INTO umbox_lookup(security_state_id, device_type_id, umbox_image_id, dag_order) VALUES (attack.id, dtype.id, u3.id, 1);
END;
$$ LANGUAGE plpgsql;

SELECT setCurrentState();
