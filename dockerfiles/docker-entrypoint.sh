#!/bin/bash

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

sed -ri "/Jad\/Jar download servlet/,+4d" $TOMCAT_HOME/webapps/ROOT/WEB-INF/web.xml \
        && sed -ri "$ a tomcat.util.http.parser.HttpParser.requestTargetAllow=|{}" $TOMCAT_HOME/conf/catalina.properties

envsubst < $TOMCAT_HOME/conf/server.xml.template > $TOMCAT_HOME/conf/server.xml

envsubst < $TOMCAT_HOME/webapps/ROOT/WEB-INF/classes/application.conf.template  > $TOMCAT_HOME/webapps/ROOT/WEB-INF/classes/application.conf

JAVA_OPTS="-Xms$JAVA_XMS -Xmx$JAVA_XMX \
	 -\"javaagent://$TOMCAT_HOME/jmx_prometheus_javaagent-0.7.jar=$JMX_AGENT_PORT:$TOMCAT_HOME/jmx_exporter.json\""

exec $TOMCAT_HOME/bin/catalina.sh run
