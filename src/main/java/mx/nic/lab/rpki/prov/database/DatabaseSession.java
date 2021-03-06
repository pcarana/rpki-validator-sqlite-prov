package mx.nic.lab.rpki.prov.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

import mx.nic.lab.rpki.db.exception.InitializationException;

/**
 * Instance to handle a database session
 *
 */
public class DatabaseSession {

	/**
	 * Milliseconds that the implementation will wait to retry in case of a DB lock
	 */
	public static final long BUSY_RETRY_MS = 200L;

	/**
	 * Query execution timeout in seconds
	 */
	public static final int QUERY_TIMEOUT = 10;

	/**
	 * Data source to get/store data
	 */
	private static DataSource dataSource;

	/**
	 * Used for logging
	 */
	private static final Logger logger = Logger.getLogger(DatabaseSession.class.getName());

	/**
	 * Initialize the DB connection based on the configuration provided
	 * 
	 * @param config
	 * @throws InitializationException
	 */
	public static void initConnection(Properties config) throws InitializationException {
		/*
		 * At compile time, the application does not know the data access
		 * implementation, and the data access implementation does not know the
		 * application.
		 * 
		 * So we don't know for sure where the data source can be found, and the
		 * application cannot tell us. There is no interface for the application to
		 * provide us with a data source, because the data source is OUR problem. From
		 * the app's perspective, it might not even exist.
		 * 
		 * So what we're going to do is probe the candidate locations and stick with
		 * what works.
		 */
		dataSource = findDataSource(config);
		if (dataSource != null) {
			logger.info("Data source found.");
			return;
		}
		logger.info("I could not find the API data source in the context. "
				+ "This won't be a problem if I can find it in the configuration. Attempting that now... ");
		dataSource = loadDataSourceFromProperties(config);
	}

	/**
	 * End the DB connection and unregister driver
	 */
	public static void endConnection() {
		if (dataSource != null) {
			Enumeration<Driver> drivers = DriverManager.getDrivers();
			while (drivers.hasMoreElements()) {
				Driver driver = drivers.nextElement();
				try {
					DriverManager.deregisterDriver(driver);
					logger.log(Level.INFO, String.format("deregistering jdbc driver: %s", driver));
				} catch (SQLException e) {
					logger.log(Level.SEVERE, String.format("Error deregistering driver %s", driver), e);
				}
			}
			dataSource = null;
		}
	}

	private static DataSource findDataSource(Properties config) {
		Context context;
		try {
			context = new InitialContext();
		} catch (NamingException e) {
			logger.log(Level.INFO, "I could not instance an initial context. "
					+ "I will not be able to find the data source by JDNI name.", e);
			return null;
		}

		String jdniName = config.getProperty("db_resource_name");
		if (jdniName != null) {
			return findDataSource(context, jdniName);
		}

		// Try the default string.
		// In some server containers (such as Wildfly), the default string is
		// "java:/comp/env/jdbc/rpki-api".
		// In other server containers (such as Payara), the string is
		// "java:comp/env/jdbc/rpki-api".
		// In other server containers (such as Tomcat), it doesn't matter.
		DataSource result = findDataSource(context, "java:/comp/env/jdbc/rpki-api");
		if (result != null) {
			return result;
		}

		return findDataSource(context, "java:comp/env/jdbc/rpki-api");
	}

	private static DataSource findDataSource(Context context, String jdniName) {
		logger.info("Attempting to find data source '" + jdniName + "'...");
		try {
			return (DataSource) context.lookup(jdniName);
		} catch (NamingException e) {
			logger.info("Data source not found. Attempting something else...");
			return null;
		}
	}

	private static DataSource loadDataSourceFromProperties(Properties config) throws InitializationException {
		String driverClassName = config.getProperty("driverClassName");
		String url = config.getProperty("url");
		if (driverClassName == null || url == null) {
			throw new InitializationException("I can't find a data source in the configuration.");
		}

		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driverClassName);
		dataSource.setUrl(url);
		dataSource.setDefaultAutoCommit(true);

		// Load the test query, if not present then load the most common
		// (http://stackoverflow.com/questions/3668506)
		String testQuery = config.getProperty("testQuery", "select 1");
		try {
			testDatabase(dataSource, testQuery);
		} catch (SQLException e) {
			throw new InitializationException("The database connection test yielded failure.", e);
		}
		try {
			createTables(dataSource);
		} catch (IOException | SQLException e) {
			throw new InitializationException("The database tables creation failed.", e);
		}
		return dataSource;
	}

	private static void testDatabase(BasicDataSource ds, String testQuery) throws SQLException {
		try (Connection connection = ds.getConnection(); Statement statement = connection.createStatement();) {
			logger.log(Level.FINE, "Executing QUERY: " + testQuery);
			ResultSet resultSet = statement.executeQuery(testQuery);

			if (!resultSet.next()) {
				throw new SQLException("'" + testQuery + "' returned no rows.");
			}
		}
	}

	/**
	 * Try to create the tables (just in case that they don't exist)
	 * 
	 * @param dataSource
	 * @throws IOException
	 * @throws SQLException
	 */
	private static void createTables(DataSource dataSource) throws IOException, SQLException {
		String fileName = "META-INF/createDatabase.sql";
		InputStream in = DatabaseSession.class.getClassLoader().getResourceAsStream(fileName);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			StringBuilder querySB = new StringBuilder();
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				// Supported comments start with "--"
				if (!currentLine.startsWith("--")) {
					querySB.append(currentLine);
					if (currentLine.trim().endsWith(";")) {
						String queryString = querySB.toString();
						// Execute the statement
						try (Connection connection = dataSource.getConnection();
								Statement statement = connection.createStatement()) {
							statement.executeUpdate(queryString);
						}
						querySB.setLength(0);
					} else {
						querySB.append(" ");
					}
				}
			}
		}
	}

	/**
	 * Get the connection from the loaded DataSource
	 * 
	 * @return A {@link Connection} from the {@link DataSource}
	 * @throws SQLException
	 */
	public static Connection getConnection() throws SQLException {
		synchronized (DatabaseSession.class) {
			// Set autocommit to true
			Connection con = dataSource.getConnection();
			con.setAutoCommit(true);
			return con;
		}
	}
}
