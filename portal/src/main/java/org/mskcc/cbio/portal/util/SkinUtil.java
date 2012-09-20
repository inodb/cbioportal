package org.mskcc.cbio.portal.util;

/**
 * Utility Class Containing Skin Configuration Options.
 *
 * @author Ethan Cerami.
 */
public class SkinUtil {
    public static final String DEFAULT_TITLE = "cBio Cancer Genomics Portal";
    public static final String DEFAULT_EMAIL_CONTACT = "cbioportal at googlegroups dot com";
    public static final String DEFAULT_AUTHORIZATION_MESSAGE = "Access to this portal is only " +
            "available to authorized users.";
    private static final String PROPERTY_SKIN_EMAIL_CONTACT = "skin.email_contact";
    private static final String PROPERTY_SKIN_SHOW_NEWS_TAB = "skin.show_news_tab";
    private static final String PROPERTY_SKIN_SHOW_DATA_TAB = "skin.show_data_tab";
    private static final String PROPERTY_SKIN_RIGHT_NAV_SHOW_DATA_SETS =
            "skin.right_nav.show_data_sets";
    private static final String PROPERTY_SKIN_RIGHT_NAV_SHOW_EXAMPLES =
            "skin.right_nav.show_examples";
    private static final String PROPERTY_SKIN_RIGHT_NAV_SHOW_TESTIMONIALS =
            "skin.right_nav.show_testimonials";
    private static final String PROPERTY_SKIN_AUTHORIZATION_MESSAGE = "skin.authorization_message";
    private static final String PROPERTY_AUTHENTICATION_REQUIRED = "authenticate";
    private static final String PROPERTY_BITLY_USER = "bitly.user";
    private static final String PROPERTY_BITLY_API_KEY = "bitly.api_key";

    /**
     * Gets the Site Title.
     * @return site title.
     */
    public static String getTitle() {
        Config config = Config.getInstance();
        String skinTitle = config.getProperty("skin.title");
        if (skinTitle == null) {
            return DEFAULT_TITLE;
        } else {
            return skinTitle;
        }
    }

    /**
     * Gets the Site Blurb.
     * @return site blurb.
     */
    public static String getBlurb() {
        Config config = Config.getInstance();
        return config.getProperty("skin.blurb");
    }

    /**
     * Gets the Site Tag Line.
     * @return site tag line.
     */
    public static String getTagLineImage() {
        Config config = Config.getInstance();
        String tagLineImage = config.getProperty("skin.tag_line_image");
        if (tagLineImage == null) {
            tagLineImage = "images/tag_line.png";
        } else {
            tagLineImage = "images/" + tagLineImage;
        }
        return tagLineImage;
    }

    /**
     * Gets the Site Email Contact.
     * Emails should be in the form of:  xxx AT yyy DOT com.
     * @return site email contact.
     */
    public static String getEmailContact() {
        Config config = Config.getInstance();
        String emailAddress = config.getProperty(PROPERTY_SKIN_EMAIL_CONTACT);

        if (emailAddress == null) {
            emailAddress = DEFAULT_EMAIL_CONTACT;
        }

        //  Return email address within mailme span, so that we can de-obfuscate with JQuery.
        return ("<span class=\"mailme\" title=\"Contact us\">" + emailAddress + "</span>");
    }

    /**
     * Gets the bitly user name.
     * @return bitly user name.
     */
    public static String getBitlyUser() {
        Config config = Config.getInstance();
        return config.getProperty(PROPERTY_BITLY_USER);
    }

    /**
     * Gets the bitly api key.
     * @return bitly api key.
     */
    public static String getBitlyApiKey() {
        Config config = Config.getInstance();
        return config.getProperty(PROPERTY_BITLY_API_KEY);
    }

    /**
     * Determines if users must authenticate or not.
     * @return true or false.
     */
    public static boolean usersMustAuthenticate() {
        Config config = Config.getInstance();
        String authFlag = config.getProperty(PROPERTY_AUTHENTICATION_REQUIRED);
        return authFlag != null && new Boolean(authFlag);
    }
    
    /**
     * Determines whether to include networks
     * @return true or false
     */
    public static boolean includeNetworks() {
        Config config = Config.getInstance();
        return Boolean.parseBoolean(config.getProperty("include_networks"));
    }

    /**
     * Determines whether we should show the news tab.
     * @return true or false
     */
    public static boolean showNewsTab() {
        Config config = Config.getInstance();
        String showFlag = config.getProperty(PROPERTY_SKIN_SHOW_NEWS_TAB);
        return showFlag == null || Boolean.parseBoolean(showFlag);
    }

    /**
     * Determines whether we should the data tab.
     * @return true or false
     */
    public static boolean showDataTab() {
        Config config = Config.getInstance();
        String showFlag = config.getProperty(PROPERTY_SKIN_SHOW_DATA_TAB);
        return showFlag == null || Boolean.parseBoolean(showFlag);
    }

    /**
     * Determines whether we should show data sets in the right nav bar.
     * @return true or false
     */
    public static boolean showRightNavDataSets() {
        Config config = Config.getInstance();
        String showFlag = config.getProperty(PROPERTY_SKIN_RIGHT_NAV_SHOW_DATA_SETS);
        return showFlag == null || Boolean.parseBoolean(showFlag);
    }

    /**
     * Determines whether we should show examples in the right nav bar.
     * @return true or false
     */
    public static boolean showRightNavExamples() {
        Config config = Config.getInstance();
        String showFlag = config.getProperty(PROPERTY_SKIN_RIGHT_NAV_SHOW_EXAMPLES);
        return showFlag == null || Boolean.parseBoolean(showFlag);
    }

    /**
     * Determines whether we should show examples in the right nav bar.
     * @return true or false
     */
    public static boolean showRightNavTestimonials() {
        Config config = Config.getInstance();
        String showFlag = config.getProperty(PROPERTY_SKIN_RIGHT_NAV_SHOW_TESTIMONIALS);
        return showFlag == null || Boolean.parseBoolean(showFlag);
    }

    /**
     * Gets the Authorization Message to Display to the User.
     * @return authorization message.
     */
    public static String getAuthorizationMessage() {
        Config config = Config.getInstance();
        String authMessage = config.getProperty(PROPERTY_SKIN_AUTHORIZATION_MESSAGE);
        return authMessage == null ? DEFAULT_AUTHORIZATION_MESSAGE : authMessage;
    }

    /**
     * Gets the Data Sets Header.
     * @return String
     */
    public static String getDataSetsHeader() {
        Config config = Config.getInstance();
        return config.getProperty("skin.data_sets_header");
    }

    /**
     * Gets the Data Sets Footer.
     * @return String
     */
    public static String getDataSetsFooter() {
        Config config = Config.getInstance();
        return config.getProperty("skin.data_sets_footer");
    }
}