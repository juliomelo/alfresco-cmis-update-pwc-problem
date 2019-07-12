FROM maven:3.6-jdk-8

COPY pom.xml /usr/src/update-properties-test/

# Download all packages
RUN mvn -B -f /usr/src/update-properties-test/pom.xml package && mvn -B -f /usr/src/update-properties-test/pom.xml clean

# Define entrypoint and workdir
WORKDIR /usr/src/update-properties-test/target
ENTRYPOINT java -Dhttp.nonProxyHosts=alfresco -cp lib/*:update-properties-test-1.0-SNAPSHOT.jar tests.UpdatePropertiesTest ${USER} ${PASSWORD}

# Compile
COPY src /usr/src/update-properties-test/src
RUN mvn -B -f /usr/src/update-properties-test/pom.xml package


