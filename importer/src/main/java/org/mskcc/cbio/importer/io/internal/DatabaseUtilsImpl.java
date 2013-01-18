/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
**
** This library is free software; you can redistribute it and/or modify it
** under the terms of the GNU Lesser General Public License as published
** by the Free Software Foundation; either version 2.1 of the License, or
** any later version.
**
** This library is distributed in the hope that it will be useful, but
** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
** documentation provided hereunder is on an "as is" basis, and
** Memorial Sloan-Kettering Cancer Center 
** has no obligations to provide maintenance, support,
** updates, enhancements or modifications.  In no event shall
** Memorial Sloan-Kettering Cancer Center
** be liable to any party for direct, indirect, special,
** incidental or consequential damages, including lost profits, arising
** out of the use of this software and its documentation, even if
** Memorial Sloan-Kettering Cancer Center 
** has been advised of the possibility of such damage.  See
** the GNU Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public License
** along with this library; if not, write to the Free Software Foundation,
** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
**/

// package
package org.mskcc.cbio.importer.io.internal;

// imports
import org.mskcc.cbio.importer.DatabaseUtils;
import org.mskcc.cbio.importer.io.internal.DataSourceFactoryBean;
import org.mskcc.cbio.importer.util.Shell;
import org.mskcc.cbio.importer.util.MetadataUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import javax.sql.DataSource;

/**
 * Class which can create database/database schema dynamically.
 */
public class DatabaseUtilsImpl implements DatabaseUtils {

	// our logger
	private static final Log LOG = LogFactory.getLog(DatabaseUtilsImpl.class);

	// some context files
	private static final String importerContextFile = "classpath:applicationContext-importer.xml";
	private static final String createSchemaContextFile = "classpath:applicationContext-createSchema.xml";

	// the follow db properties are set here for convenient access by our clients

	// db user 
	private String databaseUser;
	@Value("${db.user}")
    public void setDatabaseUser(String databaseUser) { this.databaseUser = databaseUser; }
	@Override
    public String getDatabaseUser() { return this.databaseUser; }

	// db password
	private String databasePassword;
	@Value("${db.password}")
	public void setDatabasePassword(String databasePassword) { this.databasePassword = databasePassword; }
	@Override
    public String getDatabasePassword() { return this.databasePassword; }

	// db connection
	private String databaseConnectionString;
	@Value("${db.connection_string}")
	public void setDatabaseConnectionString(String databaseConnectionString) { this.databaseConnectionString = databaseConnectionString; }
	@Override
    public String getDatabaseConnectionString() { return this.databaseConnectionString; }

	// db schema
	private String portalDatabaseSchema;
	@Value("${db.portal_schema}")
	public void setPortalDatabaseSchema(String portalDatabaseSchema) { this.portalDatabaseSchema = portalDatabaseSchema; }
	@Override
    public String getPortalDatabaseSchema() {
		return MetadataUtils.getCanonicalPath(this.portalDatabaseSchema);
	}

	// importer database name
	private String importerDatabaseName;
	@Value("${db.importer_db_name}")
	public void setImporterDatabaseName(String importerDatabaseName) { this.importerDatabaseName = importerDatabaseName; }
	@Override
    public String getImporterDatabaseName() { return this.importerDatabaseName; }

	// portal database name
	private String portalDatabaseName;
	@Value("${db.portal_db_name}")
	public void setPortalDatabaseName(String portalDatabaseName) { this.portalDatabaseName = portalDatabaseName; }
	@Override
    public String getPortalDatabaseName() { return this.portalDatabaseName; }

    /**
	 * Creates a database and optional schema.
	 * 
	 * @param databaseName String
	 * @param createSchema boolean
	 */
	@Override
	public void createDatabase(String databaseName, boolean createSchema) {

		if (LOG.isInfoEnabled()) {
			LOG.info("createDatabase(): " + databaseName);
		}

		// we want to get a reference to the database util factory bean interface
		// so we can set a datasource which matches the bean DatabaseUtil bean name
		ApplicationContext context = new ClassPathXmlApplicationContext(importerContextFile);
		DataSourceFactoryBean dataSourceFactoryBean =
			(DataSourceFactoryBean)context.getBean("&dataSourceFactory");

		// create the database - drop if it exists
		createDatabase(dataSourceFactoryBean, databaseName, true);

		if (createSchema) {
			// create a datasource to this database name - important to set the map key
			// to be equal to the bean name within the createSchema context file
			dataSourceFactoryBean.createDataSourceMapping("createSchema", databaseName);

			// load the context that auto-creates tables
			context = new ClassPathXmlApplicationContext(createSchemaContextFile);
		}
	}

	/**
	 * Execute the given script on the given db.
	 *
	 * @param databaseName String
	 * @param databaseScript String
	 * @param databaseUser String
	 * @param databasePassword String
	 */
	@Override
	public boolean executeScript(String databaseName, String databaseScript,
								 String databaseUser, String databasePassword) {

		// use mysql to create new schema
		String[] command = new String[] {"mysql",
										 "--user=" + databaseUser,
										 "--password=" + databasePassword,
										 databaseName,
										 "-e",
										 "source " + databaseScript };
		if (LOG.isInfoEnabled()) {
			LOG.info("executing: " + Arrays.asList(command));
		}

		return Shell.exec(Arrays.asList(command), ".");
	}

	/**
	 * Creates a database with the given name.
	 *
	 * @param dataSourceFactoryBean DataSourceFactoryBean
	 * @param databaseName String
	 * @param dropDatabase boolean
	 * @return boolean
	 */
	private boolean createDatabase(DataSourceFactoryBean dataSourceFactoryBean,
								   String databaseName, boolean dropDatabase) {

		boolean toReturn = true;

		// create simple JdbcTemplate if necessary
		DataSource dataSource = dataSourceFactoryBean.getDataSource("");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		try {
			// drop if desired
			if (dropDatabase) {
				jdbcTemplate.execute("DROP DATABASE IF EXISTS " + databaseName);
			}
			// create
			jdbcTemplate.execute("CREATE DATABASE " + databaseName);
			if (LOG.isInfoEnabled()) {
				LOG.info("createDatabase(): " + databaseName + " successfully created.");
			}
		}
		catch (DataAccessException e) {
			LOG.error(e);
			toReturn = false;
		}

		// outta here
		return toReturn;
	}
}
