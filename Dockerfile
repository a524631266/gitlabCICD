From java:8

ENV TZ=Asia/Shanghai

RUN ln -snf /usr/share/zoneinfo/$TZ  /etc/localtime && echo $TZ > /etc/timezone

COPY target/gitlab-1.0-SNAPSHOT.jar /
CMD [ "java","-jar","/gitlab-1.0-SNAPSHOT.jar"]