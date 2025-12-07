FROM eclipse-temurin:21-jre

WORKDIR /app

ENV TZ=Asia/Shanghai
ENV JVM_OPTIONS=" \
-Xms1024m \
-Xmx1024m \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:+AlwaysPreTouch \
-XX:+ExitOnOutOfMemoryError \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=/app/dumps/heapdump.hprof \
-XX:NativeMemoryTracking=summary \
-Xlog:gc*,gc+heap=debug:file=/app/logs/jvm_gc.log:time,uptime,level,tags:filecount=5,filesize=10M \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
"
ENV ARGS=""

RUN mkdir /app/logs && \
    apt update && \
    apt install -y vim-tiny iputils-ping curl tini && \
    apt clean

COPY ./power-scheduler-server-bootstrap/build/libs/app.jar app.jar

EXPOSE 8080

LABEL maintainer="grayrat1999@163.com" \
    org.opencontainers.image.source="https://github.com/grayrat1999/power-scheduler" \
    org.opencontainers.image.description="PowerScheduler server application" \
    org.opencontainers.image.licenses="MIT"

ENTRYPOINT ["/usr/bin/tini", "--", "sh", "-c", "exec java $JVM_OPTIONS -jar /app/app.jar $ARGS"]
