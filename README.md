# 📱 Smart Contact Manager (SCM)

A modern, cloud-ready contact management web application built using **Spring Boot 3.2.5**, **Java 17**, **Spring Security 6**, and **MySQL**. It enables users to securely store, organize, export, and share personal and professional contacts with native smartphone integration.

---

##  Key Features

  Authentication & Security:
  * Role-based access control (`ROLE_USER`, `ROLE_ADMIN`) powered by Spring Security 6.
  * Encrypted password hashing with `BCryptPasswordEncoder`.
  * Secure session management and route protection (`/user/**`).

  Comprehensive Contact Management (CRUD):
  * Add contacts with profile pictures (Multipart image upload).
  * Server-side paginated contacts view (`Pageable` & `Page<Contact>`).
  * Detailed contact view with direct action buttons: **Call**, **WhatsApp**, and **Email**.
  * Update and delete contacts with animated **SweetAlert2** confirmation dialogs.

 Native vCard QR Code Sharing:
  * Generates dynamic vCard 3.0 QR codes for individual contacts.
  * Any smartphone camera (iOS / Android / Google Lens) scanning the QR code instantly prompts to **"Add to Contacts"** with pre-filled details.
 
 Favorite / Starred Contacts:
  * 1-click asynchronous (AJAX) favorite tagging without page reload.
  * Filter toolbar to instantly view all contacts or only starred contacts.

 Real-time Asynchronous Search:
  * REST API-driven instant search (`/search/{query}`) returning live contact suggestions as you type.

 Bulk Import & Export:
  * **Export:** One-click download of all contacts to CSV (compatible with Microsoft Excel & Google Sheets).
  * **Bulk Import:** Upload CSV files to import dozens or hundreds of contacts in seconds with sample template download support.

 Responsive Modern UI:
  * Built with Bootstrap 4, FontAwesome 6, and custom CSS.
  * Collapsible desktop sidebar and off-canvas slide-in mobile drawer.
  * Compact 3-dots action menu for clean mobile table display.



 Tech Stack

* **Backend:** Java 17, Spring Boot 3.2.5
* **Security:** Spring Security 6 (FilterChain, DaoAuthenticationProvider)
* **Data Access:** Spring Data JPA, Hibernate 6
* **Database:** MySQL 8.x
* **Frontend:** Thymeleaf 3, HTML5, CSS3, JavaScript (Fetch API & jQuery)
* **UI Components:** Bootstrap 4, FontAwesome 6, SweetAlert2, QRCode.js
* **Build Tool:** Apache Maven



##  Project Structure

```text
smartcontactmanager/
├── src/main/java/com/smart/smartcontactmanager/
│   ├── config/          # Spring Security & UserDetails implementations
│   ├── controller/      # HomeController, UserController, SearchController
│   ├── dao/             # UserRepository, ContactRepository (JPA)
│   ├── entities/        # User and Contact JPA entities
│   └── helper/          # SessionHelper, Message alert wrapper
├── src/main/resources/
│   ├── static/          # CSS, JS, and image assets
│   ├── templates/       # Public templates (home, login, signup, about)
│   │   └── normal/      # Protected dashboard templates (user_dashboard, show_contacts, etc.)
│   └── application.properties
└── pom.xml
