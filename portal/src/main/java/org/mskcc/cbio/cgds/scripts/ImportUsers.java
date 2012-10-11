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
package org.mskcc.cbio.cgds.scripts;

// imports
import org.mskcc.cbio.cgds.model.User;
import org.mskcc.cbio.cgds.dao.DaoUser;
import org.mskcc.cbio.cgds.model.UserAuthorities;
import org.mskcc.cbio.cgds.dao.DaoUserAuthorities;

import org.mskcc.cbio.cgds.util.ConsoleUtil;
import org.mskcc.cbio.cgds.util.ProgressMonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Arrays;

/**
 * Import a file of users and their authorities.
 *
 * File contains the fields:
 *
 * EMAIL_ADDRESS\tUSERNAME\tENABLED\tAUTHORITIES
 *
 * AUTHORITIES is semicolon separated list of authorites
 * 
 * @author Arthur Goldberg goldberg@cbio.mskcc.org
 * @author Benjamin Gross
 */
public class ImportUsers {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("command line usage: java org.mskcc.cgds.scripts.ImportUsers <users_file.txt>");
            System.exit(1);
        }

        ProgressMonitor pMonitor = new ProgressMonitor();
        pMonitor.setConsoleMode(true);

        File file = new File(args[0]);
        FileReader reader = new FileReader(file);
        BufferedReader buf = new BufferedReader(reader);
        String line = buf.readLine();
        int count = 0;
        while (line != null) {
            if (pMonitor != null) {
                pMonitor.incrementCurValue();
                ConsoleUtil.showProgress(pMonitor);
            }
            if (!line.startsWith("#") && line.trim().length() > 0) {
                try {
                    addUser(line);
                    count++;
                } catch (Exception e) {
                    System.err.println("Could not add line '" + line + "'. " + e);
                }
            }
            line = buf.readLine();
        }
        System.err.println("Added " + count + " user access rights.");
        ConsoleUtil.showWarnings(pMonitor);
        System.err.println("Done.");
    }

    private static void addUser(String line) throws Exception {
        line = line.trim();
        String parts[] = line.split("\t");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Missing a user attribute, parts: " + parts.length);
        }
        String email = parts[0];
        String name = parts[1];
        Boolean enabled = new Boolean(parts[2]);
        List authorities = Arrays.asList(parts[3].split(";"));

        // if user doesn't exist create them
        User user = DaoUser.getUserByEmail(email);
        if (null == user) {
            user = new User(email, name, enabled);
            DaoUser.addUser(user);
        }

        // if exist, delete user authorities
        UserAuthorities currentAuthorities = DaoUserAuthorities.getUserAuthorities(user);
        if (currentAuthorities != null) {
            DaoUserAuthorities.removeUserAuthorities(user);
        }

        // add new authorities
        UserAuthorities userAuthorities = new UserAuthorities(email, authorities);
        DaoUserAuthorities.addUserAuthorities(userAuthorities);
    }
}