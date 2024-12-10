# SENG696_CRS
Agent-Based Car Rental System


A car rental management system built using JADE (Java Agent DEvelopment Framework).

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/AakashSorathiya/SENG696_CRS/tree/master
   ```
   
## Prerequisites

Before running this project, you need to install the following dependencies:

1. **JADE Framework**
   - Download JADE from the [official Tilab website](https://jade.tilab.com/)
   - Follow JADE's installation instructions for your operating system
   - Add JADE to the project's build path

2. **MySQL**
   - Download and install MySQL Server from the [official MySQL website](https://dev.mysql.com/downloads/mysql/)
   - Make sure the MySQL service is running on your system
   - Remember your MySQL root password for later configuration

3. **MySQL Connector/J**
   - Download MySQL Connector/J from the [official MySQL website](https://dev.mysql.com/downloads/connector/j/)
   - Add the connector JAR file to the project's dependencies

   ## Database Setup

1. Create the database:
   - Open MySQL command line or your preferred MySQL client
   - Execute the following command:
     ```sql
     CREATE DATABASE car_rental;
     ```

2. Configure database connection:
   - Navigate to `src/database/DatabaseConnection.java`
   - Modify the following fields with your MySQL credentials:
     ```java
      private static final String URL = "jdbc:mysql://localhost:3306/car_rental";
      private static final String USER = "";
      private static final String PASSWORD = "";
     ```

## Running the Application

1. Make sure MySQL service is running on your system

2. Launch the JADE platform with your application:
   ```bash
   java jade.Boot -gui -agents "master:agents.MasterAgent;reg:agents.RegistrationAgent;res:agents.ReservationAgent;veh:agents.VehicleManagementAgent;pay:agents.PaymentAgent"
   ```

   If you are using intellij IDE, you can add this command to the config file of the project and run the program.

   
