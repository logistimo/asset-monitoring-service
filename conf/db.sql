CREATE USER 'logistimo'@'%' IDENTIFIED BY  'logistimo';
grant all privileges on logistimo_tms.* to logistimo@'%';
FLUSH PRIVILEGES;

CREATE TABLE user_accounts (
  id bigint NOT NULL AUTO_INCREMENT,
  organizationname character varying(255) NOT NULL,
  password character varying(255) NOT NULL,
  username character varying(255) NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE user_accounts ADD CONSTRAINT user_accounts_username_key UNIQUE (username);

CREATE TABLE devices (
  id bigint NOT NULL AUTO_INCREMENT,
  deviceid character varying(255) NOT NULL,
  uniquehash character varying(255) NOT NULL,
  vendorid character varying(255) NOT NULL,
  transmitterid character varying(255) DEFAULT NULL,
  devicemodel character varying(255) DEFAULT NULL,
  deviceversion character varying(255) DEFAULT NULL,
  deviceimei character varying(255) DEFAULT NULL,
  simnumber character varying(255) DEFAULT NULL,
  simid character varying(255) DEFAULT NULL,
  countrycode character varying(5) DEFAULT NULL,
  timezone double precision,
  gsmmoduleversion character varying(255) DEFAULT NULL,
  statusupdatedtime bigint,
  devicestate integer DEFAULT 0,
  stateupdatedtime bigint DEFAULT 0,
  temperature DOUBLE PRECISION DEFAULT 0,
  tmpUpdatedTime INT DEFAULT 0,
  sInt INT DEFAULT 0,
  pInt INT DEFAULT 0,
  iAct INT DEFAULT 0,
  iaStt INT DEFAULT 0,
  iActPIntCounts INT DEFAULT 0,
  PRIMARY KEY (id)
);
ALTER TABLE devices ADD CONSTRAINT devices_uniquehash_key UNIQUE (uniquehash);
CREATE INDEX devices_deviceid_vendorid_idx ON devices(deviceid, vendorid) USING btree ;

CREATE TABLE temperature_readings (
  id bigint NOT NULL AUTO_INCREMENT,
  temperature double precision NOT NULL,
  timeofreading integer NOT NULL,
  type integer NOT NULL,
  device_id bigint,
  PRIMARY KEY (id)
);
ALTER TABLE temperature_readings ADD CONSTRAINT fkc56151d28112706 FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
ALTER TABLE temperature_readings ADD CONSTRAINT unique_temp_reading UNIQUE (device_id, timeofreading, type, temperature);
CREATE INDEX temperature_readings_device_id_idx ON temperature_readings(device_id) USING hash;
CREATE INDEX temperature_readings_device_id_timeofreading_type_temp_idx ON temperature_readings(device_id, timeofreading, type, temperature) USING btree;
CREATE INDEX temperature_readings_device_id_timeofreading_type_idx ON temperature_readings(device_id, timeofreading, type) USING btree;

CREATE TABLE tags (
  id bigint NOT NULL AUTO_INCREMENT,
  tagname character varying(255) NOT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX tags_tagname_idx ON tags(tagname) USING hash;

CREATE TABLE device_alarms (
  id bigint NOT NULL AUTO_INCREMENT,
  type integer NOT NULL,
  status integer NOT NULL,
  time integer NOT NULL,
  error_code character varying(5),
  device_id bigint,
  created_on timestamp DEFAULT now(),
  error_message character varying(255) DEFAULT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE device_alarms ADD CONSTRAINT device_alarm_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_alarm_device_id_idx ON device_alarms(device_id) USING hash;

CREATE TABLE devices_tags (
  devices_id bigint NOT NULL,
  tags_id bigint NOT NULL
);
ALTER TABLE devices_tags ADD CONSTRAINT devices_tags_pkey PRIMARY KEY (devices_id, tags_id);
ALTER TABLE devices_tags ADD CONSTRAINT fkfe22c23b352e371f FOREIGN KEY (devices_id) REFERENCES devices(id) ON DELETE CASCADE;
ALTER TABLE devices_tags ADD CONSTRAINT fkfe22c23b7a5e790f FOREIGN KEY (tags_id) REFERENCES tags(id);

CREATE TABLE device_status (
  id bigint NOT NULL AUTO_INCREMENT,
  config_updated_time integer,
  config_updated_type integer,
  device_id bigint,
  created_on timestamp DEFAULT now(),
  PRIMARY KEY(id)
);
ALTER TABLE device_status ADD CONSTRAINT device_status_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_status_device_id_idx ON device_status(device_id) USING hash;

CREATE TABLE device_admin_settings (
  id bigint NOT NULL AUTO_INCREMENT,
  phone_number text NOT NULL,
  password text,
  device_id bigint,
  sender_id text,
  created_on timestamp DEFAULT now(),
  PRIMARY KEY(id)
);
ALTER TABLE device_admin_settings ADD CONSTRAINT device_admin_settings_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_admin_settings_device_id_idx ON device_admin_settings(device_id) USING hash;

CREATE TABLE device_admin_settings_push_status (
  id bigint NOT NULL AUTO_INCREMENT,
  status text NOT NULL,
  sent_time timestamp NOT NULL,
  acknowledged_time timestamp,
  error_code text,
  error_message text,
  device_id bigint,
  created_on timestamp DEFAULT now(),
  PRIMARY KEY(id)
);
ALTER TABLE device_admin_settings_push_status ADD CONSTRAINT device_admin_settings_push_status_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_admin_settings_push_status_device_id_idx ON device_admin_settings_push_status(device_id) USING hash;

CREATE TABLE device_apn_settings (
  id bigint NOT NULL AUTO_INCREMENT,
  name text NOT NULL,
  address text,
  username text,
  password text,
  device_id bigint,
  created_on timestamp DEFAULT now(),
  PRIMARY KEY(id)
);
ALTER TABLE device_apn_settings ADD CONSTRAINT device_apn_settings_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_apn_settings_device_id_idx ON device_apn_settings(device_id) USING hash;

CREATE TABLE device_configurations (
  id bigint NOT NULL AUTO_INCREMENT,
  configuration text NOT NULL,
  device_id bigint,
  created_on timestamp DEFAULT now(),
  tag_id bigint,
  firmware_version text,
  PRIMARY KEY(id)
);
ALTER TABLE device_configurations ADD CONSTRAINT device_configurations_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
ALTER TABLE device_configurations ADD CONSTRAINT device_configurations_tags_fkey FOREIGN KEY (tag_id) REFERENCES tags(id);
CREATE INDEX device_configurations_device_id_idx ON device_configurations(device_id) USING hash;
CREATE INDEX device_configurations_tag_id_idx ON device_configurations(tag_id) USING hash;

CREATE TABLE device_config_push_status (
  id bigint NOT NULL AUTO_INCREMENT,
  status text NOT NULL,
  sent_time timestamp NOT NULL,
  acknowledged_time timestamp,
  error_code text,
  error_message text,
  device_id bigint,
  created_on timestamp DEFAULT now(),
  PRIMARY KEY(id)
);
ALTER TABLE device_config_push_status ADD CONSTRAINT device_config_push_status_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_config_push_status_device_id_idx ON device_config_push_status(device_id) USING hash;

CREATE TABLE device_ready (
  id bigint NOT NULL AUTO_INCREMENT,
  transmitter_id character varying(255) DEFAULT NULL,
  device_model character varying(255) DEFAULT NULL,
  device_sensor_firmware_version character varying(255) DEFAULT NULL,
  device_gsm_firmware_version character varying(255) DEFAULT NULL,
  device_imei character varying(255) DEFAULT NULL,
  sim_phone character varying(255) DEFAULT NULL,
  device_id bigint,
  created_on timestamp DEFAULT now(),
  sim_id character varying(255) DEFAULT NULL,
  PRIMARY KEY(id)
);
ALTER TABLE device_ready ADD CONSTRAINT device_ready_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_ready_device_id_idx ON device_ready(device_id) USING hash;

CREATE TABLE device_temperature_request (
  id bigint NOT NULL AUTO_INCREMENT,
  number_of_request integer,
  device_id bigint,
  created_on timestamp DEFAULT now(),
  status text,
  PRIMARY KEY(id)
);
ALTER TABLE device_temperature_request ADD CONSTRAINT device_temperature_request_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_temperature_request_device_id_idx ON device_temperature_request(device_id) USING hash;

CREATE TABLE temperature_statistics (
  id bigint NOT NULL AUTO_INCREMENT,
  dayofcomputation bigint NOT NULL,
  durationhigh integer NOT NULL,
  durationlow integer NOT NULL,
  firsthighalerttime bigint NOT NULL,
  firstlowalerttime bigint NOT NULL,
  firstsensorconnectionfailuretime bigint NOT NULL,
  highalertambienttemperature double precision,
  highesttemperature double precision,
  lowalertambienttemperaturelow double precision,
  lowesttemperature double precision,
  meantemperature double precision,
  numberinternetpushfailures integer NOT NULL,
  numberofalerts integer NOT NULL,
  numberofhighalerts integer NOT NULL,
  numberofinternetpushes integer NOT NULL,
  numberoflowalerts integer NOT NULL,
  numberofsmssent integer NOT NULL,
  sensorconnectionfailureduration integer NOT NULL,
  device_id bigint,
  PRIMARY KEY(id)
);
ALTER TABLE temperature_statistics ADD CONSTRAINT temperature_statistics_fk FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX temperature_statistics_device_id_idx ON temperature_statistics(device_id) USING hash;

CREATE TABLE daily_aggregations (
  id bigint NOT NULL AUTO_INCREMENT,
  day integer NOT NULL,
  durationoutofrange bigint NOT NULL,
  month integer NOT NULL,
  year integer NOT NULL,
  device_id bigint,
  PRIMARY KEY(id)
);
ALTER TABLE daily_aggregations ADD CONSTRAINT daily_aggregations_fk FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX daily_aggregations_device_id_idx ON daily_aggregations(device_id) USING hash;


CREATE TABLE daily_stats (
  id bigint NOT NULL AUTO_INCREMENT,
  day integer NOT NULL,
  timezone_offset DOUBLE PRECISION DEFAULT 0,
  number_of_excursions integer,
  mean_temperature double precision NOT NULL,
  min_temperature double precision NOT NULL,
  max_temperature double precision NOT NULL,
  high_alert_status integer NOT NULL,
  high_alert_time integer NOT NULL,
  high_alert_duration integer NOT NULL,
  high_alert_ambient_temperature double precision NOT NULL,
  low_alert_status integer NOT NULL,
  low_alert_time integer NOT NULL,
  low_alert_duration integer NOT NULL,
  low_alert_ambient_temperature double precision NOT NULL,
  external_sensor_status integer NOT NULL,
  external_sensor_time integer NOT NULL,
  external_sensor_duration integer NOT NULL,
  device_connection_status integer NOT NULL,
  device_connection_time integer NOT NULL,
  device_connection_duration integer NOT NULL,
  battery_status integer NOT NULL,
  battery_time integer NOT NULL,
  battery_actual_volt double precision NOT NULL,
  number_of_sms_sent integer NOT NULL,
  number_of_internet_pushes integer NOT NULL,
  number_of_internet_failures integer NOT NULL,
  device_id bigint,
  created_on timestamp DEFAULT now(),
  battery_low_volt double precision DEFAULT 0,
  battery_high_volt double precision DEFAULT 0,
  battery_charging_time integer DEFAULT 0,
  battery_warning_dur integer DEFAULT 0,
  battery_alarm_dur integer DEFAULT 0,
  high_alert_nalarms bigint DEFAULT 0,
  high_alert_cnfms character varying(255) DEFAULT NULL,
  low_alert_nalarms bigint DEFAULT 0,
  low_alert_cnfms character varying(255) DEFAULT NULL,
  external_sensor_nalarms bigint DEFAULT 0,
  device_connection_alarms bigint DEFAULT 0,
  battery_nalarms bigint DEFAULT 0,
  power_available_time bigint DEFAULT 0,
  high_alert_cnf boolean DEFAULT false,
  low_alert_cnf boolean DEFAULT false,
  PRIMARY KEY(id)
);
ALTER TABLE daily_stats ADD CONSTRAINT daily_stats_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX daily_stats_device_id_idx ON daily_stats(device_id) USING hash;

CREATE TABLE daily_stats_device_errors (
  id bigint NOT NULL AUTO_INCREMENT,
  error_code character varying(50) NOT NULL,
  count integer NOT NULL,
  time integer NOT NULL,
  daily_stats_id bigint,
  created_on timestamp DEFAULT now(),
  PRIMARY KEY(id)
);
ALTER TABLE daily_stats_device_errors ADD CONSTRAINT daily_stats_device_errors_fkey FOREIGN KEY (daily_stats_id) REFERENCES daily_stats(id) ON DELETE CASCADE;
CREATE INDEX daily_stats_device_errors_daily_stats_id_idx ON daily_stats_device_errors(daily_stats_id) USING hash;

--2.0.7
ALTER TABLE devices ADD COLUMN createdOn TIMESTAMP DEFAULT current_timestamp;

CREATE TABLE task (
  id bigint NOT NULL AUTO_INCREMENT,
  task_options text NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE alarm_log(
  id bigint NOT NULL AUTO_INCREMENT,
  alarm_time INTEGER NOT NULL,
  alarm_type INTEGER NOT NULL,
  temperature DOUBLE DEFAULT NULL,
  temperature_type INTEGER DEFAULT NULL,
  device_alarm_type INTEGER DEFAULT NULL,
  device_alarm_status INTEGER DEFAULT NULL,
  device_battery_volt DOUBLE DEFAULT NULL,
  device_firmware_error_code CHARACTER VARYING(50) DEFAULT NULL,
  device_id bigint,
  PRIMARY KEY(id)
);

ALTER TABLE alarm_log ADD CONSTRAINT alarm_log_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX alarm_log_device_id_idx ON alarm_log(device_id) USING hash;
CREATE INDEX alarm_log_device_id_alarm_time_idx ON alarm_log(device_id, alarm_time) USING btree;

-- 2.1.0
CREATE TABLE asset(
  id bigint NOT NULL AUTO_INCREMENT,
  uniquehash VARCHAR(50) NOT NULL,
  asset_id VARCHAR(100) NOT NULL ,
  manc_id VARCHAR(50) NOT NULL,
  asset_type VARCHAR(20) NOT NULL,
  temperature_state INT DEFAULT 0,
  temperature_state_updated_time INTEGER DEFAULT 0,
  status INT DEFAULT 0,
  status_updated_time INTEGER DEFAULT 0,
  created_on TIMESTAMP DEFAULT current_timestamp,
  updated_on TIMESTAMP DEFAULT current_timestamp,
  PRIMARY KEY(id)
);

CREATE INDEX asset_mId_aId_idx ON asset(manc_id, asset_id) USING BTREE;
CREATE INDEX asset_at_idx ON asset(asset_type) USING HASH;

CREATE TABLE asset_mapping(
  id BIGINT NOT NULL AUTO_INCREMENT,
  location_id INT DEFAULT 0,
  relation_type INT NOT NULL,
  is_primary INT DEFAULT 0,
  asset_id BIGINT,
  relatedAsset_id BIGINT,
  PRIMARY KEY(id)
);

ALTER TABLE asset_mapping ADD CONSTRAINT asset_mapping_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES devices(id) ON DELETE CASCADE;
ALTER TABLE asset_mapping ADD CONSTRAINT asset_mapping_mapping_asset_id_fkey FOREIGN KEY (relatedAsset_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX asset_mapping_asset_id_idx ON asset_mapping(asset_id) USING HASH;
CREATE INDEX asset_mapping_mapping_asset_id_idx ON asset_mapping(relatedAsset_id) USING HASH;
CREATE INDEX asset_mapping_asset_id_mapping_asset_id_idx ON asset_mapping(asset_id, relatedAsset_id) USING BTREE;
CREATE INDEX asset_mapping_asset_id_is_primary_idx ON asset_mapping(asset_id, is_primary) USING BTREE;

ALTER TABLE temperature_readings ADD COLUMN monitored_asset_id BIGINT DEFAULT NULL;
ALTER TABLE temperature_readings ADD COLUMN location_id INT DEFAULT NULL;

CREATE TABLE device_power_transition(
  id BIGINT NOT NULL AUTO_INCREMENT,
  state INT NOT NULL,
  transition_time INT NOT NULL,
  device_id BIGINT,
  PRIMARY KEY(id)
);

ALTER TABLE device_power_transition ADD CONSTRAINT device_power_transition_device_id_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_power_transition_device_id_idx ON device_power_transition(device_id) USING HASH;
ALTER TABLE device_alarms ADD  COLUMN power_availability INT DEFAULT 0;

ALTER TABLE daily_stats ADD  COLUMN available_disk_space DOUBLE PRECISION DEFAULT 0;
ALTER TABLE daily_stats ADD  COLUMN number_of_temperature_cached INT DEFAULT 0;
ALTER TABLE daily_stats ADD  COLUMN number_of_dvc_cached INT DEFAULT 0;

ALTER TABLE devices ADD COLUMN altPhoneNumber CHARACTER VARYING(30) DEFAULT NULL;
ALTER TABLE devices ADD COLUMN altSimId CHARACTER VARYING(100) DEFAULT NULL;
ALTER TABLE devices ADD COLUMN activeSensors CHARACTER VARYING(30) DEFAULT NULL;
ALTER TABLE devices ADD COLUMN sensors CHARACTER VARYING(30) DEFAULT NULL;

ALTER TABLE device_apn_settings ADD COLUMN alt_name CHARACTER VARYING(50) DEFAULT NULL;
ALTER TABLE device_apn_settings ADD COLUMN alt_address CHARACTER VARYING(255) DEFAULT NULL;
ALTER TABLE devices DROP COLUMN activeSensors;
ALTER TABLE devices DROP COLUMN sensors;

CREATE TABLE temperature_sensors(
  id BIGINT NOT NULL AUTO_INCREMENT,
  sensor_id VARCHAR(10) NOT NULL,
  code VARCHAR(20) NOT NULL,
  status INT DEFAULT 0,
  device_id BIGINT,
  PRIMARY KEY(id)
);

ALTER TABLE temperature_sensors ADD CONSTRAINT temperature_sensors_device_id_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX temperature_sensors_device_id_idx ON temperature_sensors(device_id) USING HASH;

CREATE TABLE device_meta_data(
  id BIGINT NOT NULL AUTO_INCREMENT,
  ky VARCHAR(50) NOT NULL,
  value VARCHAR(100) NOT NULL,
  device_id BIGINT,
  PRIMARY KEY(id)
);

ALTER TABLE device_meta_data ADD CONSTRAINT device_meta_data_id_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;
CREATE INDEX device_meta_data_id_idx ON device_meta_data(device_id) USING HASH;
CREATE INDEX device_meta_data_id_key_idx on device_meta_data(device_id, ky) USING BTREE;

CREATE TABLE asset_user(
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_name VARCHAR(50) NOT NULL,
  user_type INT NOT NULL,
  device_id BIGINT,
  PRIMARY KEY(id),
  FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX asset_user_device_id_idx on asset_user(device_id) USING HASH;
CREATE INDEX asset_user_device_id_user_type_idx ON asset_user(device_id, user_type) USING BTREE;

CREATE TABLE asset_type(
  id INT NOT NULL AUTO_INCREMENT,
  asset_name VARCHAR(50) NOT NULL,
  is_temp_sensitive BOOLEAN DEFAULT FALSE,
  is_gsm_enabled BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (id)
);

ALTER TABLE devices ADD COLUMN createdBy VARCHAR(50) NOT NULL;
ALTER TABLE devices ADD COLUMN updatedBy VARCHAR(50) NOT NULL;
ALTER TABLE devices ADD COLUMN updatedOn TIMESTAMP DEFAULT current_timestamp;
ALTER TABLE devices ADD COLUMN imgUrls TEXT DEFAULT NULL;
ALTER TABLE devices ADD COLUMN vendorName VARCHAR(50) DEFAULT NULL;
ALTER TABLE devices ADD COLUMN  assetType_id INT;
ALTER TABLE devices ADD CONSTRAINT devices_asset_type_key FOREIGN KEY (assetType_id) REFERENCES asset_type(id);

ALTER TABLE devices ADD COLUMN locationId VARCHAR(50) DEFAULT NULL;

ALTER TABLE asset_type ADD COLUMN type INT NOT NULL;
INSERT INTO asset_type VALUES(1, 'Temperature Logger', true, true, 1);
INSERT INTO asset_type VALUES(4, 'Temperature Sensors', true, true, 0);
INSERT INTO asset_type VALUES(2, 'Fridge', true, false, 2);
INSERT INTO asset_type VALUES(3, 'Deep freezer', true, false, 2);


ALTER TABLE devices CHANGE COLUMN devicestate temperatureState integer DEFAULT 0;
ALTER TABLE devices CHANGE COLUMN stateupdatedtime temperatureStateUT INT DEFAULT 0;
ALTER TABLE devices CHANGE COLUMN iAct activityState INT DEFAULT 0;
ALTER TABLE devices CHANGE COLUMN iaStt activityStateUT INT DEFAULT 0;
ALTER TABLE devices CHANGE COLUMN tmpUpdatedTime temperatureUT INT DEFAULT 0;
ALTER TABLE devices ADD COLUMN workingState INT DEFAULT 0;
ALTER TABLE devices ADD COLUMN workingStateUT INT DEFAULT 0;
ALTER TABLE devices ADD COLUMN deviceAlarmState INT DEFAULT 0;
ALTER TABLE devices ADD COLUMN deviceAlarmStateUT INT DEFAULT 0;

RENAME TABLE device_status to device_config_status;
CREATE TABLE device_status(
  id INT NOT NULL AUTO_INCREMENT,
  status_key VARCHAR(20) NOT NULL,
  status INTEGER NOT NULL,
  status_ut INTEGER NOT NULL,
  location_id INTEGER,
  sensor_id VARCHAR(5),
  temperature DOUBLE,
  temperature_ut INTEGER,
  temperature_abnormal_status INTEGER,
  device_id BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX device_status_device_id_idx ON device_status(device_id) USING HASH;

ALTER TABLE alarm_log ADD COLUMN temperature_abnormal_type INTEGER;
ALTER TABLE device_alarms ADD COLUMN sensor_id VARCHAR(5) DEFAULT NULL;

ALTER TABLE temperature_readings drop COLUMN monitored_asset_id;
ALTER TABLE temperature_readings ADD COLUMN monitoredAsset_id bigint;
ALTER TABLE temperature_readings ADD CONSTRAINT temperature_readings_monitored_asset_id_fkey FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;

ALTER TABLE alarm_log ADD COLUMN monitoring_position_id INTEGER;

ALTER TABLE temperature_readings DROP FOREIGN KEY fkc56151d28112706;
ALTER TABLE temperature_readings DROP FOREIGN KEY temperature_readings_monitored_asset_id_fkey;

ALTER TABLE asset_mapping DROP FOREIGN KEY asset_mapping_asset_id_fkey;
ALTER TABLE asset_mapping DROP FOREIGN KEY asset_mapping_mapping_asset_id_fkey;

ALTER TABLE asset_mapping ADD CONSTRAINT asset_mapping_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES devices(id) ON DELETE CASCADE;
ALTER TABLE asset_mapping ADD CONSTRAINT asset_mapping_mapping_asset_id_fkey FOREIGN KEY (relatedAsset_id) REFERENCES devices(id) ON DELETE CASCADE;

ALTER TABLE alarm_log ADD COLUMN sensor_id VARCHAR(10) DEFAULT NULL;


INSERT INTO asset_type VALUES(5, 'Walk In Cooler', true, false, 2);
INSERT INTO asset_type VALUES(6, 'Walk In Freezer', true, false, 2);
UPDATE asset_type SET type=2 WHERE id=3;


#212
ALTER TABLE temperature_readings ADD COLUMN power_availability INTEGER;
ALTER TABLE user_accounts ADD COLUMN type INTEGER DEFAULT 2;

INSERT INTO asset_type VALUES(7, 'Fridge', true, false, 2);

#213
Alter table temperature_readings add source INTEGER(1) DEFAULT NULL;
alter table alarm_log drop foreign key alarm_log_fkey;
alter table asset_mapping drop foreign key asset_mapping_asset_id_fkey;
alter table asset_mapping drop foreign key asset_mapping_mapping_asset_id_fkey;
alter table asset_user drop foreign key asset_user_ibfk_1;
alter table daily_aggregations drop foreign key daily_aggregations_fk;
alter table daily_stats drop foreign key daily_stats_fkey;
alter table daily_stats_device_errors drop foreign key daily_stats_device_errors_fkey;
alter table device_admin_settings drop foreign key device_admin_settings_fkey;
alter table device_admin_settings_push_status drop foreign key device_admin_settings_push_status_fkey;
alter table device_alarms drop foreign key device_alarm_fkey;
alter table device_apn_settings drop foreign key device_apn_settings_fkey;
alter table device_config_push_status drop foreign key device_config_push_status_fkey;
alter table device_config_status drop foreign key device_status_fkey;
alter table device_configurations drop foreign key device_configurations_fkey;
alter table device_configurations drop foreign key device_configurations_tags_fkey;
alter table device_meta_data drop foreign key device_meta_data_id_fkey;
alter table device_power_transition drop foreign key device_power_transition_device_id_fkey;
alter table device_ready drop foreign key device_ready_fkey;
alter table device_status drop foreign key device_status_ibfk_1;
alter table device_temperature_request drop foreign key device_temperature_request_fkey;
alter table devices drop foreign key devices_asset_type_key;
alter table devices_tags drop foreign key fkfe22c23b352e371f;
alter table devices_tags drop foreign key fkfe22c23b7a5e790f;
alter table temperature_sensors drop foreign key temperature_sensors_device_id_fkey;
alter table temperature_statistics drop foreign key temperature_statistics_fk;
