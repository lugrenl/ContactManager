plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web
    implementation("org.springframework.boot:spring-boot-starter-web:3.3.5")
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-jdbc
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc:3.4.1")
    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.7.5")
    // https://mvnrepository.com/artifact/org.projectlombok/lombok
    compileOnly("org.projectlombok:lombok:1.18.36")
    // https://mvnrepository.com/artifact/org.hibernate.orm/hibernate-core
    implementation("org.hibernate.orm:hibernate-core:6.6.6.Final")
    // https://mvnrepository.com/artifact/org.springframework/spring-orm
    implementation("org.springframework:spring-orm:6.2.2")
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-jpa
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.2")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    testImplementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // https://mvnrepository.com/artifact/org.assertj/assertj-core
    testImplementation("org.assertj:assertj-core:3.27.3")
    // https://mvnrepository.com/artifact/org.springframework/spring-test
    testImplementation("org.springframework:spring-test:6.2.2")

    //Other Dependencies >>
    //LOMBOK Dependencies
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    compileOnly("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
    testCompileOnly("org.projectlombok:lombok:1.18.36")
}

tasks.test {
    useJUnitPlatform()
}