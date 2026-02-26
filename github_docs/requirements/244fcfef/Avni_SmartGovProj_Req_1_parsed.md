1. Introduction

The Hall Management System (HMS) is a software application designed to manage and automate the booking, scheduling, and administration of halls such as seminar halls, auditoriums, conference rooms, and marriage halls. The system helps avoid booking conflicts, improves transparency, and simplifies coordination between users and administrators.

2. Stakeholders

Hall Administrator

Event Organizers / Users

Facility Management Staff

Institution / Organization Management

3. User Roles

Admin – Manages halls, approvals, users, pricing, and reports

Staff/Manager – Manages daily operations, maintenance, and schedules

User/Customer – Requests bookings, views availability, and makes payments

4. Functional Requirements
4.1 User Management

The system shall allow administrators to create, update, and delete user accounts.

The system shall support different roles such as admin, staff, and user.

The system shall provide secure login and logout functionality.

The system shall allow users to update their profile information.

The system shall deactivate blocked or inactive user accounts.

4.2 Hall Management

The system shall allow administrators to add new halls with details such as name, capacity, location, facilities, and pricing.

The system shall allow updating and deleting hall details.

The system shall maintain hall availability status in real time.

The system shall allow tagging halls with types (seminar, auditorium, banquet, etc.).

4.3 Booking Management

The system shall allow users to request hall bookings for specific dates and time slots.

The system shall prevent double booking for the same hall and time period.

The system shall allow administrators to approve, reject, or modify booking requests.

The system shall store booking details including event name, organizer, date, time, and duration.

4.4 Availability Checking

The system shall display available halls based on selected date and time.

The system shall allow users to filter halls by capacity, facilities, and price.

The system shall update availability automatically after booking approval.

4.5 Payment Management

The system shall calculate booking charges based on duration and hall pricing.

The system shall allow recording of payment status (paid, pending, refunded).

The system shall support advance and full payment options.

The system shall generate payment receipts.

4.6 Cancellation and Refund Management

The system shall allow users to cancel bookings within defined rules.

The system shall calculate refund amounts based on cancellation policies.

The system shall update hall availability after cancellation.

4.7 Maintenance and Facilities Management

The system shall allow staff to mark halls as unavailable due to maintenance.

The system shall allow tracking of maintenance schedules.

The system shall prevent booking of halls under maintenance.

4.8 Notifications

The system shall notify users about booking confirmation, rejection, or cancellation.

The system shall send reminders before the event date.

The system shall notify administrators of new booking requests.

4.9 Reports and Analytics

The system shall generate reports on bookings, revenue, and hall utilization.

The system shall generate daily, monthly, and yearly reports.

The system shall allow exporting reports in standard formats.

5. Non-Functional Requirements
5.1 Performance

The system shall support multiple concurrent users.

Availability checks shall be processed quickly.

The system shall handle peak booking periods efficiently.

5.2 Security

The system shall enforce role-based access control.

The system shall encrypt sensitive user and payment data.

The system shall maintain logs of booking and payment activities.

5.3 Usability

The system shall provide a simple and intuitive user interface.

The system shall be accessible through standard web browsers.

The system shall require minimal training for users and staff.

5.4 Reliability

The system shall ensure accurate booking and payment records.

The system shall provide data backup and recovery mechanisms.

The system shall minimize downtime.

5.5 Scalability

The system shall support addition of new halls and users.

The system shall allow future integration with external payment gateways.

5.6 Maintainability

The system shall follow a modular and well-documented design.

The system shall be easy to maintain and enhance.

The system shall support configuration changes without major redevelopment.

6. Constraints

The system shall operate using standard web technologies.

The system shall comply with organizational policies and rules.

The system shall work within existing infrastructure.

7. Assumptions

Users have access to the internet and basic technical knowledge.

Administrators are responsible for approving bookings.

Pricing and policies are defined by management.

8. Acceptance Criteria

Users can view hall availability and request bookings successfully.

The system prevents booking conflicts.

Administrators can manage halls, bookings, and payments accurately.

Reports reflect correct and up-to-date booking data.