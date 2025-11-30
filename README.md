# SQL queries executor service

Simple Spring Application with REST API for storing and executing analytical SQL queries over a dataset.

### Project Details

- **Framework**: Spring Boot 3.5.6

- **Database**: H2 (in-memory, auto-configured)

- **Java Version**: 21

- **Default Port**: 8080

## How to run

### Prerequisites

- Java 21 or later
- Gradle 7.0 or later (included with the project via Gradle Wrapper)

## Quick start

```bash
# Build the Project
./gradlew build

# Run the Application
./gradlew bootRun
```

Application starts at http://localhost:8080

## API Endpoints

### Query Management

|Method|Endpoint|Description|Response|
|:-|:-|:-|:-|
|Post|/api/queries|Store a SQL query for later execution (plain text body)|Saved query id|
|GET|/api/queries|Get all stored queries|Json with all stored queries|

### Query Execution

|Method|Endpoint|Description|Response|
|:-|:-|:-|:-|
|Post|/api/queries/{queryId}/execute|Start async execution of stored query|Job id, status of job, message|
|GET|/api/executions/{jobId}/status|Check job execution status|Job id, status of job|
|GET|/api/executions/{jobId}/result|Get query results (when completed)|Job id, status of job, result of query (if completed)|


# Solution design

## Main part implementation
### Application Architecture Overview

The application is built using a three-tier architecture (Controller, Service, Repository) to ensure a clean separation of concerns, making the system robust and maintainable. The design intentionally divides the logic into two main responsibilities: managing query metadata (storing and listing queries) and executing queries against the dataset.

1. Query Management Components

	#### This set of components handles the Create, Read, Update, and Delete (CRUD) operations for the stored SQL queries.

- **StoredQuery** (Entity): A JPA entity that represents a single record in the database. It contains fields like an id and the query text itself.

- **StoredQueryRepository** (Repository): A Spring Data JPA repository that provides the persistence mechanism for StoredQuery entities, handling all interactions with the database for saving, finding, and listing them.

- **StoredQueryService** (Service): This service layer encapsulates the business logic related to query management. It uses the StoredQueryRepository to fulfill tasks like adding a new query or retrieving a list of all saved queries.

2. Query Execution Components
	#### This set of components is specifically responsible for running a saved query and returning its results.

- **QueryExecutionRepository** (Repository): This data access component is responsible for executing raw SQL strings against the in-memory database using JDBC and fetching the raw dataset.

- **QueryExecutionService** (Service): Orchestrates the query execution process. It retrieves a query's SQL text using the management components and then uses the QueryExecutionRepository to run it and format the resulting data into a two-dimensional array.

3. API Layer
- **QueryController** (Controller): This class exposes the application's functionality through a REST API. It defines the endpoints, receives incoming HTTP requests, and delegates the processing to the appropriate service (StoredQueryService or QueryExecutionService) before returning the final JSON response to the client.


## Future Enhancements:
#### This implementation is a solid foundation, but could be improved in a real-world scenario:

### 1. Integration Tests

#### To ensure the reliability of the application and validate the entire request-response flow, integration tests were added alongside existing unit tests.

- Approach:
	#### The testing suite was expanded to include end-to-end tests that simulate real user interactions. These tests start the full Spring application context.

- Current Status:
    #### Some workflows have been covered by integration tests. Due to time constraints, comprehensive test coverage for every edge case and component interaction has not been completed but can be expanded.

### 2. Performance Improvement via Caching

#### To improve performance for repeated queries, the application can leverage Spring Boot's declarative caching mechanism. This avoids re-executing queries for which the result has already been computed.

- Implementation
	- Enable Caching: Add the @EnableCaching annotation to the main application class to activate Spring's caching functionality.
	- Create New Method And Annotate: In QueryExecutionService, create a new method that takes the query string, executes it, and returns the result. This is the method to be cached. Add the @Cacheable("queryResults").
	- Update the executeQuery Method: Now, modify existing **executeQuery** method to call new fetchQueryResult method instead of calling the repository directly.


- Potential Issues
	#### While simple, this approach has considerations for a production environment:

	- Memory Consumption: The default cache is a simple in-memory map. If many unique queries with large results are executed, the cache can grow very large, consuming significant application memory and potentially leading to an OutOfMemoryError.

	- No Automatic Eviction: By default, cached items are stored forever. Without a strategy to remove old or infrequently used entries (cache eviction), the memory usage will only ever increase.

	- Data Staleness: In a scenario where the underlying data could change, the cache would serve outdated results. While not an issue for this assignment's static dataset, it's a critical problem in most real-world applications that requires a cache invalidation strategy.

### 3. Other enhancements

- Security: Incorporating Spring Security would allow for role-based access control, ensuring only authorized users can execute queries or view results.

- Data Lifecycle Management: A cleanup mechanism (e.g., a scheduled job) could be added to automatically delete old QueryExecutionJob entities from the database to save memory.

- Database Strategy: For a production system, it would be beneficial to use a persistent database (like PostgreSQL) for long-term data (StoredQuery entities) and a fast, in-memory database (like Redis) for the transient QueryExecutionJob data.
