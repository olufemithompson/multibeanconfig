# MultiBeanConfig
MultiBeanConfig is a lightweight library for managing multiple beans of the same class in your Spring applications. 

With this library, you can easily configure each bean separately using properties, while still being able to pull in default configurations when needed. This helps you maintain clean, organized code and ensures that your beans have all the settings they require.

## Features

- **Multiple Bean Registration**: Easily register multiple beans of the same class with distinct configurations.
- **Custom Configurations**: Allow each bean to have its own settings defined in the properties file.
- **Default Values**: Automatically use default configurations for any missing values specific to a bean.
- **Automatic Injection**: Support dependency injection via `@Autowired` and `@Value` annotations, whether using constructor or field injection.

## Getting Started
### Installation
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.olufemithompson</groupId>
    <artifactId>multibeanconfig</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Usage
#### Step 1: Define your beans and their configurations in your application.yml:
In your application.yml, you can define your beans and their configurations. 
The example below demonstrates how to define two beans of the same class, *HttpClientService*, named *defaultClient* and failOverClient*. 
Each bean has its own separate configuration.
```yaml
config:
  client-id: '123456'
  client-secret: 'abcde'
  scopes:
    - 'read'
    - 'write'

multiple:
  default-client:
    class: HttpClientService
    config:
      client-id: '123456'
      client-secret: 'abcde'
      scopes:
        - 'read'
        - 'write'
        - 'delete'

  fail-over-client:
    class: HttpClientService
    config:
      client-id: '123456'
      client-secret: 'abcde'
```
###Explanation of the Configuration
- **Default Configuration**: The config section at the top defines default values that can be used by any bean if specific values are missing in the bean's own configuration.
- **Bean-Specific Configurations**: Each bean under multiple has its own config section where you can define unique settings.
- **Automatic Value Inheritance**: If a property is not found in a bean's configuration, it will automatically pull from the default configuration defined earlier.

#### Step 2: Use the @MultipleBean Annotation
Annotate your service class with `@MultipleBean` to indicate that it should be configured using the MultiBeanConfig library. Here’s an example of how to use the `@MultipleBean` annotation with the `HttpClientService` defined in the previous `application.yml`.

##### A: Inject Configuration via Constructor
In this approach, the `HttpClientService` class receives its configuration through the constructor.

```java
@MultipleBean
public class HttpClientService {

    private final HttpConfig config;
    
    public HttpClientService(HttpConfig config) {
        this.config = config;
    }
}
```

##### B: Inject Configuration via Autowiring
Alternatively, you can use Spring's @Autowired annotation to inject the configuration directly into the class.
```java
@MultipleBean
public class HttpClientService {

    @Autowired
    private  HttpConfig config;
    
}
```
This is assuming you have defined an *HttpConfig* class annotated with *@ConfigurationProperties*:
```java
@ConfigurationProperties("config")
@Getter
@Setter
public class HttpConfig {

    private String clientId;
    private String clientSecret;
    private List<String> scopes;
    
}
```

##### C: Inject Configuration Properties with @Value
You can also inject specific configuration properties directly into your class fields using the @Value annotation.
```java
@MultipleBean
public class HttpClientService {

    @Value("${config.client-id}")
    private  String clientId;

    @Value("${config.client-secret}")
    private  String clientSecret;

    @Value("${config.scopes}")
    private  List<String> scopes;
    
}
```


#### Step 3: Define a service that requires your multiple bean.
You can define a service that requires multiple beans of the same class. Use constructor injection to obtain the beans:
```java
@Service
public class ConfiguredService {

    private final HttpClientService defaultClient;
    private final HttpClientService failOverClient;
    
    public ConfiguredService(HttpClientService defaultClient,
                             HttpClientService failOverClient) {
        this.defaultClient = defaultClient;
        this.failOverClient = failOverClient;
    }
}
```
Or, you can use the *@Autowired* annotation for field injection:
```java
@Service
public class YourService {

    @Autowired
    private HttpClientService defaultClient;

    @Autowired
    private HttpClientService failOverClient;
}
```
### Explanation of the Code
In this section, you learned how to utilize the @MultipleBean annotation to manage your beans efficiently within your Spring application:

- **Constructor Injection**: This method ensures that the configuration is provided when the bean is created.

- **Autowiring**: This approach allows Spring to automatically inject the dependencies into your class, making it easier to manage but less explicit compared to constructor injection.

- **@Value Injection**: Using @Value, you can directly assign configuration values to class fields.

- **HttpConfig Class**: This class centralizes your configuration properties and maps them from the application.yml file, allowing for cleaner and more organized code.

## Contributing
Contributions are welcome! If you have suggestions or improvements, feel free to submit a pull request.

## License
This project is licensed under the Apache License - see the LICENSE file for details.