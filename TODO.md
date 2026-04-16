# Auth-Service Clean Architecture Implementation TODO

## Steps:

- [x] Step 1: Update pom.xml (Java 17, add Kafka, MapStruct, jjwt)
- [x] Step 2: Update application.yaml (configs for DB, JWT, Kafka)
- [x] Step 3: Create domain layer (User entity, Role enum, usecases)
- [x] Step 4: Create infrastructure - persistence (UserRepository)
- [x] Step 5: Create infrastructure - security (JwtUtil, JwtAuthenticationFilter, SecurityConfig)
- [x] Step 6: Create infrastructure - Kafka (UserEventProducer, config)
- [x] Step 7: Create application layer (dtos, UserMapper MapStruct, services/usecases)
- [x] Step 8: Create presentation layer (AuthController, UserController)
- [x] Step 9: Add Liquibase changelog for User table
- [x] Step 10: Update main app class if needed, add tests
- [x] Step 11: Test build/run (mvn compile, mvn spring-boot:run)

All steps complete!

Progress: Starting Step 1.
