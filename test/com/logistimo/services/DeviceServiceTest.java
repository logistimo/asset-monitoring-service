/*
 * Copyright © 2017 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

package com.logistimo.services;

import com.logistimo.db.AssetMapping;
import com.logistimo.db.AssetType;
import com.logistimo.db.Device;
import com.logistimo.db.DeviceStatus;
import com.logistimo.db.DeviceStatusLog;
import com.logistimo.models.asset.AssetMapModel;
import com.logistimo.models.device.response.DeviceStatusModel;
import com.logistimo.utils.AssetStatusConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.running;
import static play.test.Helpers.stop;


/**
 * Created by smriti on 07/02/18.
 */
@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Device.class, DeviceStatusLog.class, DeviceStatus.class, AssetMapping.class})
public class DeviceServiceTest {
  Device device;
  DeviceStatus deviceStatus;
  DeviceStatusLog statusLog;
  DeviceStatusModel deviceStatusModel;

  Integer eventStartTime = 1504325145;
  Long deviceId = 10l;
  String updatedByUser = "testuser";
  final String ACTIVEMQ_URL = "activemq.url";

  @Before
  public void setup() throws Exception {
    device = spy(Device.class);
    deviceStatus = spy(DeviceStatus.class);
    statusLog = spy(DeviceStatusLog.class);

    doNothing().when(device).update();
    doNothing().when(deviceStatus).update();
    doNothing().when(statusLog).update();

    mockStatic(DeviceStatusLog.class);
    when(DeviceStatusLog.getDeviceStatusForGivenStartTime(deviceId,
        AssetStatusConstants.WORKING_STATUS_KEY, eventStartTime)).thenReturn(statusLog);

    mockStatic(DeviceStatus.class);
    when(DeviceStatus.getDeviceStatus(device, AssetStatusConstants.WORKING_STATUS_KEY)).thenReturn(deviceStatus);
  }

  @Test
  public void testUpdateDeviceWorkingStatus() throws Exception {
    Map<String,String> props = new HashMap<>();
    props.put(ACTIVEMQ_URL,"");
    props.putAll(inMemoryDatabase());
    running(fakeApplication(props), () -> {
      DeviceService deviceService = spy(DeviceService.class);

      getDeviceStatus(1l);
      getDeviceModel(deviceId, "ILR001", "haier");
      getDeviceStatusModel(1, updatedByUser);

      when(deviceService.getCurrentTimeInSeconds()).thenReturn(eventStartTime);

      mockStatic(Device.class);
      when(Device.findDevice(device.vendorId, device.deviceId)).thenReturn(device);

      doNothing().when(deviceService).updateDeviceStatusLog(device, deviceStatusModel, deviceStatus, deviceStatus.status);
      doNothing().when(deviceService).postDeviceStatus(device, deviceStatus);

      String result = deviceService.updateDeviceWorkingStatus(device.vendorId, device.deviceId, deviceStatusModel);
      assertEquals("Device working status updated successfully", result);
      assertEquals(deviceStatusModel.st , deviceStatus.status);
      assertEquals(eventStartTime , deviceStatus.statusUpdatedTime);
      assertEquals(updatedByUser , deviceStatus.statusUpdatedBy);
      assertEquals(eventStartTime, statusLog.endTime);
      assertEquals(deviceStatusModel.st, statusLog.nextStatus);
    });
    stop(fakeApplication());
  }

  @Test
  public void testUpdateWorkingStatusWithoutVendorId() {
    Map<String,String> props = new HashMap<>();
    props.put(ACTIVEMQ_URL,"");
    props.putAll(inMemoryDatabase());

    running(fakeApplication(props), () -> {

      DeviceService deviceService = spy(DeviceService.class);
      getDeviceStatusModel(3, updatedByUser);
      String result = deviceService.updateDeviceWorkingStatus(null, "ILR001", deviceStatusModel);

      assertEquals("One of the vendorId and deviceId is mandatory", result);
    });
    stop(fakeApplication());

  }

  @Test
  public void testUpdateWorkingStatusWithoutDeviceId() {
    Map<String, String> props = new HashMap<>();
    props.put(ACTIVEMQ_URL, "");
    props.putAll(inMemoryDatabase());

    running(fakeApplication(props), () -> {

      DeviceService deviceService = spy(DeviceService.class);
      getDeviceStatusModel(3, updatedByUser);
      String result = deviceService.updateDeviceWorkingStatus("haier", null, deviceStatusModel);

      assertEquals("One of the vendorId and deviceId is mandatory", result);
    });
    stop(fakeApplication(props));

  }

