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
- Maven
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
    
    **1. Create Database**:
    - Click "New" in PHPMyAdmin
    - Name: `autolinkdb`
    - Click "Create"
    
    **2. Import SQL (autolinkdb.sql File)**:
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

We welcome contributions!

1. Fork the repository.  
2. Create a new branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes and commit them:
   ```bash
   git commit -m "Add your message here"
   ```
4. Push to the branch:
   ```bash
   git push origin feature/your-feature-name
   ```
5. Open a pull request detailing your changes.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact

For questions or feedback, please open an issue on the [GitHub repository](https://github.com/AutolinkTechX/AutolinkCleanVersion/issues).
