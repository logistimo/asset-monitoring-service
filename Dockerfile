#Copyright © 2017 Logistimo.

#This file is part of Logistimo.

#Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
#low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).

#This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
#Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
#later version.

#This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
#warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
#for more details.

#You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
#<http://www.gnu.org/licenses/>.

#You can be released from the requirements of the license by purchasing a commercial license. To know more about
#the commercial license, please contact us at opensource@logistimo.com

FROM tomcat:7-jre8
MAINTAINER dockers@logistimo.com

ARG warname

ENV TOMCAT_HOME /usr/local/tomcat

RUN rm -rf $TOMCAT_HOME/webapps/* \
	&& apt-get update \
	&& apt-get install -y gettext-base

ADD target/$warname $TOMCAT_HOME/webapps/

ADD dockerfiles/context.xml.template $TOMCAT_HOME/conf/

ADD dockerfiles/server.xml.template $TOMCAT_HOME/conf/

RUN unzip -o $TOMCAT_HOME/webapps/$warname \
        -d $TOMCAT_HOME/webapps/ROOT/ \
        && rm -rf $TOMCAT_HOME/webapps/$warname

ENV MYSQL_HOST_URL="jdbc:mysql://localhost:3306/logistimo_tms" \
	MYSQL_USER=logistimo \
	MYSQL_PASS=logistimo \
	PUSH_TEMP_ALERTS="http://localhost:8090/tempmonitoring?a=logtemp" \
	PUSH_ALARM_ALERTS="http://localhost:8090/tempmonitoring?a=logdevicealarms" \
	PUSH_EVENT="http://localhost:8090/s2/api/assetstatus/" \
	APN_PUSH_NXL="http://localhost:8090/smsservice/devices/apn?country=%country%&phone=%phone%" \
	CONFIG_PUSH_NXL="http://localhost:8090/smsservice/devices/config" \
	LS_CONFIG_PULL="http://localhost:8090/v2/config" \
	BER_SENT_STATUS_REQUEST="http://localhost:8090/temp/%vid%/%did%?country=%country%&phone=%phone%" \
	BER_ADMIN_PUSH_URL="http://localhost:8090/devices/admin?country=%country%&phone=%phone%" \
	BER_APN_PUSH_URL="http://localhost:8090/devices/apn?country=%country%&phone=%phone%" \
	BER_CONFIG_PUSH_URL="http://localhost:8090/devices/config" \
	ACTIVEMQ_HOST="tcp://localhost:61616" \
	ZKR_HOST="localhost:2181" \
	REDIS_HOST=localhost \
	REDIS_PORT=6379 \
	AMS_PORT=8080 \
	REDIS_SENTINELS=[] \
	SENTINEL_HOST= \
	SENTINEL_MASTER= \
	APP_SECRET=logisitmo \
	ADMIN_PASS=logistimo \
	JAVA_XMS=1024m \
	JAVA_XMX=1024m \
	SERVICE_NAME=logi-ams \
    APM_SERVER_URL=localhost:8200 \
	JMX_AGENT_PORT=8088

ENV JAVA_OPTS $JAVA_OPTS

RUN cd $TOMCAT_HOME && wget http://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.7/jmx_prometheus_javaagent-0.7.jar \
	&& wget http://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/0.6.0/elastic-apm-agent-0.6.0.jar

ADD dockerfiles/jmx_exporter.json $TOMCAT_HOME/jmx_exporter.json

COPY dockerfiles/docker-entrypoint.sh /docker-entrypoint.sh

RUN chmod +x /docker-entrypoint.sh

EXPOSE 8080-8090

WORKDIR "/usr/local/tomcat"

CMD ["/docker-entrypoint.sh"]