  @Test
  public void testToAssetMapModelForMonitoringAsset() {
    Map<String, String> props = new HashMap<>();
    props.put(ACTIVEMQ_URL, "");
    props.putAll(inMemoryDatabase());

    running(fakeApplication(props), () -> {

      List<AssetMapping> assetMappingList = new ArrayList<>();
      AssetMapping
          assetMapping =
          constructAssetMapping("ILR001", "haier", AssetType.ILR, "TL1", "berlinger",
              AssetType.TEMPERATURE_LOGGER);
      assetMappingList.add(assetMapping);

      DeviceService deviceService = spy(DeviceService.class);
      doReturn(null).when(deviceService).getDeviceMetaDatas(assetMapping.asset);

      mockStatic(AssetMapping.class);
      when(AssetMapping.findAssetRelationByAsset(any())).thenReturn(assetMappingList);

      Map<Integer, AssetMapModel>
          assetModel =
          deviceService.toAssetMapModels(assetMappingList, true);

      assertNotNull(assetModel);
      assertEquals(assetMapping.asset.deviceId, assetModel.get(1).getdId());
      assertEquals(assetMapping.asset.vendorId, assetModel.get(1).getvId());
      assertEquals(assetMapping.asset.assetType.assetType, AssetType.MONITORED_ASSET);

    });
    stop(fakeApplication(props));

  }

  @Test
  public void testToAssetMapModelForMonitoredAsset() {
    Map<String, String> props = new HashMap<>();
    props.put(ACTIVEMQ_URL, "");
    props.putAll(inMemoryDatabase());

    running(fakeApplication(props), () -> {

      List<AssetMapping> assetMappingList = new ArrayList<>();
      AssetMapping
          assetMapping =
          constructAssetMapping("WIC001", "bpsolar", AssetType.WALK_IN_COOLER, "TL001", "nexleaf",
              AssetType.TEMPERATURE_LOGGER);
      assetMappingList.add(assetMapping);

      DeviceService deviceService = spy(DeviceService.class);
      doReturn(null).when(deviceService).getDeviceMetaDatas(assetMapping.relatedAsset);

      mockStatic(AssetMapping.class);
      when(AssetMapping.findAssetRelationByAsset(any())).thenReturn(assetMappingList);

      Map<Integer, AssetMapModel>
          assetModel =
          deviceService.toAssetMapModels(assetMappingList, false);

      assertNotNull(assetModel);
      assertEquals(assetMapping.relatedAsset.deviceId, assetModel.get(1).getdId());
      assertEquals(assetMapping.relatedAsset.vendorId, assetModel.get(1).getvId());
      assertEquals(assetMapping.relatedAsset.assetType.assetType, AssetType.MONITORING_ASSET);

    });
    stop(fakeApplication(props));

  }

  private void getDeviceStatus(Long id) {
    deviceStatus.id = id;
    deviceStatus.statusUpdatedTime = eventStartTime;
    deviceStatus.statusKey = AssetStatusConstants.WORKING_STATUS_KEY;
  }

  private void getDeviceModel(Long id, String deviceId, String vendorId) {
    device.id = id;
    device.deviceId = deviceId;
    device.vendorId = vendorId;
    device.updatedOn = new Date();
  }

  private void getDeviceStatusModel(Integer status, String statusUpdatedBy) {
    deviceStatusModel = new DeviceStatusModel();
    deviceStatusModel.st = status;
    deviceStatusModel.stub = statusUpdatedBy;
  }

  private AssetMapping constructAssetMapping(String assetDeviceId, String assetVendorId,
                                             Integer assetId,
                                             String relatedAssetDeviceId,
                                             String relatedAssetVendorId, Integer relatedAssetId) {

    Device asset = new Device();
    asset.deviceId = assetDeviceId;
    asset.vendorId = assetVendorId;
    asset.assetType = new AssetType();
    asset.assetType.id = assetId;
    asset.assetType.assetType = AssetType.MONITORED_ASSET;

    Device relatedAsset = new Device();
    relatedAsset.deviceId = relatedAssetDeviceId;
    relatedAsset.vendorId = relatedAssetVendorId;
    relatedAsset.assetType = new AssetType();
    relatedAsset.assetType.id = relatedAssetId;
    relatedAsset.assetType.assetType = AssetType.MONITORING_ASSET;

    AssetMapping assetMapping = new AssetMapping();
    assetMapping.asset = asset;
    assetMapping.relatedAsset = relatedAsset;

    return assetMapping;
  }
}