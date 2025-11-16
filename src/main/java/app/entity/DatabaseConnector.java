package app.entity;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class DatabaseConnector {
    private static Connection conn;
    private static final String URL = System.getenv("DATABASE_URL");
    private static final String USER = System.getenv("DATABASE_USER");
    private static final String PASSWORD = System.getenv("DATABASE_PASSWORD");
    static{
        try{
            String dbUrl = URL;
            String dbUser = USER;
            String dbPassword = PASSWORD;

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream("C:\\SchoolProjects\\keys.txt")) {
                    props.load(fis);
                    dbUrl = props.getProperty("url");
                    dbUser = props.getProperty("user");
                    dbPassword = props.getProperty("password");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } 
            conn = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Account getAccountByUsername(String queryUsername) throws SQLException {
        PreparedStatement stmt = DatabaseConnector.conn.prepareStatement(
                "SELECT * \n" +
                        "FROM Accounts AS A\n" +
                        "LEFT JOIN AccountStylePreferences AS ASP ON ASP.account_id = A.account_id\n" +
                        "WHERE A.username = ?;");
        stmt.setString(1, queryUsername);
        ResultSet rs = stmt.executeQuery();
        return convertToAccount(rs);
    }
    private static Account convertToAccount(ResultSet rs) throws SQLException {
        if(!rs.next()){
            return null;
        }
        List<String> styles_list = new ArrayList<>();
        int id = rs.getInt("account_id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String profile_picture_url = rs.getString("profile_picture_url");
        double home_latitude = rs.getDouble("home_latitude");
        double home_longitude = rs.getDouble("home_longitude");
        String style = rs.getString("style_name");
        do {
            styles_list.add(style);
        } while (rs.next());
        return new Account(id,username,password,profile_picture_url,home_latitude,home_longitude,styles_list);
    }

    public static Profile getProfileByUsername2(String queryUsername) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement(
                "SELECT * \n" +
                        "FROM Artists AS A\n" +
                        "LEFT JOIN Posts2 AS P ON P.account_id = A.account_id\n" +
                        "LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.artist_id = A.artist_id\n" +
                        "LEFT JOIN Accounts AS Acc ON Acc.account_id = A.account_id\n" +
                        "WHERE username = ? ;");
        profileQueryStatement.setString(1, queryUsername);
        ResultSet rs = profileQueryStatement.executeQuery();
        List<Profile> artistProfilesList = convertToArtistProfiles(rs);
        return artistProfilesList.isEmpty() ? null : artistProfilesList.getFirst();
    }
    private static List<Profile> convertToArtistProfiles(ResultSet rs) throws SQLException {
        if(!rs.next()){
            return Collections.emptyList();
        }
        List<Profile> artistProfiles = new ArrayList<>();
        while(!rs.isAfterLast()) {
            int account_id = rs.getInt("account_id");
            String username = rs.getString("username");
            String profile_picture_url = rs.getString("profile_picture_url");
            String biography = rs.getString("biography");
            String work_address = rs.getString("work_address");
            double work_longitude = rs.getDouble("work_longitude");
            double work_latitude = rs.getDouble("work_latitude");

            List<String> styles = new ArrayList<>();
            String style = rs.getString("style_name");
            if(style != null) {
                styles.add(style);
            }
            while(rs.next()) {
                if(!(rs.getInt("account_id") == account_id)) {
                    break;
                }
                styles.add(style);
            }
            artistProfiles.add(new Profile(account_id, username, profile_picture_url,biography, work_address, work_longitude, work_latitude, styles));
        }
        return artistProfiles;

    }

    public static List<Profile> getProfilesLike(String pattern) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT * \n" +
                    "FROM Artists AS A\n" +
                    "LEFT JOIN Posts2 AS P ON P.account_id = A.account_id\n" +
                    "LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.artist_id = A.artist_id\n" +
                    "LEFT JOIN Accounts AS ACC ON ACC.account_id = A.account_id\n" +
                    "WHERE username LIKE ?"
        );
        stmt.setString(1, "%" + pattern + "%");
        ResultSet rs = stmt.executeQuery();
        return convertToArtistProfiles(rs);
    }

    public static List<Profile> getProfilesWithinBounds(double latitudeFrom, double latitudeTo, double longitudeFrom, double longitudeTo) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement(
                "SELECT * \n" +
                        "FROM Artists AS A\n" +
                        "LEFT JOIN Posts2 AS P ON P.account_id = A.account_id\n" +
                        "LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.artist_id = A.artist_id\n" +
                        "LEFT JOIN Accounts AS ACC ON ACC.account_id = A.account_id\n" +
                        "WHERE A.work_latitude >= ? AND A.work_latitude <= ? AND A.work_longitude >= ? AND A.work_longitude <= ?;");
        profileQueryStatement.setDouble(1, latitudeFrom);
        profileQueryStatement.setDouble(2, latitudeTo);
        profileQueryStatement.setDouble(3, longitudeFrom);
        profileQueryStatement.setDouble(4, longitudeTo);
        ResultSet rs = profileQueryStatement.executeQuery();
        List<Profile> profileList = convertToArtistProfiles(rs);
        if(profileList.isEmpty()){
            return Collections.emptyList();
        }
        else {
            return profileList;
        }
    }

    public static void createUser(String username, String password, boolean isArtist) throws SQLException {
        PreparedStatement insertAccountsStmt = DatabaseConnector.conn.prepareStatement(
                "INSERT INTO Accounts(username, password) " +
                        "VALUES (?, ?);",
                Statement.RETURN_GENERATED_KEYS
        );
        insertAccountsStmt.setString(1, username);
        insertAccountsStmt.setString(2, password);
        insertAccountsStmt.executeUpdate();


        ResultSet rs = insertAccountsStmt.getGeneratedKeys();
        if(rs.next()) {
            int user_id = rs.getInt(1);
            if(isArtist) {
                PreparedStatement insertArtistsStmt = DatabaseConnector.conn.prepareStatement(
                        "INSERT INTO Artists(account_id)" +
                                "VALUES (?);"
                );
                insertArtistsStmt.setInt(1, user_id);
                insertArtistsStmt.executeUpdate();
            }
        }
    }






}
