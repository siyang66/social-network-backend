# SENIOR Backend Assignment

**Language:** English  
**Level:** Senior  
**Responsible:** S Sureena  
**Status:** In Use  

## Assignment
**Building an Advanced Backend System for a Social Network**

## Objective
This assignment is designed to challenge your expertise in:
- Database design
- Query optimization
- API development
- Fraud detection
- Microservices architecture

## Requirements

### 1. Database Design
Create a performant relational database structure to store the following data:
- User profiles with common data (e.g. name, age)
- All profile visits performed by a user
- All user profiles a user has liked

> ⚠️ Do **NOT** use Hibernate or JPA.

### 2. Query Design
Create a query that retrieves **all profile visitors of a user**, sorted in the way you consider most appropriate.

### 3. Java Domain Model
Design a Java class that models user profiles with:
- Name
- Age
- Additional user-defined fields

### 4. Validation & Data Integrity
Implement validation mechanisms to ensure **data consistency and integrity** within the Java class.

### 5. Visit API
Develop a `/user/visit` API endpoint that records when user **A** visits user **B**.

### 6. Like API
Develop a `/user/like` API endpoint that records when user **A** likes user **B**.

### 7. Fraud Detection Logic
If a user has **visited and liked 100 users within the first 10 minutes**, mark that user as **fraud**.

### 8. Bulk Data Insertion
Develop a data insertion method that handles **bulk inserts efficiently**, without using Hibernate or JPA.

### 9. Microservices Architecture & State Propagation
Analyze the existing monolithic backend and propose a detailed microservices architecture.
- Define Services, APIs, and Data Flow.
- Address API versioning and circuit breaking.
- Provide a deep dive into how you handle eventual consistency. Specifically, if a user is marked as 'Fraud' in the Fraud service, how is that state propagated across the system while ensuring the user cannot perform further actions in the Interaction Service.

## Submission
Submit your assignment by creating a **comprehensive GitHub repository** that includes:
- All required code
- Documentation
- Explanations of design decisions and strategies

Send the repository link to:
- simon@meet5.com  
and CC:
- sureena@meet5.com  
- joschiko@meet5.com  

## Additional Information

### Second Round Interview
- 00 - 30 mins: Introductions and background.
- 30 - 75 mins: Technical assignment review.
- We will walk through your solution together; be prepared to explain your logic and specific coding choices.

**Code Defense**
