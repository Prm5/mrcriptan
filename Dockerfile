FROM openjdk:21

WORKDIR /app

COPY /target/mrcriptan-1.0-jar-with-dependencies.jar /app/mrcriptan.jar

ENTRYPOINT [ "java", "-jar", "mrcriptan.jar" ]