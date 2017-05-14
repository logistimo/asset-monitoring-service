/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `alarm_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `alarm_type` int(11) NOT NULL,
  `temperature` double DEFAULT NULL,
  `temperature_type` int(11) DEFAULT NULL,
  `device_alarm_type` int(11) DEFAULT NULL,
  `device_alarm_status` int(11) DEFAULT NULL,
  `device_battery_volt` double DEFAULT NULL,
  `device_firmware_error_code` varchar(50) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `temperature_abnormal_type` int(11) DEFAULT NULL,
  `monitoring_position_id` int(11) DEFAULT NULL,
  `sensor_id` varchar(10) DEFAULT NULL,
  `start_time` int(11) DEFAULT NULL,
  `end_time` int(11) DEFAULT NULL,
  `updated_on` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `alarm_log_device_id_idx` (`device_id`) USING HASH,
  KEY `alarm_log_device_id_alarm_time_idx` (`device_id`) USING BTREE,
  KEY `alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_1` (`device_id`,`alarm_type`,`device_alarm_status`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_2` (`id`,`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=65794 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `asset` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uniquehash` varchar(50) NOT NULL,
  `asset_id` varchar(100) NOT NULL,
  `manc_id` varchar(50) NOT NULL,
  `asset_type` varchar(20) NOT NULL,
  `temperature_state` int(11) DEFAULT '0',
  `temperature_state_updated_time` int(11) DEFAULT '0',
  `status` int(11) DEFAULT '0',
  `status_updated_time` int(11) DEFAULT '0',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `asset_mId_aId_idx` (`manc_id`,`asset_id`) USING BTREE,
  KEY `asset_at_idx` (`asset_type`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `asset_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `location_id` int(11) DEFAULT '0',
  `relation_type` varchar(30) NOT NULL,
  `asset_id` bigint(20) DEFAULT NULL,
  `relatedAsset_id` bigint(20) DEFAULT NULL,
  `is_primary` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `asset_mapping_asset_id_idx` (`asset_id`) USING HASH,
  KEY `asset_mapping_mapping_asset_id_idx` (`relatedAsset_id`) USING HASH,
  KEY `asset_mapping_asset_id_mapping_asset_id_idx` (`asset_id`,`relatedAsset_id`) USING BTREE,
  KEY `asset_mapping_asset_id_is_primary_idx` (`asset_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5894 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `asset_type` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `asset_name` varchar(50) NOT NULL,
  `is_temp_sensitive` tinyint(1) DEFAULT '0',
  `is_gsm_enabled` tinyint(1) DEFAULT '0',
  `type` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `asset_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_name` varchar(50) NOT NULL,
  `user_type` int(11) NOT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `asset_user_device_id_idx` (`device_id`) USING HASH,
  KEY `asset_user_device_id_user_type_idx` (`device_id`,`user_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bk1_alarm_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `alarm_type` int(11) NOT NULL,
  `temperature` double DEFAULT NULL,
  `temperature_type` int(11) DEFAULT NULL,
  `device_alarm_type` int(11) DEFAULT NULL,
  `device_alarm_status` int(11) DEFAULT NULL,
  `device_battery_volt` double DEFAULT NULL,
  `device_firmware_error_code` varchar(50) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `temperature_abnormal_type` int(11) DEFAULT NULL,
  `monitoring_position_id` int(11) DEFAULT NULL,
  `sensor_id` varchar(10) DEFAULT NULL,
  `start_time` int(11) DEFAULT NULL,
  `end_time` int(11) DEFAULT NULL,
  `updated_on` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `alarm_log_device_id_idx` (`device_id`) USING HASH,
  KEY `alarm_log_device_id_alarm_time_idx` (`device_id`) USING BTREE,
  KEY `alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_1` (`device_id`,`alarm_type`,`device_alarm_status`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_2` (`id`,`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bk2_alarm_log` (
  `alarm_type` int(11) NOT NULL,
  `temperature` double NOT NULL,
  `temperature_type` int(11) NOT NULL,
  `temperature_abnormal_type` int(11) NOT NULL,
  `device_alarm_type` int(11) NOT NULL,
  `device_alarm_status` int(11) NOT NULL,
  `device_battery_volt` int(11) NOT NULL,
  `device_firmware_error_code` text,
  `device_id` bigint(20) NOT NULL,
  `start_time` int(11) NOT NULL,
  `end_time` int(11) DEFAULT NULL,
  `monitoring_position_id` int(11) DEFAULT NULL,
  `sensor_id` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bk_alarm_log` (
  `id` bigint(20) NOT NULL DEFAULT '0',
  `alarm_time` int(11) NOT NULL,
  `alarm_type` int(11) NOT NULL,
  `temperature` double DEFAULT NULL,
  `temperature_type` int(11) DEFAULT NULL,
  `device_alarm_type` int(11) DEFAULT NULL,
  `device_alarm_status` int(11) DEFAULT NULL,
  `device_battery_volt` double DEFAULT NULL,
  `device_firmware_error_code` varchar(50) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `temperature_abnormal_type` int(11) DEFAULT NULL,
  `monitoring_position_id` int(11) DEFAULT NULL,
  `sensor_id` varchar(10) DEFAULT NULL,
  `start_time` int(11) DEFAULT NULL,
  `end_time` int(11) DEFAULT NULL,
  `updated_on` datetime DEFAULT NULL,
  KEY `alarm_log_device_id_idx` (`device_id`) USING HASH,
  KEY `alarm_log_device_id_alarm_time_idx` (`device_id`,`alarm_time`) USING BTREE,
  KEY `alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`,`alarm_time`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`,`alarm_time`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_1` (`device_id`,`alarm_type`,`device_alarm_status`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`,`alarm_time`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_2` (`id`,`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`,`alarm_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `daily_aggregations` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `day` int(11) NOT NULL,
  `durationoutofrange` bigint(20) NOT NULL,
  `month` int(11) NOT NULL,
  `year` int(11) NOT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `daily_aggregations_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `daily_stats` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `day` int(11) NOT NULL,
  `timezone_offset` double DEFAULT '0',
  `number_of_excursions` int(11) DEFAULT NULL,
  `mean_temperature` double NOT NULL,
  `min_temperature` double NOT NULL,
  `max_temperature` double NOT NULL,
  `high_alert_status` int(11) NOT NULL,
  `high_alert_time` int(11) NOT NULL,
  `high_alert_duration` int(11) NOT NULL,
  `high_alert_ambient_temperature` double NOT NULL,
  `low_alert_status` int(11) NOT NULL,
  `low_alert_time` int(11) NOT NULL,
  `low_alert_duration` int(11) NOT NULL,
  `low_alert_ambient_temperature` double NOT NULL,
  `external_sensor_status` int(11) NOT NULL,
  `external_sensor_time` int(11) NOT NULL,
  `external_sensor_duration` int(11) NOT NULL,
  `device_connection_status` int(11) NOT NULL,
  `device_connection_time` int(11) NOT NULL,
  `device_connection_duration` int(11) NOT NULL,
  `battery_status` int(11) NOT NULL,
  `battery_time` int(11) NOT NULL,
  `battery_actual_volt` double NOT NULL,
  `number_of_sms_sent` int(11) NOT NULL,
  `number_of_internet_pushes` int(11) NOT NULL,
  `number_of_internet_failures` int(11) NOT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `battery_low_volt` double DEFAULT '0',
  `battery_high_volt` double DEFAULT '0',
  `battery_charging_time` int(11) DEFAULT '0',
  `battery_warning_dur` int(11) DEFAULT '0',
  `battery_alarm_dur` int(11) DEFAULT '0',
  `high_alert_nalarms` bigint(20) DEFAULT '0',
  `high_alert_cnfms` varchar(255) DEFAULT NULL,
  `low_alert_nalarms` bigint(20) DEFAULT '0',
  `low_alert_cnfms` varchar(255) DEFAULT NULL,
  `external_sensor_nalarms` bigint(20) DEFAULT '0',
  `device_connection_alarms` bigint(20) DEFAULT '0',
  `battery_nalarms` bigint(20) DEFAULT '0',
  `power_available_time` bigint(20) DEFAULT '0',
  `high_alert_cnf` tinyint(1) DEFAULT '0',
  `low_alert_cnf` tinyint(1) DEFAULT '0',
  `available_disk_space` double DEFAULT '0',
  `number_of_temperature_cached` int(11) DEFAULT '0',
  `number_of_dvc_cached` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `daily_stats_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `daily_stats_device_errors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `error_code` varchar(50) NOT NULL,
  `count` int(11) NOT NULL,
  `time` int(11) NOT NULL,
  `daily_stats_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `daily_stats_device_errors_daily_stats_id_idx` (`daily_stats_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_admin_settings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `phone_number` text NOT NULL,
  `password` text,
  `device_id` bigint(20) DEFAULT NULL,
  `sender_id` text,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `device_admin_settings_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_admin_settings_push_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `status` text NOT NULL,
  `sent_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `acknowledged_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `error_code` text,
  `error_message` text,
  `device_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `device_admin_settings_push_status_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_alarms` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` int(11) NOT NULL,
  `status` int(11) NOT NULL,
  `time` int(11) NOT NULL,
  `error_code` varchar(5) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `error_message` varchar(255) DEFAULT NULL,
  `power_availability` int(11) DEFAULT '0',
  `sensor_id` varchar(5) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `device_alarm_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB AUTO_INCREMENT=68584 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_apn_settings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` text NOT NULL,
  `address` text,
  `username` text,
  `password` text,
  `device_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `alt_name` varchar(50) DEFAULT NULL,
  `alt_address` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `device_apn_settings_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_config_push_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `status` text NOT NULL,
  `sent_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `acknowledged_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `error_code` text,
  `error_message` text,
  `device_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `device_config_push_status_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_config_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `config_updated_time` int(11) DEFAULT NULL,
  `config_updated_type` int(11) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `device_status_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_configurations` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `configuration` text NOT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tag_id` bigint(20) DEFAULT NULL,
  `firmware_version` text,
  PRIMARY KEY (`id`),
  KEY `device_configurations_device_id_idx` (`device_id`) USING HASH,
  KEY `device_configurations_tag_id_idx` (`tag_id`) USING HASH
) ENGINE=InnoDB AUTO_INCREMENT=814 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_meta_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ky` varchar(50) NOT NULL,
  `value` varchar(100) NOT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `device_meta_data_id_idx` (`device_id`) USING HASH,
  KEY `device_meta_data_id_key_idx` (`device_id`,`ky`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=64794 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_power_transition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `state` int(11) NOT NULL,
  `transition_time` int(11) NOT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `device_power_transition_device_id_idx` (`device_id`) USING HASH,
  KEY `temperature_sensors_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB AUTO_INCREMENT=8126 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_ready` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `transmitter_id` varchar(255) DEFAULT NULL,
  `device_model` varchar(255) DEFAULT NULL,
  `device_sensor_firmware_version` varchar(255) DEFAULT NULL,
  `device_gsm_firmware_version` varchar(255) DEFAULT NULL,
  `device_imei` varchar(255) DEFAULT NULL,
  `sim_phone` varchar(255) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `sim_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `device_ready_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_status` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `status_key` varchar(20) NOT NULL,
  `status` int(11) NOT NULL,
  `status_ut` int(11) NOT NULL,
  `location_id` int(11) DEFAULT NULL,
  `sensor_id` varchar(5) DEFAULT NULL,
  `temperature` double DEFAULT NULL,
  `temperature_ut` int(11) DEFAULT NULL,
  `temperature_abnormal_status` int(11) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `device_status_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB AUTO_INCREMENT=70830 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_status_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `device_id` bigint(20) DEFAULT NULL,
  `status_key` varchar(50) NOT NULL,
  `status` int(11) DEFAULT NULL,
  `status_ut` int(11) DEFAULT NULL,
  `start_time` int(11) DEFAULT NULL,
  `end_time` int(11) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  `previous_status` int(11) DEFAULT NULL,
  `next_status` int(11) DEFAULT NULL,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8341 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_temperature_request` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `number_of_request` int(11) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` text,
  PRIMARY KEY (`id`),
  KEY `device_temperature_request_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `devices` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `deviceid` varchar(255) NOT NULL,
  `uniquehash` varchar(255) NOT NULL,
  `vendorid` varchar(255) NOT NULL,
  `transmitterid` varchar(255) DEFAULT NULL,
  `devicemodel` varchar(255) DEFAULT NULL,
  `deviceversion` varchar(255) DEFAULT NULL,
  `deviceimei` varchar(255) DEFAULT NULL,
  `simnumber` varchar(255) DEFAULT NULL,
  `simid` varchar(255) DEFAULT NULL,
  `countrycode` varchar(5) DEFAULT NULL,
  `timezone` double DEFAULT NULL,
  `gsmmoduleversion` varchar(255) DEFAULT NULL,
  `statusupdatedtime` bigint(20) DEFAULT NULL,
  `temperatureState` int(11) DEFAULT '0',
  `temperatureStateUT` int(11) DEFAULT '0',
  `temperature` double DEFAULT '0',
  `temperatureUT` int(11) DEFAULT '0',
  `sInt` int(11) DEFAULT '0',
  `pInt` int(11) DEFAULT '0',
  `activityState` int(11) DEFAULT '0',
  `activityStateUT` int(11) DEFAULT '0',
  `iActPIntCounts` int(11) DEFAULT '0',
  `createdOn` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `altPhoneNumber` varchar(30) DEFAULT NULL,
  `altSimId` varchar(100) DEFAULT NULL,
  `createdBy` varchar(50) NOT NULL,
  `updatedBy` varchar(50) NOT NULL,
  `updatedOn` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `imgUrls` text,
  `vendorName` varchar(50) DEFAULT NULL,
  `assetType_id` int(11) DEFAULT NULL,
  `locationId` varchar(50) DEFAULT NULL,
  `workingState` int(11) DEFAULT '0',
  `workingStateUT` int(11) DEFAULT '0',
  `deviceAlarmState` int(11) DEFAULT '0',
  `deviceAlarmStateUT` int(11) DEFAULT '0',
  `firstTempTime` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `devices_uniquehash_key` (`uniquehash`),
  KEY `devices_deviceid_vendorid_idx` (`deviceid`,`vendorid`) USING BTREE,
  KEY `devices_asset_type_key` (`assetType_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5992 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `devices_tags` (
  `devices_id` bigint(20) NOT NULL,
  `tags_id` bigint(20) NOT NULL,
  PRIMARY KEY (`devices_id`,`tags_id`),
  KEY `fkfe22c23b7a5e790f` (`tags_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `devices_tmp` (
  `id` bigint(20) NOT NULL DEFAULT '0',
  `deviceid` varchar(255) NOT NULL,
  `uniquehash` varchar(255) NOT NULL,
  `vendorid` varchar(255) NOT NULL,
  `transmitterid` varchar(255) DEFAULT NULL,
  `devicemodel` varchar(255) DEFAULT NULL,
  `deviceversion` varchar(255) DEFAULT NULL,
  `deviceimei` varchar(255) DEFAULT NULL,
  `simnumber` varchar(255) DEFAULT NULL,
  `simid` varchar(255) DEFAULT NULL,
  `countrycode` varchar(5) DEFAULT NULL,
  `timezone` double DEFAULT NULL,
  `gsmmoduleversion` varchar(255) DEFAULT NULL,
  `statusupdatedtime` bigint(20) DEFAULT NULL,
  `temperatureState` int(11) DEFAULT '0',
  `temperatureStateUT` int(11) DEFAULT '0',
  `temperature` double DEFAULT '0',
  `temperatureUT` int(11) DEFAULT '0',
  `sInt` int(11) DEFAULT '0',
  `pInt` int(11) DEFAULT '0',
  `activityState` int(11) DEFAULT '0',
  `activityStateUT` int(11) DEFAULT '0',
  `iActPIntCounts` int(11) DEFAULT '0',
  `createdOn` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `altPhoneNumber` varchar(30) DEFAULT NULL,
  `altSimId` varchar(100) DEFAULT NULL,
  `createdBy` varchar(50) NOT NULL,
  `updatedBy` varchar(50) NOT NULL,
  `updatedOn` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `imgUrls` text,
  `vendorName` varchar(50) DEFAULT NULL,
  `assetType_id` int(11) DEFAULT NULL,
  `locationId` varchar(50) DEFAULT NULL,
  `workingState` int(11) DEFAULT '0',
  `workingStateUT` int(11) DEFAULT '0',
  `deviceAlarmState` int(11) DEFAULT '0',
  `deviceAlarmStateUT` int(11) DEFAULT '0',
  `firstTempTime` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `new_alarm_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `alarm_type` int(11) NOT NULL,
  `temperature` double DEFAULT NULL,
  `temperature_type` int(11) DEFAULT NULL,
  `device_alarm_type` int(11) DEFAULT NULL,
  `device_alarm_status` int(11) DEFAULT NULL,
  `device_battery_volt` double DEFAULT NULL,
  `device_firmware_error_code` varchar(50) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `temperature_abnormal_type` int(11) DEFAULT NULL,
  `monitoring_position_id` int(11) DEFAULT NULL,
  `sensor_id` varchar(10) DEFAULT NULL,
  `start_time` int(11) DEFAULT NULL,
  `end_time` int(11) DEFAULT NULL,
  `updated_on` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `alarm_log_device_id_idx` (`device_id`) USING HASH,
  KEY `alarm_log_device_id_alarm_time_idx` (`device_id`) USING BTREE,
  KEY `alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_1` (`device_id`,`alarm_type`,`device_alarm_status`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_2` (`id`,`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tags` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tagname` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tags_tagname_idx` (`tagname`) USING HASH
) ENGINE=InnoDB AUTO_INCREMENT=493 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_options` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2043 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `temperature_readings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `temperature` double NOT NULL,
  `timeofreading` int(11) NOT NULL,
  `type` int(11) NOT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `location_id` int(11) DEFAULT NULL,
  `monitoredAsset_id` bigint(20) DEFAULT NULL,
  `power_availability` int(11) DEFAULT NULL,
  `source` int(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_temp_reading` (`device_id`,`timeofreading`,`type`,`temperature`),
  KEY `temperature_readings_device_id_idx` (`device_id`) USING HASH,
  KEY `temperature_readings_device_id_timeofreading_type_temp_idx` (`device_id`,`timeofreading`,`type`,`temperature`) USING BTREE,
  KEY `temperature_readings_device_id_timeofreading_type_idx` (`device_id`,`timeofreading`,`type`) USING BTREE,
  KEY `tr_timeofreading` (`timeofreading`) USING HASH,
  KEY `timeofreading_maid` (`monitoredAsset_id`,`timeofreading`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=69920 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `temperature_sensors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sensor_id` varchar(10) NOT NULL,
  `code` varchar(20) NOT NULL,
  `status` int(11) DEFAULT '0',
  `device_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `temperature_sensors_device_id_fkey` (`device_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4130 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `temperature_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dayofcomputation` bigint(20) NOT NULL,
  `durationhigh` int(11) NOT NULL,
  `durationlow` int(11) NOT NULL,
  `firsthighalerttime` bigint(20) NOT NULL,
  `firstlowalerttime` bigint(20) NOT NULL,
  `firstsensorconnectionfailuretime` bigint(20) NOT NULL,
  `highalertambienttemperature` double DEFAULT NULL,
  `highesttemperature` double DEFAULT NULL,
  `lowalertambienttemperaturelow` double DEFAULT NULL,
  `lowesttemperature` double DEFAULT NULL,
  `meantemperature` double DEFAULT NULL,
  `numberinternetpushfailures` int(11) NOT NULL,
  `numberofalerts` int(11) NOT NULL,
  `numberofhighalerts` int(11) NOT NULL,
  `numberofinternetpushes` int(11) NOT NULL,
  `numberoflowalerts` int(11) NOT NULL,
  `numberofsmssent` int(11) NOT NULL,
  `sensorconnectionfailureduration` int(11) NOT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `temperature_statistics_device_id_idx` (`device_id`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tmp_alarm_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `alarm_type` int(11) NOT NULL,
  `temperature` double DEFAULT NULL,
  `temperature_type` int(11) DEFAULT NULL,
  `device_alarm_type` int(11) DEFAULT NULL,
  `device_alarm_status` int(11) DEFAULT NULL,
  `device_battery_volt` double DEFAULT NULL,
  `device_firmware_error_code` varchar(50) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `temperature_abnormal_type` int(11) DEFAULT NULL,
  `monitoring_position_id` int(11) DEFAULT NULL,
  `sensor_id` varchar(10) DEFAULT NULL,
  `start_time` int(11) DEFAULT NULL,
  `end_time` int(11) DEFAULT NULL,
  `updated_on` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `alarm_log_device_id_idx` (`device_id`) USING HASH,
  KEY `alarm_log_device_id_alarm_time_idx` (`device_id`) USING BTREE,
  KEY `alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_1` (`device_id`,`alarm_type`,`device_alarm_status`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_2` (`id`,`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tmp_imm_alarm_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `alarm_type` int(11) NOT NULL,
  `temperature` double DEFAULT NULL,
  `temperature_type` int(11) DEFAULT NULL,
  `device_alarm_type` int(11) DEFAULT NULL,
  `device_alarm_status` int(11) DEFAULT NULL,
  `device_battery_volt` double DEFAULT NULL,
  `device_firmware_error_code` varchar(50) DEFAULT NULL,
  `device_id` bigint(20) DEFAULT NULL,
  `temperature_abnormal_type` int(11) DEFAULT NULL,
  `monitoring_position_id` int(11) DEFAULT NULL,
  `sensor_id` varchar(10) DEFAULT NULL,
  `start_time` int(11) DEFAULT NULL,
  `end_time` int(11) DEFAULT NULL,
  `updated_on` datetime DEFAULT CURRENT_TIMESTAMP,
  `end_time1` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `alarm_log_device_id_idx` (`device_id`) USING HASH,
  KEY `alarm_log_device_id_alarm_time_idx` (`device_id`) USING BTREE,
  KEY `alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx` (`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_1` (`device_id`,`alarm_type`,`device_alarm_status`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE,
  KEY `bk_alarm_log_device_id_alarm_time_full_idx_2` (`id`,`device_id`,`alarm_type`,`device_alarm_type`,`monitoring_position_id`,`sensor_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tmp_index` (
  `id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_accounts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `organizationname` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `type` int(11) DEFAULT '2',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_accounts_username_key` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
