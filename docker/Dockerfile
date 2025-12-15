FROM maven:3.9-eclipse-temurin-17-alpine

# set keycloak-db-userfederation directories
RUN mkdir -p /opt/keycloak-db-userfederation
WORKDIR /opt/keycloak-db-userfederation

# set image cmd
ADD cmd.sh /opt/cmd.sh
RUN chmod 744 /opt/cmd.sh

CMD [ "/opt/cmd.sh" ]