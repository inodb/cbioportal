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
package org.mskcc.cbio.importer.model;

// imports

/**
 * Class which contains datasource metadata.
 */
public final class DataSourceMetadata {

	// bean properties
	private String dataSource;
    private String downloadDirectory;
    private String latestRunDownload;

    /**
     * Create a DataSourceMetadata instance with specified properties.
     *
	 * @param dataSource String
	 * @param downloadDirectory String
     * @param latestRunDownload String
     */
    public DataSourceMetadata(final String dataSource,
							  final String downloadDirectory,
							  final String latestRunDownload) {

		if (dataSource == null) {
            throw new IllegalArgumentException("dataSource must not be null");
		}
		this.dataSource = dataSource;

		if (downloadDirectory == null) {
            throw new IllegalArgumentException("downloadDirectory must not be null");
		}
		this.downloadDirectory = downloadDirectory;

		if (latestRunDownload == null) {
            throw new IllegalArgumentException("latestRunDownload must not be null");
		}
		this.latestRunDownload = latestRunDownload;
	}

	public String getDataSource() { return dataSource; }
	public String getDownloadDirectory() { return downloadDirectory; }
	public String getLatestRunDownload() { return latestRunDownload; }
	public void setLatestRunDownload(final String latestRunDownload) { this.latestRunDownload = latestRunDownload; }
}
