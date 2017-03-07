/*
 *  Copyright (C) 2014 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * 
 *  GPLv3 + Classpath exception
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fao.geonet.kernel.security.shibboleth;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ProfileManager;
import jeeves.server.resources.ResourceManager;
import jeeves.utils.Log;
import jeeves.utils.SerialFactory;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.kernel.security.GeonetworkUser;
import org.fao.geonet.lib.Lib;
import org.jdom.Element;
import java.util.Objects;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class ShibbolethUserUtils {

	private static final String VIA_SHIBBOLETH = "Via Shibboleth";
	private static final String SHIBBOLETH_FLAG = "SHIBBOLETH";

    static MinimalUser parseUser(ServletRequest request, ResourceManager resourceManager, ProfileManager profileManager, ShibbolethUserConfiguration config) {
        return MinimalUser.create(request, config);
    }

    public static class MinimalUser {

        private String username;
        private String name;
        private String surname;
        private String profile;
        private String email;

        static MinimalUser create(ServletRequest request, ShibbolethUserConfiguration config) {

            // Read in the data from the environment vars
            HttpServletRequest req = (HttpServletRequest)request;

            String username = Objects.toString(req.getAttribute(config.getUsernameKey()), "");
            String surname = Objects.toString(req.getAttribute(config.getSurnameKey()), "");
            String firstname = Objects.toString(req.getAttribute(config.getFirstnameKey()), "");
            String email = Objects.toString(req.getAttribute(config.getFirstnameKey()), "");
            String profile = config.getProfileKey();


            if(username.trim().length() > 0) {

                MinimalUser user = new MinimalUser();
                user.setUsername(username);
                user.setName(firstname);
                user.setSurname(surname);
                user.setProfile(profile);
                return user;

            } else {
                return null;
            }
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * @return the inserted/updated user or null if no valid user found or any error happened
     */
    static GeonetworkUser setupUser(ServletRequest request, ResourceManager resourceManager, ProfileManager profileManager, SerialFactory serialFactory, ShibbolethUserConfiguration config)
        throws Exception
	{
    //        ServiceContext context = null;

		// Get the header keys to lookup from the settings
//		SettingManager sm = gc.getSettingManager();
//		String prefix = "system/shib";
//		String usernameKey = sm.getValue(prefix + "/attrib/username");
//		String surnameKey = sm.getValue(prefix + "/attrib/surname");
//		String firstnameKey = sm.getValue(prefix + "/attrib/firstname");
//		String profileKey = sm.getValue(prefix + "/attrib/profile");
//        String groupKey = sm.getValue(prefix + "/attrib/group");
//        String defGroup =  sm.getValue(prefix +"/defaultGroup");


		// Read in the data from the environment vars
        HttpServletRequest req = (HttpServletRequest)request;

        String username = Objects.toString(req.getAttribute(config.getUsernameKey()), "");
        String surname = Objects.toString(req.getAttribute(config.getSurnameKey()), "");
        String firstname = Objects.toString(req.getAttribute(config.getFirstnameKey()), "");
        String email = Objects.toString(req.getAttribute(config.getEmailKey()), "");
        String profile = config.getProfileKey();
        String group = config.getGroupKey();


        if(username != null && username.trim().length() > 0) { // ....add other cnstraints to be sure it's a real shibbolet login and not fake

            // Make sure the profile name is an exact match
            profile = profileManager.getCorrectCase(profile);
            if (profile.equals("")) {
                profile = ProfileManager.GUEST;
            }

            if (group.equals("")) {
                group = config.getDefaultGroup();
            }

            Dbms dbms = (Dbms)resourceManager.open(Geonet.Res.MAIN_DB);

            // only accept the first 256 chars
            if (username.length() > 256) { 
                username = username.substring(0, 256);
            }

            // Create or update the user
            updateUser(dbms, serialFactory, username, surname, firstname, profile, group, config.isUpdateProfile(), email);

            // try and load the user from db

            String query = "SELECT * FROM Users WHERE username = ? ";

            List list = dbms.select(query, username).getChildren();

            if (list.isEmpty()) {// should not happen!
                Log.warning(Geonet.LOG_AUTH, "Got no user with username '"+username+"' in Shibboleth auth");
                return null;
            }

            Element userEl = (Element) list.get(0);

            GeonetworkUser user = new GeonetworkUser(profileManager, username, userEl);
            return user;
        }

        return null;
	}


	//--------------------------------------------------------------------------

	/**
	 * Update the user to match the provided details, or create a new record
	 * for them if they don't have one already.
	 *
	 * @param context The Jeeves ServiceContext
	 * @param dbms The database connection.
	 * @param username The user's username, must not be null.
	 * @param surname The surname of the user
	 * @param firstname The first name of the user.
	 * @param profile The name of the user type.
	 * @throws SQLException If the record cannot be saved.
	 */
	private static void updateUser(Dbms dbms, SerialFactory serialFactory,
            String username, String surname, String firstname,
            String profile, String group,
            boolean updateProfile, String email) throws SQLException
	{

        boolean groupProvided = ((group != null) && (!(group.equals(""))));
        int groupId = -1;

        if (groupProvided) {
            String query = "SELECT id FROM Groups WHERE name=?";

            List list  = dbms.select(query, group).getChildren();

            if (list.isEmpty()) {
                groupId = serialFactory.getSerial(dbms, "Groups");

                query = "INSERT INTO GROUPS(id, name) VALUES(?,?)";
                dbms.execute(query, groupId, group);
                Lib.local.insert(dbms, "Groups", groupId, group);

            } else {
                String gi = ((Element) list.get(0)).getChildText("id");

                groupId = Integer.valueOf(gi);
            }
        }
		//--- update user information into the database

        int usersUpdated;

        if(updateProfile) {
            String query = "UPDATE Users SET name=?, surname=?, profile=?, password=?, authtype=? WHERE username=?";
            usersUpdated = dbms.execute(query, firstname, surname, profile, VIA_SHIBBOLETH, SHIBBOLETH_FLAG, username);
        } else {
            String query = "UPDATE Users SET name=?, surname=?, password=?, authtype=? WHERE username=?";
            usersUpdated = dbms.execute(query, firstname, surname, VIA_SHIBBOLETH, SHIBBOLETH_FLAG, username);
        }

		//--- if the user was not found --> add it

		if (usersUpdated == 0)
		{
			int userId = serialFactory.getSerial(dbms, "Users");

			String query = 	"INSERT INTO Users(id, username, name, surname, profile, password, authtype, email) "+
						"VALUES(?,?,?,?,?,?,?,?)";

			dbms.execute(query, userId, username, firstname, surname, profile, VIA_SHIBBOLETH, SHIBBOLETH_FLAG, email);

            if (groupProvided) {
                String query2 = "SELECT count(*) as numr FROM UserGroups WHERE groupId=? and userId=?";
                List list  = dbms.select(query2, groupId, userId).getChildren();

                String count = ((Element) list.get(0)).getChildText("numr");

                 if (count.equals("0")) {
                     query = "INSERT INTO UserGroups(userId, groupId, profile) "+
                             "VALUES(?,?,?)";
                     dbms.execute(query, userId, groupId, profile);

                 }
            }
		}

		dbms.commit();
	}

    private static String getHeader(HttpServletRequest req, String name, String defValue) {

		String value = req.getHeader(name);

		if (value == null)
			return defValue;

		if (value.length() == 0)
			return defValue;

		return value;
    }
}