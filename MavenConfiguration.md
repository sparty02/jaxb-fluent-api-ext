
```
<project 
  xmlns="http://maven.apache.org/POM/4.0.0" 
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd"> 
  <!-- [...] --> 
  <dependencies> 
    <dependency> 
      <groupId>javax.xml.bind</groupId> 
      <artifactId>jaxb-api</artifactId> 
      <version>2.1</version> 
    </dependency> 
    <!-- [...] --> 
  </dependencies> 
  <!-- [...] --> 
  <repositories> 
    <!-- [...] --> 
    <repository> 
      <id>jaxb-fluent-api-ext-repository</id> 
      <name>Jaxb Fluent API Ext Maven 2 Repository</name> 
      <url>http://jaxb-fluent-api-ext.googlecode.com/svn/maven/release</url> 
    </repository> 
    <!-- [...] --> 
  </repositories> 
  <!-- [...] --> 
  <build> 
    <!-- [...] --> 
    <plugins> 
      <!-- [...] --> 
      <plugin> 
        <groupId>org.jvnet.jaxb2.maven2</groupId> 
        <artifactId>maven-jaxb2-plugin</artifactId> 
        <executions> 
          <execution> 
            <goals> 
              <goal>generate</goal> 
            </goals> 
          </execution> 
        </executions> 
        <configuration> 
          <verbose>true</verbose> 
          <schemaIncludes> 
            <include>my-schema-file-location.xsd</include> 
          </schemaIncludes> 
          <args> 
            <arg>-Xfluent-api-ext</arg> 
          </args> 
          <plugins> 
            <plugin> 
              <groupId>redmosquito</groupId> 
              <artifactId>jaxb-fluent-api-ext</artifactId> 
              <version>0.0.1</version> 
            </plugin> 
          </plugins> 
        </configuration> 
      </plugin> 
      <!-- [...] --> 
    </plugins> 
    <!-- [...] --> 
  </build> 
  <!-- [...] --> 
</project> 
```

Further information available on:
  * the [JAXB2 Maven2 plugin site](https://maven-jaxb2-plugin.dev.java.net/)
  * the [official reference guide](https://maven-jaxb2-plugin.dev.java.net/docs/guide.html)