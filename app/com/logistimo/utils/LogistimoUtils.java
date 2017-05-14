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

package com.logistimo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.logistimo.db.DeviceMetaData;
import com.logistimo.exception.LogistimoException;
import org.apache.commons.codec.binary.Base64;
import play.Logger;
import play.Play;
import org.json.JSONException;
import org.json.JSONObject;
import play.libs.Json;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class LogistimoUtils {
    private final static Logger.ALogger LOGGER = Logger.of(LogistimoUtils.class);
    private static ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static Validator validator = factory.getValidator();
    private static Gson gson = new GsonBuilder().create();


    public static int transformPageNumberToPosition(int pageNumber, int pageSize) {
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        return ((pageNumber - 1) * pageSize); //if page is 2 and size is 50 return 50
    }

    public static long availableNumberOfPages(int pageSize, long count) {
        if (count % pageSize == 0) {
            return count / pageSize;
        } else {
            return count / pageSize + 1;
        }
    }

    public static int transformPageSize(int pageSize) {
        if (pageSize != -1)
            return pageSize;
        return Play.application().configuration().getInt("page.size");
    }

    /**
     * Authorization header will always be encoded in UTF-8 format, need to decoded before validating against credentials
     *
     * @param authorization
     * @return
     */
    public static String[] decodeHeader(String authorization) {
        try {
            byte[] decoded = Base64.decodeBase64(authorization.substring(6).getBytes("UTF-8"));
            String credentials = new String(decoded);
            return credentials.split(":");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Exception while decoding authorization header", e);
            throw new UnsupportedOperationException(e);
        }
    }

    //Validates the given object - Bean validation.
    public static <T> void validateObject(T obj) throws LogistimoException {
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(obj);

        if (constraintViolations.size() > 0) {
            List<String> message = new ArrayList<String>(1);

            Iterator<ConstraintViolation<T>> iterator = constraintViolations.iterator();
            while (iterator.hasNext()) {
                ConstraintViolation<T> constraintViolation = iterator.next();
                message.add(constraintViolation.getPropertyPath() + LogistimoConstant.SPACE + constraintViolation.getMessage());
            }

            throw new LogistimoException(message.toString());
        }

    }

    public static String constructErrorMessage(String message, String vendorId, String deviceId){
        if(message == null){
            message = "Unknown error.";
        }

        return message.concat(" For vendor " + vendorId + " and device " + deviceId);
    }

    public static <T> T getValidatedObject(String jsonNode, Class<T> klass) throws LogistimoException {
        if(jsonNode == null){
            throw new LogistimoException("JSON data is required.");
        }

        T obj = gson.fromJson(jsonNode, klass);
        LogistimoUtils.validateObject(obj);
        return obj;
    }

    public static String toJson(Object o){
        if(o != null){
            return gson.toJson(o);
        }
        return null;
    }

    public static <T> T toObject(String jsonNode, Class<T> klass){
        if(jsonNode == null){
            return null;
        }

        return gson.fromJson(jsonNode, klass);
    }

    public static String generateVirtualDeviceId(String deviceId, String sensorId){
        return deviceId + "-" + sensorId;
    }

    public static String extractDeviceId(String deviceId){
        return deviceId.substring(0, deviceId.lastIndexOf("-"));
    }

    public static String extractSensorId(String deviceId){
        return deviceId.substring(deviceId.lastIndexOf("-") + 1);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> constructDeviceMetaDataFromJSON(String parentKey, Map<String, Object> result){
        Map<String, String> metaDataMap = new HashMap<>(1);
        String currentKey;
        for(String key : result.keySet()){
            Object object = result.get(key);
            currentKey = key;
            if(parentKey != null){
                currentKey = parentKey + "." + key;
            }
            if(object != null){
                if(object instanceof LinkedHashMap){
                    metaDataMap.putAll(constructDeviceMetaDataFromJSON(currentKey, (LinkedHashMap) object));
                }else{
                    metaDataMap.put(currentKey, object.toString());
                }
            }
        }
        return metaDataMap;
    }

    public static JSONObject constructDeviceMetaJsonFromMap(Map<String, String> metaDataMap) {
        JSONObject jsonObject = new JSONObject();
        try{
            for(String key : metaDataMap.keySet()){
                String keys[] = key.split("\\.");
                JSONObject jsonObjectTmp = jsonObject;
                for(int i = 0 ; i<keys.length; i++ ){
                    if(!jsonObjectTmp.has(keys[i])){
                        jsonObjectTmp.put(keys[i], new JSONObject());
                    }
                    if(i == keys.length-1){
                        jsonObjectTmp.put(keys[i], metaDataMap.get(key));
                    }else{
                        jsonObjectTmp = jsonObjectTmp.getJSONObject(keys[i]);
                    }
                }
            }
        }catch (Exception e){
            LOGGER.warn("{} while constructing device meta data json from map {}", e.getMessage(), metaDataMap.toString(), e);
        }
        return jsonObject;
    }

    public static Boolean isInteger(String value){
        try {
            Integer.parseInt(value);
        }catch (Exception e){
            return false;
        }
        return true;
    }

    public static Integer getMetaDataValue(DeviceMetaData deviceMetaData){
        try{
            return Integer.parseInt(deviceMetaData.value);
        }catch (Exception e){
            return 0;
        }
    }

    public static long convertDateToEpoch(Date date){
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        return calendar.getTimeInMillis();
    }
}
