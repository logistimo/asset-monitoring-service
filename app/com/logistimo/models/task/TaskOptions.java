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

package com.logistimo.models.task;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kaniyarasu on 11/08/15.
 */
public class TaskOptions<T> implements Serializable{

    //Task type, whether Background or data logger task
    private int type = TaskType.BACKGROUND_TASK.getValue();

    //Target service class, this should extend Executable
    private Class<T> clazz;

    //Data to be processed in target service, Object should implements Serializable
    private String content;

    //Specific for activemq
    private Map<String, Object> headers = new HashMap<>(1);

    //Specific for target task
    private Map<String, Object> options = new HashMap<>(1);

    //Execution delay in milli seconds
    private Long delayInMillis = -1L;

    private int numberOfRetry = 0;

    private Long initiatedTime;

    public TaskOptions(int type, Class<T> clazz, String content, Map<String, Object> options) {
        this.type = type;
        this.clazz = clazz;
        this.content = content;
        this.options = options;
        this.initiatedTime = System.currentTimeMillis();
    }

    public TaskOptions(int type, Class<T> clazz, String content, Map<String, Object> options, long delayInMillis) {
        this.type = type;
        this.clazz = clazz;
        this.content = content;
        this.options = options;
        this.delayInMillis = delayInMillis;
        this.initiatedTime = System.currentTimeMillis();
    }

    public TaskOptions(int type, Class<T> clazz, String content, long delayInMillis) {
        this.type = type;
        this.clazz = clazz;
        this.content = content;
        this.delayInMillis = delayInMillis;
        this.initiatedTime = System.currentTimeMillis();
    }

    public TaskOptions(){
        this.initiatedTime = System.currentTimeMillis();
    }

    public Long getInitiatedTime() {
        return initiatedTime;
    }

    public void setInitiatedTime(Long initiatedTime) {
        this.initiatedTime = initiatedTime;
    }

    public int getNumberOfRetry() {
        return numberOfRetry;
    }

    public void setNumberOfRetry(int numberOfRetry) {
        this.numberOfRetry = numberOfRetry;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Long getDelayInMillis() {
        return delayInMillis;
    }

    public void setDelayInMillis(Long etaMillis) {
        this.delayInMillis = etaMillis;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void incrNumberOfRetry(){
        this.numberOfRetry ++;
    }

    @Override
    public String toString() {
        return "TaskOptions{" +
                "type=" + type +
                ", clazz=" + clazz +
                ", content='" + content + '\'' +
                ", headers=" + headers +
                ", options=" + options +
                ", delayInMillis=" + delayInMillis +
                ", numberOfRetry=" + numberOfRetry +
                '}';
    }
}
