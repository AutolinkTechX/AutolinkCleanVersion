# Autolink – Recycled Car Parts Desktop App

Autolink is a desktop application built with Java 17+ that offers an eco-friendly platform for buying and managing recycled car parts. It connects users with trusted recycling suppliers and promotes sustainable automotive maintenance.

## Table of Contents
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## Features
- **Advanced Search**: Find parts by name, type, vehicle model, or supplier
- **Shopping Cart & Checkout**: Add parts to cart and complete purchases
- **Order History**: Track past orders and current status
- **Supplier Management**: Admin portal for inventory management
- **Secure Authentication**: Protected user accounts with encryption

## Installation

### Prerequisites
- Java 17 or later
- Maven 3.6+
- MySQL Database (via XAMPP/WAMP recommended)

### Step-by-Step Setup

1. **Clone and enter project directory**:
    ```bash
    git clone https://github.com/AutolinkTechX/AutolinkCleanVersion.git
    cd AutolinkCleanVersion
    ```

2. **Build with Maven**:
    ```bash
    mvn clean install
    ```

3. **Database Setup**:

    - Install XAMPP or WAMP
    - Start MySQL and Apache services
    - Access PHPMyAdmin at `http://localhost/phpmyadmin`
    
    **Create Database**:
    - Click "New" in PHPMyAdmin
    - Name: `autolinkdb`
    - Click "Create"
    
    **Import SQL (if available)**:
    - Select `autolinkdb`
    - Navigate to the "Import" tab
    - Upload `autolinkdb.sql`
    - Click "Execute"

4. **Configure Database Connection**:
    - Edit the configuration file: `src/main/java/org/example/pidev/utils/MyDatabase.java`
    - Set the database connection details:
    ```java
    String url = "jdbc:mysql://localhost:3306/autolinkdb";
    String user = "root";
    String password = ""; // Default for XAMPP/WAMP
    ```

5. **Launch Application**:
    ```bash
    java -jar target/AutolinkCleanVersion.jar
    ```

## Usage

### For Buyers
- Browse parts using search filters
- Add items to shopping cart
- Secure checkout process
- Track order history

### For Administrators
- Manage supplier accounts
- Monitor inventory levels
- Process orders and shipments
- Generate sales reports

## Contributing
1. Fork the repository
2. Create a feature branch:
    ```bash
    git checkout -b feature/your-feature
    ```
3. Commit changes:
    ```bash
    git commit -m "Description of changes"
    ```
4. Push to branch:
    ```bash
    git push origin feature/your-feature
    ```
5. Open pull request with detailed description

## License
MIT License - See LICENSE file for full details.

## Contact
- Issue Tracker: GitHub Issues
- Support Email: support@autolink.example.com
- Developer Team: dev-team@autolink.example.com
