Movie Hall Management System (MHMS)
1. Introduction
1.1 Purpose

The Movie Hall Management System (MHMS) is a software system designed to automate and manage movie hall operations such as movie scheduling, ticket booking, seat allocation, payments, staff management, and reporting.

1.2 Scope

The system will be used by:

Customers (online ticket booking)

Movie Hall Staff

Admin (theatre owner/manager)

The system supports:

Online & offline ticket booking

Real-time seat availability

Secure payment processing

Reports and analytics

2. Stakeholders
Stakeholder	Role
Customers	Book tickets, select seats
Ticket Counter Staff	Offline booking
Admin	Manage movies, halls, pricing
Payment Gateway	Online payments
System Admin	Maintenance
3. User Roles

Customer

Staff

Admin

4. Use Case Diagram (Textual)

Actors & Use Cases

Customer

Register / Login

Browse Movies

View Show Timings

Select Seats

Book Ticket

Make Payment

Download / View Ticket

Cancel Booking

Staff

Login

Book Ticket (Offline)

Print Ticket

Check Seat Availability

Admin

Login

Add / Update Movies

Create Shows

Configure Seat Layout

Set Ticket Prices

Manage Users

Generate Reports

5. Detailed Use Case Descriptions
UC-1: User Registration
Field	Description
Actor	Customer
Description	User creates an account
Preconditions	User not registered
Postconditions	Account created

Flow

User enters name, email, password

System validates data

Account created successfully

UC-2: Browse Movies
Actor	Customer
Description	View currently running movies

Flow

User opens app

System displays movies, posters, language, rating

UC-3: Book Ticket (Online)
Actor	Customer
Preconditions	User logged in
Postconditions	Ticket booked

Main Flow

Select movie

Select show time

Select seats

Make payment

Ticket generated

Alternate Flow

Payment failure → booking cancelled

UC-4: Offline Booking
Actor	Staff
Description	Ticket booking at counter

Flow

Staff logs in

Selects movie and show

Assigns seats

Collects cash

Prints ticket

UC-5: Admin Movie Management
Actor	Admin
Description	Manage movie details

Flow

Admin logs in

Adds / edits movie

Sets duration, language, rating

6. Functional Requirements (FR)
FR-1 User Management

System shall allow users to register and login

System shall support role-based access

FR-2 Movie Management

Admin shall add/update/delete movies

Admin shall manage show timings

FR-3 Seat Management

System shall show real-time seat availability

System shall prevent double booking

FR-4 Ticket Booking

System shall allow seat selection

System shall generate unique ticket ID

FR-5 Payment

System shall integrate online payment gateway

System shall confirm booking after payment success

FR-6 Reports

Admin shall view daily, weekly, monthly reports

7. Non-Functional Requirements (NFR)
Category	Requirement
Performance	Seat availability updated within 1 second
Security	Passwords encrypted
Reliability	99.5% uptime
Scalability	Support multiple halls
Usability	Simple UI
Availability	24×7 system
8. System Constraints

Requires internet for online booking

Payment gateway dependency

Browser / mobile compatibility

9. Assumptions

Users have basic internet access

Payment gateway is reliable

Admin manages seat layouts

10. Test Cases
TC-1: User Login
Field	Value
Test Case ID	TC-01
Description	Login with valid credentials
Input	Valid email, password
Expected Output	Login successful
Status	Pass
TC-2: Invalid Login
Test Case ID	TC-02
Input	Invalid password
Expected Output	Error message
TC-3: Seat Booking
Test Case ID	TC-03
Description	Book available seat
Expected Output	Seat booked
TC-4: Double Booking Prevention
Test Case ID	TC-04
Description	Book already booked seat
Expected Output	Booking rejected
TC-5: Payment Failure
Test Case ID	TC-05
Description	Payment fails
Expected Output	Booking cancelled
TC-6: Admin Add Movie
Test Case ID	TC-06
Description	Add new movie
Expected Output	Movie added
TC-7: Report Generation
Test Case ID	TC-07
Description	Generate daily report
Expected Output	Report displayed
11. Future Enhancements

Food & beverage booking

Loyalty points

Mobile app support

AI-based movie recommendations

12. Conclusion

The Movie Hall Management System automates cinema operations, reduces manual effort, prevents booking conflicts, and improves customer experience through efficient ticket booking and management.