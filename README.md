# JetBrains Internship Test Task

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



That is the core of application, simple implementation of given task, but it has a lot of flaws some of them are stated in task description such as: "The queries should not modify the data inside the database.", "For larger datasets, queries might take some time to execute.", "Along with unit tests, you can also add integration tests that run a full flow of adding, listing and running a query.", "The analytic data usually isn’t updated often, in this assignment it’s not updated at all. You can use this fact to improve the performance of your service."

## Bonus assignments implementation

### 1. Ensuring Read-Only Queries ("The queries should not modify the data inside the database.")

#### To guarantee that queries do not modify the database, a validation strategy was implemented before any query is saved.

- Implementation:
    #### A new QueryValidationService was introduced to act as a security gateway. Before a query is persisted via the StoredQueryService, it undergoes a series of checks:

     - Keyword Analysis: It verifies that the query is a SELECT statement.

     - Command Blacklisting: The query is scanned for dangerous, data-modifying keywords such as DROP, DELETE, INSERT, UPDATE, CREATE, ALTER, TRUNCATE.

     - Statement Validation: It ensures the submission contains only a single SQL statement, preventing stacked query attacks.

     - Pattern Matching: A simple check for common SQL injection patterns is performed. Any query that fails these validation rules is rejected and not stored in the database.


- Limitations & Trade-offs:
    #### Due to simplicity this approach provides a first line of defense but has some trade-offs:

     - The SQL injection pattern checking is very simplistic and could be bypassed by sophisticated attacks.

     - Keywords like UNION are blacklisted for security, which may restrict some complex (but valid) analytical queries.

### 2. Asynchronous Query Execution ("For larger datasets, queries might take some time to execute.")

#### To handle queries that may take a long time to execute without blocking the client or causing HTTP timeouts, the execution process was redesigned to be asynchronous.

- Architecture & Components:
    #### The core of this solution is a new JPA entity, QueryExecutionJob, which tracks the state of each execution request.

    - QueryExecutionJob Entity: This entity contains fields to monitor a job's lifecycle, including its own id, the storedQueryId it's linked to, a status (with possible values PENDING, RUNNING, COMPLETED, FAILED), a result field to store the data, and an errorMessage field for failures.

    - QueryExecutionJobRepository & Service: Standard repository and service layers were created to manage the lifecycle of QueryExecutionJob entities (creating, saving, and retrieving them).


- Asynchronous Execution Flow:
    #### The QueryExecutionService was refactored for the new workflow:

    - The primary execution method is now annotated with @Async, which tells Spring to run it in a separate background thread.

    - When a query execution is requested, the service immediately creates a QueryExecutionJob with a PENDING status and returns its ID to the client.

    - In the background, the actual SQL query is executed (QueryExecutionJob entity's staus is RUNNING). Upon completion, the service updates the QueryExecutionJob entity's status to either COMPLETED (and populates the result) or FAILED (and populates the errorMessage).


- Modified REST API:
    #### The API now follows a non-blocking logic where the client polls for the result:

    - POST /api/queries/{queryId}/execute: This endpoint initiates a new execution job. It immediately returns a 201 Created status with the location of the new job resource.

    - GET /api/executions/{jobId}/status: Allows the client to check the current status of the job (RUNNING, FAILED, etc.).

    - GET /api/executions/{jobId}/result: Once the job status is COMPLETED, the client can use this endpoint to retrieve the query result. If FAILED, it retrieves the error message.


- Future Enhancements:
    #### This implementation is a solid foundation, but could be improved in a real-world scenario:

    - Security: Incorporating Spring Security would allow for role-based access control, ensuring only authorized users can execute queries or view results.

    - Data Lifecycle Management: A cleanup mechanism (e.g., a scheduled job) could be added to automatically delete old QueryExecutionJob entities from the database to save memory.

    - Database Strategy: For a production system, it would be beneficial to use a persistent database (like PostgreSQL) for long-term data (StoredQuery entities) and a fast, in-memory database (like Redis) for the transient QueryExecutionJob data.

### 3. Integration Tests ("Along with unit tests, you can also add integration tests that run a full flow of adding, listing and running a query.")

#### To ensure the reliability of the application and validate the entire request-response flow, integration tests were added alongside existing unit tests.

- Approach:
	#### The testing suite was expanded to include end-to-end tests that simulate real user interactions. These tests start the full Spring application context.

- Current Status:
    #### Some workflows have been covered by integration tests. Due to time constraints, comprehensive test coverage for every edge case and component interaction has not been completed but can be expanded.

### 4. Performance Improvement via Caching ("The analytic data usually isn’t updated often, in this assignment it’s not updated at all. You can use this fact to improve the performance of your service.") 

#### Due to time limitation I have not implemented this assingment, but this is my plan for it.

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
